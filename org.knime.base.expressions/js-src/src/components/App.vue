<script setup lang="ts">
import {
  consoleHandler,
  ScriptingEditor,
  setActiveEditorStoreForAi,
  MIN_WIDTH_FOR_DISPLAYING_LEFT_PANE,
} from "@knime/scripting-editor";
import {
  type ExpressionVersion,
  type ExpressionNodeSettings,
  getExpressionScriptingService,
} from "@/expressionScriptingService";
import Button from "webapps-common/ui/components/Button.vue";
import PlayIcon from "webapps-common/ui/assets/img/icons/play.svg";
import SplitButton from "webapps-common/ui/components/SplitButton.vue";
import DropdownIcon from "webapps-common/ui/assets/img/icons/arrow-dropdown.svg";
import SubMenu from "webapps-common/ui/components/SubMenu.vue";
import Tooltip from "webapps-common/ui/components/Tooltip.vue";
import LoadingIcon from "webapps-common/ui/components/LoadingIcon.vue";
import { useWindowSize, onKeyStroke } from "@vueuse/core";
import {
  computed,
  onMounted,
  ref,
  reactive,
  nextTick,
  watch,
  type ComponentPublicInstance,
} from "vue";
import FunctionCatalog from "@/components/function-catalog/FunctionCatalog.vue";
import ColumnOutputSelector, {
  type AllowedDropDownValue,
  type ColumnSelectorState,
} from "@/components/ColumnOutputSelector.vue";
import ExpressionEditorPane, {
  type ExpressionEditorPaneExposes,
} from "./ExpressionEditorPane.vue";
import type { FunctionCatalogData } from "./functionCatalogTypes";
import { useStore } from "@/store";
import { runDiagnostics } from "@/expressionDiagnostics";
import registerKnimeExpressionLanguage from "../registerKnimeExpressionLanguage";
import {
  COMBINED_SPLITTER_WIDTH,
  MIN_WIDTH_FOR_DISPLAYING_DESCRIPTION,
  MIN_WIDTH_FUNCTION_CATALOG,
  SWITCH_TO_SMALL_DESCRIPTION,
  WIDTH_OF_INPUT_OUTPUT_PANE,
} from "@/components/function-catalog/contraints";
import { convertFunctionsToInsertionItems } from "./convertFunctionsToInsertionItems";
import * as monaco from "monaco-editor";

const language = "knime-expression";

const DEFAULT_NUMBER_OF_ROWS_TO_RUN = 10;

const scriptingService = getExpressionScriptingService();
const store = useStore();

// Populated by the initial settings
const columnSelectorStates = reactive<{ [key: string]: ColumnSelectorState }>(
  {},
);

const generateNewKey = () => {
  // JavaScript doesn't have a nice way to generate a random UUID so for now,
  // just bodge something random.
  // TODO: Replace with a proper UUID generator.
  return Math.random().toString(36).substring(2);
};

// Overwritten by the initial settings
const expressionVersion = ref<ExpressionVersion>({
  languageVersion: 0,
  builtinFunctionsVersion: 0,
  builtinAggregationsVersion: 0,
});

const multiEditorComponentRefs = reactive<{
  [key: string]: ExpressionEditorPaneExposes;
}>({});

const columnStateWatchers = reactive<{ [key: string]: Function }>({});
const editorStateWatchers = reactive<{ [key: string]: Function }>({});

// The canonical source of ordering truth, used to index the columnSelectorStates
// and the multiEditorComponentRefs. Things are run in the order defined here,
// saved in the order defined here, etc.
const orderedEditorKeys = reactive<string[]>([]);

const numberOfEditors = computed(() => orderedEditorKeys.length);

/** Called by components when they're created, stores a ref to the component in our dict */
const createElementReference = (title: string) => {
  return (el: Element | ComponentPublicInstance | null) => {
    multiEditorComponentRefs[title] =
      el as unknown as ExpressionEditorPaneExposes;
  };
};

