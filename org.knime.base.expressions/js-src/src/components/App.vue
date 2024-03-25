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
import ColumnOutputSelector, {
  type ColumnSelectorState,
  type AllowedDropDownValue,
} from "@/components/ColumnOutputSelector.vue";

const MIN_WIDTH_FUNCTION_CATALOG = 280;

const scriptingService = getScriptingService();
const mainEditor = editor.useMainCodeEditorStore();

const allowedReplacementColumns = ref<AllowedDropDownValue[]>([]);
const columnSectorState = ref<ColumnSelectorState>();

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

  scriptingService.getInputObjects().then((result) => {
    if (result && result.length > 0 && result[0].subItems) {
      allowedReplacementColumns.value = result[0].subItems.map(
        (c: { name: string }) => {
          return { id: c.name, text: c.name };
        },
      );
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
      :right-pane-minimum-width-in-pixel="MIN_WIDTH_FUNCTION_CATALOG"
    >
      <template #code-editor-controls>
        <ColumnOutputSelector
          v-model="columnSectorState"
          default-output-mode="create"
          :allowed-replacement-columns="allowedReplacementColumns"
        />

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
