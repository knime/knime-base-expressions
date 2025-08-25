import "vitest-canvas-mock";
import { vi } from "vitest";
import { Consola, LogLevels } from "consola";

import { initMocked } from "@knime/scripting-editor";

import {
  BASE_INITIAL_DATA,
  DEFAULT_INITIAL_SETTINGS,
} from "../src/__mocks__/mock-data";

export const consola = new Consola({
  level: LogLevels.log,
});

// @ts-expect-error TODO how to tell TS that consola is a global?
window.consola = consola;

// Mock the scrollIntoView method, which is not implemented in jsdom.
// https://github.com/vuejs/vue-test-utils/issues/1219
Element.prototype.scrollIntoView = vi.fn();

// TODO(AP-24858) adapt the mock or remove it
// NB: We do not use importActual here, because we want to ensure that no original code of
// @knime/ui-extension-service is used. The original code could cause unexpected timeouts on
// `getConfig`.
vi.mock("@knime/ui-extension-service", () => ({
  JsonDataService: {
    getInstance: vi.fn(() =>
      Promise.resolve({
        // We only need to mock the baseService used by the embedded UI Extensions
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
        },
      }),
    ),
  },
}));

// Initialize @knime/scripting-editor with mock data
initMocked({
  scriptingService: {
    sendToService: vi.fn(),
    getOutputPreviewTableInitialData: vi.fn(() => Promise.resolve(undefined)),
    registerEventHandler: vi.fn(),
    connectToLanguageServer: vi.fn(),
    isKaiEnabled: vi.fn(),
    isLoggedIntoHub: vi.fn(),
    getAiDisclaimer: vi.fn(),
    getAiUsage: vi.fn(),
    isCallKnimeUiApiAvailable: vi.fn(() => Promise.resolve(false)),
  },
  settingsService: {
    getSettings: vi.fn(() => Promise.resolve(DEFAULT_INITIAL_SETTINGS)),
    registerSettingsGetterForApply: vi.fn(),
    registerSettings: vi.fn(() => vi.fn()),
  },
  initialData: BASE_INITIAL_DATA,
  displayMode: "small",
});
