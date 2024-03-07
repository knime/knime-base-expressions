import "vitest-canvas-mock";
import { vi } from "vitest";
import { Consola, LogLevel } from "consola";
import { type ScriptingServiceType } from "@knime/scripting-editor";

export const consola = new Consola({
  level: LogLevel.Log,
});

// @ts-expect-error
window.consola = consola;

vi.mock("@knime/scripting-editor", async () => {
  const initialSettings = { script: "myexpression" };
  const mockScriptingService = {
    sendToService: vi.fn((args) => {
      // If this method is not mocked, the tests fail with a hard to debug
      // error otherwise, so we're really explicit here.
      throw new Error(
        `ScriptingService.sendToService should have been mocked for method ${args}`,
      );
    }),
    registerEventHandler: vi.fn(),
    isCodeAssistantEnabled: vi.fn(() => Promise.resolve(true)),
    isCodeAssistantInstalled: vi.fn(() => Promise.resolve(true)),
    inputsAvailable: vi.fn(() => Promise.resolve(true)),
    getInputObjects: vi.fn(() => Promise.resolve([])),
    getOutputObjects: vi.fn(() => Promise.resolve([])),
    getFlowVariableInputs: vi.fn(() => Promise.resolve({} as any)),
    getInitialSettings: vi.fn(() => Promise.resolve(initialSettings)),
    registerSettingsGetterForApply: vi.fn(),
  } satisfies Partial<ScriptingServiceType>;

  const scriptEditorModule = await vi.importActual("@knime/scripting-editor");

  // Replace the original implementations by the mocks
  // @ts-ignore
  Object.assign(scriptEditorModule.getScriptingService(), mockScriptingService);

  return {
    ...scriptEditorModule,
    getScriptingService: () => {
      return mockScriptingService;
    },
  };
});
