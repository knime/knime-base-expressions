<script setup lang="ts">
import type { FunctionCatalogEntryData } from "@/components/functionCatalogTypes";
import { computed } from "vue";
import { functionDataToHtml } from "@/components/function-catalog/functionDescriptionToMarkdown";
import Description from "webapps-common/ui/components/Description.vue";

const props = defineProps<{
  functionData: FunctionCatalogEntryData;
}>();

const functionDataAsHTML = computed(() =>
  functionDataToHtml(props.functionData),
);
</script>

<template>
  <div class="node-description">
    <Description
      :text="functionDataAsHTML"
      render-as-html
      class="markdown-function-desc"
    />
  </div>
</template>

<style lang="postcss">
.markdown-function-desc {
  & h3 {
    line-height: 18px;
    margin-bottom: 5px;
    margin-top: 0;
    font-size: 15px;

    &:first-child {
      margin-top: 0;
      margin-bottom: 10px;
    }
  }

  & h4 {
    line-height: 16px;
    margin: 0;
    font-size: 13px;
  }
}
</style>

<style scoped>
.node-description {
  height: 100%;
  padding-right: 8px;
  padding-bottom: 8px;
  overflow: auto;

  & .description {
    font-size: 13px;
    line-height: 18px;
  }

  /* Style refinement for Code */
  & :deep(tt),
  & :deep(pre),
  & :deep(code),
  & :deep(samp) {
    font-size: 11px;
    line-height: 16px;
  }

  & :deep(pre code) {
    border: none;
  }
}
</style>
