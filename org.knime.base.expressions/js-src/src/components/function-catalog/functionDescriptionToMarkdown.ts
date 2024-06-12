import type { FunctionData } from "@/components/functionCatalogTypes";

export const functionDataToMarkdown = (func: FunctionData): string => {
  const args = func.arguments
    .map((arg) => `- **${arg.name}**: ${arg.description}`)
    .join("\n");
  const returnDescription = func.returnDescription
    ? `  \n${func.returnDescription}`
    : "";
  const description = `\n\n${func.description}`;

  return (
    `## ${func.name}` +
    "\n\n### Arguments: " +
    `\n${args}` +
    "\n\n### Returns:  " +
    `\n${func.returnType}` +
    `${returnDescription}` +
    "\n\n### Description:" +
    `${description}`
  );
};
