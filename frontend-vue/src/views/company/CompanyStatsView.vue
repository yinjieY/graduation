<template>
  <Layout :role="'company'">
    <div class="stats-container">
      <h1 class="page-title">数据统计分析</h1>
      
      <div class="stats-filters">
        <select v-model="timeRange" @change="loadStats">
          <option value="7">最近7天</option>
          <option value="30">最近30天</option>
          <option value="90">最近90天</option>
          <option value="365">最近一年</option>
        </select>
      </div>
      
      <div class="stats-grid">
        <!-- 扫码量统计 -->
        <div class="stats-card">
          <h2 class="card-title">扫码量统计</h2>
          <div class="stats-chart" v-if="scanStats">
            <div class="chart-bar" v-for="(item, index) in scanStats.dailyScans" :key="index">
              <div class="bar-container">
                <div class="bar" :style="{ height: `${(item.count / maxScanCount) * 100}%` }"></div>
              </div>
              <div class="bar-label">{{ item.date }}</div>
              <div class="bar-value">{{ item.count }}</div>
            </div>
          </div>
          <div v-else class="loading-state">加载中...</div>
        </div>
        
        <!-- 区域分布 -->
        <div class="stats-card">
          <h2 class="card-title">区域分布</h2>
          <div class="region-stats" v-if="regionStats">
            <div class="region-item" v-for="(item, index) in regionStats" :key="index">
              <div class="region-name">{{ item.region }}</div>
              <div class="region-bar">
                <div class="region-bar-fill" :style="{ width: `${(item.count / maxRegionCount) * 100}%` }"></div>
              </div>
              <div class="region-count">{{ item.count }}</div>
            </div>
          </div>
          <div v-else class="loading-state">加载中...</div>
        </div>
        
        <!-- 预警统计 -->
        <div class="stats-card">
          <h2 class="card-title">预警统计</h2>
          <div class="alert-stats" v-if="alertStats">
            <div class="alert-item">
              <div class="alert-type">红色预警</div>
              <div class="alert-count">{{ alertStats.red || 0 }}</div>
            </div>
            <div class="alert-item">
              <div class="alert-type">黄色预警</div>
              <div class="alert-count">{{ alertStats.yellow || 0 }}</div>
            </div>
            <div class="alert-item">
              <div class="alert-type">蓝色预警</div>
              <div class="alert-count">{{ alertStats.blue || 0 }}</div>
            </div>
          </div>
          <div v-else class="loading-state">加载中...</div>
        </div>
        
        <!-- 批次溯源率 -->
        <div class="stats-card">
          <h2 class="card-title">批次溯源率</h2>
          <div class="batch-stats" v-if="batchStats">
            <div class="batch-item" v-for="(item, index) in batchStats" :key="index">
              <div class="batch-name">{{ item.batchName }}</div>
              <div class="batch-rate">
                <div class="batch-rate-fill" :style="{ width: `${item.traceRate}%` }"></div>
              </div>
              <div class="batch-rate-value">{{ item.traceRate }}%</div>
            </div>
          </div>
          <div v-else class="loading-state">加载中...</div>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import Layout from '../../components/Layout.vue';
import { useApi, handleApiError } from '../../composables/useApi';
import { useNotification } from '../../composables/useNotification';

const api = useApi();
const { showError } = useNotification();

const loading = ref(false);
const timeRange = ref('7');
const scanStats = ref(null);
const regionStats = ref(null);
const alertStats = ref(null);
const batchStats = ref(null);

const maxScanCount = computed(() => {
  if (!scanStats.value || !scanStats.value.dailyScans.length) return 1;
  return Math.max(...scanStats.value.dailyScans.map(item => item.count));
});

const maxRegionCount = computed(() => {
  if (!regionStats.value || !regionStats.value.length) return 1;
  return Math.max(...regionStats.value.map(item => item.count));
});

