import { enableAutoUnmount, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "../App.vue";
import registerKnimeExpressionLanguage from "@/registerKnimeExpressionLanguage";

vi.mock("@/registerKnimeExpressionLanguage", () => ({
  default: vi.fn(() => vi.fn()),
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
});
