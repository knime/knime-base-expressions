<script setup lang="ts">
import { ValueSwitch, Dropdown, InputField } from "@knime/components";
import {
  useReadonlyStore,
  useShouldFocusBePainted,
} from "@knime/scripting-editor";
import type { OutputInsertionMode } from "@/common/types";
import { watch } from "vue";

export type SelectorState = {
  outputMode: OutputInsertionMode;
  create: string;
  replace: string;
};

export type AllowedDropDownValue = {
  id: string;
  text: string;
};

type PropType = {
  entityName: string;
  allowedReplacementEntities: AllowedDropDownValue[];
  isValid?: boolean;
};

const props = withDefaults(defineProps<PropType>(), {
  isValid: true,
});
const readOnly = useReadonlyStore();

const modelValue = defineModel<SelectorState>({
  default: {
    outputMode: "APPEND",
    replace: "",
    create: "New Entity",
  } satisfies SelectorState,
});
watch(
  modelValue,
  () => {
    // Set the initial value for the replace field if it has not been set by backend
    if (
      modelValue.value.outputMode === "REPLACE_EXISTING" &&
      modelValue.value.replace === null
    ) {
      modelValue.value.replace =
        props.allowedReplacementEntities?.[0]?.id ?? "";
    }
  },
  { deep: true },
);

const allowedOperationModes = [
  { id: "APPEND", text: "Append" },
  { id: "REPLACE_EXISTING", text: "Replace" },
];

const paintFocus = useShouldFocusBePainted();
</script>

<template>
  <span class="output-selector-container">
    <span class="output-label">{{ `Output ${entityName}` }} </span>
    <ValueSwitch
      v-model="modelValue.outputMode"
      :possible-values="allowedOperationModes"
      :disabled="props.allowedReplacementEntities.length === 0 || readOnly"
      class="switch-button"
      :class="{ 'focus-painted': paintFocus }"
      compact
    />
    <div
      v-if="modelValue.outputMode === 'APPEND'"
      class="output-selector-child right"
    >
      <InputField
        id="input-field-to-add-new-entity"
        v-model="modelValue.create"
        type="text"
        class="input"
        :placeholder="`New ${entityName}...`"
        :is-valid="isValid"
        :disabled="readOnly"
        compact
      />
    </div>
    <div v-else class="output-selector-child right">
      <!-- eslint-disable vue/attribute-hyphenation typescript complains with ':aria-label' instead of ':ariaLabel'-->
      <Dropdown
        id="dropdown-box-to-select-entity"
        v-model="modelValue.replace"
        :ariaLabel="`${entityName} selection`"
        :possible-values="allowedReplacementEntities"
        class="input"
        direction="up"
        :is-valid="isValid"
        :disabled="readOnly"
        compact
      />
    </div>
  </span>
</template>

<style scoped lang="postcss">
.output-selector-container {
  display: flex;
  flex-flow: row wrap;
  height: fit-content;
  flex-grow: 1;
  gap: var(--space-8);
  align-items: center;
  container-type: inline-size;
  justify-content: center;
  padding: var(--space-4);
}

.output-selector-child {
  flex-grow: 1;
  height: 100%;
  display: flex;
  top: 0;
  gap: var(--space-8);
  width: 150px;
}

.input {
  width: 100%;
}

.output-selector-child.left {
  flex-wrap: wrap;
  container-type: inline-size;
}

@container (width < 390px) {
  .output-label {
    display: none;
  }
}

.output-selector-child.right {
  overflow: visible;
  top: 0;
  align-items: flex-start;
  max-width: 100%;
  min-width: 100px;
  height: var(--single-line-form-height-compact);
}

/* Stop the dropdown being taller than the bar. This takes some forcing, since it REALLY doesn't want to be smaller than about 40px */
.input,
:deep(.dropdown),
:deep(#dropdown-box-to-select-entity),
:deep(#button-dropdown-box-to-select-entity) {
  max-width: 400px;
}

.output-label {
  font-size: small;
  font-weight: bold;
}

.switch-button {
  position: relative;
}

/* Create a selection highlight slightly bigger than the buttons themselves */
.switch-button.focus-painted:focus-within::after {
  --border-spacing: var(--space-4);

  content: "";
  border: 2px solid var(--knime-cornflower);
  position: absolute;
  pointer-events: none;
  z-index: 1;
  left: calc(-1 * var(--border-spacing));
  top: calc(-1 * var(--border-spacing));
  height: calc(100% + 2 * var(--border-spacing));
  width: calc(100% + 2 * var(--border-spacing));

  /* Canonical way to get largest possible pill-shaped border */
  border-radius: 999vw;
}
</style>
