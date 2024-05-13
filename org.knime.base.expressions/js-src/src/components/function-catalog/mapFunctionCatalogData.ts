import type {
  FunctionCatalogData,
  FunctionData,
} from "@/components/functionCatalogTypes";

export type CatalogData = Record<string, FunctionData[]>;
export const mapFunctionCatalogData = (
  data: FunctionCatalogData,
): CatalogData =>
  data.functions.reduce((catalog, func) => {
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
    };
    if (catalog[functionData.category]) {
      catalog[functionData.category].push(functionData);
    } else {
      catalog[functionData.category] = [functionData];
    }
    return catalog;
  }, {} as CatalogData);
