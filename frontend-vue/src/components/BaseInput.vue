<template>
  <div class="base-input">
    <label v-if="label" class="input-label" :for="id">{{ label }}</label>
    <input
      :id="id"
      :type="type"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
      :placeholder="placeholder"
      :disabled="disabled"
      :readonly="readonly"
      :class="{ 'input-error': error }"
      :required="required"
    />
    <div v-if="error" class="input-error-message">{{ error }}</div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  label: {
    type: String,
    default: ''
  },
  type: {
    type: String,
    default: 'text'
  },
  placeholder: {
    type: String,
    default: ''
  },
  disabled: {
    type: Boolean,
    default: false
  },
  readonly: {
    type: Boolean,
    default: false
  },
  error: {
    type: String,
    default: ''
  },
  required: {
    type: Boolean,
    default: false
  },
  id: {
    type: String,
    default: computed(() => `input-${Math.random().toString(36).substr(2, 9)}`)
  }
});

const emit = defineEmits(['update:modelValue']);
</script>

<style scoped>
.base-input {
  margin-bottom: 16px;
}

.input-label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #334155;
}

input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

input:disabled {
  background: #f1f5f9;
  cursor: not-allowed;
  color: #94a3b8;
}

input.input-error {
  border-color: #ef4444;
}

.input-error-message {
  margin-top: 4px;
  font-size: 12px;
  color: #ef4444;
}
</style>