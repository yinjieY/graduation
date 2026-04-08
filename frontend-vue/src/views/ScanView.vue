<template>
  <div class="panel">
    <h2 class="panel-title">扫码溯源页面</h2>
    <div class="muted">无需登录，支持溯源查询、扫码上报与消费者反馈</div>
    <div v-if="notice" class="status-line" :class="{ error: noticeType === 'error' }">{{ notice }}</div>

    <section class="card">
      <h3>扫码参数</h3>
      <div class="row">
        <input v-model="scanForm.qsId" placeholder="qsId" />
        <input v-model="scanForm.batchId" placeholder="batchId" />
        <input v-model="scanForm.companyId" placeholder="companyId" />
        <input v-model="scanForm.signaturePayload" placeholder="payload" />
        <input v-model="scanForm.signature" placeholder="signature" />
      </div>
      <div class="ops" style="margin-top:8px;">
        <button :disabled="loading" @click="doQuery">查询溯源</button>
        <button :disabled="loading" @click="doReport">上报扫码</button>
      </div>
    </section>

    <section class="card">
      <h3>消费者反馈</h3>
      <div class="row">
        <select v-model="feedbackForm.feedbackType">
          <option value="CROSS_REGION">串货</option>
          <option value="COUNTERFEIT">假冒</option>
          <option value="OTHER">其他</option>
        </select>
        <input v-model="feedbackForm.region" placeholder="地区" />
        <input type="file" accept="image/*" @change="onFileChange" />
        <input v-model="feedbackForm.description" placeholder="描述(可选)" />
      </div>
      <div class="ops" style="margin-top:8px;"><button :disabled="loading" @click="submitFeedbackForm">提交反馈</button></div>
      <div class="muted" style="margin-top:8px;">反馈编号：{{ feedbackId || '-' }}</div>
    </section>

    <section class="card">
      <h3>反馈进度查询</h3>
      <div class="row">
        <input v-model="feedbackQueryId" placeholder="feedbackId" />
        <button :disabled="loading" @click="queryStatus">查询状态</button>
      </div>
    </section>

    <section class="card">
      <h3>结果</h3>
      <pre>{{ JSON.stringify(resultData, null, 2) }}</pre>
    </section>
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
  browser: 'Browser',
  ip: '',
  locationSource: 'browser'
});

const feedbackForm = reactive({
  feedbackType: 'CROSS_REGION',
  region: '',
  description: '',
  image: null
});

const feedbackId = ref('');
const feedbackQueryId = ref('');
const resultData = ref({});
const loading = ref(false);
const notice = ref('');
const noticeType = ref('info');

function setNotice(message, type = 'info') {
  notice.value = message;
  noticeType.value = type;
}

async function withLoading(task, successMessage = '') {
  loading.value = true;
  try {
    await task();
    if (successMessage) {
      setNotice(successMessage);
    }
  } catch (error) {
    setNotice(error?.message || '请求失败，请稍后重试', 'error');
  } finally {
    loading.value = false;
  }
}

function onFileChange(e) {
  const files = e?.target?.files;
  feedbackForm.image = files && files[0] ? files[0] : null;
}

function fillGeo(position) {
  scanForm.latitude = position.coords.latitude;
  scanForm.longitude = position.coords.longitude;
}

async function doQuery() {
  await withLoading(async () => {
    const res = await queryTrace(scanForm.qsId);
    resultData.value = res;
  });
}

async function doReport() {
  await withLoading(async () => {
    const res = await reportScan(scanForm);
    resultData.value = res;
  }, '扫码行为上报成功');
}

async function submitFeedbackForm() {
  if (!feedbackForm.image) {
    setNotice('请先上传图片', 'error');
    return;
  }
  await withLoading(async () => {
    const formData = new FormData();
    formData.set('qsId', scanForm.qsId);
    formData.set('feedbackType', feedbackForm.feedbackType);
    formData.set('deviceFingerprint', scanForm.deviceFingerprint);
    formData.set('region', feedbackForm.region);
    formData.set('description', feedbackForm.description);
    if (scanForm.latitude) formData.set('latitude', String(scanForm.latitude));
    if (scanForm.longitude) formData.set('longitude', String(scanForm.longitude));
    formData.set('image', feedbackForm.image);

    const res = await submitFeedback(formData);
    resultData.value = res;
    feedbackId.value = res?.data?.feedbackId || '';
    feedbackQueryId.value = feedbackId.value;
  }, '反馈提交成功，已生成反馈编号');
}

async function queryStatus() {
  if (!feedbackQueryId.value) {
    setNotice('请先输入反馈编号', 'error');
    return;
  }
  await withLoading(async () => {
    const res = await queryFeedbackStatus(feedbackQueryId.value);
    resultData.value = res;
  });
}

onMounted(() => {
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(fillGeo, () => {}, { timeout: 4000 });
  }
});
</script>

