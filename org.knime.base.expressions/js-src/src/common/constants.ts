import { MarkerSeverity } from "monaco-editor";

export const EXPRESSION_MARKERS_OWNER = "expression-diagnostics";
export const DIAGNOSTIC_SEVERITY_TO_MARKER_SEVERITY = {
  ERROR: MarkerSeverity.Error,
  WARNING: MarkerSeverity.Warning,
  INFO: MarkerSeverity.Info,
  HINT: MarkerSeverity.Hint,
};

export const LANGUAGE = "knime-expression";
export const DEFAULT_NUMBER_OF_ROWS_TO_RUN = 10;

export const WATCH_DEBOUNCE_TIMEOUT = 500;
