<script setup lang="ts">
import { RouterView } from "vue-router";
import NavBar from "./components/NavBar.vue";
</script>

<template>
  <header>
    <NavBar />
    <div class="h-px w-full bg-gray-400 dark:bg-gray-600"></div>
  </header>

  <RouterView />
</template>
