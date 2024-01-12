<script setup lang="ts">
import { ScriptingEditor, getScriptingService } from "@knime/scripting-editor";

const scriptingService = getScriptingService();

const saveSettings = async (settings: any) => {
  await scriptingService.saveSettings(settings);
  scriptingService.closeDialog();
};

// TODO(language-features) register knime expression language
</script>

<template>
  <main>
    <ScriptingEditor
      :title="`Expression (Labs)`"
      language="knime-expression"
      file-name="main.knexp"
      @save-settings="saveSettings"
    />
  </main>
</template>

<style>
@import url("webapps-common/ui/css");
</style>
