<script setup lang="ts">
import { KdsButton } from "@knime/kds-components";

export interface IconRendererProps {
  action: () => void;
}

const props = defineProps<IconRendererProps>();
</script>

<template>
  <KdsButton
    aria-label="Replaced column"
    variant="transparent"
    size="xsmall"
    leading-icon="replace"
    class="replaced-button"
    @click="props.action"
  />
</template>

<style scoped lang="postcss">
.replaced-button {
  gap: var(--kds-spacing-container-0-12x);
  width: var(--kds-dimension-component-width-1-25x);
  height: var(--kds-dimension-component-height-1-25x);
  padding-right: var(--kds-spacing-container-none);
  padding-left: var(--kds-spacing-container-none);
  background-color: var(--kds-color-background-neutral-initial);
  border: var(--kds-border-action-transparent);
  border-radius: var(--kds-border-radius-container-0-25x);

  &:hover {
    background-color: var(--kds-color-background-neutral-hover);
  }
}
</style>
