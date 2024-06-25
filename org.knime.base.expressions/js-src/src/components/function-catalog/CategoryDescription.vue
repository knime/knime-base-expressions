<script setup lang="ts">
defineProps<{
  category: {
    name: string;
    description: string;
  };
}>();
</script>

<template>
  <div class="category-description-panel">
    <h4 class="category-header">{{ `Category: ${category.name}` }}</h4>
    <div class="category-description">
      {{ category.description }}
    </div>
  </div>
</template>

<style scoped>
.category-header {
  font-size: x-large;
  margin: 0 auto;
  text-align: center;
}

.category-description {
  margin-left: var(--space-4);
  margin-top: var(--space-12);
}

.category-description-panel {
  font-size: small;
}
</style>
