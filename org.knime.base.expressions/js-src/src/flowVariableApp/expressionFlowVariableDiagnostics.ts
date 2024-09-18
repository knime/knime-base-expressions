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
 * @param allFlowVariableOutputNames a list of all output flow variables names.
 */
export const runFlowVariableDiagnostics = async (
  editorStates: UseCodeEditorReturn[],
  allFlowVariableOutputNames: string[],
): Promise<EditorErrorState[]> => {
  const newTexts = editorStates.map((editorState) => editorState.text.value);

  const diagnostics: ExpressionDiagnostic[][] =
    await getScriptingService().sendToService("getFlowVariableDiagnostics", [
      newTexts,
      allFlowVariableOutputNames,
    ]);

  diagnostics.forEach((diagnosticsForThisEditor, index) =>
    markDiagnosticsInEditor(diagnosticsForThisEditor, editorStates[index]),
  );

  return diagnostics.map(getEditorErrorStateFromDiagnostics);
};
