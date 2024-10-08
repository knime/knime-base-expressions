import { enableAutoUnmount, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import OutputSelector, { type SelectorState } from "../OutputSelector.vue";
import { Dropdown, InputField, ValueSwitch } from "@knime/components";

describe("OutputSelector", () => {
  enableAutoUnmount(afterEach);

  const columnsToReplace = [
    { id: "a", text: "A" },
    { id: "b", text: "B" },
    { id: "c", text: "C" },
    { id: "d", text: "D" },
  ];

  const doMount = (
    modelValue: SelectorState = {
      outputMode: "APPEND",
      create: "test column",
      replace: "a",
    },
  ) => {
    return mount(OutputSelector, {
      props: {
        selectorType: "column",
        allowedReplacementEntities: columnsToReplace,
        itemType: "column",
        modelValue,
      },
      attachTo: "body", // needed for label clicking to work
    });
  };

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("shows input field on APPEND mode", () => {
    const wrapper = doMount({
      outputMode: "APPEND",
      create: "a1",
      replace: "c",
    });
    expect(wrapper.findComponent(ValueSwitch).props().modelValue).equals(
      "APPEND",
    );
    const inputField = wrapper.findComponent(InputField);
    expect(inputField.isVisible()).toBe(true);
    expect(inputField.props("modelValue")).toBe("a1");
    const dropdown = wrapper.findComponent(Dropdown);
    expect(dropdown.exists()).toBe(false);
  });

  it("shows dropdown field on REPLACE mode", () => {
    const wrapper = doMount({
      outputMode: "REPLACE_EXISTING",
      create: "a1",
      replace: "c",
    });
    expect(wrapper.findComponent(ValueSwitch).props().modelValue).equals(
      "REPLACE_EXISTING",
    );
    const inputField = wrapper.findComponent(InputField);
    expect(inputField.exists()).toBe(false);
    const dropdown = wrapper.findComponent(Dropdown);
    expect(dropdown.isVisible()).toBe(true);
    expect(dropdown.props("modelValue")).toBe("c");
  });

  it("switches mode on ValueSwitch click", async () => {
    const wrapper = doMount();

    const leftButton = wrapper.find(".value-switch label:first-child");
    await leftButton.trigger("click");
    expect(wrapper.props("modelValue")?.outputMode).equals("APPEND");

    const rightButton = wrapper.find(".value-switch label:last-child");
    await rightButton.trigger("click");
    expect(wrapper.props("modelValue")?.outputMode).equals("REPLACE_EXISTING");
  });
});
