import {
  getScriptingService,
  type UseCodeEditorReturn,
} from "@knime/scripting-editor";
import {
  type Diagnostic,
  type ExpressionDiagnosticResult,
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
): Promise<Diagnostic[]> => {
  const newTexts = editorStates.map((editorState) => editorState.text.value);

  const results: ExpressionDiagnosticResult[] =
    await getScriptingService().sendToService("getRowMapperDiagnostics", [
      newTexts,
      allOutputColumnNames,
    ]);

  results.forEach(({ diagnostics }, index) =>
    markDiagnosticsInEditor(diagnostics, editorStates[index]),
  );

  return results.map(({ diagnostics, returnType }) => ({
    errorState: getEditorErrorStateFromDiagnostics(diagnostics),
    returnType,
  }));
};
