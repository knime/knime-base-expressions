<script setup lang="ts">
import {
  consoleHandler,
  getScriptingService,
  ScriptingEditor,
} from "@knime/scripting-editor";
import Button from "webapps-common/ui/components/Button.vue";
import PlayIcon from "webapps-common/ui/assets/img/icons/play.svg";
import { onMounted, ref } from "vue";
import FunctionCatalog from "@/components/function-catalog/FunctionCatalog.vue";
import ColumnOutputSelector, {
  type ColumnSelectorState,
  type AllowedDropDownValue,
} from "@/components/ColumnOutputSelector.vue";
import { onKeyStroke } from "@vueuse/core";
import * as monaco from "monaco-editor";

import registerKnimeExpressionLanguage from "../registerKnimeExpressionLanguage";

// Register language - but bear in mind we'll have to re-register it once we have
// column names, so that we can add autocompletion of columns.
const language = "knime-expression";

const MIN_WIDTH_FUNCTION_CATALOG = 280;
import MultiEditorPane from "./MultiEditorPane.vue";
import type { FunctionCatalogData } from "./functionCatalogTypes";
import { useStore } from "@/store";

const scriptingService = getScriptingService();
const store = useStore();

const allowedReplacementColumns = ref<AllowedDropDownValue[]>([]);
const columnSectorState = ref<ColumnSelectorState>();

const multiEditorComponentRef = ref<typeof MultiEditorPane | null>(null);

const functionCatalogData = ref<FunctionCatalogData>();

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

  const functionCatalogPromise: Promise<FunctionCatalogData> =
    getScriptingService()
      .sendToService("getFunctionCatalog")
      .then((data) => {
        functionCatalogData.value = data;
        return data;
      });

  Promise.all([
    scriptingService.getInputObjects(),
    scriptingService.getFlowVariableInputs(),
    functionCatalogPromise,
  ]).then((result) => {
    registerKnimeExpressionLanguage({
      columnNamesForCompletion: result[0]?.[0]?.subItems
        ? result[0][0].subItems.map((c) => ({ name: c.name, type: c.type }))
        : [],
      flowVariableNamesForCompletion: result[1]?.subItems
        ? result[1].subItems.map((c) => ({ name: c.name, type: c.type }))
        : [],
      extraCompletionItems: result[2].functions.map((f) => ({
        text: `${f.name}(${Array(f.arguments.length).join(", ")})`,
        kind: monaco.languages.CompletionItemKind.Function,
        extraDetailForMonaco: { documentation: { value: f.description } },
      })),
      languageName: language,
    });
  });

  scriptingService.getInitialSettings().then((settings) => {
    multiEditorComponentRef.value
      ?.getEditorState()
      .setInitialText(settings.script);
  });
});

const runExpressions = () => {
  // TODO make this work with multiple editors
  if (store.expressionValid) {
    scriptingService.sendToService("runExpression", [
      multiEditorComponentRef.value?.getEditorState().text.value,
    ]);
  }
};

getScriptingService().registerSettingsGetterForApply(() => {
  return {
    script: multiEditorComponentRef.value?.getEditorState().text.value,
  };
});

const onFunctionInsertionTriggered = (payload: {
  eventSource: string;
  text: string;
}) => {
  multiEditorComponentRef.value?.insertText(payload.eventSource, payload.text);
};

// Shift+Enter while editor has focus runs expressions
onKeyStroke("Enter", (evt: KeyboardEvent) => {
  if (
    evt.shiftKey &&
    multiEditorComponentRef.value?.getEditorState().editor.value?.hasTextFocus()
  ) {
    evt.preventDefault();
    runExpressions();
  }
});
</script>

<template>
  <main>
    <ScriptingEditor
      title="Expression (Labs)"
      :right-pane-minimum-width-in-pixel="MIN_WIDTH_FUNCTION_CATALOG"
      :show-control-bar="true"
      :language="language"
    >
      <template #editor>
        <MultiEditorPane
          ref="multiEditorComponentRef"
          title="Expression Editor"
          file-name="_1.knexp"
          :language="language"
        />
      </template>

      <template #code-editor-controls>
        <ColumnOutputSelector
          v-model="columnSectorState"
          default-output-mode="create"
          :allowed-replacement-columns="allowedReplacementColumns"
        />

        <Button
          primary
          compact
          :disabled="!inputsAvailable || !store.expressionValid"
          @click="runExpressions"
          ><PlayIcon /> Run</Button
        >
      </template>
      <template #right-pane>
        <template v-if="functionCatalogData">
          <FunctionCatalog
            :function-catalog-data="functionCatalogData"
            :initially-expanded="true"
            @function-insertion-event="onFunctionInsertionTriggered"
          />
        </template>
      </template>
    </ScriptingEditor>
  </main>
</template>

<style>
@import url("webapps-common/ui/css");
</style>
