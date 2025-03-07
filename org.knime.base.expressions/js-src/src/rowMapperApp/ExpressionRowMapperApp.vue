<script setup lang="ts">
import { onMounted, ref, shallowRef } from "vue";

import {
  AlertingService,
  DialogService,
  JsonDataService,
} from "@knime/ui-extension-service";

// @ts-ignore
import * as largeLibForSlowingDownLoading from "./large";

const smallOrLargeMode = ref<"small" | "large">("small");

// OPTION 1 - Nothing else going on
// DialogService.getInstance().then((dialogService) => {
//   smallOrLargeMode.value = dialogService.getInitialDisplayMode();

//   dialogService.addOnDisplayModeChangeCallback(({ mode }) => {
//     smallOrLargeMode.value = mode;
//   });
// });

// OPTION 2 - Initializing all the services before setting up the callback
// const myAsyncInitFn = async () => {
//   // Intitialize all my services -- I need them at some point...
//   const dialogService = await DialogService.getInstance();
//   const jsonDataService = await JsonDataService.getInstance();
//   const alertingService = await AlertingService.getInstance();
//   console.log("App: intialized services");

//   // Set up the display mode callback
//   smallOrLargeMode.value = dialogService.getInitialDisplayMode();
//   dialogService.addOnDisplayModeChangeCallback(({ mode }) => {
//     smallOrLargeMode.value = mode;
//   });
// };
// myAsyncInitFn();

// OPTION 3 - Having an unnecessary await before setting up the callback
// const unnecessaryAsync = () =>
//   new Promise((resolve) => {
//     setTimeout(() => {
//       resolve(null);
//     }, 0);
//   });

// const myAsyncInitFn = async () => {
//   // Intitialize all my services -- I need them at some point...
//   const dialogService = await DialogService.getInstance();
//   await unnecessaryAsync();
//   console.log("App: intialized services");

//   // Set up the display mode callback
//   smallOrLargeMode.value = dialogService.getInitialDisplayMode();
//   dialogService.addOnDisplayModeChangeCallback(({ mode }) => {
//     smallOrLargeMode.value = mode;
//   });
// };
// myAsyncInitFn();

// OPTION 4 - Initializing all the services before setting up the callback (but all at once)
// TODO - Note that the ready message is sent multiple times
const myAsyncInitFn = async () => {
  // Intitialize all my services -- I need them at some point...

  const [dialogService, jsonDataService, alertingService] = await Promise.all([
    DialogService.getInstance(),
    JsonDataService.getInstance(),
    AlertingService.getInstance(),
  ]);
  console.log("App: intialized services");

  // Set up the display mode callback
  smallOrLargeMode.value = dialogService.getInitialDisplayMode();
  dialogService.addOnDisplayModeChangeCallback(({ mode }) => {
    smallOrLargeMode.value = mode;
  });
};
myAsyncInitFn();
</script>

<template>
  <main>
    {{ smallOrLargeMode }}
  </main>
</template>
