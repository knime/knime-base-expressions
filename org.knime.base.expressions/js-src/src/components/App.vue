<script setup lang="ts">
import {
  consoleHandler,
  MIN_WIDTH_FOR_DISPLAYING_LEFT_PANE,
  ScriptingEditor,
  setActiveEditorStoreForAi,
  insertionEventHelper,
  OutputTablePreview,
  type InsertionEvent,
  COLUMN_INSERTION_EVENT,
  useReadonlyStore,
} from "@knime/scripting-editor";
import {
  type ExpressionNodeSettings,
  type ExpressionVersion,
  getExpressionScriptingService,
} from "@/expressionScriptingService";
import {
  Button,
  SplitButton,
  SubMenu,
  Tooltip,
  LoadingIcon,
  FunctionButton,
} from "@knime/components";
import PlayIcon from "@knime/styles/img/icons/play.svg";
import DropdownIcon from "@knime/styles/img/icons/arrow-dropdown.svg";
import PlusIcon from "@knime/styles/img/icons/circle-plus.svg";
import { onKeyStroke, useWindowSize } from "@vueuse/core";
import {
  type ComponentPublicInstance,
  computed,
  nextTick,
  onMounted,
  reactive,
  ref,
  watch,
} from "vue";
import FunctionCatalog, {
  FUNCTION_INSERTION_EVENT,
} from "@/components/function-catalog/FunctionCatalog.vue";
import ColumnOutputSelector, {
  type AllowedDropDownValue,
  type ColumnSelectorState,
} from "@/components/ColumnOutputSelector.vue";
import ExpressionEditorPane, {
  type ExpressionEditorPaneExposes,
} from "./ExpressionEditorPane.vue";
import type { FunctionCatalogData } from "./functionCatalogTypes";
import {
  runColumnOutputDiagnostics,
  runDiagnostics,
} from "@/expressionDiagnostics";
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
import { v4 as uuidv4 } from "uuid";

const language = "knime-expression";

const DEFAULT_NUMBER_OF_ROWS_TO_RUN = 10;

const scriptingService = getExpressionScriptingService();

const activeEditorFileName = ref<string | null>(null);

// Populated by the initial settings
const columnSelectorStates = reactive<{ [key: string]: ColumnSelectorState }>(
  {},
);

const columnSelectorStateErrorMessages = reactive<{
  [key: string]: string | null;
}>({});

/**
 * Generate a new key for a new editor. This is used to index the columnSelectorStates
 * and the multiEditorComponentRefs, plus it's used to keep track of the order of the editors.
 */
const generateNewKey = () => {
  return uuidv4();
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

const createEditorTitle = (index: number) => `Expression (${index + 1})`;
const getEditorTitleFromKey = (key: string) =>
  createEditorTitle(orderedEditorKeys.indexOf(key));

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
  if (activeEditorFileName.value === null) {
    return null;
  }

  return multiEditorComponentRefs[activeEditorFileName.value];
};

const onEditorFocused = (filename: string) => {
  activeEditorFileName.value = filename;
  setActiveEditorStoreForAi(
    multiEditorComponentRefs[filename]?.getEditorState(),
  );

  // Scroll to the editor. The focusing somehow interferes with scrolling,
  // so wait for a tick first.
  nextTick().then(() => {
    multiEditorComponentRefs[filename]
      ?.getEditorState()
      .editor.value?.getDomNode()
      ?.scrollIntoView({
        behavior: "smooth",
        block: "nearest",
      });
  });
};

const functionCatalogData = ref<FunctionCatalogData>();
const inputsAvailable = ref(false);
const severityLevels = ref<string[]>([]);

const getFirstEditor = (): ExpressionEditorPaneExposes => {
  return multiEditorComponentRefs[orderedEditorKeys[0]];
};

const runDiagnosticsFunction = async () => {
  severityLevels.value = await runDiagnostics(
    orderedEditorKeys
      .map((key) => multiEditorComponentRefs[key])
      .map((editor) => editor.getEditorState()),
    orderedEditorKeys
      .map((key) => columnSelectorStates[key])
      .map((state) =>
        state.outputMode === "APPEND" ? state.createColumn : null,
      ),
  );

  const columnValidities = runColumnOutputDiagnostics(
    orderedEditorKeys.map((key) => columnSelectorStates[key]),
    columnsInInputTable.value.map((column) => column.id),
  );

  for (let i = 0; i < orderedEditorKeys.length; i++) {
    const key = orderedEditorKeys[i];

    columnSelectorStateErrorMessages[key] = columnValidities[i];

    if (severityLevels.value[i] === "ERROR" || columnValidities[i] !== null) {
      multiEditorComponentRefs[key].setErrorLevel("ERROR");
    } else {
      multiEditorComponentRefs[key].setErrorLevel("OK");
    }
  }
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

  useReadonlyStore().value = initialSettings.setByFlowVariables || false;

  for (let i = 0; i < initialSettings.scripts.length; i++) {
    const key = orderedEditorKeys[i];

    multiEditorComponentRefs[key]
      .getEditorState()
      .setInitialText(initialSettings.scripts[i]);

    multiEditorComponentRefs[key].getEditorState().editor.value?.updateOptions({
      readOnly: useReadonlyStore().value,
      readOnlyMessage: {
        value: "Read-Only-Mode: configuration is set by flow variables.",
      },
      renderValidationDecorations: "on",
    });

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
  activeEditorFileName.value = orderedEditorKeys[0];

  // Run initial diagnostics now that we've set the initial text
  runDiagnosticsFunction();
});

