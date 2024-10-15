<script setup lang="ts">
import ArrowIcon from "@knime/styles/img/icons/arrow-right.svg";

export interface IconRendererProps {
  action: () => void;
}

const props = defineProps<IconRendererProps>();
</script>

<template>
  <span class="type" @click="props.action">
    <ArrowIcon class="input-output-item-icon" />
  </span>
</template>

<style scoped lang="postcss">
.type {
  cursor: pointer;
  padding: var(--space-4);
  display: inline-flex;

  &:hover {
    background-color: var(--knime-stone-light);
    border-radius: 50%;
    cursor: grab;
  }
}

.input-output-item-icon {
  width: 12px;
  height: 12px;
  stroke: var(--knime-masala);
  stroke-width: 3px;
}
</style>
