import {
  getInitialDataService,
  getScriptingService,
  getSettingsService,
} from "@knime/scripting-editor";
import { createInitialDataServiceMock } from "@knime/scripting-editor/initial-data-service-browser-mock";
import { createScriptingServiceMock } from "@knime/scripting-editor/scripting-service-browser-mock";
import { createSettingsServiceMock } from "@knime/scripting-editor/settings-service-browser-mock";

import { INPUT_OBJECTS, ROW_MAPPER_INITIAL_DATA } from "@/__mocks__/mock-data";
import { log } from "@/common/functions";
import type { ExpressionRowMapperNodeSettings } from "@/expressionSettingsService";
import type { ExpressionDiagnosticResult } from "@/generalDiagnostics";

export const DEFAULT_ROW_MAPPER_INITIAL_SETTINGS: ExpressionRowMapperNodeSettings =
  {
    scripts: ["mocked default script"],
    outputModes: ["APPEND"],
    createdColumns: ["mocked output col"],
    replacedColumns: [INPUT_OBJECTS[0].subItems![1].name],
    languageVersion: 1,
    builtinFunctionsVersion: 1,
    builtinAggregationsVersion: 1,
  };

if (import.meta.env.MODE === "development.browser") {
  const scriptingService = createScriptingServiceMock({
    sendToServiceMockResponses: {
      runRowMapperExpression: (options: any[] | undefined) => {
        log("runRowFilterExpression", options);
        return Promise.resolve();
      },
      getRowMapperDiagnostics: (
        options,
      ): Promise<ExpressionDiagnosticResult[]> =>
        Promise.resolve(
          options![0].map(() => ({ diagnostics: [], returnType: "STRING" })),
        ),
    },
  });

  Object.assign(getScriptingService(), scriptingService);

  Object.assign(
    getInitialDataService(),
    createInitialDataServiceMock(ROW_MAPPER_INITIAL_DATA),
  );

  Object.assign(
    getSettingsService(),
    createSettingsServiceMock(DEFAULT_ROW_MAPPER_INITIAL_SETTINGS),
  );
}
