<script setup lang="ts">
import {
  consoleHandler,
  editor,
  getScriptingService,
  ScriptingEditor,
} from "@knime/scripting-editor";
import Button from "webapps-common/ui/components/Button.vue";
import PlayIcon from "webapps-common/ui/assets/img/icons/play.svg";
import { onMounted, ref } from "vue";
import { getFunctionCatalogData } from "@/components/mockFunctionCatalogData";
import FunctionCatalog from "@/components/function-catalog/FunctionCatalog.vue";

const scriptingService = getScriptingService();
const mainEditor = editor.useMainCodeEditorStore();

// Run button

const inputsAvailable = ref(false);
onMounted(() => {
  scriptingService.inputsAvailable().then((result) => {
    inputsAvailable.value = result;
    if (!result) {
      consoleHandler.writeln({
        warning: "No input available. Connect an executed node.",
      });
    }
  });
});

const runExpression = () => {
  scriptingService.sendToService("runExpression", [
    mainEditor.value?.text.value,
  ]);
};

const functionCatalogData = getFunctionCatalogData();

// TODO(language-features) register knime expression language
</script>

<template>
  <main>
    <ScriptingEditor
      :title="`Expression (Labs)`"
      language="knime-expression"
      file-name="main.knexp"
      :right-pane-minimum-width-in-pixel="280"
    >
      <template #code-editor-controls>
        <Button
          primary
          compact
          :disabled="!inputsAvailable"
          @click="runExpression"
          ><PlayIcon /> Run</Button
        >
      </template>
      <template #right-pane>
        <FunctionCatalog
          :function-catalog-data="functionCatalogData"
          :initially-expanded="true"
        />
      </template>
    </ScriptingEditor>
  </main>
</template>

<style>
@import url("webapps-common/ui/css");
</style>
