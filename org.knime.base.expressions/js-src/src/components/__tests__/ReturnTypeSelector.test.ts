import { afterEach, describe, expect, it, vi } from "vitest";
import { enableAutoUnmount, mount } from "@vue/test-utils";

import { Dropdown } from "@knime/components";

import type { AllowedReturnTypes } from "@/components/ReturnTypeSelector.vue";
import ReturnTypeSelector from "@/components/ReturnTypeSelector.vue";
import { type FlowVariableType } from "@/flowVariableApp/flowVariableTypes";
import { DataType } from "@knime/kds-components";

describe("ReturnTypeSelector", () => {
  enableAutoUnmount(afterEach);

  const integerReturnTypes: AllowedReturnTypes[] = [
    {
      id: "Long",
      text: "B",
      type: { id: "LongId", text: "LongText" },
    },
    {
      id: "Integer",
      text: "A",
      type: { id: "IntegerId", text: "IntegerText" },
    },
  ];

  const unknownReturnType: AllowedReturnTypes[] = [
    {
      id: "Unknown",
      text: "Unknown",
      type: { id: "UnknownId", text: "UnknownText" },
    },
  ];

  const stringReturnType: AllowedReturnTypes[] = [
    {
      id: "String",
      text: "String",
      type: { id: "StringId", text: "StringText" },
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

  it("renders the data type component", () => {
    const wrapper = doMount("String", stringReturnType);
    const dataType = wrapper.findComponent(DataType);
    expect(dataType.exists()).toBe(true);
    expect(dataType.props()).toStrictEqual(expect.objectContaining({

      iconName: "StringId",
      iconTitle: "StringText",
    }));
  });
});
