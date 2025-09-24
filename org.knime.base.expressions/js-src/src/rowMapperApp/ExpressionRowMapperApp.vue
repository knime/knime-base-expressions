<script setup lang="ts">
import { onMounted, ref } from "vue";

import {
  type InputOutputModel,
  InputOutputPane,
  OutputTablePreview,
  ScriptingEditor,
  type SubItem,
  type SubItemType,
  consoleHandler,
  getScriptingService,
  useReadonlyStore,
} from "@knime/scripting-editor";

import {
  DEFAULT_NUMBER_OF_ROWS_TO_RUN,
  INITIAL_PANE_SIZES,
  LANGUAGE,
  NO_DATA_TEXT,
} from "@/common/constants";
import { mapConnectionInfoToErrorMessage } from "@/common/functions";
import {
  buildAppendedOutput,
  replaceSubItems,
} from "@/common/inputOutputUtils";
import type { ExpressionVersion } from "@/common/types";
import type { IconRendererProps } from "@/components/IconRenderer.vue";
import MultiEditorContainer, {
  type EditorState,
  type EditorStates,
} from "@/components/MultiEditorContainer.vue";
import type { SelectorState } from "@/components/OutputSelector.vue";
import RunButton from "@/components/RunButton.vue";
import FunctionCatalog from "@/components/function-catalog/FunctionCatalog.vue";
import { getRowMapperInitialDataService } from "@/expressionInitialDataService";
import {
  type ExpressionRowMapperNodeSettings,
  getRowMapperSettingsService,
} from "@/expressionSettingsService";
import { runOutputDiagnostics } from "@/generalDiagnostics";
import registerKnimeExpressionLanguage from "@/languageSupport/registerKnimeExpressionLanguage";
import { runRowMapperDiagnostics } from "@/rowMapperApp/expressionRowMapperDiagnostics";

const initialData = getRowMapperInitialDataService().getInitialData();
const initialSettings = getRowMapperSettingsService().getSettings();
const runButtonDisabledErrorReason = ref<string | null>(null);
const multiEditorContainerRef =
  ref<InstanceType<typeof MultiEditorContainer>>();
const currentInputOutputItems = ref<InputOutputModel[]>();
const appendedSubItems = ref<SubItem<Record<string, any>>[]>([]);

const getInitialItems = (): InputOutputModel[] => {
  return [
    ...initialData.inputObjects.map((inputItem) => structuredClone(inputItem)),
    structuredClone(initialData.flowVariables),
  ];
};

const refreshInputOutputItems = (
  { states, activeEditorKey }: EditorStates,
  returnTypes: SubItemType[],
) => {
  if (!activeEditorKey) {
    return;
  }

  currentInputOutputItems.value = getInitialItems();

  const lastIndexToConsider = states.findIndex(
    (state) => state.key === activeEditorKey,
  );

  if (lastIndexToConsider === -1) {
    return;
  }

  const statesUntilActiveWithReturnTypes = states
    .slice(0, lastIndexToConsider)
    .map((state, index) => ({
      selectorState: state.selectorState,
      key: state.key,
      returnType: returnTypes[index],
    }));

  const focusEditorActionBuilder = (editorKey: string) => () => {
    multiEditorContainerRef.value?.focusEditor(editorKey);
  };

  currentInputOutputItems.value[0].subItems =
    currentInputOutputItems.value[0].subItems?.map((subItem) =>
      replaceSubItems(
        subItem as SubItem<IconRendererProps>,
        statesUntilActiveWithReturnTypes,
        focusEditorActionBuilder,
      ),
    );

  if (
    statesUntilActiveWithReturnTypes.some(
      (state) => state.selectorState.outputMode === "APPEND",
    )
  ) {
    const appendedInputOutputItem = buildAppendedOutput(
      statesUntilActiveWithReturnTypes,
      focusEditorActionBuilder,
      {
        name: "appended columns",
        portType: "table",
        subItemCodeAliasTemplate:
          currentInputOutputItems.value[0].subItemCodeAliasTemplate,
      },
    );
    appendedSubItems.value = appendedInputOutputItem.subItems ?? [];
    currentInputOutputItems.value.push(appendedInputOutputItem);
  } else {
    appendedSubItems.value = [];
  }
};

