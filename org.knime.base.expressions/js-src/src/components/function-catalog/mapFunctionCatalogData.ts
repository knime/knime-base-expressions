import type {
  FunctionCatalogData,
  FunctionData,
} from "@/components/functionCatalogTypes";

export type CatalogData = Record<string, FunctionData[]>;
export const mapFunctionCatalogData = (
  data: FunctionCatalogData,
): CatalogData =>
  data.functions.reduce((acc, curr) => {
    const functionData = {
      ...curr,
      displayName: `${curr.name} (${curr.arguments.map((arg) => arg.name).join(", ")})`,
    };
    if (acc[curr.category]) {
      acc[curr.category].push(functionData);
    } else {
      acc[curr.category] = [functionData];
    }
    return acc;
  }, {} as CatalogData);
