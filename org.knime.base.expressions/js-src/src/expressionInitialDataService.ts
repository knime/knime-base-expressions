import { getInitialDataService } from "@knime/scripting-editor";
import type { ExpressionInitialData } from "@/common/types";

export const getExpressionInitialDataService = () => ({
  ...getInitialDataService(),
  getInitialData: async (): Promise<ExpressionInitialData> =>
    (await getInitialDataService().getInitialData()) as ExpressionInitialData,
});
