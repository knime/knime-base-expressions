export type FunctionData = {
  name: string;
  category: string;
  keywords: string[];
  displayName?: string;
  description: string;
  arguments: {
    name: string;
    type: string;
    description: string;
    vararg?: boolean;
  }[];
  returnType: string;
  returnDescription?: string;
};

export type CategoryData = {
  name: string;
  description?: string;
};

export type FunctionCatalogData = {
  categories: CategoryData[];
  functions: FunctionData[];
};
