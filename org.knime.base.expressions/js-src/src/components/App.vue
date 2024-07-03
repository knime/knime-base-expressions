<script setup lang="ts">
import {
  consoleHandler,
  ScriptingEditor,
  setActiveEditorStoreForAi,
  MIN_WIDTH_FOR_DISPLAYING_LEFT_PANE,
} from "@knime/scripting-editor";
import {
  type ExpressionVersion,
  getExpressionScriptingService,
} from "@/expressionScriptingService";
import Button from "webapps-common/ui/components/Button.vue";
import PlayIcon from "webapps-common/ui/assets/img/icons/play.svg";
import SplitButton from "webapps-common/ui/components/SplitButton.vue";
import DropdownIcon from "webapps-common/ui/assets/img/icons/arrow-dropdown.svg";
import SubMenu from "webapps-common/ui/components/SubMenu.vue";
import Tooltip from "webapps-common/ui/components/Tooltip.vue";
import { useWindowSize, onKeyStroke } from "@vueuse/core";
import { computed, onMounted, ref, reactive, nextTick } from "vue";
import FunctionCatalog from "@/components/function-catalog/FunctionCatalog.vue";
import ColumnOutputSelector, {
  type AllowedDropDownValue,
  type ColumnSelectorState,
} from "@/components/ColumnOutputSelector.vue";
import * as monaco from "monaco-editor";
import MultiEditorPane, {
  type MultiEditorPaneExposes,
} from "./MultiEditorPane.vue";
import type {
  FunctionCatalogData,
  FunctionCatalogEntryData,
} from "./functionCatalogTypes";
import { useStore } from "@/store";

import registerKnimeExpressionLanguage from "../registerKnimeExpressionLanguage";
import { functionDataToMarkdown } from "@/components/function-catalog/functionDescriptionToMarkdown";
import {
  COMBINED_SPLITTER_WIDTH,
  MIN_WIDTH_FOR_DISPLAYING_DESCRIPTION,
  MIN_WIDTH_FUNCTION_CATALOG,
  SWITCH_TO_SMALL_DESCRIPTION,
  WIDTH_OF_INPUT_OUTPUT_PANE,
} from "@/components/function-catalog/contraints";

const language = "knime-expression";

const DEFAULT_NUMBER_OF_ROWS_TO_RUN = 10;

const scriptingService = getExpressionScriptingService();
const store = useStore();

const allowedReplacementColumns = ref<AllowedDropDownValue[]>([]);

// These should be immediately overridden by the scripting service
const columnSelectorState = ref<ColumnSelectorState>({
  outputMode: "APPEND",
  createColumn: "",
  replaceColumn: "",
});

// Overwritten by the initial settings
const expressionVersion = ref<ExpressionVersion>({
  languageVersion: 0,
  builtinFunctionsVersion: 0,
  builtinAggregationsVersion: 0,
});

const multiEditorComponentRefs = reactive<{
  [title: string]: MultiEditorPaneExposes;
}>({});

const createElementReference = (title: string) => {
  return (el: any) => {
    multiEditorComponentRefs[title] = el as unknown as MultiEditorPaneExposes;
  };
};

