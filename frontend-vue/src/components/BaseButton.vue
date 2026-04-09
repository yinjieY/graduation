<template>
  <button
    class="base-button"
    :class="[
      `button-${type}`,
      `button-${size}`,
      { 'button-disabled': disabled }
    ]"
    :disabled="disabled"
    @click="$emit('click')"
  >
    <span v-if="loading" class="button-loading">
      <div class="loading-spinner"></div>
    </span>
    <span v-else-if="icon" class="button-icon">{{ icon }}</span>
    <span class="button-text"><slot></slot></span>
  </button>
</template>

<script setup>
const props = defineProps({
  type: {
    type: String,
    default: 'primary',
    validator: (value) => ['primary', 'secondary', 'success', 'warning', 'error', 'info'].includes(value)
  },
  size: {
    type: String,
    default: 'medium',
    validator: (value) => ['small', 'medium', 'large'].includes(value)
  },
  disabled: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  },
  icon: {
    type: String,
    default: ''
  }
});

const emit = defineEmits(['click']);
</script>

<style scoped>
.base-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;
}

/* 按钮类型 */
.button-primary {
  background: #3b82f6;
  color: white;
}

.button-primary:hover:not(.button-disabled) {
  background: #2563eb;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.button-secondary {
  background: #f1f5f9;
  color: #334155;
  border: 1px solid #e2e8f0;
}

.button-secondary:hover:not(.button-disabled) {
  background: #e2e8f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.button-success {
  background: #10b981;
  color: white;
}

.button-success:hover:not(.button-disabled) {
  background: #059669;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

.button-warning {
  background: #f59e0b;
  color: white;
}

.button-warning:hover:not(.button-disabled) {
  background: #d97706;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
}

.button-error {
  background: #ef4444;
  color: white;
}

.button-error:hover:not(.button-disabled) {
  background: #dc2626;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
}

.button-info {
  background: #64748b;
  color: white;
}

.button-info:hover:not(.button-disabled) {
  background: #475569;
  box-shadow: 0 4px 12px rgba(100, 116, 139, 0.3);
}

/* 按钮大小 */
.button-small {
  padding: 6px 12px;
  font-size: 12px;
}

.button-medium {
  padding: 8px 16px;
  font-size: 14px;
}

.button-large {
  padding: 10px 20px;
  font-size: 16px;
}

/* 禁用状态 */
.button-disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 加载状态 */
.button-loading {
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.button-text {
  transition: all 0.2s ease;
}

/* 图标 */
.button-icon {
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>