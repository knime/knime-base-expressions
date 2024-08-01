import { getInitialDataService } from "@knime/scripting-editor";
import type { ExpressionInitialData } from "./types";

export const getExpressionInitialDataService = () => ({
  ...getInitialDataService(),
  getInitialData: async () =>
    (await getInitialDataService().getInitialData()) as ExpressionInitialData,
});
