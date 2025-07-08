<script setup lang="ts">
import { ScriptingEditor } from "@knime/scripting-editor";
</script>

<template>
  <main>
    <ScriptingEditor
      file-name="values.json"
      language="json"
      :show-control-bar="true"
      :initial-pane-sizes="{
        right: 0,
        left: 0,
        bottom: 30,
      }"
    />
  </main>
</template>

<style lang="postcss">
@import url("@knime/styles/css");
</style>

<style lang="postcss" scoped></style>
