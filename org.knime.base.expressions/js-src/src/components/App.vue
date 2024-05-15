<script setup lang="ts">
import {
  consoleHandler,
  ScriptingEditor,
  setActiveEditorStoreForAi,
} from "@knime/scripting-editor";
import { getExpressionScriptingService } from "@/expressionScriptingService";
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
import MultiEditorPane, {
  type MultiEditorPaneExposes,
} from "./MultiEditorPane.vue";
import type { FunctionCatalogData } from "./functionCatalogTypes";
import { useStore } from "@/store";

import registerKnimeExpressionLanguage from "../registerKnimeExpressionLanguage";

// Register language - but bear in mind we'll have to re-register it once we have
// column names, so that we can add autocompletion of columns.
const language = "knime-expression";

const MIN_WIDTH_FUNCTION_CATALOG = 280;

const scriptingService = getExpressionScriptingService();
const store = useStore();

const allowedReplacementColumns = ref<AllowedDropDownValue[]>([]);

// These should be immediately overridden by the scripting service
const columnSectorState = ref<ColumnSelectorState>({
  outputMode: "APPEND",
  createColumn: "",
  replaceColumn: "",
});

const multiEditorComponentRef = ref<MultiEditorPaneExposes | null>(null);

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

  const functionCatalogPromise: Promise<FunctionCatalogData> = scriptingService
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

    columnSectorState.value = {
      outputMode: settings.columnOutputMode,
      createColumn: settings.createdColumn,
      replaceColumn: settings.replacedColumn,
    };
  });

  setActiveEditorStoreForAi(multiEditorComponentRef.value?.getEditorState());
});

const runExpressions = () => {
  // TODO make this work with multiple editors
  if (store.expressionValid) {
    scriptingService.sendToService("runExpression", [
      multiEditorComponentRef.value?.getEditorState().text.value,
    ]);
  }
};

scriptingService.registerSettingsGetterForApply(() => {
  return {
    script: multiEditorComponentRef.value?.getEditorState().text.value ?? "",
    columnOutputMode: columnSectorState.value?.outputMode,
    createdColumn: columnSectorState.value?.createColumn,
    replacedColumn: columnSectorState.value?.replaceColumn,
  };
});

const onFunctionInsertionTriggered = (payload: {
  eventSource: string;
  text: string;
}) => {
  multiEditorComponentRef.value?.getEditorState().insertText(payload.text);
};

const onInputOutputItemInsertionTriggered = (codeToInsert: string) => {
  // Note that we're ignoring requiredImport, because the expression editor
  // doesn't need imports.
  multiEditorComponentRef.value
    ?.getEditorState()
    .insertColumnReference(codeToInsert);
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
      @input-output-item-insertion="onInputOutputItemInsertionTriggered"
    >
      <template #editor>
        <MultiEditorPane
          ref="multiEditorComponentRef"
          title="Expression Editor"
          file-name="_1.knexp"
          :language="language"
        >
          <!-- Controls displayed once per editor -->
          <template #multi-editor-controls>
            <div class="editor-controls">
              <ColumnOutputSelector
                v-model="columnSectorState"
                :allowed-replacement-columns="allowedReplacementColumns"
              />
            </div>
          </template>
        </MultiEditorPane>
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
      <!-- Controls for the very bottom bar -->
      <template #code-editor-controls>
        <Button
          primary
          compact
          :disabled="!inputsAvailable || !store.expressionValid"
          @click="runExpressions"
        >
          <PlayIcon /> Run
        </Button>
      </template>
    </ScriptingEditor>
  </main>
</template>

<style lang="postcss">
@import url("webapps-common/ui/css");

.editor-controls {
  width: 100%;
  display: flex;
  justify-content: space-between;
  padding: 1px 10px;
  height: fit-content;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
