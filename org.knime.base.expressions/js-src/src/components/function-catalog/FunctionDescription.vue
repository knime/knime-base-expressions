<script setup lang="ts">
import MarkdownIt from "markdown-it";
import type { FunctionData } from "@/components/functionCatalogTypes";
import { computed } from "vue";

// https://stackoverflow.com/questions/66514253/ignore-specified-html-blocks-in-markdown-it
// html rendering is disabled by default
const markdown = new MarkdownIt();

const props = defineProps<{
  functionData: FunctionData;
}>();

const markdownHTMLContent = computed(() =>
  markdown.render(props.functionData.description),
);
</script>

<template>
  <div class="function-description-panel">
    <h4 class="function-header">{{ functionData.name }}</h4>
    <h5 class="function-subtitle">Arguments:</h5>
    <ul class="function-arg-list has-left-indent">
      <li v-for="arg in functionData.arguments" :key="arg.name">
        <span style="font-weight: bold; padding-left: -20px">{{
          arg.name
        }}</span
        >:
        {{ arg.description }}
      </li>
    </ul>
    <div style="margin-top: 10px">
      <h5 class="function-subtitle" style="display: inline">Returns:</h5>
      <span class="return-type">{{ functionData.returnType }}</span>
    </div>
    <div
      v-if="functionData.returnDescription"
      class="markdown-function-desc has-left-indent"
    >
      {{ functionData.returnDescription }}
    </div>
    <h5 class="function-subtitle">Description:</h5>
    <!-- eslint-disable vue/no-v-html -->
    <div
      class="markdown-function-desc has-left-indent"
      v-html="markdownHTMLContent"
    />
  </div>
</template>

<style>
.markdown-function-desc h1,
.markdown-function-desc h2,
.markdown-function-desc h3,
.markdown-function-desc h4,
.markdown-function-desc h5 {
  font-size: small;
  line-height: normal;
}

.markdown-function-desc h6 {
  font-size: small;
  margin-top: 0;
}

.markdown-function-desc p {
  font-size: small;
  margin-top: 0;
}

.markdown-function-desc {
  padding-top: 5px;
  overflow: auto;
  margin: 0 auto;
  padding-left: 0;
}
</style>

<style scoped>
.function-header {
  font-size: x-large;
  margin: 0 auto auto 0;
  text-align: left;
}

.function-subtitle {
  font-size: medium;
  margin-bottom: 0;
  margin-top: 10px;
}

.has-left-indent {
  border-left: 5px solid var(--knime-silver-sand-semi);
  padding-left: 25px;
}

.return-type {
  font-size: small;
  margin-left: 1em;
}

.function-description-panel {
  font-size: small;
}

.function-description-panel ul {
  margin: 0 auto;
  list-style-type: none;
}
</style>
