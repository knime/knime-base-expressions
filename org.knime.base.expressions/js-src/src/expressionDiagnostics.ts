import {
  getScriptingService,
  type UseCodeEditorReturn,
} from "@knime/scripting-editor";
import { editor as MonacoEditor, MarkerSeverity, Range } from "monaco-editor";
import { useStore } from "./store";

type Diagnostic = {
  message: string;
  severity: "ERROR" | "WARNING" | "INFO" | "HINT";
  location: {
    start: number;
    stop: number;
  };
};

const EXPRESSION_MARKERS_OWNER = "expression-diagnostics";
const DIAGNOSTIC_SEVERITY_TO_MARKER_SEVERITY = {
  ERROR: MarkerSeverity.Error,
  WARNING: MarkerSeverity.Warning,
  INFO: MarkerSeverity.Info,
  HINT: MarkerSeverity.Hint,
};

const store = useStore();

/**
 * Run the diagnostics for all editors and set the markers in the respective editors.
 *
 * @param editorStates
 * @param appendedColumns the new columns. This has to have the same length as editorStates, so if any
 * editorState is not associated with a new column, pass a null string.
 */
export const runDiagnostics = (
  editorStates: UseCodeEditorReturn[],
  appendedColumns: (string | null)[],
) => {
  const newTexts = editorStates.map((editorState) => editorState.text.value);

  getScriptingService()
    .sendToService("getDiagnostics", [newTexts, appendedColumns])
    .then((diagnostics: Diagnostic[][]) => {
      // Mark the diagnostics in the editor
      diagnostics.forEach((diagnosticsForThisEditor, i) => {
        const editorState = editorStates[i];
        const markers = diagnosticsForThisEditor.map(
          (diagnostic) =>
            ({
              ...Range.fromPositions(
                editorState.editorModel.getPositionAt(
                  diagnostic.location.start,
                ),
                editorState.editorModel.getPositionAt(diagnostic.location.stop),
              ),
              message: diagnostic.message,
              severity:
                DIAGNOSTIC_SEVERITY_TO_MARKER_SEVERITY[diagnostic.severity],
            }) satisfies MonacoEditor.IMarkerData,
        );

        MonacoEditor.setModelMarkers(
          editorState.editorModel,
          EXPRESSION_MARKERS_OWNER,
          markers,
        );
      });

      store.expressionValid = !diagnostics.some((diagnosticsForThisEditor) =>
        diagnosticsForThisEditor.some((d) => d.severity === "ERROR"),
      );
    });
};
