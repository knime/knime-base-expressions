import { beforeEach, describe, expect, it, vi } from "vitest";
import { nextTick } from "vue";
import { flushPromises, mount } from "@vue/test-utils";

import { FLOW_VARIABLE_INITIAL_DATA } from "@/__mocks__/mock-data";
import type { Diagnostic } from "@/generalDiagnostics";
import registerKnimeExpressionLanguage from "@/languageSupport/registerKnimeExpressionLanguage";
import ExpressionFlowVariableApp from "../ExpressionFlowVariableApp.vue";
import { DEFAULT_FLOW_VARIABLE_INITIAL_SETTINGS } from "../__mocks__/browser-mock-flow-variable-services";

vi.mock("@/languageSupport/registerKnimeExpressionLanguage", () => ({
  default: vi.fn(() => vi.fn()),
}));

const mockedScriptingService = vi.hoisted(() => ({
  sendToService: vi.fn(),
  getInitialData: vi.fn(() => Promise.resolve(FLOW_VARIABLE_INITIAL_DATA)),
  registerSettingsGetterForApply: vi.fn(),
  registerEventHandler: vi.fn(),
}));

vi.mock("@knime/scripting-editor", async () => ({
  ...(await vi.importActual("@knime/scripting-editor")),
  getScriptingService: vi.fn(() => mockedScriptingService),
}));

vi.mock("@/expressionInitialDataService", () => ({
  getFlowVariableInitialDataService: vi.fn(() => ({
    getInitialData: vi.fn(() => FLOW_VARIABLE_INITIAL_DATA),
  })),
}));

vi.mock("@/expressionSettingsService", () => ({
  getFlowVariableSettingsService: vi.fn(() => ({
    getSettings: vi.fn(() =>
      Promise.resolve(DEFAULT_FLOW_VARIABLE_INITIAL_SETTINGS),
    ),
    registerSettingsGetterForApply: vi.fn(),
  })),
}));

vi.mock("@/flowVariableApp/expressionFlowVariableDiagnostics", () => ({
  runFlowVariableDiagnostics: vi.fn(() =>
    Promise.resolve([
      {
        errorState: { level: "OK" },
        returnType: { displayName: "BOOLEAN" },
      } satisfies Diagnostic,
    ]),
  ),
}));

describe("ExpressionFlowVariableApp.vue", () => {
  const doMount = () => {
    const wrapper = mount(ExpressionFlowVariableApp, {
      global: {
        stubs: {
          TabBar: true,
          OutputConsole: true,
        },
      },
    });
    return { wrapper };
  };

  beforeEach(() => {
    vi.resetModules();
  });

  it("renders the ScriptingEditor component with the correct language", async () => {
    const { wrapper } = doMount();

    await flushPromises();
    await nextTick();

    const scriptingComponent = wrapper.findComponent({
      name: "ScriptingEditor",
    });
    expect(scriptingComponent.exists()).toBeTruthy();
    expect(scriptingComponent.props("language")).toBe("knime-expression");
  });

  it("registers the knime expression language and runs diagnostics", async () => {
    doMount();

    await flushPromises();

    expect(registerKnimeExpressionLanguage).toHaveBeenCalled();
  });

  it("renders function catalog with data from service", async () => {
    const { wrapper } = doMount();

    // Resolve the promise - the function catalog should now be rendered
    await flushPromises();

    const functionCatalog = wrapper.findComponent({ name: "FunctionCatalog" });
    expect(functionCatalog.exists()).toBeTruthy();
    expect(functionCatalog.props("functionCatalogData")).toEqual(
      FLOW_VARIABLE_INITIAL_DATA.functionCatalog,
    );
  });

  it("renders flow variable selector and check that it is updated when promises are resolved", async () => {
    const { wrapper } = doMount();

    await flushPromises();
    await nextTick();

    const flowVariableSelector = wrapper.findComponent({
      name: "OutputSelector",
    });

    expect(flowVariableSelector.exists()).toBeTruthy();

    await flushPromises();
    await nextTick();

    expect(flowVariableSelector.props("modelValue")).toEqual({
      create: DEFAULT_FLOW_VARIABLE_INITIAL_SETTINGS.createdFlowVariables[0],
      outputMode: "APPEND",
      replace: DEFAULT_FLOW_VARIABLE_INITIAL_SETTINGS.replacedFlowVariables[0],
    });
  });
});
