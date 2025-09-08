import { getInitialData } from "@knime/scripting-editor";

import type {
  FlowVariableInitialData,
  GenericExpressionInitialData,
  RowFilterInitialData,
  RowMapperInitialData,
} from "@/common/types";

// TODO(async-init) these do not have to be async anymore
const getExpressionInitialDataService = <
  T extends GenericExpressionInitialData,
>() => ({
  getInitialData: (): Promise<T> => {
    return Promise.resolve(getInitialData() as T);
  },
});

export const getRowMapperInitialDataService =
  getExpressionInitialDataService<RowMapperInitialData>;
export const getFlowVariableInitialDataService =
  getExpressionInitialDataService<FlowVariableInitialData>;
export const getRowFilterInitialDataService =
  getExpressionInitialDataService<RowFilterInitialData>;
