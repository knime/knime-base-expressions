import { getScriptingService } from "@knime/scripting-editor";
import { enableAutoUnmount, flushPromises, mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import App from "../App.vue";
import registerKnimeExpressionLanguage from "@/registerKnimeExpressionLanguage";
import { type FunctionCatalogData } from "../functionCatalogTypes";

vi.mock("@/registerKnimeExpressionLanguage", () => ({
  default: vi.fn(() => vi.fn()),
}));

const lock = <T = void>() => {
  let resolve: (resolvedValue: T) => void = () => {};
  const promise = new Promise<T>((r) => {
    resolve = r;
  });
  return { promise, resolve };
};

const TEST_FUNCTION_CATALOG = {
  categories: [
    {
      name: "test",
      description: "Test category",
    },
  ],
  functions: [
    {
      name: "testFunction",
      category: "test",
      keywords: ["test"],
      displayName: "Test Function",
      description: "Test function",
      arguments: [
        {
          name: "arg1",
          type: "string",
          description: "Argument 1",
        },
      ],
      returnType: "string",
      returnDescription: "Return value",
    },
  ],
} satisfies FunctionCatalogData;

const TEST_MATH_CONSTANTS = [
  {
    name: "PI",
    value: 3,
    type: "Float",
    documentation: "The *real* value of Pi",
  },
  { name: "E", value: 3, type: "String", documentation: "The letter E" },
];

vi.mock("@/expressionScriptingService", () => ({
  getExpressionScriptingService: () => ({
    ...getScriptingService(),
    getFunctions: vi.fn(() => Promise.resolve(TEST_FUNCTION_CATALOG)),
    getMathConstants: vi.fn(() => Promise.resolve(TEST_MATH_CONSTANTS)),
  }),
}));

describe("App.vue", () => {
  enableAutoUnmount(afterEach);

  const doMount = () => {
    const wrapper = mount(App, {
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
    vi.mocked(getScriptingService().sendToService).mockImplementation(
      (methodName: string) => {
        if (methodName === "getFunctionCatalog") {
          return Promise.resolve(TEST_FUNCTION_CATALOG);
        } else if (methodName === "getMathConstants") {
          return Promise.resolve(TEST_MATH_CONSTANTS);
        }
        throw new Error(
          `Called unexpected scripting service method ${methodName}`,
        );
      },
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("renders the ScriptingEditor component with the correct language", () => {
    const { wrapper } = doMount();
    const scriptingComponent = wrapper.findComponent({
      name: "ScriptingEditor",
    });
    expect(scriptingComponent.exists()).toBeTruthy();
    expect(scriptingComponent.props("language")).toBe("knime-expression");
  });

  it("registers the knime expression language", () => {
    doMount();

    expect(registerKnimeExpressionLanguage).toHaveBeenCalled();
  });

  it("renders function catalog with data from service", async () => {
    const { promise, resolve } = lock<FunctionCatalogData>();

    vi.mocked(getScriptingService().sendToService).mockImplementation(
      (methodName: string) => {
        // eslint-disable-next-line vitest/no-conditional-tests
        if (methodName === "getFunctionCatalog") {
          return promise;
        } else {
          return Promise.resolve();
        }
      },
    );
    const { wrapper } = doMount();
    await flushPromises();

    // Resolve the promise - the function catalog should now be rendered
    resolve(TEST_FUNCTION_CATALOG);
    await flushPromises();
    const functionCatalog = wrapper.findComponent({ name: "FunctionCatalog" });
    expect(functionCatalog.exists()).toBeTruthy();
    expect(functionCatalog.props("functionCatalogData")).toEqual(
      TEST_FUNCTION_CATALOG,
    );
  });
});
