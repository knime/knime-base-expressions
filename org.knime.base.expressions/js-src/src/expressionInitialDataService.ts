import { getInitialDataService } from "@knime/scripting-editor";
import type {
  ExpressionFlowVariableInitialData,
  ExpressionInitialData,
  ExpressionRowFilterInitialData,
  ExpressionRowMapperInitialData,
} from "@/common/types";

export const getExpressionInitialDataService = <
  T extends ExpressionInitialData,
>() => ({
  ...getInitialDataService(),
  getInitialData: async (): Promise<T> =>
    (await getInitialDataService().getInitialData()) as T,
});

export const getRowMapperInitialDataService =
  getExpressionInitialDataService<ExpressionRowMapperInitialData>;

export const getRowFilterInitialDataService =
  getExpressionInitialDataService<ExpressionRowFilterInitialData>;

export const getFlowVariableInitialDataService =
  getExpressionInitialDataService<ExpressionFlowVariableInitialData>;
