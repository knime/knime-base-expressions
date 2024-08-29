import "vitest-canvas-mock";
import { Consola, LogLevel } from "consola";
import {
  DEFAULT_INITIAL_DATA,
  DEFAULT_INITIAL_SETTINGS,
} from "../src/__mocks__/mock-data";
import { vi } from "vitest";

export const consola = new Consola({
  level: LogLevel.Log,
});

// @ts-expect-error
window.consola = consola;

// The ui extension service is used basically everywhere.
// This mock will prevent the EventPoller from crashing the test.
// Therefore, a global mock is necessary.
vi.mock("@knime/ui-extension-service", async () => ({
  ...(await vi.importActual("@knime/ui-extension-service")),
  JsonDataService: {
    getInstance: vi.fn(() =>
      Promise.resolve({
        initialData: vi.fn(() =>
          Promise.resolve({
            settings: DEFAULT_INITIAL_SETTINGS,
            initialData: DEFAULT_INITIAL_DATA,
          }),
        ),
        baseService: {
          getConfig: vi.fn(() =>
            Promise.resolve({
              nodeId: "nodeId",
              projectId: "projectId",
              workflowId: "workflowId",
              resourceInfo: {
                baseUrl: "http://localhost/",
                path: "something/something/someFile.html",
              },
            }),
          ),
          callNodeDataService: vi.fn(() => Promise.resolve({})),
        },
        data: vi.fn(() => {
          // This promise never resolves, so this method will never return.
          // This stops the EventPoller from getting an unexpected event,
          // which would cause it to throw an error and crash the test.
          return new Promise(() => {});
        }),
      }),
    ),
  },
  ReportingService: {},
  AlertingService: {
    getInstance: vi.fn(() =>
      Promise.resolve({
        sendAlert: vi.fn(),
      }),
    ),
  },
}));
