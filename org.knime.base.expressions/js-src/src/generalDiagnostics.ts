import type { Diagnostic } from "@/common/types";
import type { UseCodeEditorReturn } from "@knime/scripting-editor";
import { editor as MonacoEditor, Range } from "monaco-editor";
import {
  DIAGNOSTIC_SEVERITY_TO_MARKER_SEVERITY,
  EXPRESSION_MARKERS_OWNER,
} from "@/common/constants";
import type { SelectorState } from "@/components/OutputSelector.vue";

export const markDiagnosticsInEditor = (
  diagnosticsForThisEditor: Diagnostic[],
  editorState: UseCodeEditorReturn,
) => {
  // Mark the diagnostics in the editor

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
};

export const evaluateDiagnostics = (diagnosticsForThisEditor: Diagnostic[]) => {
  if (diagnosticsForThisEditor.some((d) => d.severity === "ERROR")) {
    return "ERROR";
  } else if (diagnosticsForThisEditor.some((d) => d.severity === "WARNING")) {
    return "WARNING";
  } else {
    return "OK";
  }
};

export const runOutputDiagnostics = (
  label: string,
  orderedStates: SelectorState[],
  inputItems: string[],
): (string | null)[] => {
  const appendedItemsSoFar: string[] = [];

  return orderedStates.map((state) => {
    if (state.outputMode === "APPEND") {
      if (appendedItemsSoFar.includes(state.create)) {
        return `${label} "${state.create}" was appended twice!`;
      }
      if (inputItems.includes(state.create)) {
        return `${label} "${state.create}" already exists in input ${label.toLowerCase()}s`;
      }
      appendedItemsSoFar.push(state.create);
    }

    if (
      state.outputMode === "REPLACE_EXISTING" &&
      !(
        inputItems.includes(state.replace) ||
        appendedItemsSoFar.includes(state.replace)
      )
    ) {
      return `${label} "${state.replace}" does not exist in input or appended ${label.toLowerCase()}s`;
    }
    return null;
  });
};
