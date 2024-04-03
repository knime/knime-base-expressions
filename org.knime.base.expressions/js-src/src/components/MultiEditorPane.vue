<script setup lang="ts">
import { ref } from "vue";
import { onKeyStroke } from "@vueuse/core";
import { editor } from "@knime/scripting-editor";
import { editor as MonacoEditor } from "monaco-editor";

interface Props {
  language: string;
  fileName: string;
  title: string;
  showTitleBar?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  showTitleBar: true,
});

// Main editor
const editorContainer = ref<HTMLDivElement>();
const editorState = editor.useCodeEditor({
  language: props.language,
  fileName: props.fileName,
  container: editorContainer,
});

const getEditorState = () => editorState;
const insertText = (eventSource: string, text: string) => {
  const editor = editorState.editor.value;

  // Replaces anything highlighted if there is anything highlighted. Otherwise just insert at cursor
  const insertionOperation = {
    range: editor?.getSelection(),
    text,
    forceMoveMarkers: true,
  } as MonacoEditor.ISingleEditOperation;

  editor?.executeEdits(eventSource, [insertionOperation]);
};
defineExpose({ getEditorState, insertText });

onKeyStroke("Escape", () => {
  if (editorState.editor.value?.hasTextFocus()) {
    (document.activeElement as HTMLElement)?.blur();
  }
});

// register undo changes from outside the editor
onKeyStroke("z", (e) => {
  const key = navigator.userAgent.toLowerCase().includes("mac")
    ? e.metaKey
    : e.ctrlKey;

  // If we have multiple editors, only trigger undo if the editor has focus
  if (key && editorState.editor.value?.hasTextFocus()) {
    editorState.editor.value?.trigger("window", "undo", {});
  }
});
</script>

<template>
  <div class="editor-container">
    <div v-if="showTitleBar" class="editor-title-bar">
      {{ props.title }}
    </div>
    <div
      ref="editorContainer"
      :class="['code-editor', { 'has-title-bar': showTitleBar }]"
    />
  </div>
</template>

<style scoped>
.code-editor {
  height: calc(max(100%, var(--min-editor-height)));
}

.code-editor.has-title-bar {
  height: calc(max(100% - var(--title-bar-height), var(--min-editor-height)));
}

.editor-container {
  --title-bar-height: 40px;
  --min-editor-height: 70px;

  margin-bottom: 20px;
  margin-left: 20px;
  margin-right: 20px;
  box-shadow: 0 0 5px 5px var(--knime-silver-sand);
  height: calc(100% - 20px);
}

.editor-container:focus-within {
  box-shadow: 0 0 5px 5px var(--knime-cornflower);
}

/* Editor gets an extra margin iff it's the first one of its type. */

/* Basically gives us some nice margin collapsing. */
.editor-container:first-child {
  margin-top: 20px;
}

.editor-title-bar {
  height: var(--title-bar-height);
  padding-left: 20px;
  display: flex;
  align-items: center;
  background-color: var(--knime-porcelain);
}
</style>
