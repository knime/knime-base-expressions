import { type InitMockData } from "@knime/scripting-editor";
import { createInitialDataServiceMock } from "@knime/scripting-editor/initial-data-service-browser-mock";
import { createScriptingServiceMock } from "@knime/scripting-editor/scripting-service-browser-mock";
import { createSettingsServiceMock } from "@knime/scripting-editor/settings-service-browser-mock";

import { ROW_FILTER_INITIAL_DATA } from "@/__mocks__/mock-data";
import { log } from "@/common/functions";
import type { ExpressionRowFilterNodeSettings } from "@/expressionSettingsService";

export const DEFAULT_ROW_FILTER_INITIAL_SETTINGS: ExpressionRowFilterNodeSettings =
  {
    languageVersion: 1,
    builtinFunctionsVersion: 1,
    builtinAggregationsVersion: 1,
    script: "mocked default script",
  };

export default {
  scriptingService: createScriptingServiceMock({
    sendToServiceMockResponses: {
      runRowMapperExpression: (options: any[] | undefined) => {
        log("runRowMapperExpression", options);
        return Promise.resolve();
      },
      getRowFilterDiagnostics: () => Promise.resolve([]),
    },
  }),
  initialDataService: createInitialDataServiceMock(ROW_FILTER_INITIAL_DATA),
  settingsService: createSettingsServiceMock(
    DEFAULT_ROW_FILTER_INITIAL_SETTINGS,
  ),
  displayMode: "large",
} satisfies InitMockData;
