import type {
  FunctionCatalogData,
  FunctionCatalogEntryData,
} from "@/components/functionCatalogTypes";
import type {
  MathConstant,
  MathConstantData,
} from "@/expressionScriptingService";

export type CatalogData = Record<string, FunctionCatalogEntryData[]>;
export const mapFunctionCatalogData = (
  data: FunctionCatalogData,
  mathConstants: MathConstantData,
  maxArgs: number = 2,
): CatalogData => {
  const functionData = data.functions.reduce((catalog, func) => {
    const ellipseOrNot = func.arguments.length > 2 ? ", ..." : "";
    const firstTwoArgs = func.arguments
      .slice(0, maxArgs)
      .map((value) => {
        return value.vararg ? `${value.name}...` : value.name;
      })
      .join(", ");
    const fullArgs = func.arguments
      .map((value) => {
        return value.vararg ? `${value.name}...` : value.name;
      })
      .join(", ");
    const functionData = {
      ...func,
      displayName: `${func.name}(${firstTwoArgs}${ellipseOrNot})`,
      entryType: "function",
      displayNameWithFullArgs: `${func.name}(${fullArgs})`,
    } satisfies FunctionCatalogEntryData;
    if (catalog[functionData.category]) {
      catalog[functionData.category].push(functionData);
    } else {
      catalog[functionData.category] = [functionData];
    }
    return catalog;
  }, {} as CatalogData);

  const mathConstantsData = {
    [mathConstants.category.name]: mathConstants.constants.map(
      (constant: MathConstant) => {
        return {
          name: constant.name,
          category: mathConstants.category.name,
          keywords: [],
          description: constant.documentation,
          returnType: constant.type,
          entryType: "mathConstant",
          value: constant.value,
        } as FunctionCatalogEntryData;
      },
    ),
  };

  return { ...functionData, ...mathConstantsData } as CatalogData;
};
