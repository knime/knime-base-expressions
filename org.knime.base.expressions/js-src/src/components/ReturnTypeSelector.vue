<script setup lang="ts">
import { Dropdown } from "@knime/components";
import { useReadonlyStore } from "@knime/scripting-editor";
import { type FlowVariableType } from "@/flowVariableApp/flowVariableTypes";
import { ref, watch } from "vue";

export type AllowedReturnTypes = {
  id: FlowVariableType;
  text: string;
};

type PropType = { allowedReturnTypes: AllowedReturnTypes[] };

const props = defineProps<PropType>();

const readOnly = useReadonlyStore();

const selectorModel = defineModel<FlowVariableType>({
  default: "Unknown",
});

// This takes advantage of the fact that there are currently
// only a choice between IntType and LongType
// All other types have a 1-to-1 correspondence with the selected type
const lastIntegerValue = ref<FlowVariableType | null>(null);
watch(selectorModel, (selectorModel) => {
  if (selectorModel === "Integer" || selectorModel === "Long") {
    lastIntegerValue.value = selectorModel;
  }
});

watch(
  () => props.allowedReturnTypes,
  (newValue, oldValue) => {
    const allowedValues = newValue.map((item) => item.id);

    // check if the allowed values have changed by value
    // if they have not changed, we do not need to update the value
    // this prevents recursive update loops
    if (oldValue && newValue && oldValue.length === newValue.length) {
      if (oldValue.every((value, index) => value.id === allowedValues[index])) {
        return;
      }
    }

    if (
      lastIntegerValue.value &&
      lastIntegerValue.value !== selectorModel.value &&
      allowedValues.includes(lastIntegerValue.value)
    ) {
      selectorModel.value = lastIntegerValue.value;
    } else if (
      !selectorModel.value ||
      !allowedValues.includes(selectorModel.value)
    ) {
      selectorModel.value = allowedValues[0];
    }
  },
);
</script>

<template>
  <!-- eslint-disable vue/attribute-hyphenation typescript complains with ':aria-label' instead of ':ariaLabel'-->
  <Dropdown
    id="dropdown-box-to-select-entity"
    v-model="selectorModel"
    ariaLabel="return type selection"
    :possible-values="allowedReturnTypes"
    direction="up"
    :disabled="readOnly || allowedReturnTypes.length <= 1"
    compact
  />
</template>

<style scoped lang="postcss"></style>
