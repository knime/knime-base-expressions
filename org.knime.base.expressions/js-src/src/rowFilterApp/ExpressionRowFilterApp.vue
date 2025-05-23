<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { onKeyStroke } from "@vueuse/core";

import {
  OutputTablePreview,
  ScriptingEditor,
  consoleHandler,
  getScriptingService,
  getSettingsService,
  setActiveEditorStoreForAi,
  useReadonlyStore,
} from "@knime/scripting-editor";

import { DEFAULT_NUMBER_OF_ROWS_TO_RUN, LANGUAGE } from "@/common/constants";
import {
  mapConnectionInfoToErrorMessage,
  registerInsertionListener,
} from "@/common/functions";
import type { ExpressionVersion, RowFilterInitialData } from "@/common/types";
import ExpressionEditorPane, {
  type ExpressionEditorPaneExposes,
} from "@/components/ExpressionEditorPane.vue";
import RunButton from "@/components/RunButton.vue";
import FunctionCatalog from "@/components/function-catalog/FunctionCatalog.vue";
import { MIN_WIDTH_FUNCTION_CATALOG } from "@/components/function-catalog/contraints";
import { getRowFilterInitialDataService } from "@/expressionInitialDataService";
import {
  type ExpressionRowFilterNodeSettings,
  getRowFilterSettingsService,
} from "@/expressionSettingsService";
import type {
  EditorErrorState,
  ExpressionDiagnostic,
} from "@/generalDiagnostics";
import registerKnimeExpressionLanguage from "@/languageSupport/registerKnimeExpressionLanguage";
import { runRowFilterDiagnostics } from "@/rowFilterApp/expressionRowFilterDiagnostics";

const editorRef = ref<Required<ExpressionEditorPaneExposes> | null>(null);

const initialData = ref<RowFilterInitialData>();
const initialSettings = ref<ExpressionRowFilterNodeSettings>();
const errorState = ref<EditorErrorState>({ level: "OK" });

const runDiagnosticsFunction = async () => {
  const editorReference = editorRef.value;

  if (!editorReference) {
    return;
  }

  errorState.value = await runRowFilterDiagnostics(
    editorReference.getEditorState(),
  );
};

onMounted(async () => {
  [initialData.value, initialSettings.value] = await Promise.all([
    getRowFilterInitialDataService().getInitialData(),
    getRowFilterSettingsService().getSettings(),
  ]);

  if (initialData.value?.inputConnectionInfo[1].status !== "OK") {
    consoleHandler.writeln({
      warning: "No input available. Connect an executed node.",
    });
  }
  registerKnimeExpressionLanguage({
    columnGetter: () => initialData.value?.inputObjects[0].subItems ?? [],
    flowVariableGetter: () => initialData.value?.flowVariables.subItems ?? [],
    functionData: initialData.value?.functionCatalog.functions,
  });

  useReadonlyStore().value =
    initialSettings.value.settingsAreOverriddenByFlowVariable || false;

  const editorReference = editorRef.value;
  if (!editorReference) {
    return;
  }

  editorReference.getEditorState().setInitialText(initialSettings.value.script);
  editorReference.getEditorState().editor.value?.updateOptions({
    readOnly: useReadonlyStore().value,
    readOnlyMessage: {
      value: "Read-Only-Mode: configuration is set by flow variables.",
    },
    renderValidationDecorations: "on",
  });

  setActiveEditorStoreForAi(editorReference.getEditorState());

  getScriptingService().registerEventHandler(
    "updateWarning",
    (warning: ExpressionDiagnostic) => {
      if (warning) {
        errorState.value = {
          level: "WARNING",
          message: warning.message,
        };
        consoleHandler.writeln({
          warning: warning.message,
        });
      }
    },
  );

  const register = await getSettingsService().registerSettings("model");
  const onScriptChange = register(initialSettings.value.script);

  watch(editorReference.getEditorState().text, () => {
    runDiagnosticsFunction();
    onScriptChange.setValue(editorReference.getEditorState().text.value);
  });
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
  (): ExpressionRowFilterNodeSettings => {
    const expressionVersion: ExpressionVersion = {
      languageVersion: initialSettings.value!.languageVersion,
      builtinFunctionsVersion: initialSettings.value!.builtinFunctionsVersion,
      builtinAggregationsVersion:
        initialSettings.value!.builtinAggregationsVersion,
    };

    return {
      ...expressionVersion,
      script: editorRef.value?.getEditorState().text.value ?? "",
    };
  },
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
  const connectionErrors = mapConnectionInfoToErrorMessage(
    initialData.value?.inputConnectionInfo,
  );
  if (connectionErrors) {
    return connectionErrors;
  } else if (errorState.value.level === "ERROR") {
    return "To evaluate your expression, resolve existing errors first.";
  } else {
    return null;
  }
});
</script>

<template>
  <main>
    <ScriptingEditor
      :right-pane-minimum-width-in-pixel="MIN_WIDTH_FUNCTION_CATALOG"
      :show-control-bar="true"
      :language="LANGUAGE"
      :initial-pane-sizes="{
        right: 30,
        left: 20,
        bottom: 30,
      }"
      :additional-bottom-pane-tab-content="[
        {
          label: 'Output preview',
          slotName: 'bottomPaneTabSlot:outputPreview',
        },
      ]"
    >
      <!-- Extra content in the bottom tab pane -->
      <template #bottomPaneTabSlot:outputPreview="{ grabFocus }">
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
        <template v-if="initialData?.functionCatalog">
          <FunctionCatalog
            :function-catalog-data="initialData.functionCatalog"
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
