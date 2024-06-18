<script setup lang="ts">
import {
  consoleHandler,
  getScriptingService,
  ScriptingEditor,
  setActiveEditorStoreForAi,
} from "@knime/scripting-editor";
import { getExpressionScriptingService } from "@/expressionScriptingService";
import Button from "webapps-common/ui/components/Button.vue";
import PlayIcon from "webapps-common/ui/assets/img/icons/play.svg";
import SplitButton from "webapps-common/ui/components/SplitButton.vue";
import DropdownIcon from "webapps-common/ui/assets/img/icons/arrow-dropdown.svg";
import SubMenu from "webapps-common/ui/components/SubMenu.vue";
import { computed, onMounted, ref } from "vue";
import FunctionCatalog from "@/components/function-catalog/FunctionCatalog.vue";
import ColumnOutputSelector, {
  type AllowedDropDownValue,
  type ColumnSelectorState,
} from "@/components/ColumnOutputSelector.vue";
import { onKeyStroke } from "@vueuse/core";
import * as monaco from "monaco-editor";
import MultiEditorPane, {
  type MultiEditorPaneExposes,
} from "./MultiEditorPane.vue";
import type { FunctionCatalogData, FunctionData } from "./functionCatalogTypes";
import { useStore } from "@/store";

import registerKnimeExpressionLanguage from "../registerKnimeExpressionLanguage";
import { functionDataToMarkdown } from "@/components/function-catalog/functionDescriptionToMarkdown";

const language = "knime-expression";

const MIN_WIDTH_FUNCTION_CATALOG = 280;

const DEFAULT_NUMBER_OF_ROWS_TO_RUN = 10;

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

const convertFunctionsToInsertionItems = (functions: FunctionData[]) => {
  return functions.map((func) => {
    // closest we can get to a list comprehension over a range in JS
    const listOfIndices = [...Array(func.arguments.length).keys()];

    // This snippet syntax is used to allow for tabbing through the arguments
    // when the function is inserted.
    const argumentsWithSnippetSyntax = listOfIndices
      .map((i) => `$\{${i + 1}:${func.arguments[i].name}}`)
      .join(", ");

    const argumentsWithoutSnippetSyntax =
      func.arguments.length > 0 ? "..." : "";

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

  columnSectorState.value = {
    outputMode: initialSettings.columnOutputMode,
    createColumn: initialSettings.createdColumn,
    replaceColumn: initialSettings.replacedColumn,
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
      ...mathsConstants.map((constant) => ({
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

  getScriptingService()
    .sendToService("getDocumentationContext")
    .then((res) => console.log(res));
});

const runExpressions = (rows: number) => {
  // TODO make this work with multiple editors
  if (store.expressionValid) {
    scriptingService.sendToService("runExpression", [
      multiEditorComponentRef.value?.getEditorState().text.value,
      rows,
      columnSectorState.value.outputMode,
      columnSectorState.value.outputMode === "APPEND"
        ? columnSectorState.value.createColumn
        : columnSectorState.value.replaceColumn,
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
  functionName: string;
  functionArgs: string[];
}) => {
  multiEditorComponentRef.value?.getEditorState().editor.value?.focus();
  multiEditorComponentRef.value
    ?.getEditorState()
    .insertFunctionReference(payload.functionName, payload.functionArgs);
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

const runButtonEnabled = computed(() => {
  return inputsAvailable.value && store.expressionValid;
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
          title="Expression editor"
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
        <SplitButton>
          <Button
            primary
            compact
            :disabled="!runButtonEnabled"
            @click="runExpressions(DEFAULT_NUMBER_OF_ROWS_TO_RUN)"
          >
            <PlayIcon />Evaluate first
            {{ DEFAULT_NUMBER_OF_ROWS_TO_RUN }} rows</Button
          >
          <SubMenu
            :items="[
              { text: 'Evaluate first 100 rows', metadata: 100 },
              { text: 'Evaluate first 1000 rows', metadata: 1000 },
            ]"
            button-title="Run more rows"
            orientation="top"
            :disabled="!runButtonEnabled"
            @item-click="
              (_evt: any, item: any) => runExpressions(item.metadata)
            "
          >
            <DropdownIcon />
          </SubMenu>
        </SplitButton>
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
