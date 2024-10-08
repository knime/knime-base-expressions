import type { AllowedReturnTypes } from "@/components/ReturnTypeSelector.vue";

export type ExpressionReturnType =
  | "UNKNOWN"
  | "STRING"
  | "INTEGER"
  | "BOOLEAN"
  | "FLOAT";

export type FlowVariableType =
  | "String"
  | "Long"
  | "Integer"
  | "Boolean"
  | "Double"
  | "Unknown";

export const mapExpressionTypeToFlowVariableType = (
  type: ExpressionReturnType,
): FlowVariableType => {
  switch (type) {
    case "STRING":
      return "String";
    case "INTEGER":
      return "Long";
    case "BOOLEAN":
      return "Boolean";
    case "FLOAT":
      return "Double";
    default:
      return "Unknown";
  }
};

export const getDropDownValuesForCurrentType = (
  type: ExpressionReturnType,
): AllowedReturnTypes[] => {
  if (type === "INTEGER") {
    return [
      {
        id: "Long",
        text: "Long",
      },
      {
        id: "Integer",
        text: "Integer",
      },
    ];
  }

  const flowVariableType = mapExpressionTypeToFlowVariableType(type);
  return [
    {
      id: flowVariableType,
      text: flowVariableType,
    },
  ];
};
