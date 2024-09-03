<script setup lang="ts">
import {
  consoleHandler,
  getScriptingService,
  OutputTablePreview,
  ScriptingEditor,
  setActiveEditorStoreForAi,
  useReadonlyStore,
} from "@knime/scripting-editor";
import { onKeyStroke } from "@vueuse/core";
import { computed, onMounted, ref, watch } from "vue";
import FunctionCatalog from "@/components/function-catalog/FunctionCatalog.vue";
import registerKnimeExpressionLanguage from "../registerKnimeExpressionLanguage";
import { MIN_WIDTH_FUNCTION_CATALOG } from "@/components/function-catalog/contraints";

import ExpressionEditorPane, {
  type ExpressionEditorPaneExposes,
} from "@/components/ExpressionEditorPane.vue";
import type { FunctionCatalogData } from "@/components/functionCatalogTypes";
import { runRowFilterDiagnostics } from "@/rowFilterApp/expressionRowFilterDiagnostics";
import { DEFAULT_NUMBER_OF_ROWS_TO_RUN, LANGUAGE } from "@/common/constants";
import {
  calculateInitialPaneSizes,
  registerInsertionListener,
} from "@/common/functions";
import RunButton from "@/components/RunButton.vue";
import type { ExpressionVersion, EditorErrorState } from "@/common/types";
import { getExpressionInitialDataService } from "@/expressionInitialDataService";
import {
  type ExpressionRowFilterNodeSettings,
  getRowFilterSettingsService,
} from "@/expressionSettingsService";

// Overwritten by the initial settings
const expressionVersion = ref<ExpressionVersion>({
  languageVersion: 0,
  builtinFunctionsVersion: 0,
  builtinAggregationsVersion: 0,
});

const editorRef = ref<Required<ExpressionEditorPaneExposes> | null>(null);

const functionCatalogData = ref<FunctionCatalogData>();
const inputsAvailable = ref(false);
const errorState = ref<EditorErrorState>({ level: "OK" });

const runDiagnosticsFunction = async () => {
  const editorReference = editorRef.value;

  if (!editorReference) {
    return;
  }

  const errorLevel = await runRowFilterDiagnostics(
    editorReference.getEditorState(),
  );

  if (errorLevel === "OK") {
    errorState.value = {
      level: errorLevel,
    };
  } else {
    errorState.value = {
      level: errorLevel,
      message: "A syntax error ocurred.",
    };
  }
};

onMounted(async () => {
  const [initialData, settings] = await Promise.all([
    getExpressionInitialDataService().getInitialData(),
    getRowFilterSettingsService().getSettings(),
  ]);

  const { inputsAvailable: availableInputs, functionCatalog } = initialData;

  expressionVersion.value = {
    languageVersion: settings.languageVersion,
    builtinFunctionsVersion: settings.builtinFunctionsVersion,
    builtinAggregationsVersion: settings.builtinAggregationsVersion,
  };

  inputsAvailable.value = availableInputs;
  if (!availableInputs) {
    consoleHandler.writeln({
      warning: "No input available. Connect an executed node.",
    });
  }

  functionCatalogData.value = functionCatalog;

  registerKnimeExpressionLanguage(initialData);

  useReadonlyStore().value =
    settings.settingsAreOverriddenByFlowVariable || false;

  const editorReference = editorRef.value;
  if (!editorReference) {
    return;
  }

  editorReference.getEditorState().setInitialText(settings.script);
  editorReference.getEditorState().editor.value?.updateOptions({
    readOnly: useReadonlyStore().value,
    readOnlyMessage: {
      value: "Read-Only-Mode: configuration is set by flow variables.",
    },
    renderValidationDecorations: "on",
  });

  watch(editorReference.getEditorState().text, runDiagnosticsFunction);

  setActiveEditorStoreForAi(editorReference.getEditorState());

  // Run initial diagnostics now that we've set the initial text
  await runDiagnosticsFunction();
});

const runRowFilterExpressions = (rows: number) => {
  const editorState = editorRef.value?.getEditorState();

  if (editorState && errorState.value.level !== "ERROR") {
    getScriptingService().sendToService("runRowFilterExpression", [
      editorState.text.value,
      rows,
    ]);
  }
};

getRowFilterSettingsService().registerSettingsGetterForApply(
  (): ExpressionRowFilterNodeSettings => ({
    ...expressionVersion.value,
    script: editorRef.value?.getEditorState().text.value ?? "",
  }),
);

registerInsertionListener(() => editorRef.value);

// Shift+Enter while editor has focus runs expressions
onKeyStroke("Enter", (evt: KeyboardEvent) => {
  const editorState = editorRef.value?.getEditorState();
  if (!editorState) {
    return;
  }

  if (evt.shiftKey && editorState.editor.value?.hasTextFocus()) {
    evt.preventDefault();
    runRowFilterExpressions(DEFAULT_NUMBER_OF_ROWS_TO_RUN);
  }
});

const runButtonDisabledErrorReason = computed(() => {
  if (!inputsAvailable.value) {
    return "To evaluate your expression, first connect an executed node.";
  } else if (errorState.value.level === "ERROR") {
    return "To evaluate your expression, first resolve syntax errors.";
  } else {
    return null;
  }
});

const initialPaneSizes = calculateInitialPaneSizes();
</script>

<template>
  <main>
    <ScriptingEditor
      :right-pane-minimum-width-in-pixel="MIN_WIDTH_FUNCTION_CATALOG"
      :show-control-bar="true"
      :language="LANGUAGE"
      :show-output-table="true"
      :initial-pane-sizes="{
        right: initialPaneSizes.right,
        left: initialPaneSizes.left,
        bottom: 30,
      }"
      :additional-bottom-pane-tab-content="[
        {
          label: 'Output preview',
          value: 'outputPreview',
        },
      ]"
    >
      <!-- Extra content in the bottom tab pane -->
      <template #outputPreview="{ grabFocus }">
        <OutputTablePreview @output-table-updated="grabFocus()" />
      </template>

      <template #editor>
        <ExpressionEditorPane
          ref="editorRef"
          :title="'Filter expression'"
          :file-name="'filterExpressionEditorFileName'"
          :language="LANGUAGE"
          :ordering-options="{
            isOnly: true,
            disableMultiEditorControls: true,
          }"
          :error-state="errorState"
        >
          <!-- Controls displayed once per editor -->
          <template #multi-editor-controls>
            <div class="editor-controls">
              All rows that match your filter expression are available through
              the output port.
            </div>
          </template>
        </ExpressionEditorPane>
      </template>

      <template #right-pane>
        <template v-if="functionCatalogData">
          <FunctionCatalog
            :function-catalog-data="functionCatalogData"
            :initially-expanded="false"
          />
        </template>
      </template>

      <!-- Controls displayed once only -->
      <template #code-editor-controls="{ showButtonText }">
        <RunButton
          :run-button-disabled-error-reason="runButtonDisabledErrorReason"
          :show-button-text="showButtonText"
          @run-expressions="runRowFilterExpressions"
        />
      </template>
    </ScriptingEditor>
  </main>
</template>

<style lang="postcss">
@import url("@knime/styles/css");
</style>

<style lang="postcss" scoped>
.editor-controls {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  font-family: Roboto, serif;
  font-size: 13px;
  font-weight: 400;
  line-height: 15.23px;
  text-align: center;
  height: 60px;
  padding: var(--space-8) var(--space-12);
  gap: 0;
}
</style>
