import {
  getScriptingService,
  type NodeSettings,
} from "@knime/scripting-editor";

export type OutputInsertionMode = "APPEND" | "REPLACE_EXISTING";

export type ExpressionNodeSettings = NodeSettings & {
  columnOutputMode: OutputInsertionMode;
  createdColumn: string;
  replacedColumn: string;
};

const scriptingService = getScriptingService();
const expressionScriptingService = {
  ...scriptingService,
  registerSettingsGetterForApply: (
    settingsGetter: () => ExpressionNodeSettings,
  ) => scriptingService.registerSettingsGetterForApply(settingsGetter),
  getInitialSettings: () =>
    scriptingService.getInitialSettings() as Promise<ExpressionNodeSettings>,
};

export type ExpressionScriptingServiceType = typeof expressionScriptingService;

export const getExpressionScriptingService =
  (): ExpressionScriptingServiceType => expressionScriptingService;
