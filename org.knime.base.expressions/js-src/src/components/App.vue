<script setup lang="ts">
import {
  consoleHandler,
  ScriptingEditor,
  setActiveEditorStoreForAi,
} from "@knime/scripting-editor";
import {
  type ExpressionVersion,
  getExpressionScriptingService,
  type MathConstantData,
} from "@/expressionScriptingService";
import Button from "webapps-common/ui/components/Button.vue";
import PlayIcon from "webapps-common/ui/assets/img/icons/play.svg";
import SplitButton from "webapps-common/ui/components/SplitButton.vue";
import DropdownIcon from "webapps-common/ui/assets/img/icons/arrow-dropdown.svg";
import SubMenu from "webapps-common/ui/components/SubMenu.vue";
import Tooltip from "webapps-common/ui/components/Tooltip.vue";
import { onKeyStroke } from "@vueuse/core";
import { computed, onMounted, ref } from "vue";
import FunctionCatalog from "@/components/function-catalog/FunctionCatalog.vue";
import ColumnOutputSelector, {
  type AllowedDropDownValue,
  type ColumnSelectorState,
} from "@/components/ColumnOutputSelector.vue";
import * as monaco from "monaco-editor";
import MultiEditorPane, {
  type MultiEditorPaneExposes,
} from "./MultiEditorPane.vue";
import type { FunctionCatalogData, FunctionData } from "./functionCatalogTypes";
import { useStore } from "@/store";

import registerKnimeExpressionLanguage from "../registerKnimeExpressionLanguage";
import { functionDataToMarkdown } from "@/components/function-catalog/functionDescriptionToMarkdown";

const language = "knime-expression";

const MIN_WIDTH_FUNCTION_CATALOG = 300;
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

const multiEditorComponentRef = ref<MultiEditorPaneExposes | null>(null);
const functionCatalogData = ref<FunctionCatalogData>();
const mathConstantData = ref<MathConstantData>();
const inputsAvailable = ref(false);

const convertFunctionsToInsertionItems = (functions: FunctionData[]) => {
  return functions.map((func) => {
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
  });
};

onMounted(async () => {
  const [
    initialSettings,
    availableInputs,
    inputObjects,
    flowVariableInputs,
    functionCatalog,
    mathsConstants,
  ] = await Promise.all([
    scriptingService.getInitialSettings(),
    scriptingService.inputsAvailable(),
    scriptingService.getInputObjects(),
    scriptingService.getFlowVariableInputs(),
    scriptingService.getFunctions(),
    scriptingService.getMathConstants(),
  ]);

  multiEditorComponentRef.value
    ?.getEditorState()
    .setInitialText(initialSettings.script);

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
  functionCatalogData.value.categories.push(mathsConstants.category);
  mathConstantData.value = mathsConstants;

  if (inputObjects && inputObjects.length > 0 && inputObjects[0].subItems) {
    allowedReplacementColumns.value = inputObjects[0]?.subItems?.map(
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
      ...mathConstantData.value.constants.map((constant) => ({
        text: constant.name,
        kind: monaco.languages.CompletionItemKind.Constant,
        extraDetailForMonaco: {
          documentation: { value: constant.documentation },
          detail: `Type: ${constant.type}`,
        },
      })),
    ],
    functionData: functionCatalog.functions,
    languageName: language,
  });

  setActiveEditorStoreForAi(multiEditorComponentRef.value?.getEditorState());
});

const runExpressions = (rows: number) => {
  // TODO make this work with multiple editors
  if (store.expressionValid) {
    scriptingService.sendToService("runExpression", [
      multiEditorComponentRef.value?.getEditorState().text.value,
      rows,
      columnSelectorState.value.outputMode,
      columnSelectorState.value.outputMode === "APPEND"
        ? columnSelectorState.value.createColumn
        : columnSelectorState.value.replaceColumn,
    ]);
  }
};

scriptingService.registerSettingsGetterForApply(() => {
  return {
    ...expressionVersion.value,
    script: multiEditorComponentRef.value?.getEditorState().text.value ?? "",
    columnOutputMode: columnSelectorState.value?.outputMode,
    createdColumn: columnSelectorState.value?.createColumn,
    replacedColumn: columnSelectorState.value?.replaceColumn,
  };
});

const onFunctionInsertionTriggered = (payload: {
  eventSource: string;
  functionName: string;
  functionArgs: string[] | null;
}) => {
  multiEditorComponentRef.value?.getEditorState().editor.value?.focus();

  if (payload.functionArgs === null) {
    multiEditorComponentRef.value
      ?.getEditorState()
      .insertFunctionReference(payload.functionName, null);
  } else {
    multiEditorComponentRef.value
      ?.getEditorState()
      .insertFunctionReference(payload.functionName, payload.functionArgs);
  }
};

const onInputOutputItemInsertionTriggered = (codeToInsert: string) => {
  multiEditorComponentRef.value?.getEditorState().editor.value?.focus();

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
</script>

<template>
  <main>
    <ScriptingEditor
      :right-pane-minimum-width-in-pixel="MIN_WIDTH_FUNCTION_CATALOG"
      :show-control-bar="true"
      :language="language"
      @input-output-item-insertion="onInputOutputItemInsertionTriggered"
    >
      <template #editor>
        <MultiEditorPane
          ref="multiEditorComponentRef"
          title="Expression editor"
          file-name="_1.knexp"
          :language="language"
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
            :constant-data="mathConstantData!"
            :initially-expanded="false"
            @function-insertion-event="onFunctionInsertionTriggered"
          />
        </template>
      </template>
      <!-- Controls for the very bottom bar -->
      <template #code-editor-controls="{ showButtonText }">
        <Tooltip :text="runButtonDisabledErrorReason">
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
                  : null
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
  padding: 1px 10px;
  height: fit-content;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.hide-button-text {
  margin-right: -15px;
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
