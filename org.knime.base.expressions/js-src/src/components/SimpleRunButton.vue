<script setup lang="ts">
import { computed } from "vue";

import { Tooltip } from "@knime/components";
import { KdsButton } from "@knime/kds-components";

interface PropType {
  runButtonDisabledErrorReason: string | null;
  showButtonText: boolean;
}

const emit = defineEmits<(event: "run-expressions") => void>();

const handleRunExpressions = () => {
  emit("run-expressions");
};

const props = defineProps<PropType>();

const buttonText = computed(() => (props.showButtonText ? "Evaluate" : ""));
</script>

<template>
  <Tooltip class="error-tooltip" :text="runButtonDisabledErrorReason ?? ''">
    <KdsButton
      :label="buttonText"
      leading-icon="execute"
      primary
      compact
      :disabled="runButtonDisabledErrorReason !== null"
      :class="{
        'hide-button-text': !showButtonText,
      }"
      @click="handleRunExpressions()"
    />
  </Tooltip>
</template>

<style lang="postcss" scoped>
.hide-button-text {
  margin-right: calc(-1 * var(--space-16));
}

.run-button {
  display: inline;
}

/* Needed to be on top of the splitpane splitters */
.error-tooltip :deep(.text) {
  z-index: 1;
}
</style>
