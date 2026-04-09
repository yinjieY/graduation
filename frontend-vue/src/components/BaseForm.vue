<template>
  <form class="base-form" @submit.prevent="handleSubmit">
    <slot></slot>
    <div class="form-actions" v-if="$slots.actions">
      <slot name="actions"></slot>
    </div>
  </form>
</template>

<script setup>
const emit = defineEmits(['submit']);

const handleSubmit = () => {
  emit('submit');
};
</script>

<style scoped>
.base-form {
  width: 100%;
}

.form-actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}
</style>