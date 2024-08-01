import type { ExpressionNodeSettings } from "./types";
import { getSettingsService } from "@knime/scripting-editor";

export const getExpressionSettingsService = () => ({
  ...getSettingsService(),
  getSettings: async () =>
    (await getSettingsService().getSettings()) as ExpressionNodeSettings,
  registerSettingsGetterForApply: (
    settingsGetter: () => ExpressionNodeSettings,
  ) => getSettingsService().registerSettingsGetterForApply(settingsGetter),
});
