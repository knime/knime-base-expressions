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
  const mockScriptingService = {
    sendToService: vi.fn((args) => {
      // If this method is not mocked, the tests fail with a hard to debug
      // error otherwise, so we're really explicit here.
      throw new Error(
        `ScriptingService.sendToService should have been mocked for method ${args}`,
      );
    }),
    getInitialSettings: vi.fn(() =>
      Promise.resolve({ script: "myexpression" }),
    ),
    saveSettings: vi.fn(),
    registerEventHandler: vi.fn(),
    connectToLanguageServer: vi.fn(),
    configureLanguageServer: vi.fn(),
    inputsAvailable: vi.fn(() => Promise.resolve(true)),
    closeDialog: vi.fn(),
    getInputObjects: vi.fn(),
    getFlowVariableInputs: vi.fn(),
  } satisfies Partial<ScriptingServiceType>;

  const scriptEditorModule = await vi.importActual("@knime/scripting-editor");

  return {
    ...scriptEditorModule,
    getScriptingService: () => {
      return mockScriptingService;
    },
  };
});
