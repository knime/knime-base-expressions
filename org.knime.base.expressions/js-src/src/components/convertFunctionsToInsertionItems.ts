import type { CompletionItemWithType } from "@/registerKnimeExpressionLanguage";
import { functionDataToMarkdown } from "./function-catalog/functionDescriptionToMarkdown";
import type { FunctionCatalogEntryData } from "./functionCatalogTypes";
import * as monaco from "monaco-editor";

/**
 * Take the data from the function catalog returned by the scripting service, and convert it into
 * a format that can be used by the monaco editor for autocompletion. Uses Monaco's snippet syntax
 * to allow for tabbing through the placeholder arguments when the function is inserted.
 *
 * @param entries the list of function catalog entries (functions and constants)
 * @returns a list of monaco completion items
 */
export const convertFunctionsToInsertionItems = (
  entries: FunctionCatalogEntryData[],
): CompletionItemWithType[] => {
  return entries.map((entry) => {
    if (entry.entryType === "function") {
      // closest we can get to a list comprehension over a range in JS
      const listOfIndices = [...Array(entry.arguments.length).keys()];

      // This snippet syntax is used to allow for tabbing through the arguments
      // when the function is inserted.
      const argumentsWithSnippetSyntax = listOfIndices
        .map((i) => `$\{${i + 1}:${entry.arguments[i].name}}`)
        .join(", ");

      const argumentsWithoutSnippetSyntax =
        entry.arguments.length > 0 ? "..." : "";

      return {
        text: `${entry.name}(${argumentsWithSnippetSyntax})`,
        kind: monaco.languages.CompletionItemKind.Function,
        extraDetailForMonaco: {
          documentation: { value: functionDataToMarkdown(entry) },
          insertTextRules:
            monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
          label: `${entry.name}(${argumentsWithoutSnippetSyntax})`,
        },
      };
    } else {
      return {
        text: entry.name,
        kind: monaco.languages.CompletionItemKind.Constant,
        extraDetailForMonaco: {
          documentation: { value: entry.description },
          detail: `Type: ${entry.returnType}`,
        },
      };
    }
  });
};