// Input columns helpers
const columnsInInputTable = ref<AllowedDropDownValue[]>([]);
const getAvailableColumnsForReplacement = (
  key: string,
): AllowedDropDownValue[] => {
  const index = orderedEditorKeys.indexOf(key);

  const columnsFromPreviousEditors = orderedEditorKeys
    .slice(0, index)
    .filter((key) => columnSelectorStates[key].outputMode === "APPEND")
    .map((key) => columnSelectorStates[key].createColumn)
    .map((column) => {
      return { id: column, text: column };
    });

  return [...columnsInInputTable.value, ...columnsFromPreviousEditors];
};

const getActiveEditor = (): ExpressionEditorPaneExposes | null => {
  if (!store.activeEditorFileName) {
    return null;
  }

  return multiEditorComponentRefs[store.activeEditorFileName];
};

const onEditorFocused = (filename: string) => {
  store.activeEditorFileName = filename;
  setActiveEditorStoreForAi(
    multiEditorComponentRefs[filename]?.getEditorState(),
  );

  // Scroll to the editor. The focusing somehow interferes with scrolling,
  // so wait for a tick first.
  nextTick().then(() => {
    multiEditorComponentRefs[filename]
      ?.getEditorState()
      .editor.value?.getDomNode()
      ?.scrollIntoView();
  });
};

const functionCatalogData = ref<FunctionCatalogData>();
const inputsAvailable = ref(false);

const getFirstEditor = (): ExpressionEditorPaneExposes => {
  return multiEditorComponentRefs[orderedEditorKeys[0]];
};

const runDiagnosticsFunction = () => {
  runDiagnostics(
    orderedEditorKeys
      .map((key) => multiEditorComponentRefs[key])
      .map((editor) => editor.getEditorState()),
    orderedEditorKeys
      .map((key) => columnSelectorStates[key])
      .map((state) =>
        state.outputMode === "APPEND" ? state.createColumn : null,
      ),
  );
};

onMounted(async () => {
  const [
    initialSettings,
    availableInputs,
    inputObjects,
    flowVariableInputs,
    functionCatalog,
  ] = await Promise.all([
    scriptingService.getInitialSettings(),
    scriptingService.inputsAvailable(),
    scriptingService.getInputObjects(),
    scriptingService.getFlowVariableInputs(),
    scriptingService.getFunctions(),
  ]);

  expressionVersion.value = {
    languageVersion: initialSettings.languageVersion,
    builtinFunctionsVersion: initialSettings.builtinFunctionsVersion,
    builtinAggregationsVersion: initialSettings.builtinAggregationsVersion,
  };

  inputsAvailable.value = availableInputs;
  if (!availableInputs) {
    consoleHandler.writeln({
      warning: "No input available. Connect an executed node.",
    });
  }

  functionCatalogData.value = functionCatalog;

  if (inputObjects && inputObjects.length > 0 && inputObjects[0].subItems) {
    columnsInInputTable.value = inputObjects[0]?.subItems?.map(
      (c: { name: string }) => {
        return { id: c.name, text: c.name };
      },
    );
  }

  registerKnimeExpressionLanguage({
    columnNamesForCompletion: inputObjects?.[0]?.subItems
      ? inputObjects[0].subItems.map((column) => ({
          name: column.name,
          type: column.type,
        }))
      : [],
    flowVariableNamesForCompletion: flowVariableInputs?.subItems
      ? flowVariableInputs.subItems.map((flowVariable) => ({
          name: flowVariable.name,
          type: flowVariable.type,
        }))
      : [],
    extraCompletionItems: [
      ...convertFunctionsToInsertionItems(functionCatalog.functions),
      ...["$[ROW_ID]", "$[ROW_INDEX]", "$[ROW_NUMBER]"].map((item) => ({
        text: item,
        kind: monaco.languages.CompletionItemKind.Variable,
        extraDetailForMonaco: {
          detail: "Type: INTEGER",
        },
      })),
    ],
    functionData: functionCatalog.functions,
    languageName: language,
  });

  for (let i = 0; i < initialSettings.scripts.length; ++i) {
    const key = generateNewKey();

    orderedEditorKeys.push(key);

    columnSelectorStates[key] = {
      outputMode: initialSettings.outputModes[i],
      createColumn: initialSettings.createdColumns[i],
      replaceColumn: initialSettings.replacedColumns[i],
    };
  }

  await nextTick(); // Wait for the editors to be rendered

  for (let i = 0; i < initialSettings.scripts.length; i++) {
    const key = orderedEditorKeys[i];

    multiEditorComponentRefs[key]
      .getEditorState()
      .setInitialText(initialSettings.scripts[i]);

    // Watch all editor text and when changes occur, rerun diagnostics
    editorStateWatchers[key] = watch(
      multiEditorComponentRefs[key].getEditorState().text,
      runDiagnosticsFunction,
    );
    columnStateWatchers[key] = watch(
      () => columnSelectorStates[key],
      runDiagnosticsFunction,
      {
        deep: true,
      },
    );
  }

  setActiveEditorStoreForAi(getFirstEditor()?.getEditorState());
  store.activeEditorFileName = orderedEditorKeys[0];

  // Run initial diagnostics now that we've set the initial text
  runDiagnosticsFunction();
});

