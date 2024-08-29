import { enableAutoUnmount, mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { MIN_WIDTH_FOR_SIDE_BY_SIZE_DESC_FUNC_CATALOG } from "../function-catalog/contraints";
import FunctionCatalog, {
  FUNCTION_INSERTION_EVENT,
} from "../function-catalog/FunctionCatalog.vue";
import type { FunctionData, ConstantData } from "../functionCatalogTypes";
import { useElementBounding } from "@vueuse/core";
import { ref } from "vue";
import { SearchInput } from "@knime/components";
import { insertionEventHelper } from "@knime/scripting-editor";

const mocks = vi.hoisted(() => {
  return {
    useElementBounding: vi.fn(),
  };
});

vi.mock("@vueuse/core", async (importOriginal) => {
  const original = await importOriginal<typeof import("@vueuse/core")>();
  return {
    ...original,
    useElementBounding: mocks.useElementBounding,
    onKeyStroke: vi.fn(),
  };
});

describe("FunctionCatalog", () => {
  enableAutoUnmount(afterEach);

  beforeEach(() => {
    vi.mocked(useElementBounding).mockReturnValue({
      width: ref(MIN_WIDTH_FOR_SIDE_BY_SIZE_DESC_FUNC_CATALOG + 1),
    } as ReturnType<typeof useElementBounding>);

    const scrollIntoViewMock = vi.fn();
    window.HTMLElement.prototype.scrollIntoView = scrollIntoViewMock;
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  const function1: FunctionData = {
    name: "function1",
    category: "category1",
    arguments: [
      { name: "arg1", type: "type1", description: "the first argument" },
      { name: "arg2", type: "type2", description: "the second argument" },
    ],
    description:
      "# Markdown\nDescription\n ## Example:\n`function1('arg1', 'arg2')`",
    keywords: ["keyWord1", "join", "merge"],
    returnType: "returnType1",
    entryType: "function",
  };
  const function2: FunctionData = {
    name: "function2",
    category: "category2",
    arguments: [
      { name: "arg1", type: "type1", description: "the first argument" },
      { name: "arg2", type: "type2", description: "the second argument" },
    ],
    description:
      "# Markdown\nDescription\n ## Example:\n`function2('arg1', 'arg2')`",
    keywords: ["keyWord2"],
    returnType: "returnType2",
    entryType: "function",
  };
  const constant1: ConstantData = {
    name: "pi",
    category: "math constants",
    description: "The constant PI",
    returnType: "number",
    entryType: "constant",
    keywords: ["pi"],
  };
  const functionCatalogData = {
    categories: [
      { name: "category1", description: "This is a description for category1" },
      { name: "category2" },
      { name: "math constants", description: "Mathematical constants" },
    ],
    functions: [function1, function2, constant1],
  };

  const doMount = (
    args: {
      props: Partial<InstanceType<typeof FunctionCatalog>["$props"]>;
      slots?: any;
    } = {
      props: {
        functionCatalogData,
        initiallyExpanded: true,
      },
    },
  ) => {
    const wrapper = mount(FunctionCatalog, {
      props: {
        functionCatalogData,
        initiallyExpanded: true,
        ...args.props,
      },
      slots: args.slots,
      attachTo: document.body,
    });

    return {
      wrapper,
    };
  };

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("renders the FunctionCatalog and show filter/search bar", () => {
    const { wrapper } = doMount();

    const searchBar = wrapper.findComponent(SearchInput);
    expect(searchBar.exists()).toBeTruthy();
  });

  it("renders the description when a function is selected", async () => {
    const { wrapper } = doMount();
    const functionCatalog = wrapper.findComponent({
      name: "FunctionCatalog",
    });
    expect(functionCatalog.exists()).toBeTruthy();

    const catalogFunction = wrapper.find(".function-header");
    await catalogFunction.trigger("click");
    const description = wrapper.find(".info-panel");

    expect(description.exists()).toBeTruthy();
    expect(description.text()).contains("Description");
    expect(description.text()).contains("Arguments");

    expect(description.classes()).not.contains("slim-mode");
  });

  it("does not render the description when the width is too small", () => {
    vi.mocked(useElementBounding).mockReturnValue({
      width: ref(MIN_WIDTH_FOR_SIDE_BY_SIZE_DESC_FUNC_CATALOG - 1),
    } as ReturnType<typeof useElementBounding>);

    const { wrapper } = doMount();

    const description = wrapper.find(".info-panel");

    expect(description.classes()).contains("slim-mode");
  });

  it("shows categories and can expand them", async () => {
    const { wrapper } = doMount({
      props: { functionCatalogData, initiallyExpanded: false },
    });

    const categories = wrapper.findAll(".category-container");

    expect(categories).toHaveLength(functionCatalogData.categories.length);

    await Promise.all(
      // We also slice to remove the math constants category
      categories.slice(0, -1).map(async (categoryContainer, index) => {
        expect(categoryContainer.text()).toContain(
          functionCatalogData.categories[index].name,
        );

        const category = categoryContainer.find(".category-header");

        expect(category.classes()).not.toContain("expanded");
        await category.trigger("click");
        expect(category.classes()).toContain("expanded");

        const functionsInCategory =
          categoryContainer.findAll(".function-header");
        expect(functionsInCategory).toHaveLength(1);
        expect(
          functionCatalogData.functions.some((func) =>
            functionsInCategory.at(0)!.text().includes(func.name),
          ),
        ).toBeTruthy();
      }),
    );
  });

  it("selects the first category by default when focussed", async () => {
    const { wrapper } = doMount({
      props: { functionCatalogData, initiallyExpanded: false },
    });
    const firstCategory = wrapper.findAll(".category-header")[0];

    await firstCategory.trigger("focus");
    expect(firstCategory.classes()).toContain("selected");

    const description = wrapper.find(".info-panel");
    expect(description.exists()).toBeTruthy();

    expect(description.text()).toContain(
      functionCatalogData.categories[0].description,
    );
  });

  it("allows to expand/collapse categories with the right/left arrow buttons", async () => {
    const { wrapper } = doMount({
      props: { functionCatalogData, initiallyExpanded: false },
    });
    const firstCategory = wrapper.findAll(".category-header")[0];

    await firstCategory.trigger("focus");
    expect(firstCategory.classes()).not.toContain("expanded");

    await firstCategory.trigger("keydown", { key: "ArrowRight" });
    expect(firstCategory.classes()).toContain("expanded");
    await firstCategory.trigger("keydown", { key: "ArrowRight" });
    expect(firstCategory.classes()).toContain("expanded");

    await firstCategory.trigger("keydown", { key: "ArrowLeft" });
    expect(firstCategory.classes()).not.toContain("expanded");
  });

  it("allows to cycle through functions/categories with the up/down arrow buttons", async () => {
    const { wrapper } = doMount();

    const firstCategory = wrapper.findAll(".category-header")[0];
    const functionInFirstCategory = wrapper.findAll(".function-header")[0];
    const secondCategory = wrapper.findAll(".category-header")[1];
    const functionInSecondCategory = wrapper.findAll(".function-header")[1];

    const functionList = wrapper.find(".function-list");
    await functionList.trigger("focus");

    expect(functionInFirstCategory.classes()).not.toContain("selected");
    await functionList.trigger("keydown", { key: "ArrowDown" });
    expect(functionInFirstCategory.classes()).toContain("selected");

    expect(secondCategory.classes()).not.toContain("selected");
    await functionList.trigger("keydown", { key: "ArrowDown" });
    expect(secondCategory.classes()).toContain("selected");

    expect(functionInSecondCategory.classes()).not.toContain("selected");
    await functionList.trigger("keydown", { key: "ArrowDown" });
    expect(functionInSecondCategory.classes()).toContain("selected");

    expect(firstCategory.classes()).not.toContain("selected");

    // Enough keydowns to loop back to the first category
    await functionList.trigger("keydown", { key: "ArrowDown" });
    await functionList.trigger("keydown", { key: "ArrowDown" });
    await functionList.trigger("keydown", { key: "ArrowDown" });

    expect(firstCategory.classes()).toContain("selected");
  });

  it("shows functions and can select them", async () => {
    const { wrapper } = doMount();

    const categoryFunctions = wrapper.findAll(".function-header");
    expect(categoryFunctions).toHaveLength(
      functionCatalogData.functions.length,
    );

    for (const [index, categoryFunction] of categoryFunctions.entries()) {
      expect(categoryFunction.text()).toContain(
        functionCatalogData.functions[index].name,
      );

      await categoryFunction.trigger("click");
      expect(categoryFunction.classes()).toContain("selected");
    }
  });

  it("fires an event when functions are double-clicked or spacebar/enter is pressed", async () => {
    const { wrapper } = doMount();
    const functionList = wrapper.find(".function-list");
    await functionList.trigger("focus");

    const functionInFirstCategory = wrapper.findAll(".function-header")[0];

    const insertionListener = vi.fn();
    insertionEventHelper
      .getInsertionEventHelper(FUNCTION_INSERTION_EVENT)
      .registerInsertionListener(insertionListener);

    // We don't expect it to trigger on single click
    await functionInFirstCategory.trigger("click");
    expect(insertionListener).not.toHaveBeenCalled();

    // Should fire three events
    await functionInFirstCategory.trigger("dblclick");
    await functionList.trigger("keydown", { key: "Enter" });
    await functionList.trigger("keydown", { key: " " });
    expect(insertionListener).toHaveBeenCalledTimes(3);
    expect(insertionListener).toHaveBeenNthCalledWith(
      1,
      expect.objectContaining({
        textToInsert: function1.name,
      }),
    );
  });

  const testCases = [
    { searchValue: "function1", expectedCount: 1, expectedText: "function1" },
    { searchValue: "category1", expectedCount: 1, expectedText: "function1" },
    { searchValue: "keyword1", expectedCount: 1, expectedText: "function1" },
  ];

  it.each(testCases)(
    "filters functions by filter/search bar for %s",
    async ({ searchValue, expectedCount, expectedText }) => {
      const { wrapper } = doMount();

      const allCategoryFunctions = wrapper.findAll(".function-header");
      expect(allCategoryFunctions).toHaveLength(
        functionCatalogData.functions.length,
      );

      const searchBar = wrapper.findComponent(SearchInput);
      await searchBar.setValue(searchValue);

      const filteredCategoryFunctions = wrapper.findAll(".function-header");
      expect(filteredCategoryFunctions).toHaveLength(expectedCount);
      expect(filteredCategoryFunctions.at(0)!.text()).toContain(expectedText);
    },
  );

  it("scrolls selected elements into view", async () => {
    const scrollIntoViewMock = vi.fn();
    window.HTMLElement.prototype.scrollIntoView = scrollIntoViewMock;

    const { wrapper } = doMount();

    const categoryFunctions = wrapper.findAll(".function-header");

    for (const [i, categoryFunction] of categoryFunctions.entries()) {
      await categoryFunction.trigger("click");
      expect(scrollIntoViewMock).toHaveBeenCalledTimes(i + 1);
    }
  });
});
