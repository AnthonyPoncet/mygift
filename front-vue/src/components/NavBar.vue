<script lang="ts" setup>
import { useLanguageStore, Languages } from "@/stores/language";
import { useUserStore } from "@/stores/user";
import { useRoute, useRouter } from "vue-router";
import SquareImage from "./SquareImage.vue";
import blank_profile_picture from "@/assets/images/blank_profile_picture.png";
import { onUnmounted, ref } from "vue";

const route = useRoute();
const router = useRouter();
const languageStore = useLanguageStore();

const languageMenuVisible = ref(false);

function changeLanguage(language: Languages) {
  languageStore.updateLanguage(language);
}

function logout() {
  useUserStore().logout();
  router.push({ name: "home" });
}

function triggerLanguageMenu() {
  if (languageMenuVisible.value) {
    hideMenuListener();
  } else {
    languageMenuVisible.value = true;
    window.addEventListener("click", hideMenuListener);
  }
}

function hideMenuListener() {
  if (languageMenuVisible.value) {
    languageMenuVisible.value = false;
    window.removeEventListener("click", hideMenuListener);
  }
}

onUnmounted(() => {
  hideMenuListener();
});
</script>

<template>
  <nav class="bg-gray-100 dark:bg-gray-900">
    <div class="mx-auto px-3 sm:px-6 lg:px-8">
      <div class="flex h-16 items-center justify-between">
        <div class="flex shrink-0">
          <RouterLink
            class="cursor-pointer text-xl font-normal hover:text-black dark:hover:text-white"
            to="/"
            >MyGift</RouterLink
          >
        </div>

        <div class="hidden h-full grow justify-center sm:flex">
          <div class="mr-10 flex h-full">
            <div class="flex w-full flex-col items-center">
              <template v-if="useUserStore().user === null">
                <RouterLink
                  :class="
                    route.name === 'home' || route.name === 'signin'
                      ? 'flex h-full w-full cursor-pointer items-center justify-center text-xl font-bold'
                      : 'text-grey-700 flex h-full w-full cursor-pointer items-center justify-center text-base font-normal hover:text-black dark:text-gray-300 dark:hover:text-white'
                  "
                  to="/signin"
                  >{{ useLanguageStore().language.messages.nav_bar__signin }}</RouterLink
                >
              </template>
              <div
                v-if="route.name === 'home' || route.name === 'signin'"
                class="mb-1 h-1 w-30 rounded-md bg-red-500"
              ></div>
            </div>
          </div>
          <div class="flex h-full">
            <div class="flex w-full flex-col items-center">
              <template v-if="useUserStore().user === null">
                <RouterLink
                  :class="
                    route.name === 'signup'
                      ? 'flex h-full w-full cursor-pointer items-center justify-center text-xl font-bold'
                      : 'text-grey-700 flex h-full w-full cursor-pointer items-center justify-center text-base font-normal hover:text-black dark:text-gray-300 dark:hover:text-white'
                  "
                  to="/signup"
                  >{{ useLanguageStore().language.messages.nav_bar__signup }}</RouterLink
                >
              </template>
              <div v-if="route.name === 'signup'" class="mb-1 h-1 w-30 rounded-md bg-red-500"></div>
            </div>
          </div>
        </div>

        <div class="relative flex shrink justify-end">
          <button
            class="relative cursor-pointer p-1 hover:text-black dark:hover:text-white"
            @click.stop="triggerLanguageMenu"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="currentColor"
              class="size-6"
              viewBox="0 0 16 16"
            >
              <path
                d="M4.545 6.714 4.11 8H3l1.862-5h1.284L8 8H6.833l-.435-1.286zm1.634-.736L5.5 3.956h-.049l-.679 2.022z"
              />
              <path
                d="M0 2a2 2 0 0 1 2-2h7a2 2 0 0 1 2 2v3h3a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-3H2a2 2 0 0 1-2-2zm2-1a1 1 0 0 0-1 1v7a1 1 0 0 0 1 1h7a1 1 0 0 0 1-1V2a1 1 0 0 0-1-1zm7.138 9.995q.289.451.63.846c-.748.575-1.673 1.001-2.768 1.292.178.217.451.635.555.867 1.125-.359 2.08-.844 2.886-1.494.777.665 1.739 1.165 2.93 1.472.133-.254.414-.673.629-.89-1.125-.253-2.057-.694-2.82-1.284.681-.747 1.222-1.651 1.621-2.757H14V8h-3v1.047h.765c-.318.844-.74 1.546-1.272 2.13a6 6 0 0 1-.415-.492 2 2 0 0 1-.94.31"
              />
            </svg>
          </button>
          <transition
            enter-active-class="transition ease-out duration-100"
            enter-from-class="transform opacity-0 scale-95"
            enter-to-class="transform opacity-100 scale-100"
            leave-active-class="transition ease-in duration-75"
            leave-from-class="transform opacity-100 scale-100"
            leave-to-class="transform opacity-0 scale-95"
          >
            <div
              v-if="languageMenuVisible"
              class="absolute right-0 z-10 mt-9 w-auto origin-top-right rounded-md bg-gray-50 py-1 shadow-lg ring-1 ring-black/5 focus:outline-hidden dark:bg-gray-950"
            >
              <template v-for="language in Languages" :key="language">
                <button
                  class="block w-full px-4 py-2 text-sm hover:bg-gray-100 hover:text-black dark:hover:bg-gray-900 dark:hover:text-white"
                  @click="changeLanguage(language)"
                >
                  {{ language }}
                </button>
              </template>
            </div>
          </transition>
        </div>
      </div>
    </div>

    <!--- Mobile menu -->
    <div class="mx-auto sm:hidden">
      <div class="flex h-16 items-center justify-between">
        <div class="flex h-full basis-1/2">
          <div class="flex w-full flex-col items-center">
            <template v-if="useUserStore().user === null">
              <RouterLink
                :class="
                  route.name === 'home' || route.name === 'signin'
                    ? 'flex h-full w-full cursor-pointer items-center justify-center text-xl font-bold'
                    : 'text-grey-700 flex h-full w-full cursor-pointer items-center justify-center text-base font-normal dark:text-gray-300'
                "
                to="/signin"
                >{{ useLanguageStore().language.messages.nav_bar__signin }}</RouterLink
              >
            </template>
            <div
              v-if="route.name === 'home' || route.name === 'signin'"
              class="mb-1 h-1 w-30 rounded-md bg-red-500"
            ></div>
          </div>
        </div>
        <div class="flex h-full basis-1/2">
          <div class="flex w-full flex-col items-center">
            <template v-if="useUserStore().user === null">
              <RouterLink
                :class="
                  route.name === 'signup'
                    ? 'flex h-full w-full cursor-pointer items-center justify-center text-xl font-bold'
                    : 'text-grey-700 flex h-full w-full cursor-pointer items-center justify-center text-base font-normal dark:text-gray-300'
                "
                to="/signup"
                >{{ useLanguageStore().language.messages.nav_bar__signup }}</RouterLink
              >
            </template>
            <div v-if="route.name === 'signup'" class="mb-1 h-1 w-30 rounded-md bg-red-500"></div>
          </div>
        </div>
      </div>
    </div>
  </nav>
</template>
