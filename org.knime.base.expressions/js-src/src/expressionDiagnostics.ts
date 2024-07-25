import {
  getScriptingService,
  type UseCodeEditorReturn,
} from "@knime/scripting-editor";
import { editor as MonacoEditor, MarkerSeverity, Range } from "monaco-editor";
import { useStore } from "./store";
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

const store = useStore();

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

  store.expressionValid = !diagnostics.some((diagnosticsForThisEditor) =>
    diagnosticsForThisEditor.some((d) => d.severity === "ERROR"),
  );

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
): boolean[] => {
  const appendedColumnsSoFar: string[] = [];

  const stateValidities: boolean[] = [];

  for (const state of orderedStates) {
    if (
      state.outputMode === "APPEND" &&
      (inputColumns.includes(state.createColumn) ||
        appendedColumnsSoFar.includes(state.createColumn))
    ) {
      stateValidities.push(false);
    } else if (
      state.outputMode === "REPLACE_EXISTING" &&
      !(
        inputColumns.includes(state.replaceColumn) ||
        appendedColumnsSoFar.includes(state.replaceColumn)
      )
    ) {
      stateValidities.push(false);
    } else {
      stateValidities.push(true);
    }

    if (state.outputMode === "APPEND") {
      appendedColumnsSoFar.push(state.createColumn);
    }
  }

  return stateValidities;
};
