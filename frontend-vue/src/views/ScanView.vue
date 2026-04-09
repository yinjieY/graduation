<template>
  <div class="scan-page">
    <div class="scan-bg"></div>

    <div class="header">
      <div class="header-ornament"></div>
      <div class="brand-line">Youxian Tofu Trace Platform</div>
      <div class="brand-title">攸县香干<br>区块链溯源系统</div>
      <div class="brand-sub">官方溯源 · 区块链存证 · 国密签名防伪</div>
    </div>

    <div class="stamp-wrap">
      <div class="stamp" :class="stampConfig.class">
        <div class="stamp-inner">
          <div class="stamp-icon" :class="{'spin': stampConfig.class === 'checking'}">{{ stampConfig.icon }}</div>
          <div class="stamp-label">{{ stampConfig.text }}</div>
        </div>
      </div>
    </div>

    <div class="cards">
      <div class="card">
        <div class="card-header">
          <div class="card-icon">🛡</div>
          <div class="card-title">风险等级评估</div>
        </div>
        <div class="risk-bar">
          <div class="risk-seg" :class="{ 'active low': riskLevel === 'low' }">低风险</div>
          <div class="risk-seg" :class="{ 'active mid': riskLevel === 'mid' }">中风险</div>
          <div class="risk-seg" :class="{ 'active high': riskLevel === 'high' }">高风险</div>
        </div>
        <div class="conclusion" :class="riskLevel">{{ conclusion }}</div>
      </div>

      <div class="card">
        <div class="card-header">
          <div class="card-icon">✅</div>
          <div class="card-title">校验结果</div>
        </div>
        <div class="kv">
          <div class="kv-row">
            <div class="kv-key">官方性</div>
            <div class="kv-val">
              <span class="badge" :class="getBadgeClass(officialStatus)">{{ officialStatus }}</span>
            </div>
          </div>
          <div class="divider"></div>
          <div class="kv-row">
            <div class="kv-key">扫码状态</div>
            <div class="kv-val">{{ scanStatus }}</div>
          </div>
          <div class="divider"></div>
          <div class="kv-row">
            <div class="kv-key">上报状态</div>
            <div class="kv-val">{{ reportStatus }}</div>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <div class="card-icon">📦</div>
          <div class="card-title">产品批次信息</div>
        </div>
        <div class="kv">
          <div class="kv-row"><div class="kv-key">批次编号</div><div class="kv-val">{{ traceInfo.batchId }}</div></div>
          <div class="divider"></div>
          <div class="kv-row"><div class="kv-key">生产日期</div><div class="kv-val">{{ traceInfo.productionDate }}</div></div>
          <div class="divider"></div>
          <div class="kv-row"><div class="kv-key">配料信息</div><div class="kv-val">{{ traceInfo.ingredients }}</div></div>
          <div class="divider"></div>
          <div class="kv-row"><div class="kv-key">执行标准</div><div class="kv-val">{{ traceInfo.standard }}</div></div>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <div class="card-icon">🏭</div>
          <div class="card-title">企业与二维码信息</div>
        </div>
        <div class="kv">
          <div class="kv-row"><div class="kv-key">企业名称</div><div class="kv-val">{{ traceInfo.companyName }}</div></div>
          <div class="divider"></div>
          <div class="kv-row"><div class="kv-key">企业地址</div><div class="kv-val">{{ traceInfo.companyAddress }}</div></div>
          <div class="divider"></div>
          <div class="kv-row"><div class="kv-key">二维码ID</div><div class="kv-val">{{ traceInfo.qsId }}</div></div>
          <div class="divider"></div>
          <div class="kv-row"><div class="kv-key">二维码状态</div><div class="kv-val">{{ traceInfo.qsStatus }}</div></div>
          <div class="divider"></div>
          <div class="kv-row"><div class="kv-key">签发时间</div><div class="kv-val">{{ traceInfo.issueTime }}</div></div>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <div class="card-icon">📍</div>
          <div class="card-title">定位信息</div>
        </div>
        <div class="kv">
          <div class="kv-row"><div class="kv-key">定位状态</div><div class="kv-val">{{ locationStatus }}</div></div>
          <div class="divider"></div>
          <div class="kv-row"><div class="kv-key">定位说明</div><div class="kv-val">{{ locationReason }}</div></div>
          <div class="divider"></div>
          <div class="kv-row"><div class="kv-key">本次坐标</div><div class="kv-val">{{ coordinateText }}</div></div>
        </div>
        <div class="loc-note">
          📌 位置信息仅用于判断是否存在异地异常扫码行为，辅助风险评估与预警，不会影响个人信用或权益。
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <div class="card-icon">💬</div>
          <div class="card-title">质量反馈与维权</div>
        </div>
        <div class="feedback-form">
          <select v-model="feedbackForm.feedbackType" class="nice-input">
            <option value="CROSS_REGION">怀疑串货 / 异地扫码</option>
            <option value="COUNTERFEIT">怀疑假冒伪劣</option>
            <option value="OTHER">其他质量问题</option>
          </select>
          <input v-model="feedbackForm.region" placeholder="您的所在地区（必填）" class="nice-input" />
          <textarea v-model="feedbackForm.description" placeholder="请详细描述您遇到的问题（可选，200字内）" class="nice-input"></textarea>
          <div class="file-upload">
            <span class="file-label">上传凭证照片：</span>
            <input type="file" accept="image/*" @change="onFileChange" />
          </div>

          <div class="btn-group">
            <button class="btn-primary" :disabled="loading" @click="submitFeedbackForm">
              {{ loading ? '提交中...' : '提交反馈' }}
            </button>
          </div>

          <div class="divider" style="margin: 16px 0;"></div>

          <div class="query-box">
            <input v-model="feedbackQueryId" placeholder="输入反馈编号查询进度" class="nice-input small" />
            <button class="btn-secondary" :disabled="loading" @click="queryStatus">查询</button>
          </div>
          <div v-if="feedbackId" class="feedback-id-hint">您的反馈编号：<strong>{{ feedbackId }}</strong> (请妥善保存)</div>
        </div>
      </div>

      <details class="debug-details">
        <summary>技术详情及原始报文（调试用）</summary>
        <pre>{{ JSON.stringify(debugData, null, 2) }}</pre>
      </details>

    </div>

    <div class="footer">
      <div class="footer-logo">攸县香干区块链溯源平台</div>
      <div>Youxian Tofu Blockchain Trace · 国密SM2签名 · FISCO BCOS存证</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from 'vue';
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

