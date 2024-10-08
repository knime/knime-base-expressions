<script setup lang="ts">
import {
  consoleHandler,
  getScriptingService,
  type InputOutputModel,
  InputOutputPane,
  OutputTablePreview,
  ScriptingEditor,
  type SubItem,
  useReadonlyStore,
} from "@knime/scripting-editor";
import { getExpressionInitialDataService } from "@/expressionInitialDataService";
import type { ExpressionInitialData, ExpressionVersion } from "@/common/types";
import { LoadingIcon } from "@knime/components";
import { onMounted, ref, shallowRef } from "vue";
import FunctionCatalog from "@/components/function-catalog/FunctionCatalog.vue";
import registerKnimeExpressionLanguage from "../registerKnimeExpressionLanguage";
import { MIN_WIDTH_FUNCTION_CATALOG } from "@/components/function-catalog/contraints";
import MultiEditorContainer, {
  type EditorState,
  type EditorStates,
} from "@/components/MultiEditorContainer.vue";
import { runRowMapperDiagnostics } from "@/rowMapperApp/expressionRowMapperDiagnostics";
import { DEFAULT_NUMBER_OF_ROWS_TO_RUN, LANGUAGE } from "@/common/constants";
import {
  calculateInitialPaneSizes,
  mapConnectionInfoToErrorMessage,
} from "@/common/functions";
import RunButton from "@/components/RunButton.vue";
import {
  type ExpressionRowMapperNodeSettings,
  getRowMapperSettingsService,
} from "@/expressionSettingsService";
import { runOutputDiagnostics } from "@/generalDiagnostics";
import { type TypeRendererProps } from "@/components/TypeRenderer.vue";
import {
  buildAppendedOutput,
  replaceSubItems,
} from "@/common/inputOutputUtils";
import type {
  AllowedDropDownValue,
  SelectorState,
} from "@/components/OutputSelector.vue";

const initialData = shallowRef<ExpressionInitialData | null>(null);
const initialSettings = ref<ExpressionRowMapperNodeSettings | null>(null);
const runButtonDisabledErrorReason = ref<string | null>(null);
const multiEditorContainerRef =
  ref<InstanceType<typeof MultiEditorContainer>>();
const currentInputOutputItems = ref<InputOutputModel[]>();

const getInitialItems = (): InputOutputModel[] => {
  if (initialData.value === null) {
    return [];
  }
  return [
    ...initialData.value.inputObjects.map((inputItem) =>
      structuredClone(inputItem),
    ),
    structuredClone(initialData.value.flowVariables),
  ];
};

const refreshInputOutputItems = (
  { states, activeEditorKey }: EditorStates,
  returnTypes: string[],
) => {
  if (!activeEditorKey) {
    return;
  }

  const lastIndexToConsider =
    states.findIndex((state) => state.key === activeEditorKey) + 1;
  const statesUntilActiveWithReturnTypes = states
    .slice(0, lastIndexToConsider)
    .map((state, index) => ({
      selectorState: state.selectorState,
      key: state.key,
      returnType: returnTypes[index],
    }));

  const focusEditorActionBuilder = (editorKey: string) => () => {
    multiEditorContainerRef.value?.setActiveEditor(editorKey);
  };

  // copy the initial items [input, flow variables]
  currentInputOutputItems.value = getInitialItems();

  currentInputOutputItems.value[0].subItems =
    currentInputOutputItems.value[0].subItems?.map((subItem) =>
      replaceSubItems(
        subItem as SubItem<TypeRendererProps>,
        statesUntilActiveWithReturnTypes,
        focusEditorActionBuilder,
      ),
    );

  if (
    statesUntilActiveWithReturnTypes.some(
      (state) => state.selectorState.outputMode === "APPEND",
    )
  ) {
    currentInputOutputItems.value.push(
      buildAppendedOutput(
        statesUntilActiveWithReturnTypes,
        focusEditorActionBuilder,
        {
          name: "f(X) appended columns",
          portType: "table",
          subItemCodeAliasTemplate:
            currentInputOutputItems.value[0].subItemCodeAliasTemplate,
        },
      ),
    );
  }
};

const runDiagnosticsFunction = async ({
  states,
}: EditorStates): Promise<string[]> => {
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
    initialData.value?.inputObjects[0].subItems?.map((c) => c.name) ?? [],
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
  }

  const connectionErrors = mapConnectionInfoToErrorMessage(
    initialData.value?.inputConnectionInfo,
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

onMounted(async () => {
  [initialData.value, initialSettings.value] = await Promise.all([
    getExpressionInitialDataService().getInitialData(),
    getRowMapperSettingsService().getSettings(),
  ]);

  if (initialData.value?.inputConnectionInfo[1].status !== "OK") {
    consoleHandler.writeln({
      warning: "No input available. Connect an executed node.",
    });
  }

  currentInputOutputItems.value = getInitialItems();

  registerKnimeExpressionLanguage(initialData.value);

  useReadonlyStore().value =
    initialSettings.value.settingsAreOverriddenByFlowVariable ?? false;
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
      languageVersion: initialSettings.value!.languageVersion,
      builtinFunctionsVersion: initialSettings.value!.builtinFunctionsVersion,
      builtinAggregationsVersion:
        initialSettings.value!.builtinAggregationsVersion,
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

const initialPaneSizes = calculateInitialPaneSizes();
</script>

<template>
  <main>
    <template v-if="initialData === null || initialSettings === null">
      <div class="no-editors">
        <LoadingIcon />
      </div>
    </template>
    <template v-else>
      <ScriptingEditor
        :right-pane-minimum-width-in-pixel="MIN_WIDTH_FUNCTION_CATALOG"
        :show-control-bar="true"
        :language="LANGUAGE"
        :initial-pane-sizes="{
          right: initialPaneSizes.right,
          left: initialPaneSizes.left,
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
          <MultiEditorContainer
            ref="multiEditorContainerRef"
            item-type="column"
            :default-replacement-item="
              initialData?.inputObjects[0].subItems?.filter(
                (c) => c.supported,
              )[0].name ?? ''
            "
            :default-append-item="'New Column'"
            :settings="
              initialSettings.scripts.map((script, index) => ({
                initialScript: script,
                initialSelectorState: {
                  outputMode: initialSettings!.outputModes[index],
                  create: initialSettings!.createdColumns[index],
                  replace: initialSettings!.replacedColumns[index],
                } satisfies SelectorState,
              }))
            "
            :replaceable-items-in-input-table="
              initialData!.inputObjects[0].subItems
                ?.filter((c) => c.supported)
                .map(
                  (c): AllowedDropDownValue => ({
                    id: c.name,
                    text: c.name,
                  }),
                ) ?? []
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
    </template>
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
