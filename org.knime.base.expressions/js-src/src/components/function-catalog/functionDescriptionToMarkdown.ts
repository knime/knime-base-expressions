import type {
  CategoryData,
  FunctionCatalogEntryData,
} from "@/components/functionCatalogTypes";
import MarkdownIt from "markdown-it";

export const functionDataToMarkdown = (
  func: FunctionCatalogEntryData,
): string => {
  if (func.entryType === "constant") {
    return (
      `Type: ***${func.returnType}***` +
      "\n\n###### Description" +
      `\n\n${func.description}`
    );
  } else {
    const args =
      func.arguments
        ?.map((arg) => `- **${arg.name}**: ${arg.description}`)
        ?.join("\n") ?? null;
    const returnDescription = func.returnDescription
      ? `  \n${func.returnDescription}`
      : "";
    const description = `\n\n${func.description}`;

    return (
      "\n\n###### Arguments " +
      `\n${args}` +
      "\n\n###### Return value " +
      `\n***${func.returnType}***` +
      `${returnDescription}` +
      "\n\n###### Description " +
      `${description}` +
      "\n\n###### Examples" +
      `\n\n${func.examples}`
    );
  }
};

export const functionDataToHtml = (
  functionData: FunctionCatalogEntryData,
): string => new MarkdownIt().render(functionDataToMarkdown(functionData));

export const categoryDataToMarkdown = (category: CategoryData): string => {
  return category.description;
};

export const categoryDataToHtml = (categoryData: CategoryData): string =>
  new MarkdownIt().render(categoryDataToMarkdown(categoryData));