// 计算印章的样式和文案
const stampConfig = computed(() => {
  if (heroStamp.value === '官方') return { class: 'official', icon: '✓', text: '官方认证' };
  if (heroStamp.value === '异常' || heroStamp.value === '高风险') return { class: 'risk', icon: '✕', text: '高风险' };
  if (heroStamp.value === '待确认') return { class: 'warning', icon: '⚠', text: '待确认' };
  return { class: 'checking', icon: '⟳', text: '校验中' };
});

// 计算徽章的颜色类名
function getBadgeClass(statusStr) {
  if (statusStr.includes('正常') || statusStr.includes('通过')) return 'badge-ok';
  if (statusStr.includes('失败') || statusStr.includes('非官方') || statusStr.includes('伪造')) return 'badge-err';
  if (statusStr.includes('判定中') || statusStr.includes('确认')) return 'badge-warn';
  return 'badge-muted';
}

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
    debugData.value = { ...debugData.value, error: error?.message || '请求失败' };
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
    alert('请先上传凭证图片');
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
    alert(res?.code === 200 ? '反馈提交成功！' : `反馈失败：${res?.msg}`);
  });
}

async function queryStatus() {
  if (!feedbackQueryId.value) return;
  await withLoading(async () => {
    const res = await queryFeedbackStatus(feedbackQueryId.value);
    debugData.value.feedbackStatus = res;
    alert(`当前反馈状态: ${res?.data?.status || '未查询到'}`);
  });
}

onMounted(async () => {
  await withLoading(async () => {
    await loadTrace();
    await autoLocateAndReport();
  });
});
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=ZCOOL+XiaoWei&family=Noto+Sans+SC:wght@300;400;500;700&display=swap');

/* 将 index.html 的 CSS 移植并加上作用域限制 */
.scan-page {
  --jade:       #0e6b52;
  --jade-light: #1a8f6e;
  --jade-dark:  #084035;
  --gold:       #c9983a;
  --gold-light: #e8b84b;
  --cream:      #faf8f4;
  --ink:        #1a1a1a;
  --muted:      #6e7a6e;
  --ok-bg:      #ebf7f2;
  --ok-text:    #0a6640;
  --err-bg:     #fdf0f0;
  --err-text:   #961c1c;
  --warn-bg:    #fdf7e8;
  --warn-text:  #8a5a00;
  --border:     rgba(14,107,82,0.15);
  --shadow:     0 4px 24px rgba(14,107,82,0.1);
  --shadow-lg:  0 12px 40px rgba(14,107,82,0.18);

  font-family: 'Noto Sans SC', sans-serif;
  color: var(--ink);
  position: relative;
  max-width: 480px;
  margin: 0 auto;
  padding: 0 0 48px;
  background: var(--cream);
  min-height: 100vh;
  box-shadow: 0 0 30px rgba(0,0,0,0.05); /* 让他在PC端像个手机框 */
}

