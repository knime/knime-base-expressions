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

export type ConstantData = {
  name: string;
  category: string;
  keywords: string[];
  description: string;
  returnType: string;
  entryType: "constant";
};

export type FunctionCatalogEntryData = FunctionData | ConstantData;

export type CategoryData = {
  name: string;
  description?: string;
};

export type FunctionCatalogData = {
  categories: CategoryData[];
  functions: FunctionCatalogEntryData[];
};