const runExpressions = (rows: number) => {
  if (store.expressionValid) {
    scriptingService.sendToService("runExpression", [
      orderedEditorKeys
        .map((key) => multiEditorComponentRefs[key])
        .map((ref) => ref.getEditorState().text.value),
      rows,
      orderedEditorKeys
        .map((key) => columnSelectorStates[key])
        .map((state) => state.outputMode),
      orderedEditorKeys
        .map((key) => columnSelectorStates[key])
        .map((state) =>
          state.outputMode === "APPEND"
            ? state.createColumn
            : state.replaceColumn,
        ),
    ]);
  }
};

scriptingService.registerSettingsGetterForApply(
  (): ExpressionNodeSettings => ({
    ...expressionVersion.value,
    createdColumns: orderedEditorKeys
      .map((key) => columnSelectorStates[key])
      .map((state) => state.createColumn),
    replacedColumns: orderedEditorKeys
      .map((key) => columnSelectorStates[key])
      .map((state) => state.replaceColumn),
    scripts: orderedEditorKeys
      .map((key) => multiEditorComponentRefs[key])
      .map((editor) => editor.getEditorState().text.value ?? ""),
    outputModes: orderedEditorKeys
      .map((key) => columnSelectorStates[key])
      .map((state) => state.outputMode),
  }),
);

const onEditorRequestedDelete = (filename: string) => {
  if (numberOfEditors.value === 1) {
    return;
  }

  orderedEditorKeys.splice(orderedEditorKeys.indexOf(filename), 1);
  delete columnSelectorStates[filename];
  delete multiEditorComponentRefs[filename];

  // Clean up watchers
  editorStateWatchers[filename]();
  columnStateWatchers[filename]();
  delete editorStateWatchers[filename];
  delete columnStateWatchers[filename];

  runDiagnosticsFunction();
};

const onEditorRequestedMoveUp = (filename: string) => {
  const index = orderedEditorKeys.indexOf(filename);
  if (index <= 0) {
    return;
  }

  // Swap the entries at index and index - 1
  const temp = orderedEditorKeys[index];
  orderedEditorKeys[index] = orderedEditorKeys[index - 1];
  orderedEditorKeys[index - 1] = temp;

  runDiagnosticsFunction();

  // Focus the moved editor after rerendering
  nextTick().then(() => {
    multiEditorComponentRefs[filename].getEditorState().editor.value?.focus();
  });
};

const onEditorRequestedMoveDown = (filename: string) => {
  const index = orderedEditorKeys.indexOf(filename);
  if (index >= orderedEditorKeys.length - 1) {
    return;
  }

  // Swap the entries at index and index + 1
  const temp = orderedEditorKeys[index];
  orderedEditorKeys[index] = orderedEditorKeys[index + 1];
  orderedEditorKeys[index + 1] = temp;

  runDiagnosticsFunction();

  // Focus the moved editor after rerendering
  nextTick().then(() => {
    multiEditorComponentRefs[filename].getEditorState().editor.value?.focus();
  });
};

const onFunctionInsertionTriggered = (payload: {
  eventSource: string;
  functionName: string;
  functionArgs: string[] | null;
}) => {
  getActiveEditor()?.getEditorState().editor.value?.focus();

  if (payload.functionArgs === null) {
    getActiveEditor()
      ?.getEditorState()
      .insertFunctionReference(payload.functionName, null);
  } else {
    getActiveEditor()
      ?.getEditorState()
      .insertFunctionReference(payload.functionName, payload.functionArgs);
  }
};

