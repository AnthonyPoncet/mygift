<script lang="ts" setup>
import { useLanguageStore, Languages } from "@/stores/language";
import { useUserStore } from "@/stores/user";
import { useRouter } from "vue-router";
import SquareImage from "./SquareImage.vue";
import blank_profile_picture from "@/assets/images/blank_profile_picture.png";
import { isMobile } from "./helpers/is_mobile";
import { onMounted, ref, useTemplateRef } from "vue";

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

function collapse() {
  if (isMobile) {
    navButton.value?.classList.add("collapsed");
    navbarSupportedContent.value?.classList.remove("show");
  }
}

function triggerLanguageMenu() {
  languageMenuVisible.value = true
  window.addEventListener('click', () => {
    languageMenuVisible.value = false;
});
}

</script>

<template>
  <nav class="bg-gray-100 dark:bg-gray-900">
    <div class="mx-auto px-3 sm:px-6 lg:px-8">
      <div class="flex h-16 items-center justify-between">
        <div class="flex shrink-0">
          <RouterLink class="cursor-pointer text-xl font-normal hover:text-black dark:hover:text-white" to="/">MyGift</RouterLink>
        </div>

        <div class="relative flex grow justify-end">
          <button class="relative cursor-pointer rounded-full p-1 hover:text-black dark:hover:text-white" @click="triggerLanguageMenu">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              class="size-6"
              fill="currentColor"
              viewBox="0 0 16 16"
            >
              <path
                d="M0 8a8 8 0 1 1 16 0A8 8 0 0 1 0 8m7.5-6.923c-.67.204-1.335.82-1.887 1.855A8 8 0 0 0 5.145 4H7.5zM4.09 4a9.3 9.3 0 0 1 .64-1.539 7 7 0 0 1 .597-.933A7.03 7.03 0 0 0 2.255 4zm-.582 3.5c.03-.877.138-1.718.312-2.5H1.674a7 7 0 0 0-.656 2.5zM4.847 5a12.5 12.5 0 0 0-.338 2.5H7.5V5zM8.5 5v2.5h2.99a12.5 12.5 0 0 0-.337-2.5zM4.51 8.5a12.5 12.5 0 0 0 .337 2.5H7.5V8.5zm3.99 0V11h2.653c.187-.765.306-1.608.338-2.5zM5.145 12q.208.58.468 1.068c.552 1.035 1.218 1.65 1.887 1.855V12zm.182 2.472a7 7 0 0 1-.597-.933A9.3 9.3 0 0 1 4.09 12H2.255a7 7 0 0 0 3.072 2.472M3.82 11a13.7 13.7 0 0 1-.312-2.5h-2.49c.062.89.291 1.733.656 2.5zm6.853 3.472A7 7 0 0 0 13.745 12H11.91a9.3 9.3 0 0 1-.64 1.539 7 7 0 0 1-.597.933M8.5 12v2.923c.67-.204 1.335-.82 1.887-1.855q.26-.487.468-1.068zm3.68-1h2.146c.365-.767.594-1.61.656-2.5h-2.49a13.7 13.7 0 0 1-.312 2.5m2.802-3.5a7 7 0 0 0-.656-2.5H12.18c.174.782.282 1.623.312 2.5zM11.27 2.461c.247.464.462.98.64 1.539h1.835a7 7 0 0 0-3.072-2.472c.218.284.418.598.597.933M10.855 4a8 8 0 0 0-.468-1.068C9.835 1.897 9.17 1.282 8.5 1.077V4z"
              />
            </svg>
          </button>
          <transition enter-active-class="transition ease-out duration-100" enter-from-class="transform opacity-0 scale-95" enter-to-class="transform opacity-100 scale-100" leave-active-class="transition ease-in duration-75" leave-from-class="transform opacity-100 scale-100" leave-to-class="transform opacity-0 scale-95">
            <div v-if="languageMenuVisible" class="absolute right-0 z-10 mt-9 w-48 origin-top-right rounded-md bg-white dark:bg-gray-700 py-1 shadow-lg ring-1 ring-black/5 focus:outline-hidden">
              <button>Your Profile</button>
            </div>
          </transition>
        </div>
      </div>
    </div>
  </nav>
</template>