/* 覆盖全局背景影响，打造独立沉浸感 */
.scan-bg {
  position: absolute;
  inset: 0;
  background:
      radial-gradient(ellipse 80% 60% at 50% -10%, rgba(14,107,82,0.07) 0%, transparent 70%),
      url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%230e6b52' fill-opacity='0.03'%3E%3Ccircle cx='30' cy='30' r='1.5'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
  pointer-events: none;
  z-index: 0;
}

.header {
  background: linear-gradient(160deg, var(--jade-dark) 0%, var(--jade) 60%, var(--jade-light) 100%);
  padding: 32px 20px 64px;
  position: relative;
  overflow: hidden;
  z-index: 1;
}
.header::after {
  content: '';
  position: absolute;
  bottom: -2px; left: 0; right: 0;
  height: 40px;
  background: var(--cream);
  clip-path: ellipse(55% 100% at 50% 100%);
}
.header-ornament {
  position: absolute;
  top: -30px; right: -30px;
  width: 160px; height: 160px;
  border: 2px solid rgba(201,152,58,0.2);
  border-radius: 50%;
}
.header-ornament::before {
  content: '';
  position: absolute;
  inset: 16px;
  border: 1px solid rgba(201,152,58,0.15);
  border-radius: 50%;
}
.brand-line {
  font-size: 11px;
  letter-spacing: 0.2em;
  color: rgba(255,255,255,0.6);
  text-transform: uppercase;
  margin-bottom: 10px;
}
.brand-title {
  font-family: 'ZCOOL XiaoWei', serif;
  font-size: 26px;
  color: #fff;
  line-height: 1.3;
  margin-bottom: 6px;
}
.brand-sub {
  font-size: 12px;
  color: rgba(255,255,255,0.55);
  letter-spacing: 0.05em;
}

/* 印章 */
.stamp-wrap {
  display: flex;
  justify-content: center;
  margin-top: -44px;
  margin-bottom: 20px;
  position: relative;
  z-index: 2;
}
.stamp {
  width: 88px; height: 88px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  flex-direction: column;
  gap: 2px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.05em;
  background: var(--cream);
  box-shadow: var(--shadow-lg), 0 0 0 4px var(--cream);
  transition: all 0.5s ease;
  position: relative;
  overflow: hidden;
}
.stamp-inner {
  width: 76px; height: 76px;
  border-radius: 50%;
  border: 2px solid currentColor;
  display: flex; align-items: center; justify-content: center;
  flex-direction: column;
  gap: 2px;
  transition: all 0.5s ease;
}
.stamp.checking { color: var(--muted); }
.stamp.checking .stamp-inner { border-color: var(--muted); }
.stamp.official { color: var(--jade); animation: pulse-ok 2.5s ease-in-out infinite; }
.stamp.official .stamp-inner { border-color: var(--jade); background: var(--ok-bg); }
.stamp.risk { color: var(--err-text); }
.stamp.risk .stamp-inner { border-color: var(--err-text); background: var(--err-bg); }
.stamp.warning { color: var(--warn-text); }
.stamp.warning .stamp-inner { border-color: var(--gold); background: var(--warn-bg); }
.stamp-icon { font-size: 28px; line-height: 1; }
.stamp-label { font-size: 9px; font-weight: 700; letter-spacing: 0.1em; }

@keyframes pulse-ok {
  0%, 100% { box-shadow: var(--shadow-lg), 0 0 0 4px var(--cream); }
  50% { box-shadow: var(--shadow-lg), 0 0 0 4px var(--cream), 0 0 0 10px rgba(14,107,82,0.12); }
}
@keyframes spin-check {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.spin { animation: spin-check 1.2s linear infinite; font-size: 22px; display: inline-block; }

/* 卡片 */
.cards {
  padding: 0 16px;
  position: relative;
  z-index: 2;
}
.card {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 12px;
  box-shadow: var(--shadow);
  animation: fadeUp 0.5s ease both;
}
@keyframes fadeUp {
  from { opacity: 0; transform: translateY(14px); }
  to   { opacity: 1; transform: translateY(0); }
}
.card:nth-child(1) { animation-delay: 0.05s; }
.card:nth-child(2) { animation-delay: 0.12s; }
.card:nth-child(3) { animation-delay: 0.18s; }
.card:nth-child(4) { animation-delay: 0.24s; }
.card:nth-child(5) { animation-delay: 0.30s; }
.card:nth-child(6) { animation-delay: 0.36s; }

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(14,107,82,0.1);
}
.card-icon {
  width: 32px; height: 32px;
  border-radius: 8px;
  background: var(--ok-bg);
  display: flex; align-items: center; justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}
