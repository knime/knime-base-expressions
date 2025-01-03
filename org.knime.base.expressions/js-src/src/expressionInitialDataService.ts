import { getInitialDataService } from "@knime/scripting-editor";

import type {
  FlowVariableInitialData,
  GenericExpressionInitialData,
  RowFilterInitialData,
  RowMapperInitialData,
} from "@/common/types";

const getExpressionInitialDataService = <
  T extends GenericExpressionInitialData,
>() => ({
  ...getInitialDataService(),
  getInitialData: async (): Promise<T> =>
    (await getInitialDataService().getInitialData()) as T,
});

export const getRowMapperInitialDataService =
  getExpressionInitialDataService<RowMapperInitialData>;
export const getFlowVariableInitialDataService =
  getExpressionInitialDataService<FlowVariableInitialData>;
export const getRowFilterInitialDataService =
  getExpressionInitialDataService<RowFilterInitialData>;
