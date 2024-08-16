import type { ExpressionVersion, OutputInsertionMode } from "@/common/types";
import type { GenericNodeSettings } from "@knime/scripting-editor";
import { getSettingsService } from "@knime/scripting-editor";

export type ExpressionRowFilterNodeSettings = ExpressionVersion & {
  script: string;
  settingsAreOverriddenByFlowVariable?: boolean;
};

export type ExpressionRowMapperNodeSettings = ExpressionVersion & {
  scripts: string[];
  settingsAreOverriddenByFlowVariable?: boolean;
  outputModes: OutputInsertionMode[];
  createdColumns: string[];
  replacedColumns: string[];
};

const getExpressionSettingsService = <T extends GenericNodeSettings>() => ({
  ...getSettingsService(),
  getSettings: async (): Promise<T> =>
    (await getSettingsService().getSettings()) as T,
  registerSettingsGetterForApply: (settingsGetter: () => T) =>
    getSettingsService().registerSettingsGetterForApply(settingsGetter),
});

export const getRowFilterSettingsService =
  getExpressionSettingsService<ExpressionRowFilterNodeSettings>;

export const getRowMapperSettingsService =
  getExpressionSettingsService<ExpressionRowMapperNodeSettings>;
