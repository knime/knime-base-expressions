import { reactive } from "vue";

export type StoreType = {
  expressionValid: boolean;
  activeEditorFileName: string | null;
};

const store = reactive<StoreType>({
  expressionValid: false,
  activeEditorFileName: null,
});

export const useStore = () => store;
