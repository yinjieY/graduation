<template>
  <div class="scan-shell">
    <section class="card hero-card">
      <div>
        <div class="hero-brand">Youxian Tofu Trace Platform</div>
        <div class="hero-title">官方溯源校验结果</div>
      </div>
      <div class="hero-stamp">{{ heroStamp }}</div>
    </section>

    <section class="card">
      <h3 class="title">风险等级评估</h3>
      <div class="risk-bar">
        <div class="risk-part" :class="{ active: riskLevel === 'low' }">低风险</div>
        <div class="risk-part" :class="{ active: riskLevel === 'mid' }">中风险</div>
        <div class="risk-part" :class="{ active: riskLevel === 'high' }">高风险</div>
      </div>
      <div class="conclusion" :class="riskLevel">{{ conclusion }}</div>
    </section>

    <div class="grid-two">
      <section class="card">
        <h3 class="title">校验结果</h3>
        <div class="kv"><span>官方性</span><span>{{ officialStatus }}</span></div>
        <div class="kv"><span>扫码状态</span><span>{{ scanStatus }}</span></div>
        <div class="kv"><span>上报状态</span><span>{{ reportStatus }}</span></div>
      </section>
      <section class="card">
        <h3 class="title">定位信息</h3>
        <div class="kv"><span>定位状态</span><span>{{ locationStatus }}</span></div>
        <div class="kv"><span>定位说明</span><span>{{ locationReason }}</span></div>
        <div class="kv"><span>本次坐标</span><span>{{ coordinateText }}</span></div>
      </section>
    </div>

    <div class="grid-two">
      <section class="card">
        <h3 class="title">产品批次信息</h3>
        <div class="kv"><span>批次号</span><span>{{ traceInfo.batchId }}</span></div>
        <div class="kv"><span>生产日期</span><span>{{ traceInfo.productionDate }}</span></div>
        <div class="kv"><span>配料</span><span>{{ traceInfo.ingredients }}</span></div>
        <div class="kv"><span>执行标准</span><span>{{ traceInfo.standard }}</span></div>
      </section>
      <section class="card">
        <h3 class="title">企业与二维码信息</h3>
        <div class="kv"><span>企业名称</span><span>{{ traceInfo.companyName }}</span></div>
        <div class="kv"><span>企业地址</span><span>{{ traceInfo.companyAddress }}</span></div>
        <div class="kv"><span>二维码ID</span><span>{{ traceInfo.qsId }}</span></div>
        <div class="kv"><span>二维码状态</span><span>{{ traceInfo.qsStatus }}</span></div>
        <div class="kv"><span>签发时间</span><span>{{ traceInfo.issueTime }}</span></div>
      </section>
    </div>

    <section class="card">
      <h3 class="title">质量反馈</h3>
      <div class="row">
        <select v-model="feedbackForm.feedbackType">
          <option value="CROSS_REGION">串货</option>
          <option value="COUNTERFEIT">假冒</option>
          <option value="OTHER">其他</option>
        </select>
        <input v-model="feedbackForm.region" placeholder="所在地区（必填）" />
        <input v-model="feedbackForm.description" placeholder="详细描述（可选，200字内）" />
        <input type="file" accept="image/*" @change="onFileChange" />
      </div>
      <div class="ops" style="margin-top: 10px;">
        <button :disabled="loading" @click="submitFeedbackForm">提交反馈</button>
        <input v-model="feedbackQueryId" placeholder="输入反馈编号查询进度" />
        <button class="secondary" :disabled="loading" @click="queryStatus">查询反馈状态</button>
      </div>
      <div class="muted" style="margin-top: 8px;">反馈编号：{{ feedbackId || '-' }}</div>
    </section>

    <details class="card">
      <summary>技术详情（调试用）</summary>
      <pre>{{ JSON.stringify(debugData, null, 2) }}</pre>
    </details>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { queryFeedbackStatus, submitFeedback } from '../api/feedback';
import { queryTrace, reportScan } from '../api/scan';

const params = new URLSearchParams(window.location.search);

const fpKey = 'trace_device_fp';
let fp = localStorage.getItem(fpKey);
if (!fp) {
  fp = `web-${Math.random().toString(36).slice(2)}${Date.now()}`;
  localStorage.setItem(fpKey, fp);
}

const scanForm = reactive({
  qsId: params.get('qsId') || '',
  batchId: params.get('batchId') || '',
  companyId: params.get('companyId') || '',
  signaturePayload: params.get('payload') || '',
  signature: params.get('signature') || '',
  deviceFingerprint: fp,
  browser: /MicroMessenger/i.test(navigator.userAgent || '') ? 'WeChat' : 'Browser',
  ip: '',
  locationSource: 'none'
});

const feedbackForm = reactive({
  feedbackType: 'CROSS_REGION',
  region: '',
  description: '',
  image: null
});

const loading = ref(false);
const heroStamp = ref('校验中');
const officialStatus = ref('判定中...');
const scanStatus = ref('处理中...');
const reportStatus = ref('等待上报...');
const locationStatus = ref('等待定位...');
const locationReason = ref('未开始');
const coordinateText = ref('未获取');
const riskLevel = ref('mid');
const conclusion = ref('系统正在生成结论...');
const debugData = ref({});
const feedbackId = ref('');
const feedbackQueryId = ref('');

const traceInfo = reactive({
  batchId: '-',
  productionDate: '-',
  ingredients: '-',
  standard: '-',
  companyName: '-',
  companyAddress: '-',
  qsId: '-',
  qsStatus: '-',
  issueTime: '-'
});

function setRisk(level, text) {
  riskLevel.value = level;
  conclusion.value = text;
}

