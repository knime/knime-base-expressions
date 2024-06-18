export type FunctionData = {
  name: string;
  category: string;
  keywords: string[];
  displayName?: string;
  displayNameWithFullArgs?: string;
  description: string;
  arguments: {
    name: string;
    type: string;
    description: string;
    vararg?: boolean;
  }[];
  returnType: string;
  returnDescription?: string;
  entryType: "function";
};

export type MathConstantData = {
  name: string;
  value: number;
  category: string;
  keywords: string[];
  description: string;
  returnType: string;
  entryType: "mathConstant";
};

export type FunctionCatalogEntryData = FunctionData | MathConstantData;

export type CategoryData = {
  name: string;
  description?: string;
};

export type FunctionCatalogData = {
  categories: CategoryData[];
  functions: FunctionData[];
};
