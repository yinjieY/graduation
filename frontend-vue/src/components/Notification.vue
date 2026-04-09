<template>
  <div class="notification" :class="{ 'notification--show': visible }">
    <div class="notification__content">
      <span class="notification__icon" :class="type">{{ icon }}</span>
      <span class="notification__message">{{ message }}</span>
      <button class="notification__close" @click="close">×</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';

const props = defineProps({
  message: {
    type: String,
    required: true
  },
  type: {
    type: String,
    default: 'info',
    validator: (value) => ['info', 'success', 'warning', 'error'].includes(value)
  },
  duration: {
    type: Number,
    default: 3000
  }
});

const emit = defineEmits(['close']);

const visible = ref(false);
let timer = null;

const icon = computed(() => {
  const icons = {
    info: 'ℹ',
    success: '✓',
    warning: '⚠',
    error: '✗'
  };
  return icons[props.type] || icons.info;
});

const close = () => {
  visible.value = false;
  clearTimeout(timer);
  setTimeout(() => {
    emit('close');
  }, 300);
};

onMounted(() => {
  visible.value = true;
  if (props.duration > 0) {
    timer = setTimeout(close, props.duration);
  }
});

onUnmounted(() => {
  clearTimeout(timer);
});
</script>

<style scoped>
.notification {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 10000;
  max-width: 300px;
  transform: translateX(100%);
  opacity: 0;
  transition: all 0.3s ease;
}

.notification--show {
  transform: translateX(0);
  opacity: 1;
}

.notification__content {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  background: white;
}

.notification__icon {
  margin-right: 12px;
  font-size: 18px;
  font-weight: bold;
}

.notification__icon.info {
  color: #1890ff;
}

.notification__icon.success {
  color: #52c41a;
}

.notification__icon.warning {
  color: #faad14;
}

.notification__icon.error {
  color: #f5222d;
}

.notification__message {
  flex: 1;
  font-size: 14px;
  line-height: 1.4;
}

.notification__close {
  background: none;
  border: none;
  font-size: 16px;
  cursor: pointer;
  color: #999;
  margin-left: 12px;
  padding: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.notification__close:hover {
  background: #f0f0f0;
  color: #666;
}
</style>