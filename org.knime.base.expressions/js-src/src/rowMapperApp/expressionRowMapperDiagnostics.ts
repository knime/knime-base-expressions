import {
  getScriptingService,
  type UseCodeEditorReturn,
} from "@knime/scripting-editor";
import type { ColumnSelectorState } from "./ColumnOutputSelector.vue";
import {
  evaluateDiagnostics,
  markDiagnosticsInEditor,
} from "@/common/functions";
import type { Diagnostic, ErrorLevel } from "@/common/types";

/**
 * Run the diagnostics for all editors and set the markers in the respective editors.
 *
 * @param editorStates
 * @param appendedColumns the new columns. This has to have the same length as editorStates, so if any
 * editorState is not associated with a new column, pass a null string.
 */
export const runRowMapperDiagnostics = async (
  editorStates: UseCodeEditorReturn[],
  appendedColumns: (string | null)[],
): Promise<ErrorLevel[]> => {
  const newTexts = editorStates.map((editorState) => editorState.text.value);

  const diagnostics: Diagnostic[][] = await getScriptingService().sendToService(
    "getDiagnostics",
    [newTexts, appendedColumns],
  );

  diagnostics.forEach((diagnosticsForThisEditor, index) =>
    markDiagnosticsInEditor(diagnosticsForThisEditor, editorStates[index]),
  );

  return diagnostics.map(evaluateDiagnostics);
};

export const runColumnOutputDiagnostics = (
  orderedStates: ColumnSelectorState[],
  inputColumns: string[],
): (string | null)[] => {
  const appendedColumnsSoFar: string[] = [];

  return orderedStates.map((state) => {
    if (state.outputMode === "APPEND") {
      if (appendedColumnsSoFar.includes(state.createColumn)) {
        return `Column "${state.createColumn}" was appended twice!`;
      }
      if (inputColumns.includes(state.createColumn)) {
        return `Column "${state.createColumn}" already exists in input columns`;
      }
      appendedColumnsSoFar.push(state.createColumn);
    }

    if (
      state.outputMode === "REPLACE_EXISTING" &&
      !(
        inputColumns.includes(state.replaceColumn) ||
        appendedColumnsSoFar.includes(state.replaceColumn)
      )
    ) {
      return `Column "${state.replaceColumn}" does not exist in input or appended columns`;
    }
    return null;
  });
};