const onInputOutputItemInsertionTriggered = (codeToInsert: string) => {
  getActiveEditor()?.getEditorState().editor.value?.focus();

  // Note that we're ignoring requiredImport, because the expression editor
  // doesn't need imports.
  getActiveEditor()?.getEditorState().insertColumnReference(codeToInsert);
};

// Shift+Enter while editor has focus runs expressions
onKeyStroke("Enter", (evt: KeyboardEvent) => {
  if (
    evt.shiftKey &&
    Object.values(multiEditorComponentRefs).some((editor) =>
      editor.getEditorState().editor.value?.hasTextFocus(),
    )
  ) {
    evt.preventDefault();
    runExpressions(DEFAULT_NUMBER_OF_ROWS_TO_RUN);
  }
});

const columnExists = (columnName: string) =>
  columnsInInputTable.value.findIndex((column) => column.id === columnName) !==
  -1;

const columnSelectorStateValid = computed(() => {
  if (numberOfEditors.value === 0) {
    return true;
  }

  const appendedColumnsSoFar: string[] = [];

  for (const state of orderedEditorKeys.map(
    (key) => columnSelectorStates[key],
  )) {
    if (
      state.outputMode === "APPEND" &&
      (columnExists(state.createColumn) ||
        appendedColumnsSoFar.includes(state.createColumn))
    ) {
      return false;
    }

    if (state.outputMode === "APPEND") {
      appendedColumnsSoFar.push(state.createColumn);
    }
  }
  return true;
});

const runButtonDisabledErrorReason = computed(() => {
  if (!inputsAvailable.value) {
    return "No input available. Connect an executed node.";
  } else if (!store.expressionValid) {
    return "Expression is not valid.";
    // eslint-disable-next-line no-negated-condition
  } else if (!columnSelectorStateValid.value) {
    return "Output column exists in input table.";
  } else {
    return null;
  }
});

const calculateInitialPaneSizes = () => {
  const availableWidthForPanes =
    useWindowSize().width.value - COMBINED_SPLITTER_WIDTH;

  const sizeOfInputOutputPaneInPixel =
    availableWidthForPanes < MIN_WIDTH_FOR_DISPLAYING_LEFT_PANE
      ? 0
      : WIDTH_OF_INPUT_OUTPUT_PANE;
  const relativeSizeOfInputOutputPane =
    (sizeOfInputOutputPaneInPixel / availableWidthForPanes) * 100;

  const widthOfRightPane =
    availableWidthForPanes < SWITCH_TO_SMALL_DESCRIPTION
      ? MIN_WIDTH_FUNCTION_CATALOG
      : MIN_WIDTH_FOR_DISPLAYING_DESCRIPTION;
  const relativeSizeOfRightPaneWithoutTakingIntoAccountTheLeftPane =
    (widthOfRightPane / availableWidthForPanes) * 100;

  const factorForRightPaneToTakeLeftPaneIntoAccount =
    100 / (100 - relativeSizeOfInputOutputPane);

  return {
    right:
      relativeSizeOfRightPaneWithoutTakingIntoAccountTheLeftPane *
      factorForRightPaneToTakeLeftPaneIntoAccount,
    left: relativeSizeOfInputOutputPane,
  };
};

const initialPaneSizes = calculateInitialPaneSizes();

const addNewEditorBelowExisting = async (fileNameAbove: string) => {
  const latestKey = generateNewKey();
  const desiredInsertionIndex = orderedEditorKeys.indexOf(fileNameAbove) + 1;

  columnSelectorStates[latestKey] = {
    outputMode: "APPEND",
    createColumn: "New Column",
    replaceColumn:
      (await scriptingService.getInputObjects())[0].subItems?.[0].name ?? "",
  };

  orderedEditorKeys.splice(desiredInsertionIndex, 0, latestKey);

  // Wait for the editor to render and populate the ref
  await nextTick();

  runDiagnosticsFunction();

  editorStateWatchers[latestKey] = watch(
    multiEditorComponentRefs[latestKey].getEditorState().text,
    runDiagnosticsFunction,
  );
  columnStateWatchers[latestKey] = watch(
    () => columnSelectorStates[latestKey],
    runDiagnosticsFunction,
    {
      deep: true,
    },
  );

  multiEditorComponentRefs[latestKey].getEditorState().editor.value?.focus();
};
</script>

