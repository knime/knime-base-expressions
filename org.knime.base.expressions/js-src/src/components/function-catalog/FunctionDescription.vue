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
    font-size: 15px;

    &:first-child {
      margin-top: 0;
      margin-bottom: -12px;
    }
  }

  & h4 {
    line-height: 16px;
    margin: 0;
    margin-top: 32px;
    margin-bottom: 4px;
    font-size: 13px;
  }

  & ul {
    margin: 0;
    padding-left: 20px;
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
    line-height: 16px;
  }

  /* Style refinement for Code */
  & :deep(pre),
  & :deep(code) {
    font-size: 11px;
    line-height: 16px;
  }

  & pre {
    overflow-y: scroll;
    text-wrap: nowrap;
  }

  & :deep(pre code) {
    border: none;
    overflow: hidden scroll;
  }
}
</style>
