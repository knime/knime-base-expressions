import { enableAutoUnmount, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import { Dropdown } from "@knime/components";
import type { AllowedReturnTypes } from "@/components/ReturnTypeSelector.vue";
import ReturnTypeSelector from "@/components/ReturnTypeSelector.vue";
import {
  type FlowVariableType,
  flowVariableTypes,
} from "@/flowVariableApp/flowVariableTypes";

describe("ReturnTypeSelector", () => {
  enableAutoUnmount(afterEach);

  const integerReturnTypes: AllowedReturnTypes[] = [
    { id: flowVariableTypes.Long, text: "B" },
    { id: flowVariableTypes.Integer, text: "A" },
  ];

  const unknownReturnType: AllowedReturnTypes[] = [
    { id: flowVariableTypes.Unknown, text: flowVariableTypes.Unknown },
  ];

  const stringReturnType: AllowedReturnTypes[] = [
    { id: flowVariableTypes.String, text: flowVariableTypes.String },
  ];

  const doMount = (
    modelValue: FlowVariableType = flowVariableTypes.Unknown,
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
      flowVariableTypes.Unknown,
    );

    await wrapper.setProps({ allowedReturnTypes: integerReturnTypes });

    expect(wrapper.findComponent(Dropdown).props().modelValue).equals(
      integerReturnTypes[0].id,
    );
  });

  it("remembers the last choice for integral types", async () => {
    const wrapper = doMount(flowVariableTypes.Long, integerReturnTypes);
    const dropdown = wrapper.findComponent(Dropdown);

    expect(dropdown.props().modelValue).equals(flowVariableTypes.Long);

    await wrapper.setProps({ modelValue: flowVariableTypes.Integer });
    expect(dropdown.props().modelValue).equals(flowVariableTypes.Integer);

    await wrapper.setProps({ allowedReturnTypes: unknownReturnType });
    expect(dropdown.props().modelValue).equals(flowVariableTypes.Unknown);

    await wrapper.setProps({ allowedReturnTypes: integerReturnTypes });
    expect(wrapper.findComponent(Dropdown).props().modelValue).equals(
      flowVariableTypes.Integer,
    );
  });

  it("disables the dropdown when there is only one option", () => {
    const wrapper = doMount(flowVariableTypes.String, stringReturnType);
    const dropdown = wrapper.findComponent(Dropdown);

    expect(dropdown.props().modelValue).equals(flowVariableTypes.String);
    expect(dropdown.props().disabled).toBe(true);
  });
});