.card-title {
  font-family: 'ZCOOL XiaoWei', serif;
  font-size: 16px;
  color: var(--jade-dark);
}

/* 风险条 */
.conclusion {
  border-radius: 12px;
  padding: 14px 16px;
  font-size: 13px;
  line-height: 1.6;
  border-left: 3px solid var(--muted);
  background: #f7f7f7;
  color: var(--muted);
  transition: all 0.4s ease;
}
.conclusion.low { background: var(--ok-bg); border-left-color: var(--jade); color: var(--ok-text); }
.conclusion.mid { background: var(--warn-bg); border-left-color: var(--gold); color: var(--warn-text); }
.conclusion.high { background: var(--err-bg); border-left-color: #d62626; color: var(--err-text); }

.risk-bar {
  display: flex;
  gap: 6px;
  margin-bottom: 14px;
}
.risk-seg {
  flex: 1;
  border-radius: 8px;
  padding: 8px 4px;
  text-align: center;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  background: #f2f3f2;
  color: #aaa;
  transition: all 0.4s ease;
}
.risk-seg.active.low  { background: var(--ok-bg); color: var(--ok-text); }
.risk-seg.active.mid  { background: var(--warn-bg); color: var(--warn-text); }
.risk-seg.active.high { background: var(--err-bg); color: var(--err-text); }
.risk-seg.active::after {
  content: '▼';
  display: block;
  font-size: 8px;
  margin-top: 2px;
  opacity: 0.6;
}

/* KV 布局 */
.kv { display: flex; flex-direction: column; gap: 10px; }
.kv-row {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: 8px;
  align-items: start;
  font-size: 13.5px;
}
.kv-key {
  color: var(--muted);
  font-size: 12px;
  padding-top: 1px;
  font-weight: 500;
}
.kv-val { color: var(--ink); word-break: break-word; line-height: 1.5; font-weight: 500;}

/* Badge */
.badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}
.badge-ok   { background: var(--ok-bg);   color: var(--ok-text); }
.badge-err  { background: var(--err-bg);  color: var(--err-text); }
.badge-warn { background: var(--warn-bg); color: var(--warn-text); }
.badge-muted{ background: #f0f0f0; color: #888; }

.divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--border), transparent);
  margin: 8px 0;
}

.loc-note {
  font-size: 11px;
  color: var(--muted);
  line-height: 1.6;
  padding: 10px 12px;
  background: #f8faf8;
  border-radius: 8px;
  border: 1px solid var(--border);
  margin-top: 12px;
}

/* Feedback Form Styles */
.feedback-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.nice-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid rgba(14,107,82,0.2);
  border-radius: 8px;
  font-size: 13px;
  background: var(--cream);
  color: var(--ink);
  outline: none;
  font-family: inherit;
  transition: border-color 0.3s;
}
.nice-input:focus {
  border-color: var(--jade);
}
textarea.nice-input {
  resize: vertical;
  min-height: 80px;
}
.file-upload {
  font-size: 12px;
  color: var(--muted);
}
.btn-group {
  margin-top: 4px;
}
.btn-primary {
  width: 100%;
  background: var(--jade);
  color: #fff;
  border: none;
  padding: 12px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(14,107,82,0.2);
  transition: all 0.3s;
}
.btn-primary:hover:not(:disabled) { background: var(--jade-light); }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }

.query-box {
  display: flex;
  gap: 8px;
}
.nice-input.small { flex: 1; }
.btn-secondary {
  background: #fff;
  color: var(--jade);
  border: 1px solid var(--jade);
  padding: 0 16px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-secondary:hover:not(:disabled) { background: var(--ok-bg); }
.feedback-id-hint { font-size: 12px; color: var(--jade); margin-top: 4px;}

/* Debug Details */
.debug-details {
  margin-top: 12px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--border);
  background: #fff;
}
.debug-details summary {
  padding: 14px 18px;
  cursor: pointer;
  font-size: 13px;
  color: var(--muted);
  user-select: none;
}
.debug-details pre {
  white-space: pre-wrap;
  word-break: break-all;
  background: #0f172a;
  color: #bfdbfe;
  padding: 14px;
  font-size: 11px;
  line-height: 1.6;
  border-top: 1px solid rgba(255,255,255,0.08);
  margin: 0;
}

.footer {
  text-align: center;
  padding: 24px 16px 8px;
  font-size: 11px;
  color: var(--muted);
  line-height: 1.8;
}
.footer-logo {
  font-family: 'ZCOOL XiaoWei', serif;
  font-size: 13px;
  color: var(--jade);
  letter-spacing: 0.05em;
  margin-bottom: 4px;
}
</style>