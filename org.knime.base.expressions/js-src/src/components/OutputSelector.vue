<script setup lang="ts">
import { watch } from "vue";

import { Dropdown, InputField, ValueSwitch } from "@knime/components";
import {
  useReadonlyStore,
  useShouldFocusBePainted,
} from "@knime/scripting-editor";

import type { OutputInsertionMode } from "@/common/types";

export type SelectorState = {
  outputMode: OutputInsertionMode;
  create: string;
  replace: string;
};

export type AllowedDropDownValue = {
  id: string;
  text: string;
};

export type ItemType = "flow variable" | "column";

type PropType = {
  itemType: ItemType;
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
      modelValue.value.replace === ""
    ) {
      modelValue.value.replace =
        props.allowedReplacementEntities?.[0]?.id ?? "";
    }
  },
  { deep: true },
);

const allowedOperationModes = [
  { id: "APPEND", text: props.itemType === "column" ? "Append" : "Create" },
  { id: "REPLACE_EXISTING", text: "Replace" },
];

const isStringFalsy = (s: string | null | undefined): boolean =>
  !s || s.trim() === "";

const paintFocus = useShouldFocusBePainted();
</script>

<template>
  <span class="output-selector-container">
    <div class="mode-value-switch">
      <ValueSwitch
        v-model="modelValue.outputMode"
        :possible-values="allowedOperationModes"
        :disabled="
          (props.allowedReplacementEntities.length === 0 &&
            modelValue.outputMode === 'APPEND' &&
            isStringFalsy(modelValue.replace)) ||
          readOnly
        "
        class="switch-button"
        :class="{ 'focus-painted': paintFocus }"
        compact
      />
    </div>
    <div v-if="modelValue.outputMode === 'APPEND'" class="input">
      <InputField
        id="input-field-to-add-new-entity"
        v-model="modelValue.create"
        type="text"
        :placeholder="`New ${itemType}...`"
        :is-valid="isValid"
        class="input"
        :disabled="readOnly"
        compact
      />
    </div>
    <div v-else class="input">
      <!-- eslint-disable vue/attribute-hyphenation typescript complains with ':aria-label' instead of ':ariaLabel'-->
      <Dropdown
        id="dropdown-box-to-select-entity"
        v-model="modelValue.replace"
        :ariaLabel="`${itemType} selection`"
        :possible-values="allowedReplacementEntities"
        direction="up"
        class="input"
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
  flex-direction: column;
  align-items: flex-start;
  width: 100%;
  gap: var(--space-8);
}

.input {
  width: 100%;
  max-width: 400px;
}

.mode-value-switch {
  padding: var(--space-4) 0;
}

.switch-button.focus-painted:focus-within {
  outline: 2px solid var(--knime-cornflower);
}
</style>
