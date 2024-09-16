import {
  getScriptingService,
  type UseCodeEditorReturn,
} from "@knime/scripting-editor";
import type { ExpressionDiagnostic, EditorErrorState } from "@/common/types";
import {
  getEditorErrorStateFromDiagnostics,
  markDiagnosticsInEditor,
} from "@/generalDiagnostics";

/**
 * Run the diagnostics for all editors and set the markers in the respective editors.
 *
 * @param editorStates
 * @param allOutputColumnNames  a list of output columns, so either the name of the replaced
 *                              or the appended columnName.
 */
export const runRowMapperDiagnostics = async (
  editorStates: UseCodeEditorReturn[],
  allOutputColumnNames: string[],
): Promise<EditorErrorState[]> => {
  const newTexts = editorStates.map((editorState) => editorState.text.value);

  const diagnostics: ExpressionDiagnostic[][] =
    await getScriptingService().sendToService("getRowMapperDiagnostics", [
      newTexts,
      allOutputColumnNames,
    ]);

  diagnostics.forEach((diagnosticsForThisEditor, index) =>
    markDiagnosticsInEditor(diagnosticsForThisEditor, editorStates[index]),
  );

  return diagnostics.map(getEditorErrorStateFromDiagnostics);
};