const runDiagnosticsFunction = async ({
  states,
}: EditorStates): Promise<SubItemType[]> => {
  const diagnostics = await runRowMapperDiagnostics(
    states.map((state) => state.monacoState),
    states.map((state) =>
      state.selectorState.outputMode === "APPEND"
        ? state.selectorState.create
        : state.selectorState.replace,
    ),
  );

  const columnErrorMessages = runOutputDiagnostics(
    "column",
    states.map((state) => state.selectorState),
    initialData.inputObjects[0].subItems?.map((c) => c.name) ?? [],
  );

  for (const [index, state] of states.entries()) {
    if (columnErrorMessages[index] === null) {
      multiEditorContainerRef.value?.setSelectorErrorState(state.key, {
        level: "OK",
      });
    } else {
      multiEditorContainerRef.value?.setSelectorErrorState(state.key, {
        level: "ERROR",
        message: columnErrorMessages[index],
      });
    }

    multiEditorContainerRef.value?.setEditorErrorState(
      state.key,
      diagnostics[index].errorState,
    );

    multiEditorContainerRef.value?.setCurrentExpressionReturnType(
      state.key,
      diagnostics[index].returnType,
    );
  }

  const connectionErrors = mapConnectionInfoToErrorMessage(
    initialData.inputConnectionInfo,
  );
  const haveColumnErrors = columnErrorMessages.some((error) => error !== null);
  const haveSyntaxErrors = diagnostics.some(
    ({ errorState }) => errorState.level === "ERROR",
  );

  if (connectionErrors) {
    runButtonDisabledErrorReason.value = connectionErrors;
  } else if (haveSyntaxErrors) {
    runButtonDisabledErrorReason.value =
      "To evaluate your expression, resolve existing errors first.";
  } else if (haveColumnErrors) {
    runButtonDisabledErrorReason.value =
      "To evaluate your expression, first resolve column output errors.";
  } else {
    runButtonDisabledErrorReason.value = null;
  }

  return diagnostics.map(({ returnType }) => returnType);
};

const onChange = async (editorStates: EditorStates) => {
  const returnTypes = await runDiagnosticsFunction(editorStates);
  refreshInputOutputItems(editorStates, returnTypes);
};

onMounted(() => {
  if (initialData.inputConnectionInfo[1].status !== "OK") {
    consoleHandler.writeln({
      warning: "No input available. Connect an executed node.",
    });
  }

  currentInputOutputItems.value = getInitialItems();

  registerKnimeExpressionLanguage({
    columnGetter: () => [
      ...(currentInputOutputItems.value?.[0].subItems ?? []),
      ...appendedSubItems.value,
    ],
    flowVariableGetter: () => initialData.flowVariables.subItems ?? [],
    functionData: initialData.functionCatalog.functions,
  });

  useReadonlyStore().value =
    initialSettings.settingsAreOverriddenByFlowVariable ?? false;
});

const runRowMapperExpressions = (rows: number, editorStates: EditorState[]) => {
  getScriptingService().sendToService("runExpression", [
    editorStates.map((state) => state.monacoState.text.value),
    rows,
    editorStates.map((state) => state.selectorState.outputMode),
    editorStates.map((state) =>
      state.selectorState.outputMode === "APPEND"
        ? state.selectorState.create
        : state.selectorState.replace,
    ),
  ]);
};

getRowMapperSettingsService().registerSettingsGetterForApply(
  (): ExpressionRowMapperNodeSettings => {
    const orderedEditorStates =
      multiEditorContainerRef.value!.getOrderedEditorStates();

    const expressionVersion: ExpressionVersion = {
      languageVersion: initialSettings.languageVersion,
      builtinFunctionsVersion: initialSettings.builtinFunctionsVersion,
      builtinAggregationsVersion: initialSettings.builtinAggregationsVersion,
    };

    return {
      ...expressionVersion,
      outputModes: orderedEditorStates.map(
        (state) => state.selectorState.outputMode,
      ),
      createdColumns: orderedEditorStates.map(
        (state) => state.selectorState.create,
      ),
      replacedColumns: orderedEditorStates.map(
        (state) => state.selectorState.replace,
      ),
      scripts: orderedEditorStates.map((state) => state.monacoState.text.value),
    };
  },
);
</script>

<template>
  <main>
    <ScriptingEditor
      :show-control-bar="true"
      :language="LANGUAGE"
      :initial-pane-sizes="INITIAL_PANE_SIZES"
      :additional-bottom-pane-tab-content="[
        {
          label: 'Output preview',
          slotName: 'bottomPaneTabSlot:outputPreview',
        },
      ]"
    >
      <!-- Extra content in the bottom tab pane -->
      <template #bottomPaneTabSlot:outputPreview="{ grabFocus }">
        <OutputTablePreview
          :no-data-text="NO_DATA_TEXT"
          @output-table-updated="grabFocus()"
        />
      </template>

      <template #editor>
        <MultiEditorContainer
          ref="multiEditorContainerRef"
          item-type="column"
          :default-append-item="'New Column'"
          :settings="
            initialSettings.scripts.map((script, index) => ({
              initialScript: script,
              initialSelectorState: {
                outputMode: initialSettings.outputModes[index],
                create: initialSettings.createdColumns[index],
                replace: initialSettings.replacedColumns[index],
              } satisfies SelectorState,
            }))
          "
          :replaceable-items-in-input-table="
            initialData?.columnNamesAndTypes ?? []
          "
          @on-change="onChange"
          @run-expressions="
            (states) =>
              runRowMapperExpressions(DEFAULT_NUMBER_OF_ROWS_TO_RUN, states)
          "
        />
      </template>

      <template #left-pane>
        <InputOutputPane :input-output-items="currentInputOutputItems" />
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
          @run-expressions="
            (numRows) =>
              runRowMapperExpressions(
                numRows,
                multiEditorContainerRef!.getOrderedEditorStates(),
              )
          "
        />
      </template>
    </ScriptingEditor>
  </main>
</template>

<style lang="postcss">
@import url("@knime/styles/css");
</style>

<style lang="postcss" scoped>
.no-editors {
  position: absolute;
  inset: calc(50% - 25px);
  width: 50px;
  height: 50px;
}
</style>
