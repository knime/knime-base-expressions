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

export const UNKNOWN_COLUMN_TYPE = {
  id: "unknown-datatype",
  text: "Unknown data type",
};
export const UNKNOWN_VARIABLE_TYPE = {
  id: "UNKNOWN",
  text: "Unknown variable type",
};
