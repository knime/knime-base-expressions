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

export const flowVariableTypes: {
  [key in FlowVariableType]: FlowVariableType;
} = {
  String: "String",
  Long: "Long",
  Integer: "Integer",
  Boolean: "Boolean",
  Double: "Double",
  Unknown: "Unknown",
};

export const mapExpressionTypeToFlowVariableType = (
  type: ExpressionReturnType,
): FlowVariableType => {
  switch (type) {
    case "STRING":
      return flowVariableTypes.String;
    case "INTEGER":
      return flowVariableTypes.Long;
    case "BOOLEAN":
      return flowVariableTypes.Boolean;
    case "FLOAT":
      return flowVariableTypes.Double;
    default:
      return flowVariableTypes.Unknown;
  }
};

export const getDropDownValuesForCurrentType = (
  type: ExpressionReturnType,
): AllowedReturnTypes[] => {
  if (type === "INTEGER") {
    return [
      { id: flowVariableTypes.Long, text: flowVariableTypes.Long },
      { id: flowVariableTypes.Integer, text: flowVariableTypes.Integer },
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
