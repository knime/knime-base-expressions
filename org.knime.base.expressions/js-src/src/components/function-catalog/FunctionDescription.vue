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
@import url("webapps-common/ui/css/variables/spacings.css");

.markdown-function-desc {
  & h3 {
    line-height: 18px;
    font-size: 15px;

    &:first-child {
      margin-top: 0;
    }
  }

  & h4 {
    line-height: 16px;
    margin: var(--space-32) 0 var(--space-4);
    font-size: 13px;
  }

  & ul {
    margin: 0;
    padding-left: var(--space-16);
  }
}
</style>

<style scoped>
.node-description {
  height: 100%;
  padding-right: var(--space-8);
  padding-bottom: var(--space-8);
  overflow: auto;

  & .description {
    font-size: 13px;
    line-height: 16px;
  }

  /* Style refinement for Code */
  & :deep(pre),
  & :deep(code) {
    font-size: 11px;
    line-height: 16px;
  }

  & :deep(pre code) {
    border: none;
    white-space: pre;
    overflow: auto;
    display: block;
  }
}
</style>
