import { BrowserReporter, Consola, LogLevel } from "consola";
import { useWindowSize } from "@vueuse/core";
import {
  COMBINED_SPLITTER_WIDTH,
  MIN_WIDTH_FOR_DISPLAYING_DESCRIPTION,
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
  type UseCodeEditorReturn,
} from "@knime/scripting-editor";
import type { ExpressionEditorPaneExposes } from "@/components/ExpressionEditorPane.vue";
import { editor as MonacoEditor, Range } from "monaco-editor";
import {
  DIAGNOSTIC_SEVERITY_TO_MARKER_SEVERITY,
  EXPRESSION_MARKERS_OWNER,
} from "./constants";

import type { Diagnostic } from "./types";

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

export const markDiagnosticsInEditor = (
  diagnosticsForThisEditor: Diagnostic[],
  editorState: UseCodeEditorReturn,
) => {
  // Mark the diagnostics in the editor

  const markers = diagnosticsForThisEditor.map(
    (diagnostic): MonacoEditor.IMarkerData => ({
      ...Range.fromPositions(
        editorState.editorModel.getPositionAt(diagnostic.location.start),
        editorState.editorModel.getPositionAt(diagnostic.location.stop),
      ),
      message: diagnostic.message,
      severity: DIAGNOSTIC_SEVERITY_TO_MARKER_SEVERITY[diagnostic.severity],
    }),
  );

  MonacoEditor.setModelMarkers(
    editorState.editorModel,
    EXPRESSION_MARKERS_OWNER,
    markers,
  );
};

export const evaluateDiagnostics = (diagnosticsForThisEditor: Diagnostic[]) => {
  if (diagnosticsForThisEditor.some((d) => d.severity === "ERROR")) {
    return "ERROR";
  } else if (diagnosticsForThisEditor.some((d) => d.severity === "WARNING")) {
    return "WARNING";
  } else {
    return "OK";
  }
};

export const log = (message: any, ...args: any[]) => {
  if (typeof consola === "undefined") {
    // eslint-disable-next-line no-console
    console.log(message, ...args);
  } else {
    consola.log(message, ...args);
  }
};
