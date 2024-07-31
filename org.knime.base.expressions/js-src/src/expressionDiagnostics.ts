import {
  getScriptingService,
  type UseCodeEditorReturn,
} from "@knime/scripting-editor";
import { editor as MonacoEditor, MarkerSeverity, Range } from "monaco-editor";
import type { ColumnSelectorState } from "./components/ColumnOutputSelector.vue";

type Diagnostic = {
  message: string;
  severity: "ERROR" | "WARNING" | "INFO" | "HINT";
  location: {
    start: number;
    stop: number;
  };
};

// We don't care about INFO and HINT - only errors and warnings count towards the error level
export type ErrorLevel = "ERROR" | "WARNING" | "OK";

const EXPRESSION_MARKERS_OWNER = "expression-diagnostics";
const DIAGNOSTIC_SEVERITY_TO_MARKER_SEVERITY = {
  ERROR: MarkerSeverity.Error,
  WARNING: MarkerSeverity.Warning,
  INFO: MarkerSeverity.Info,
  HINT: MarkerSeverity.Hint,
};

/**
 * Run the diagnostics for all editors and set the markers in the respective editors.
 *
 * @param editorStates
 * @param appendedColumns the new columns. This has to have the same length as editorStates, so if any
 * editorState is not associated with a new column, pass a null string.
 */
export const runDiagnostics = async (
  editorStates: UseCodeEditorReturn[],
  appendedColumns: (string | null)[],
): Promise<ErrorLevel[]> => {
  const newTexts = editorStates.map((editorState) => editorState.text.value);

  const diagnostics: Diagnostic[][] = await getScriptingService().sendToService(
    "getDiagnostics",
    [newTexts, appendedColumns],
  );

  // Mark the diagnostics in the editor
  diagnostics.forEach((diagnosticsForThisEditor, i) => {
    const editorState = editorStates[i];
    const markers = diagnosticsForThisEditor.map(
      (diagnostic): MonacoEditor.IMarkerData => ({
        ...Range.fromPositions(
          editorState.editorModel.getPositionAt(diagnostic.location.start),
          editorState.editorModel.getPositionAt(diagnostic.location.stop),
        ),
        message: diagnostic.message,
        severity: DIAGNOSTIC_SEVERITY_TO_MARKER_SEVERITY[diagnostic.severity],
      }),
    );

    MonacoEditor.setModelMarkers(
      editorState.editorModel,
      EXPRESSION_MARKERS_OWNER,
      markers,
    );
  });

  return diagnostics.map((diagnosticsForThisEditor) => {
    if (diagnosticsForThisEditor.some((d) => d.severity === "ERROR")) {
      return "ERROR";
    } else if (diagnosticsForThisEditor.some((d) => d.severity === "WARNING")) {
      return "WARNING";
    } else {
      return "OK";
    }
  });
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
