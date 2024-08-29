<script setup lang="ts">
import type {
  FunctionCatalogEntryData,
  CategoryData,
} from "@/components/functionCatalogTypes";
import { computed } from "vue";
import {
  functionDataToHtml,
  categoryDataToHtml,
} from "@/components/function-catalog/functionDescriptionToMarkdown";
import { Description } from "@knime/components";

type PropType =
  | {
      data: FunctionCatalogEntryData;
      type: "functionOrConstant";
    }
  | {
      data: CategoryData;
      type: "category";
    };

const props = defineProps<PropType>();

const descriptionAsHTML = computed(() =>
  props.type === "functionOrConstant"
    ? functionDataToHtml(props.data)
    : categoryDataToHtml(props.data),
);
const title = computed(() => {
  if (props.type === "category" || props.data.entryType === "constant") {
    return props.data.name;
  } else {
    // then it's a function and should be displayed with its args
    return props.data.displayName;
  }
});
</script>

<template>
  <div class="node-description">
    <h3 class="description-title">{{ title }}</h3>
    <hr />
    <Description
      :text="descriptionAsHTML"
      render-as-html
      class="markdown-function-desc"
    />
  </div>
</template>

<style lang="postcss">
.markdown-function-desc {
  --description-font-size: 13px;

  padding: 0 var(--space-4);
  font-size: 13px;
  line-height: 150%;

  /* Copy/pasted from NodeDescriptionContent.vue, changes how code looks */
  & :deep(tt),
  & :deep(pre),
  & :deep(code),
  & :deep(samp) {
    font-size: 11px;
    line-height: 150%;
  }
}

.description-title {
  font-size: 18px;
  margin-bottom: 10px;
  margin-top: 0;
  line-height: normal;
}
</style>
