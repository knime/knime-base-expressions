<script setup lang="ts">
import { ref, onMounted, watch } from "vue";
import ValueSwitch from "../../webapps-common/ui/components/forms/ValueSwitch.vue";
import Dropdown from "../../webapps-common/ui/components/forms/Dropdown.vue";
import InputField from "webapps-common/ui/components/forms/InputField.vue";

type OutputMode = "create" | "replace";
type Id = string | number;

export type ColumnSelectorState = {
  outputMode: OutputMode;
  column: Id;
};

export type AllowedDropDownValue = {
  id: Id;
  text: string;
};

const props = defineProps<{
  defaultOutputMode: OutputMode;
  allowedReplacementColumns: AllowedDropDownValue[];
}>();

const outputMode = ref<OutputMode>(props.defaultOutputMode);
const newColumn = ref("Output column");

// Set default like so to avoid explicit use of undefined.
const replacementColumn = ref<Id | undefined>(
  props.allowedReplacementColumns[0]?.id,
);

const allowedOperationModes = [
  { id: "create", text: "Append" },
  { id: "replace", text: "Replace" },
];

const emit = defineEmits(["update:modelValue"]);

const update = () => {
  emit("update:modelValue", {
    outputMode: outputMode.value,
    column:
      outputMode.value === "create" ? newColumn.value : replacementColumn.value,
  });
};

onMounted(() => {
  update();
});

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
);
</script>

<template>
  <div class="output-selector-parent">
    <div class="output-selector-child left">
      <span class="output-label">Output column</span>
      <ValueSwitch
        v-model="outputMode"
        :possible-values="allowedOperationModes"
        :disabled="props.allowedReplacementColumns.length === 0"
        class="switch-button"
        @change="update"
      />
    </div>
    <div v-if="outputMode === 'create'" class="output-selector-child right">
      <InputField
        id="input-field-to-add-new-column"
        v-model="newColumn"
        type="text"
        class="column-input"
        placeholder="New column..."
        @input="update"
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
        @update:model-value="update"
      />
    </div>
  </div>
</template>

<style>
.output-selector-parent {
  display: grid;
  padding-right: 20px;
}

.output-selector-child {
  flex: 1;
  height: 100%;
  place-items: center center;
}

.output-selector-child.left {
  grid-area: 1 / 1 / 2 / 2;
  display: flex;
}

.output-selector-child.right {
  grid-area: 1 / 2 / 2 / 3;
  display: flex;
  overflow: visible;
}

.input-wrapper.column-input {
  width: 100%;
  overflow: visible;
}

#dropdown-box-to-select-column div,
#input-field-to-add-new-column,
.input-wrapper.column-input,
.output-selector-child {
  height: 34px;
}

#input-field-to-add-new-column,
#dropdown-box-to-select-column {
  position: absolute;
}

.output-label {
  font-size: small;
  font-weight: bold;
}

#input-field-to-add-new-column,
#dropdown-box-to-select-column,
.output-selector-child {
  width: 240px;
}

/* Push the toggle button up against the input fields */
.switch-button {
  margin: 0 10px 0 auto;
}
</style>