function mapStatus(status) {
  const value = (status || '').toLowerCase();
  if (value === 'active') return '正常流通';
  if (value === 'frozen') return '已冻结';
  if (value === 'invalid') return '已失效';
  if (value === 'cancelled') return '已注销';
  return status || '-';
}

function onFileChange(event) {
  const files = event?.target?.files;
  feedbackForm.image = files && files[0] ? files[0] : null;
}

async function withLoading(task) {
  loading.value = true;
  try {
    await task();
  } catch (error) {
    debugData.value = { error: error?.message || '请求失败' };
  } finally {
    loading.value = false;
  }
}

async function loadTrace() {
  if (!scanForm.qsId) {
    heroStamp.value = '异常';
    officialStatus.value = '参数不完整';
    setRisk('high', '结论：缺少 qsId，无法进行官方校验。');
    return;
  }

  scanStatus.value = '正在查询溯源...';
  const res = await queryTrace(scanForm.qsId);
  debugData.value.trace = res;
  if (res?.code !== 200 || !res?.data) {
    scanStatus.value = `溯源查询失败：${res?.msg || 'unknown'}`;
    officialStatus.value = '非官方或已失效二维码';
    heroStamp.value = '异常';
    setRisk('high', '结论：未查到有效溯源记录，存在非官方风险。');
    return;
  }

  const qs = res.data.qsCode || {};
  const batch = res.data.batch || {};
  const company = res.data.company || {};

  traceInfo.batchId = batch.batchId || scanForm.batchId || '-';
  traceInfo.productionDate = batch.productionDate || '-';
  traceInfo.ingredients = batch.ingredients || '-';
  traceInfo.standard = batch.productionStandard || '-';
  traceInfo.companyName = company.name || scanForm.companyId || '-';
  traceInfo.companyAddress = company.address || '-';
  traceInfo.qsId = qs.qsId || scanForm.qsId || '-';
  traceInfo.qsStatus = mapStatus(qs.status);
  traceInfo.issueTime = qs.issueTime || '-';

  scanStatus.value = '溯源查询成功';
  if ((qs.status || '').toLowerCase() === 'active') {
    officialStatus.value = '官方二维码（待验签确认）';
    heroStamp.value = '待确认';
    setRisk('mid', '结论：系统找到官方记录，正在上报扫码行为进行校验。');
  } else {
    officialStatus.value = `非官方流通码（${traceInfo.qsStatus}）`;
    heroStamp.value = '异常';
    setRisk('high', '结论：二维码不在正常流通状态，请联系商家或监管核验。');
  }
}

function updateGeo(lat, lng, reason) {
  if (lat == null || lng == null) {
    locationStatus.value = '未拿到GPS';
    coordinateText.value = '未获取';
    scanForm.locationSource = 'none';
  } else {
    locationStatus.value = '已拿到GPS';
    coordinateText.value = `${lat.toFixed(6)}, ${lng.toFixed(6)}`;
    scanForm.locationSource = 'gps';
    scanForm.latitude = lat;
    scanForm.longitude = lng;
  }
  locationReason.value = reason;
}

async function doReport() {
  const res = await reportScan(scanForm);
  debugData.value.report = res;
  if (res?.code === 200) {
    reportStatus.value = '扫码行为上报成功';
    if (officialStatus.value.includes('官方二维码')) {
      officialStatus.value = '官方二维码（验签通过，状态正常）';
      heroStamp.value = '官方';
      setRisk('low', '结论：该二维码为官方有效码，可放心查看溯源信息。');
    }
    return;
  }

  reportStatus.value = `上报失败: ${res?.msg || 'unknown'}`;
  heroStamp.value = '高风险';
  if (officialStatus.value.includes('官方二维码')) {
    officialStatus.value = '疑似伪造二维码（验签失败）';
  } else {
    officialStatus.value = '非官方二维码';
  }
  setRisk('high', '结论：二维码校验失败，请勿继续购买或流通。');
}

async function autoLocateAndReport() {
  if (!navigator.geolocation || !window.isSecureContext) {
    updateGeo(null, null, '当前环境无法定位（需 HTTPS 或 localhost）');
    await doReport();
    return;
  }

  await new Promise((resolve) => {
    navigator.geolocation.getCurrentPosition(
      async (position) => {
        updateGeo(position.coords.latitude, position.coords.longitude, '定位成功');
        await doReport();
        resolve();
      },
      async () => {
        updateGeo(null, null, '定位失败，已按无GPS上报');
        await doReport();
        resolve();
      },
      { enableHighAccuracy: false, timeout: 2200, maximumAge: 120000 }
    );
  });
}

async function submitFeedbackForm() {
  if (!feedbackForm.image) {
    debugData.value.feedback = { msg: '请先上传图片' };
    return;
  }
  const formData = new FormData();
  formData.set('qsId', scanForm.qsId);
  formData.set('feedbackType', feedbackForm.feedbackType);
  formData.set('deviceFingerprint', scanForm.deviceFingerprint);
  formData.set('region', feedbackForm.region || '未知地区');
  formData.set('description', feedbackForm.description || '');
  if (scanForm.latitude) formData.set('latitude', String(scanForm.latitude));
  if (scanForm.longitude) formData.set('longitude', String(scanForm.longitude));
  formData.set('image', feedbackForm.image);

  await withLoading(async () => {
    const res = await submitFeedback(formData);
    debugData.value.feedback = res;
    feedbackId.value = res?.data?.feedbackId || '';
    feedbackQueryId.value = feedbackId.value;
  });
}

async function queryStatus() {
  if (!feedbackQueryId.value) return;
  await withLoading(async () => {
    const res = await queryFeedbackStatus(feedbackQueryId.value);
    debugData.value.feedbackStatus = res;
  });
}

onMounted(async () => {
  await withLoading(async () => {
    await loadTrace();
    await autoLocateAndReport();
  });
});
</script>

