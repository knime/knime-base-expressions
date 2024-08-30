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
 * @param editorState
 * @param appendedColumns the new columns. This has to have the same length as editorStates, so if any
 * editorState is not associated with a new column, pass a null string.
 */
export const runRowFilterDiagnostics = async (
  editorState: UseCodeEditorReturn,
): Promise<ErrorLevel> => {
  const newText = editorState.text.value;

  const diagnostics: Diagnostic[] = await getScriptingService().sendToService(
    "getRowFilterDiagnostics",
    [newText],
  );

  markDiagnosticsInEditor(diagnostics, editorState);

  return evaluateDiagnostics(diagnostics);
};
