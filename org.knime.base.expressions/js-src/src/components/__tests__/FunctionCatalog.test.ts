import { enableAutoUnmount, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import FunctionCatalog from "../function-catalog/FunctionCatalog.vue";
import type { FunctionData } from "../functionCatalogTypes";

describe("FunctionCatalog", () => {
  enableAutoUnmount(afterEach);

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
  };
  const functionCatalogData = {
    categories: [{ name: "category1" }, { name: "category2" }],
    functions: [function1, function2],
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
      props: { functionCatalogData, initiallyExpanded: true, ...args.props },
      slots: args.slots,
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

    const searchBar = wrapper.findComponent({
      name: "SearchInput",
    });
    expect(searchBar.exists()).toBeTruthy();
  });

  it("renders the description when a function is selected", async () => {
    const { wrapper } = doMount();
    const functionCatalog = wrapper.findComponent({
      name: "FunctionCatalog",
    });
    expect(functionCatalog.exists()).toBeTruthy();
    expect(functionCatalog.text()).contains("Select a function");

    const catalogFunction = wrapper.find(".function-header");
    await catalogFunction.trigger("click");
    const description = wrapper.find(".info-panel");

    expect(description.exists()).toBeTruthy();
    expect(description.text()).contains("Description");
    expect(description.text()).contains("Arguments");
  });

  it("shows categories and can expand them", async () => {
    const { wrapper } = doMount({
      props: { functionCatalogData, initiallyExpanded: false },
    });

    const categories = wrapper.findAll(".category-container");
    expect(categories).toHaveLength(functionCatalogData.categories.length);

    await Promise.all(
      categories.map(async (categoryContainer, index) => {
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
      await categoryFunction.trigger("click");
      expect(categoryFunction.classes()).not.toContain("selected");
    }
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

      const searchBar = wrapper.findComponent({
        name: "SearchInput",
      });
      await searchBar.setValue(searchValue);

      const filteredCategoryFunctions = wrapper.findAll(".function-header");
      expect(filteredCategoryFunctions).toHaveLength(expectedCount);
      expect(filteredCategoryFunctions.at(0)!.text()).toContain(expectedText);
    },
  );
});
