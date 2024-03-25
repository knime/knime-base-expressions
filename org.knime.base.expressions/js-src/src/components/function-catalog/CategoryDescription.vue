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
  border-left: 5px solid var(--knime-silver-sand-semi);
  padding-left: 25px;
  margin-left: 5px;
  margin-top: 10px;
}

.category-description-panel {
  font-size: small;
}
</style>
