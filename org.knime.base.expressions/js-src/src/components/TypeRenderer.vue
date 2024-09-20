<script setup lang="ts">
import ArrowIcon from "@knime/styles/img/icons/arrow-right.svg";

export interface TypeRendererProps {
  action: () => void;
  type: string;
  isReplacement: boolean;
}

const props = defineProps<TypeRendererProps>();
</script>

<template>
  <span :class="{ type: isReplacement }" @click="props.action">
    <ArrowIcon v-if="isReplacement" class="replacement-icon" />
    {{ props.type }}
  </span>
</template>

<style scoped lang="postcss">
.type {
  cursor: pointer;
}

.type:hover {
  text-decoration: underline;
  cursor: grab;
}

.replacement-icon {
  width: 10px;
  height: 10px;
  stroke: var(--knime-masala);
  stroke-width: 2px;
  translate: 0 1px;
}
</style>
