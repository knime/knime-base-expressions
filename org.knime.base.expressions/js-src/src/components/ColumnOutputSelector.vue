<script setup lang="ts">
import { watch, computed } from "vue";
import ValueSwitch from "webapps-common/ui/components/forms/ValueSwitch.vue";
import Dropdown from "webapps-common/ui/components/forms/Dropdown.vue";
import InputField from "webapps-common/ui/components/forms/InputField.vue";
import { useShouldFocusBePainted } from "@knime/scripting-editor";
import { type OutputInsertionMode } from "@/expressionScriptingService";

export type ColumnSelectorState = {
  outputMode: OutputInsertionMode;
  createColumn: string;
  replaceColumn: string;
};

export type AllowedDropDownValue = {
  id: string;
  text: string;
};

type PropType = {
  allowedReplacementColumns: AllowedDropDownValue[];
  modelValue?: ColumnSelectorState;
};

const props = withDefaults(defineProps<PropType>(), {
  modelValue: () =>
    ({
      outputMode: "APPEND",
      replaceColumn: "",
      createColumn: "New column",
    }) satisfies ColumnSelectorState,
});

const emit = defineEmits<{ "update:modelValue": [ColumnSelectorState] }>();

const localModelValue = computed({
  get: () => props.modelValue,
  set: (newModelValue: ColumnSelectorState) => {
    emit("update:modelValue", newModelValue);
  },
});

const outputMode = computed({
  get: () => localModelValue.value.outputMode,
  set: (newValue: OutputInsertionMode) => {
    localModelValue.value = {
      ...localModelValue.value,
      outputMode: newValue,
    };
  },
});

const newColumn = computed({
  get: () => localModelValue.value.createColumn,
  set: (newValue: string) => {
    localModelValue.value = {
      ...localModelValue.value,
      createColumn: newValue,
    };
  },
});

const replacementColumn = computed({
  get: () => localModelValue.value.replaceColumn,
  set: (newValue: string) => {
    localModelValue.value = {
      ...localModelValue.value,
      replaceColumn: newValue,
    };
  },
});

const allowedOperationModes = [
  { id: "APPEND", text: "Append" },
  { id: "REPLACE_EXISTING", text: "Replace" },
];

const paintFocus = useShouldFocusBePainted();

// Since these props can change after the component is mounted, we need to watch
// for changes to the allowedReplacementColumns prop and update the default
// replacementColumn value accordingly!
watch(
  () => props.allowedReplacementColumns,
  (newValue) => {
    replacementColumn.value =
      newValue.find((value) => value.id === replacementColumn.value)?.id ??
      newValue[0]?.id;
  },
  { deep: true },
);

// If the v-model changes via props, we also have to update
watch(
  () => props.modelValue,
  (newValue) => (localModelValue.value = newValue),
  { deep: true },
);
</script>

<template>
  <span class="output-selector-container">
    <span class="output-label">Output column</span>
    <ValueSwitch
      v-model="outputMode"
      :possible-values="allowedOperationModes"
      :disabled="props.allowedReplacementColumns.length === 0"
      class="switch-button"
      :class="{ 'focus-painted': paintFocus }"
      compact
    />
    <div v-if="outputMode === 'APPEND'" class="output-selector-child right">
      <InputField
        id="input-field-to-add-new-column"
        v-model="newColumn"
        type="text"
        class="column-input"
        placeholder="New column..."
        compact
      />
    </div>
    <div v-else class="output-selector-child right">
      <!-- eslint-disable vue/attribute-hyphenation typescript complains with ':aria-label' instead of ':ariaLabel'-->
      <Dropdown
        id="dropdown-box-to-select-column"
        v-model="replacementColumn"
        ariaLabel="column selection"
        :possible-values="allowedReplacementColumns"
        class="column-input"
        direction="up"
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

.column-input {
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
  height: var(--space-32);
}

/* Stop the dropdown being taller than the bar. This takes some forcing, since it REALLY doesn't want to be smaller than about 40px */
.column-input,
:deep(.dropdown),
:deep(#dropdown-box-to-select-column),
:deep(#button-dropdown-box-to-select-column) {
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