const getActiveEditor = (): MultiEditorPaneExposes | null => {
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

const convertFunctionsToInsertionItems = (
  functions: FunctionCatalogEntryData[],
) => {
  return functions.map((func) => {
    if (func.entryType === "function") {
      // closest we can get to a list comprehension over a range in JS
      const listOfIndices = [...Array(func.arguments!.length).keys()];

      // This snippet syntax is used to allow for tabbing through the arguments
      // when the function is inserted.
      const argumentsWithSnippetSyntax = listOfIndices
        .map((i) => `$\{${i + 1}:${func.arguments![i].name}}`)
        .join(", ");

      const argumentsWithoutSnippetSyntax =
        func.arguments!.length > 0 ? "..." : "";

      return {
        text: `${func.name}(${argumentsWithSnippetSyntax})`,
        kind: monaco.languages.CompletionItemKind.Function,
        extraDetailForMonaco: {
          documentation: { value: functionDataToMarkdown(func) },
          insertTextRules:
            monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
          label: `${func.name}(${argumentsWithoutSnippetSyntax})`,
        },
      };
    } else {
      return {
        text: func.name,
        kind: monaco.languages.CompletionItemKind.Constant,
        extraDetailForMonaco: {
          documentation: { value: func.description },
          detail: `Type: ${func.returnType}`,
        },
      };
    }
  });
};

const getFirstEditor = (): MultiEditorPaneExposes => {
  return multiEditorComponentRefs[Object.keys(multiEditorComponentRefs)[0]];
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

  getFirstEditor()?.getEditorState().setInitialText(initialSettings.script);

  for (const editor of Object.values(multiEditorComponentRefs)) {
    editor.getEditorState().editor.value?.updateOptions({
      readOnly: typeof initialSettings.scriptUsedFlowVariable === "string",
      readOnlyMessage: {
        value: `Read-Only-Mode: The script is set by the flow variable '${initialSettings.scriptUsedFlowVariable}'.`,
      },
    });
  }
  columnSelectorState.value = {
    outputMode: initialSettings.columnOutputMode,
    createColumn: initialSettings.createdColumn,
    replaceColumn: initialSettings.replacedColumn,
  };

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
    allowedReplacementColumns.value = inputObjects[0]?.subItems?.map(
      (c: { name: string }) => {
        return { id: c.name, text: c.name };
      },
    );
    const currentSelectionInNewValue = allowedReplacementColumns.value.find(
      (value) => value.id === columnSelectorState.value.replaceColumn,
    );
    columnSelectorState.value.replaceColumn =
      currentSelectionInNewValue?.id ?? allowedReplacementColumns.value[0]?.id;
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

  setActiveEditorStoreForAi(getFirstEditor()?.getEditorState());
});

const runExpressions = (rows: number) => {
  // TODO make this work with multiple editors
  if (store.expressionValid) {
    scriptingService.sendToService("runExpression", [
      getActiveEditor()?.getEditorState().text.value,
      rows,
      columnSelectorState.value.outputMode,
      columnSelectorState.value.outputMode === "APPEND"
        ? columnSelectorState.value.createColumn
        : columnSelectorState.value.replaceColumn,
    ]);
  }
};

scriptingService.registerSettingsGetterForApply(() => ({
  ...expressionVersion.value,
  script:
    multiEditorComponentRefs[
      Object.keys(multiEditorComponentRefs)[0]
    ].getEditorState().text.value ?? "",
  columnOutputMode: columnSelectorState.value?.outputMode,
  createdColumn: columnSelectorState.value?.createColumn,
  replacedColumn: columnSelectorState.value?.replaceColumn,
  additionalScripts: Object.keys(multiEditorComponentRefs)
    .slice(1)
    .map((key) => multiEditorComponentRefs[key])
    .map((editor) => editor.getEditorState().text.value ?? ""),
}));

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
    Object.values(multiEditorComponentRefs)
      .map((editor) => editor.getEditorState().editor.value?.hasTextFocus())
      .some((hasFocus) => hasFocus)
  ) {
    evt.preventDefault();
    runExpressions(DEFAULT_NUMBER_OF_ROWS_TO_RUN);
  }
});

const columnExists = (columnName: string) =>
  allowedReplacementColumns.value.findIndex(
    (column) => column.id === columnName,
  ) !== -1;

const columnSelectorStateValid = computed(() => {
  const createColExists = columnExists(columnSelectorState.value.createColumn);
  const outputMode = columnSelectorState.value.outputMode;
  return (
    (outputMode === "APPEND" &&
      !createColExists &&
      columnSelectorState.value.createColumn) ||
    outputMode === "REPLACE_EXISTING"
  );
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

const numberOfEditors = 1;
</script>

<template>
  <main>
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
        <MultiEditorPane
          v-for="index in numberOfEditors"
          :key="`_${index}.knexp`"
          :ref="createElementReference(`_${index}.knexp`)"
          title="Expression editor"
          :file-name="`_${index}.knexp`"
          :language="language"
          @focus="onEditorFocused(`_${index}.knexp`)"
        >
          <!-- Controls displayed once per editor -->
          <template #multi-editor-controls>
            <div class="editor-controls">
              <ColumnOutputSelector
                v-model="columnSelectorState"
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
