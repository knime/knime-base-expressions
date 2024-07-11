import { getScriptingService } from "@knime/scripting-editor";
import { enableAutoUnmount, flushPromises, mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import App from "../App.vue";
import registerKnimeExpressionLanguage from "@/registerKnimeExpressionLanguage";
import { type FunctionCatalogData } from "../functionCatalogTypes";
import type { ExpressionNodeSettings } from "@/expressionScriptingService";
import { nextTick } from "vue";

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

const TEST_FUNCTION_CATALOG: FunctionCatalogData = {
  categories: [
    {
      name: "test",
      description: "Test category",
    },
    {
      name: "math constants",
      description: "Mathematical constants",
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
      entryType: "function",
    },
    {
      name: "PI",
      returnType: "Float",
      description: "The *real* value of Pi",
      category: "math constants",
      keywords: ["pi"],
      entryType: "constant",
    },
  ],
};

const INPUT_OBJECTS = [
  {
    name: "input1",
    portType: "table",
    subItems: [
      {
        name: "column1",
        type: "string",
      },
      {
        name: "column2",
        type: "int",
      },
    ],
  },
];

const INITIAL_SETTINGS: ExpressionNodeSettings = {
  scripts: ["myInitialScript"],
  languageVersion: 1,
  builtinFunctionsVersion: 1,
  builtinAggregationsVersion: 1,
  outputModes: ["APPEND"],
  createdColumns: ["New Column from test"],
  replacedColumns: [INPUT_OBJECTS[0].subItems[0].name],
};

vi.mock("@/expressionScriptingService", () => ({
  getExpressionScriptingService: () => ({
    ...getScriptingService(),
    getFunctions: vi.fn(() => Promise.resolve(TEST_FUNCTION_CATALOG)),
    getInitialSettings: vi.fn(() => Promise.resolve(INITIAL_SETTINGS)),
    getInputObjects: vi.fn(() => Promise.resolve(INPUT_OBJECTS)),
  }),
}));

vi.mock("@/expressionDiagnostics", () => ({
  runDiagnostics: vi.fn(() => Promise.resolve([[]])),
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
        }
        if (methodName === "getInputObjects") {
          return Promise.resolve(INPUT_OBJECTS);
        }
        if (methodName === "getInitialSettings") {
          return Promise.resolve(INITIAL_SETTINGS);
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

  it("registers the knime expression language", async () => {
    doMount();

    await flushPromises();

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

  it("renders column selector and check that it is updated when promises are resolved", async () => {
    const { wrapper } = doMount();

    await flushPromises();
    await nextTick();

    const columnSelector = wrapper.findComponent({
      name: "ColumnOutputSelector",
    });

    expect(columnSelector.exists()).toBeTruthy();

    await flushPromises();
    await nextTick();

    expect(columnSelector.props("modelValue")).toEqual({
      createColumn: INITIAL_SETTINGS.createdColumns[0],
      outputMode: "APPEND",
      replaceColumn: INPUT_OBJECTS[0].subItems[0].name,
    });
  });
});
