import { BrowserReporter, Consola, LogLevel } from "consola";
import { useWindowSize } from "@vueuse/core";
import {
  COMBINED_SPLITTER_WIDTH,
  MIN_WIDTH_FOR_SIDE_BY_SIZE_DESC_FUNC_CATALOG,
  MIN_WIDTH_FUNCTION_CATALOG,
  SWITCH_TO_SMALL_DESCRIPTION,
  WIDTH_OF_INPUT_OUTPUT_PANE,
} from "@/components/function-catalog/contraints";

import { FUNCTION_INSERTION_EVENT } from "@/components/function-catalog/FunctionCatalog.vue";
import {
  COLUMN_INSERTION_EVENT,
  type InsertionEvent,
  insertionEventHelper,
  MIN_WIDTH_FOR_DISPLAYING_LEFT_PANE,
} from "@knime/scripting-editor";
import type { ExpressionEditorPaneExposes } from "@/components/ExpressionEditorPane.vue";

export const setupConsola = () => {
  const consola = new Consola({
    level: import.meta.env.DEV ? LogLevel.Trace : LogLevel.Error,
    reporters: [new BrowserReporter()],
  });
  const globalObject = typeof global === "object" ? global : window;

  // @ts-expect-error
  globalObject.consola = consola;
};

export const calculateInitialPaneSizes = () => {
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
      : MIN_WIDTH_FOR_SIDE_BY_SIZE_DESC_FUNC_CATALOG;
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

export const registerInsertionListener = (
  getActiveEditor: () => ExpressionEditorPaneExposes | null,
) => {
  insertionEventHelper
    .getInsertionEventHelper(FUNCTION_INSERTION_EVENT)
    .registerInsertionListener((insertionEvent: InsertionEvent) => {
      const editorState = getActiveEditor()?.getEditorState();
      if (!editorState) {
        return;
      }
      editorState.editor.value?.focus();

      const functionArgs = insertionEvent.extraArgs?.functionArgs;
      const functionName = insertionEvent.textToInsert;

      editorState.insertFunctionReference(functionName, functionArgs);
    });

  insertionEventHelper
    .getInsertionEventHelper(COLUMN_INSERTION_EVENT)
    .registerInsertionListener((insertionEvent: InsertionEvent) => {
      const editorState = getActiveEditor()?.getEditorState();
      if (!editorState) {
        return;
      }
      editorState.editor.value?.focus();

      const codeToInsert = insertionEvent.textToInsert;

      // Note that we're ignoring requiredImport, because the expression editor
      // doesn't need imports.
      editorState.insertColumnReference(codeToInsert);
    });
};

export const log = (message: any, ...args: any[]) => {
  if (typeof consola === "undefined") {
    // eslint-disable-next-line no-console
    console.log(message, ...args);
  } else {
    consola.log(message, ...args);
  }
};
