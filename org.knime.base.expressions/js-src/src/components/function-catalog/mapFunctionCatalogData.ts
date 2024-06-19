import type {
  FunctionCatalogData,
  FunctionCatalogEntryData,
} from "@/components/functionCatalogTypes";
import type { MathConstant } from "@/expressionScriptingService";

export type CatalogData = Record<string, FunctionCatalogEntryData[]>;
export const mapFunctionCatalogData = (
  data: FunctionCatalogData,
  mathConstants: MathConstant[],
): CatalogData => {
  const functionData = data.functions.reduce((catalog, func) => {
    const ellipseOrNot = func.arguments.length > 2 ? ", ..." : "";
    const firstTwoArgs = func.arguments
      .slice(0, 2)
      .map((value) => {
        return value.vararg ? `${value.name}...` : value.name;
      })
      .join(", ");
    const functionData = {
      ...func,
      displayName: `${func.name}(${firstTwoArgs}${ellipseOrNot})`,
      entryType: "function",
    } satisfies FunctionCatalogEntryData;
    if (catalog[functionData.category]) {
      catalog[functionData.category].push(functionData);
    } else {
      catalog[functionData.category] = [functionData];
    }
    return catalog;
  }, {} as CatalogData);

  const mathConstantsData = {
    "Math Constants": mathConstants.map((constant: MathConstant) => {
      return {
        name: constant.name,
        category: "Math Constants",
        keywords: [],
        description: constant.documentation,
        returnType: constant.type,
        entryType: "mathConstant",
        value: constant.value,
      } as FunctionCatalogEntryData;
    }),
  };

  return { ...functionData, ...mathConstantsData } as CatalogData;
};
