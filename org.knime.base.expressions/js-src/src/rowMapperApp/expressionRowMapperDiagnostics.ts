import {
  getScriptingService,
  type UseCodeEditorReturn,
} from "@knime/scripting-editor";
import type { Diagnostic, ErrorLevel } from "@/common/types";
import {
  evaluateDiagnostics,
  markDiagnosticsInEditor,
} from "@/generalDiagnostics";

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
    "getRowMapperDiagnostics",
    [newTexts, appendedColumns],
  );

  diagnostics.forEach((diagnosticsForThisEditor, index) =>
    markDiagnosticsInEditor(diagnosticsForThisEditor, editorStates[index]),
  );

  return diagnostics.map(evaluateDiagnostics);
};