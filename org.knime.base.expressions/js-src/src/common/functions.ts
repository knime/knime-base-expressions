import { BrowserReporter, Consola, LogLevel } from "consola";

import { FUNCTION_INSERTION_EVENT } from "@/components/function-catalog/FunctionCatalog.vue";
import {
  COLUMN_INSERTION_EVENT,
  type InputConnectionInfo,
  type InsertionEvent,
  insertionEventHelper,
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

/*
 *  Loops over the portInformation array and checks if non-optional ports are
 *  not connected or if the connected upstream nodes are not configured or executed.
 *  If any of these conditions are met, the first error message is returned.
 *  If no error is found, null is returned.
 */
export const mapConnectionInfoToErrorMessage = (
  inputConnectionInfo: InputConnectionInfo[] | undefined,
): string | null => {
  if (typeof inputConnectionInfo === "undefined") {
    return "No initial data available. This is an implementation error.";
  }

  const errorMessages = inputConnectionInfo
    .map((port) => {
      if (port.isOptional) {
        return null;
      }
      switch (port.status) {
        case "MISSING_CONNECTION":
          return "To evaluate your expression, connect input data first.";
        case "UNCONFIGURED_CONNECTION":
          return "To evaluate your expression, configure the previous nodes first.";
        case "UNEXECUTED_CONNECTION":
          return "To evaluate your expression, execute the previous nodes first.";
        case "OK":
          break;
      }

      return null;
    })
    .filter((message) => message !== null);

  return errorMessages.length > 0 ? errorMessages[0] : null;
};
