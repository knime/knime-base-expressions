import { reactive } from "vue";

export type StoreType = { expressionValid: boolean };

const store = reactive<StoreType>({
  expressionValid: false,
});

export const useStore = () => store;