<template>
  <main>
    <template v-if="numberOfEditors === 0">
      <div class="no-editors">
        <div class="loading-icon">
          <LoadingIcon />
        </div>
      </div>
    </template>
    <template v-else>
      <ScriptingEditor
        :right-pane-minimum-width-in-pixel="MIN_WIDTH_FUNCTION_CATALOG"
        :show-control-bar="true"
        :language="language"
        :show-output-table="true"
        :initial-pane-sizes="{
          right: initialPaneSizes.right,
          left: initialPaneSizes.left,
          bottom: 30,
        }"
        @input-output-item-insertion="onInputOutputItemInsertionTriggered"
      >
        <template #editor>
          <ExpressionEditorPane
            v-for="(key, index) in orderedEditorKeys"
            :key="key"
            :ref="createElementReference(key)"
            :title="`Expression editor (${1 + index})`"
            :file-name="key"
            :language="language"
            :is-first="index === 0"
            :is-last="index === numberOfEditors - 1"
            :is-only="numberOfEditors === 1"
            @focus="onEditorFocused(key)"
            @delete="onEditorRequestedDelete"
            @move-down="onEditorRequestedMoveDown"
            @move-up="onEditorRequestedMoveUp"
            @add-below="addNewEditorBelowExisting"
          >
            <!-- Controls displayed once per editor -->
            <template #multi-editor-controls>
              <div class="editor-controls">
                <ColumnOutputSelector
                  v-model="columnSelectorStates[key]"
                  :allowed-replacement-columns="
                    getAvailableColumnsForReplacement(key)
                  "
                />
              </div>
            </template>
          </ExpressionEditorPane>
        </template>
        <template #right-pane>
          <template v-if="functionCatalogData">
            <FunctionCatalog
              :function-catalog-data="functionCatalogData"
              :initially-expanded="false"
              @function-insertion-event="onFunctionInsertionTriggered"
            />
          </template>
        </template>
        <!-- Controls for the very bottom bar -->
        <template #code-editor-controls="{ showButtonText }">
          <Tooltip :text="runButtonDisabledErrorReason ?? ''">
            <SplitButton>
              <Button
                primary
                compact
                :disabled="runButtonDisabledErrorReason !== null"
                @click="runExpressions(DEFAULT_NUMBER_OF_ROWS_TO_RUN)"
              >
                <div
                  class="run-button"
                  :class="{
                    'hide-button-text': !showButtonText,
                  }"
                >
                  <PlayIcon />
                </div>

                {{
                  showButtonText
                    ? `Evaluate first
               ${DEFAULT_NUMBER_OF_ROWS_TO_RUN}  rows`
                    : ""
                }}</Button
              >

              <SubMenu
                :items="[
                  { text: 'Evaluate first 100 rows', metadata: 100 },
                  { text: 'Evaluate first 1000 rows', metadata: 1000 },
                ]"
                button-title="Run more rows"
                orientation="top"
                :disabled="runButtonDisabledErrorReason !== null"
                @item-click="
                  (_evt: any, item: any) => runExpressions(item.metadata)
                "
              >
                <DropdownIcon />
              </SubMenu>
            </SplitButton>
          </Tooltip>
        </template>
      </ScriptingEditor>
    </template>
  </main>
</template>

<style lang="postcss">
@import url("webapps-common/ui/css");

.editor-controls {
  width: 100%;
  display: flex;
  justify-content: space-between;
  padding: var(--space-4) var(--space-8);
  height: fit-content;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-4);
}

.hide-button-text {
  margin-right: calc(-1 * var(--space-16));
}

.run-button {
  display: inline;
}

.no-editors {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  width: 100%;

  & .loading-icon {
    width: 50px;
    height: 50px;
  }
}

.submenu {
  background-color: var(--knime-yellow);

  &:focus-within,
  &:hover {
    background-color: var(--knime-masala);

    & .submenu-toggle svg {
      stroke: var(--knime-white);
    }
  }
}
</style>