const loadStats = async () => {
  try {
    loading.value = true;
    const token = localStorage.getItem('company_token');

    const [alertList, batchList, qrList] = await Promise.all([
      api.getAlertList({}, token),
      api.getProductBatchList(token),
      api.getQrCodeList(token)
    ]);

    // 预警统计（按高/中/低映射红/黄/蓝）
    const levelCounter = { red: 0, yellow: 0, blue: 0 };
    (alertList || []).forEach((item) => {
      const level = String(item.alertLevel || '').toUpperCase();
      if (level === 'HIGH') levelCounter.red += 1;
      else if (level === 'MEDIUM') levelCounter.yellow += 1;
      else levelCounter.blue += 1;
    });
    alertStats.value = levelCounter;

    // 扫码与区域统计：从二维码日志聚合
    const days = Number(timeRange.value || 7);
    const beginTs = Date.now() - days * 24 * 60 * 60 * 1000;
    const dailyMap = new Map();
    const regionMap = new Map();
    const batchScanMap = new Map();

    const qrIds = (qrList || []).map((item) => item.qsId).filter(Boolean).slice(0, 200);
    const logsRows = await Promise.all(qrIds.map((qsId) => api.getScanLogs(qsId, token).catch(() => [])));
    logsRows.flat().forEach((log) => {
      const ts = new Date(log.scanTime || log.createdAt || Date.now()).getTime();
      if (Number.isNaN(ts) || ts < beginTs) {
        return;
      }
      const day = new Date(ts).toISOString().slice(0, 10);
      dailyMap.set(day, (dailyMap.get(day) || 0) + 1);

      const region = log.city || log.province || '未知地区';
      regionMap.set(region, (regionMap.get(region) || 0) + 1);

      const batchId = log.batchId || '';
      if (batchId) {
        batchScanMap.set(batchId, (batchScanMap.get(batchId) || 0) + 1);
      }
    });

    scanStats.value = {
      dailyScans: Array.from(dailyMap.entries())
        .sort((a, b) => a[0].localeCompare(b[0]))
        .map(([date, count]) => ({ date, count }))
    };

    regionStats.value = Array.from(regionMap.entries())
      .map(([region, count]) => ({ region, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 8);

    // 批次溯源率 = 实际扫码数 / 理论最大扫码量 × 100%
    // 理论最大扫码量 = 二维码数量 × 单个二维码最大扫码次数
    const qrBatchInfoMap = new Map();
    (qrList || []).forEach((item) => {
      const batchId = item.batchId;
      if (batchId) {
        const current = qrBatchInfoMap.get(batchId) || { count: 0, maxScans: 0 };
        qrBatchInfoMap.set(batchId, {
          count: current.count + 1,
          maxScans: current.maxScans + (item.maxAllowedScans || 5)
        });
      }
    });
    
    const batchMap = new Map();
    (batchList || []).forEach((batch) => {
      if (!batchMap.has(batch.batchId)) {
        const qrInfo = qrBatchInfoMap.get(batch.batchId);
        const issued = qrInfo ? qrInfo.count : 0;
        const maxPossibleScans = qrInfo ? qrInfo.maxScans : 0;
        const scans = batchScanMap.get(batch.batchId) || 0;
        const rate = maxPossibleScans > 0 ? Math.min(100, Math.round((scans / maxPossibleScans) * 100)) : 0;
        batchMap.set(batch.batchId, {
          batchName: batch.batchName || batch.productionStandard || batch.batchId,
          traceRate: rate,
          issued,
          scans,
          maxPossibleScans
        });
      }
    });
    
    batchStats.value = Array.from(batchMap.values()).sort((a, b) => b.traceRate - a.traceRate);
  } catch (error) {
    showError(handleApiError(error));
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadStats();
});
</script>

<style scoped>
.stats-container {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.stats-filters {
  margin-bottom: 24px;
}

.stats-filters select {
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  min-width: 150px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 24px;
}

.stats-card {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 20px;
  transition: all 0.2s ease;
}

.stats-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  border-color: #3b82f6;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #334155;
  margin: 0 0 16px 0;
}

.stats-chart {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  height: 200px;
  padding: 16px 0;
}

.chart-bar {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.bar-container {
  flex: 1;
  width: 100%;
  min-height: 20px;
  background: #f1f5f9;
  border-radius: 4px;
  overflow: hidden;
  position: relative;
}

.bar {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  background: linear-gradient(180deg, #3b82f6 0%, #2563eb 100%);
  border-radius: 4px;
  transition: height 0.3s ease;
}

.bar-label {
  font-size: 12px;
  color: #64748b;
  text-align: center;
  white-space: nowrap;
}

.bar-value {
  font-size: 12px;
  font-weight: 500;
  color: #334155;
}

.region-stats {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.region-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.region-name {
  min-width: 100px;
  font-size: 14px;
  color: #64748b;
}

.region-bar {
  flex: 1;
  height: 8px;
  background: #f1f5f9;
  border-radius: 4px;
  overflow: hidden;
}

.region-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #10b981 0%, #059669 100%);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.region-count {
  min-width: 40px;
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  text-align: right;
}

.alert-stats {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alert-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  background: #f8fafc;
}

.alert-type {
  font-size: 14px;
  color: #64748b;
}

.alert-count {
  font-size: 18px;
  font-weight: 700;
  color: #334155;
}

.batch-stats {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.batch-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.batch-name {
  min-width: 100px;
  font-size: 14px;
  color: #64748b;
}

.batch-rate {
  flex: 1;
  height: 8px;
  background: #f1f5f9;
  border-radius: 4px;
  overflow: hidden;
}

.batch-rate-fill {
  height: 100%;
  background: linear-gradient(90deg, #f59e0b 0%, #d97706 100%);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.batch-rate-value {
  min-width: 60px;
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  text-align: right;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #64748b;
  font-size: 14px;
}

@media (max-width: 768px) {
  .stats-container {
    padding: 16px;
  }
  
  .page-title {
    font-size: 18px;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .stats-card {
    padding: 16px;
  }
  
  .region-name,
  .batch-name {
    min-width: 80px;
    font-size: 12px;
  }
  
  .stats-chart {
    height: 150px;
  }
  
  .bar-label {
    font-size: 10px;
  }
}
</style>