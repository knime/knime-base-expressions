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
const upperHeader = computed(() => {
  if (props.type === "category") {
    return props.data.metaCategory ?? "";
  } else {
    return props.data.name;
  }
});
const lowerHeader = computed(() => {
  if (props.type === "category") {
    return props.data.name;
  } else if (props.data.entryType === "function") {
    return `(${props.data.arguments.map((arg) => arg.name).join(", ")})`;
  } else {
    return "";
  }
});
</script>

<template>
  <div class="node-description">
    <div
      class="titles"
      :class="{
        'is-category': props.type === 'category',
        'is-function': props.type === 'functionOrConstant',
      }"
    >
      <h3 class="description-title">{{ upperHeader }}</h3>
      <h3 class="description-title">{{ lowerHeader }}</h3>
    </div>
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
</style>

<style lang="postcss" scoped>
.description-title {
  font-size: 16px;
  margin-bottom: 0;
  margin-top: 0;
  line-height: 18px;
  min-height: 18px;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
}

.titles {
  display: flex;
  justify-content: space-between;
  flex-direction: column;
  margin-bottom: 10px;

  &.is-function {
    & .description-title:last-child {
      white-space: wrap;
      word-break: break-word;
      font-weight: 300;
      font-size: 13px;
    }
  }

  &.is-category {
    & .description-title:first-child {
      font-weight: 300;
      font-size: 13px;
    }
  }
}
</style>
