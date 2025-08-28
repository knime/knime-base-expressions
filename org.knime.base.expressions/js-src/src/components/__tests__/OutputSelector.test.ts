import { afterEach, describe, expect, it, vi } from "vitest";
import { enableAutoUnmount, mount } from "@vue/test-utils";

import { Dropdown, InputField, ValueSwitch } from "@knime/components";
import { DataType } from "@knime/kds-components";

import OutputSelector, { type SelectorState } from "../OutputSelector.vue";

describe("OutputSelector", () => {
  enableAutoUnmount(afterEach);

  const columnsToReplace = [
    { id: "a", text: "A", type: { id: "a", text: "A" } },
    { id: "b", text: "B", type: { id: "b", text: "B" } },
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

  it("shows the DataType Component in REPLACE mode", async () => {
    const wrapper = doMount({
      outputMode: "REPLACE_EXISTING",
      create: "a1",
      replace: "c",
    });
    expect(wrapper.find(".with-type").text()).toBe("C");
    const columnChoices = wrapper
      .find("#dropdown-box-to-select-entity")
      .find("ul");
    const columnDataTypeComponents = columnChoices.findAllComponents(DataType);
    expect(columnDataTypeComponents).toHaveLength(4);
    expect(columnDataTypeComponents.at(0)?.props()).toStrictEqual(
      expect.objectContaining({ iconName: "a", iconTitle: "A" }),
    );
    expect(columnDataTypeComponents.at(2)?.props("iconName")).toBe(
      "unknown-datatype",
    );

    await wrapper.setProps({
      itemType: "flow variable",
    });

    const variableChoices = wrapper
      .find("#dropdown-box-to-select-entity")
      .find("ul");
    const variableDataTypeComponents =
      variableChoices.findAllComponents(DataType);
    expect(variableDataTypeComponents).toHaveLength(4);
    expect(variableDataTypeComponents.at(0)?.props()).toStrictEqual(
      expect.objectContaining({ iconName: "a", iconTitle: "A" }),
    );
    expect(variableDataTypeComponents.at(2)?.props("iconName")).toBe("UNKNOWN");
  });

  it("shows missing values in REPLACE mode", () => {
    const wrapper = doMount({
      outputMode: "REPLACE_EXISTING",
      create: "a1",
      replace: "My Old Column",
    });
    expect(wrapper.find(".with-type").text()).toBe("(MISSING) My Old Column");
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
