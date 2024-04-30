import {
  getScriptingService,
  type UseCodeEditorReturn,
} from "@knime/scripting-editor";
import { editor as MonacoEditor, MarkerSeverity, Range } from "monaco-editor";
import { watch } from "vue";
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

export const registerExpressionDiagnostics = (
  editorState: UseCodeEditorReturn,
) => {
  watch(editorState.text, (newText) => {
    getScriptingService()
      .sendToService("getDiagnostics", [newText])
      .then((diagnostics: Diagnostic[]) => {
        // Update the store
        store.expressionValid = !diagnostics.some(
          (d) => d.severity === "ERROR",
        );

        // Mark the diagnostics in the editor
        const markers = diagnostics.map((d) => ({
          ...Range.fromPositions(
            editorState.editorModel.getPositionAt(d.location.start),
            editorState.editorModel.getPositionAt(d.location.stop),
          ),
          message: d.message,
          severity: DIAGNOSTIC_SEVERITY_TO_MARKER_SEVERITY[d.severity],
        })) satisfies MonacoEditor.IMarkerData[];
        MonacoEditor.setModelMarkers(
          editorState.editorModel,
          EXPRESSION_MARKERS_OWNER,
          markers,
        );
      });
  });
};
