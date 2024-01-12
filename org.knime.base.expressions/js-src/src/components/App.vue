<script setup lang="ts">
import { ScriptingEditor } from "@knime/scripting-editor";

// TODO(language-features) register knime expression language
</script>

<template>
  <main>
    <ScriptingEditor
      :title="`Expression (Labs)`"
      language="knime-expression"
      file-name="main.knexp"
    />
  </main>
</template>

<style>
@import url("webapps-common/ui/css");
</style>