const runExpressions = (rows: number) => {
  if (severityLevels.value.every((severity) => severity !== "ERROR")) {
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

  return latestKey;
};

const addEditorAtBottom = () => {
  addNewEditorBelowExisting(orderedEditorKeys[orderedEditorKeys.length - 1]);
};

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

const onEditorRequestedCopyBelow = async (filename: string) => {
  const newKey = await addNewEditorBelowExisting(filename);

  // Copy state from the editor above to the new editor
  columnSelectorStates[newKey] = { ...columnSelectorStates[filename] };
  multiEditorComponentRefs[newKey]
    .getEditorState()
    .setInitialText(
      multiEditorComponentRefs[filename].getEditorState().text.value,
    );
};

insertionEventHelper
  .getInsertionEventHelper(FUNCTION_INSERTION_EVENT)
  .registerInsertionListener((insertionEvent: InsertionEvent) => {
    getActiveEditor()?.getEditorState().editor.value?.focus();

    const functionArgs = insertionEvent.extraArgs?.functionArgs;
    const functionName = insertionEvent.textToInsert;

    getActiveEditor()
      ?.getEditorState()
      .insertFunctionReference(functionName, functionArgs);
  });

insertionEventHelper
  .getInsertionEventHelper(COLUMN_INSERTION_EVENT)
  .registerInsertionListener((insertionEvent: InsertionEvent) => {
    getActiveEditor()?.getEditorState().editor.value?.focus();

    const codeToInsert = insertionEvent.textToInsert;

    // Note that we're ignoring requiredImport, because the expression editor
    // doesn't need imports.
    getActiveEditor()?.getEditorState().insertColumnReference(codeToInsert);
  });

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

const runButtonDisabledErrorReason = computed(() => {
  const errors = [];

  if (!inputsAvailable.value) {
    errors.push("No input available. Connect an executed node.");
  }

  severityLevels.value.forEach((severity, index) => {
    if (severity === "ERROR") {
      errors.push(`${createEditorTitle(index)} is invalid.`);
    }
  });

  const columnErrors = Object.entries(columnSelectorStateErrorMessages)
    .map(([key, message]) =>
      message === null ? null : `${getEditorTitleFromKey(key)}: ${message}`,
    )
    .filter((message) => message !== null);

  errors.push(...columnErrors);

  if (errors.length === 0) {
    return null;
  }

  let result = errors[0];

  if (errors.length > 1) {
    result += ` And ${errors.length - 1} more error${errors.length - 1 > 1 ? "s" : ""} not shown.`;
  }

  return result;
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
            v-for="(key, index) in orderedEditorKeys"
            :key="key"
            :ref="createElementReference(key)"
            :title="createEditorTitle(index)"
            :file-name="key"
            :language="language"
            :is-first="index === 0"
            :is-last="index === numberOfEditors - 1"
            :is-only="numberOfEditors === 1"
            :is-active="activeEditorFileName === key"
            @focus="onEditorFocused(key)"
            @delete="onEditorRequestedDelete"
            @move-down="onEditorRequestedMoveDown"
            @move-up="onEditorRequestedMoveUp"
            @copy-below="onEditorRequestedCopyBelow"
          >
            <!-- Controls displayed once per editor -->
            <template #multi-editor-controls>
              <div class="editor-controls">
                <ColumnOutputSelector
                  v-model="columnSelectorStates[key]"
                  v-model:error-message="columnSelectorStateErrorMessages[key]"
                  :allowed-replacement-columns="
                    getAvailableColumnsForReplacement(key)
                  "
                />
              </div>
            </template>
          </ExpressionEditorPane>

          <FunctionButton
            class="add-new-editor-button"
            @click="addEditorAtBottom"
          >
            <PlusIcon /><span>Add new editor</span>
          </FunctionButton>
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
          <Tooltip
            class="tooltip-word-wrap"
            :text="runButtonDisabledErrorReason ?? ''"
          >
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
@import url("@knime/styles/css");

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

.add-new-editor-button {
  width: fit-content;
  margin: var(--space-16) auto;
  outline: 1px solid var(--knime-silver-sand);
}

.tooltip-word-wrap .text {
  white-space: normal;
  overflow: visible;
  width: auto;
  height: auto;
  z-index: 9999;
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
