import { enableAutoUnmount, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import { Dropdown } from "@knime/components";
import type { AllowedReturnTypes } from "@/components/ReturnTypeSelector.vue";
import ReturnTypeSelector from "@/components/ReturnTypeSelector.vue";
import { type FlowVariableType } from "@/flowVariableApp/flowVariableTypes";

describe("ReturnTypeSelector", () => {
  enableAutoUnmount(afterEach);

  const integerReturnTypes: AllowedReturnTypes[] = [
    {
      id: "Long",
      text: "B",
    },
    {
      id: "Integer",
      text: "A",
    },
  ];

  const unknownReturnType: AllowedReturnTypes[] = [
    {
      id: "Unknown",
      text: "Unknown",
    },
  ];

  const stringReturnType: AllowedReturnTypes[] = [
    {
      id: "String",
      text: "String",
    },
  ];

  const doMount = (
    modelValue: FlowVariableType = "Unknown",
    allowedReturnTypes: AllowedReturnTypes[] = [],
  ) => {
    return mount(ReturnTypeSelector, {
      props: {
        allowedReturnTypes,
        modelValue,
      },
    });
  };

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("switches automatically to a valid returnType when options change", async () => {
    const wrapper = doMount();

    expect(wrapper.findComponent(Dropdown).props().modelValue).equals(
      "Unknown",
    );

    await wrapper.setProps({ allowedReturnTypes: integerReturnTypes });

    expect(wrapper.findComponent(Dropdown).props().modelValue).equals(
      integerReturnTypes[0].id,
    );
  });

  it("remembers the last choice for integral types", async () => {
    const wrapper = doMount("Long", integerReturnTypes);
    const dropdown = wrapper.findComponent(Dropdown);

    expect(dropdown.props().modelValue).equals("Long");

    await wrapper.setProps({
      modelValue: "Integer",
    });
    expect(dropdown.props().modelValue).equals("Integer");

    await wrapper.setProps({ allowedReturnTypes: unknownReturnType });
    expect(dropdown.props().modelValue).equals("Unknown");

    await wrapper.setProps({ allowedReturnTypes: integerReturnTypes });
    expect(wrapper.findComponent(Dropdown).props().modelValue).equals(
      "Integer",
    );
  });

  it("disables the dropdown when there is only one option", () => {
    const wrapper = doMount("String", stringReturnType);
    const dropdown = wrapper.findComponent(Dropdown);

    expect(dropdown.props().modelValue).equals("String");
    expect(dropdown.props().disabled).toBe(true);
  });
});
