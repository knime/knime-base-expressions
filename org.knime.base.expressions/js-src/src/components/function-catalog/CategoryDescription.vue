<script setup lang="ts">
defineProps<{
  category: {
    shortName: string;
    description: string;
    metaCategory: string | null;
  };
}>();
</script>

<template>
  <div class="category-description-panel">
    <h4 class="category-header upper">
      {{ category.metaCategory ?? "" }}
    </h4>
    <h4 class="category-header">{{ category.shortName }}</h4>
    <div class="category-description">
      {{ category.description }}
    </div>
  </div>
</template>

<style scoped>
.category-header {
  font-size: 15px;
  margin: 0 auto;
  font-weight: 500;
  line-height: 18px;
  height: 18px;
  color: var(--knime-masala);

  &.upper {
    font-weight: 300;
    font-size: 13px;
  }
}

.category-description-panel {
  margin-left: var(--space-8);
  margin-right: var(--space-8);
  margin-top: var(--space-4);
  font-size: 13px;
  line-height: 18px;
}

.category-description {
  margin-top: var(--space-32);
}
</style>
