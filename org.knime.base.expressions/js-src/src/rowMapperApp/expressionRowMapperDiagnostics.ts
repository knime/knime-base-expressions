import {
  getScriptingService,
  type UseCodeEditorReturn,
} from "@knime/scripting-editor";
import type { Diagnostic, EditorErrorState } from "@/common/types";
import {
  getMostSevereDiagnostic,
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
): Promise<EditorErrorState[]> => {
  const newTexts = editorStates.map((editorState) => editorState.text.value);

  const diagnostics: Diagnostic[][] = await getScriptingService().sendToService(
    "getRowMapperDiagnostics",
    [newTexts, appendedColumns],
  );

  diagnostics.forEach((diagnosticsForThisEditor, index) =>
    markDiagnosticsInEditor(diagnosticsForThisEditor, editorStates[index]),
  );

  return diagnostics.map(getMostSevereDiagnostic).map((d): EditorErrorState => {
    if (d === null || (d.severity !== "ERROR" && d.severity !== "WARNING")) {
      return {
        level: "OK",
      };
    } else {
      return {
        level: d.severity,
        message: d.shortMessage,
      };
    }
  });
};
