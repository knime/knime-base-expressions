<script setup lang="ts">
import {
  ScriptingEditor,
  getScriptingService,
  editor,
  consoleHandler,
} from "@knime/scripting-editor";
import Button from "webapps-common/ui/components/Button.vue";
import PlayIcon from "webapps-common/ui/assets/img/icons/play.svg";
import { onMounted, ref } from "vue";

const scriptingService = getScriptingService();
const mainEditor = editor.useMainCodeEditorStore();

const saveSettings = async (settings: any) => {
  await scriptingService.saveSettings(settings);
  scriptingService.closeDialog();
};

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

// TODO(language-features) register knime expression language
</script>

<template>
  <main>
    <ScriptingEditor
      :title="`Expression (Labs)`"
      language="knime-expression"
      file-name="main.knexp"
      @save-settings="saveSettings"
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
    </ScriptingEditor>
  </main>
</template>

<style>
@import url("webapps-common/ui/css");
</style>
