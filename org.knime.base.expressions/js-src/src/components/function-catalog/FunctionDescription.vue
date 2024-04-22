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
        {{ arg.name }}: {{ arg.description }}
      </li>
    </ul>
    <h5 class="function-subtitle">Returns:</h5>
    <div class="has-left-indent">
      {{ functionData.returnType }}
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
  padding-top: 0;
  overflow: hidden;
  margin: 0 auto;
  padding-left: 20px;
}
</style>

<style scoped>
.function-header {
  font-size: x-large;
  margin: 0 auto;
  text-align: center;
}

.function-subtitle {
  font-size: medium;
  margin-bottom: 0;
  margin-top: 10px;
}

.has-left-indent {
  border-left: 5px solid var(--knime-silver-sand-semi);
  padding-left: 25px;
  margin-left: 5px;
}

.function-description-panel {
  font-size: small;
}

.function-description-panel ul {
  margin: 0 auto;
}
</style>
