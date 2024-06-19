import type { FunctionCatalogEntryData } from "@/components/functionCatalogTypes";
import MarkdownIt from "markdown-it";

export const functionDataToMarkdown = (
  func: FunctionCatalogEntryData,
): string => {
  if (func.entryType === "mathConstant") {
    return (
      `## ${func.name}` +
      `\n\nType: ${func.returnType}` +
      "\n\n### Description" +
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
      `### ${func.displayNameWithFullArgs ?? func.name}` +
      "\n\n#### Arguments " +
      `\n${args}` +
      "\n\n#### Return value " +
      `\n***${func.returnType}***` +
      `${returnDescription}` +
      "\n\n#### Description " +
      `${description}`
    );
  }
};

export const functionDataToHtml = (
  functionData: FunctionCatalogEntryData,
): string => new MarkdownIt().render(functionDataToMarkdown(functionData));
