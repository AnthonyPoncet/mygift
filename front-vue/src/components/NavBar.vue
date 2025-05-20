<script lang="ts" setup>
import { useLanguageStore, Languages } from "@/stores/language";
import { type OtherUser, useUserStore } from "@/stores/user";
import { useRoute, useRouter } from "vue-router";
import SquareImage from "./SquareImage.vue";
import blank_profile_picture from "@/assets/images/blank_profile_picture.png";
import { onUnmounted, ref } from "vue";

const route = useRoute();
const router = useRouter();
const languageStore = useLanguageStore();

const languageMenuVisible = ref(false);
const profileMenuVisible = ref(false);

function changeLanguage(language: Languages) {
  languageStore.updateLanguage(language);
}

function triggerLanguageMenu() {
  hideMenuListener();
  if (!languageMenuVisible.value) {
    languageMenuVisible.value = true;
    window.addEventListener("click", hideMenuListener);
  }
}

function triggerProfileMenu() {
  hideMenuListener();
  if (!profileMenuVisible.value) {
    profileMenuVisible.value = true;
    window.addEventListener("click", hideMenuListener);
  }
}

function hideMenuListener() {
  if (languageMenuVisible.value) {
    languageMenuVisible.value = false;
    window.removeEventListener("click", hideMenuListener);
  }
  if (profileMenuVisible.value) {
    profileMenuVisible.value = false;
    window.removeEventListener("click", hideMenuListener);
  }
}

function changeUser(otherUser: OtherUser) {
  hideMenuListener();
  useUserStore().changeAccount(otherUser);
}

function logout() {
  useUserStore().logout();
  router.push({ name: "home" });
}

onUnmounted(() => {
  hideMenuListener();
});
</script>

<template>
  <nav class="navbar-container">
    <div class="navbar-top-bar">
      <RouterLink class="navbar-logo" to="/">MyGift</RouterLink>

      <div class="navbar-pc-container">
        <div class="navbar-menu-item-container justify-end">
          <div
            class="navbar-menu-item"
            :class="useUserStore().user === null ? 'min-w-[161px]' : 'min-w-[141px]'"
          >
            <template v-if="useUserStore().user === null">
              <RouterLink
                :class="
                  route.name === 'home' || route.name === 'signin'
                    ? 'navbar-menu-title-selected'
                    : 'navbar-menu-title-not-selected'
                "
                to="/signin"
              >
                {{ useLanguageStore().language.messages.nav_bar__signin }}
              </RouterLink>
              <div
                v-if="route.name === 'home' || route.name === 'signin'"
                class="navbar-menu-line"
              ></div>
            </template>
            <template v-else>
              <RouterLink
                :class="
                  route.name === 'mywishlist'
                    ? 'navbar-menu-title-selected'
                    : 'navbar-menu-title-not-selected'
                "
                to="/mywishlist"
                >{{ useLanguageStore().language.messages.nav_bar__myList }}</RouterLink
              >
              <div v-if="route.name === 'mywishlist'" class="navbar-menu-line"></div>
            </template>
          </div>
        </div>
        <div class="navbar-menu-item-container justify-start">
          <div
            class="navbar-menu-item"
            :class="useUserStore().user === null ? 'min-w-[161px]' : 'min-w-[141px]'"
          >
            <template v-if="useUserStore().user === null">
              <RouterLink
                :class="
                  route.name === 'signup'
                    ? 'navbar-menu-title-selected'
                    : 'navbar-menu-title-not-selected'
                "
                to="/signup"
                >{{ useLanguageStore().language.messages.nav_bar__signup }}</RouterLink
              >
            </template>
            <template v-else>
              <RouterLink
                :class="
                  route.name === 'myfriends'
                    ? 'navbar-menu-title-selected'
                    : 'navbar-menu-title-not-selected'
                "
                to="/myfriends"
                >{{ useLanguageStore().language.messages.nav_bar__myFriends }}</RouterLink
              >
            </template>
            <div
              v-if="route.name === 'signup' || route.name === 'myfriends'"
              class="navbar-menu-line"
            ></div>
          </div>
        </div>
      </div>

      <div class="navbar-right-menu">
        <button class="cursor-pointer p-1 hover:text-gray-500" @click.stop="triggerLanguageMenu">
          <svg xmlns="http://www.w3.org/2000/svg" class="size-6" viewBox="0 0 16 16">
            <path
              fill="currentColor"
              d="M4.545 6.714 4.11 8H3l1.862-5h1.284L8 8H6.833l-.435-1.286zm1.634-.736L5.5 3.956h-.049l-.679 2.022z"
            />
            <path
              fill="currentColor"
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
            class="navbar-dropdown-menu"
            :class="useUserStore().user === null ? '' : '-translate-x-12'"
          >
            <template v-for="language in Languages" :key="language">
              <button class="navbar-dropdown-item" @click="changeLanguage(language)">
                {{ language }}
              </button>
            </template>
          </div>
        </transition>
        <button
          v-if="useUserStore().user !== null"
          class="cursor-pointer p-1 hover:text-gray-500"
          @click.stop="triggerProfileMenu"
        >
          <SquareImage
            :image-name="useUserStore().user!.picture"
            extra-classes="h-[35px] w-[35px] rounded-full"
            :alternate-image="blank_profile_picture"
          />
        </button>
        <transition
          enter-active-class="transition ease-out duration-100"
          enter-from-class="transform opacity-0 scale-95"
          enter-to-class="transform opacity-100 scale-100"
          leave-active-class="transition ease-in duration-75"
          leave-from-class="transform opacity-100 scale-100"
          leave-to-class="transform opacity-0 scale-95"
        >
          <div v-if="profileMenuVisible" class="navbar-dropdown-menu -translate-x-1">
            <RouterLink class="navbar-dropdown-item" to="/manageaccount">
              {{ useLanguageStore().language.messages.nav_bar__manageAccount }}
            </RouterLink>
            <RouterLink class="navbar-dropdown-item" to="/changeaccount">
              {{ useLanguageStore().language.messages.nav_bar__changeAccount }}
            </RouterLink>
            <hr v-if="useUserStore().user!.otherUsers.length > 0" />
            <button
              class="navbar-dropdown-item flex flex-row items-center gap-2"
              v-for="otherUser in useUserStore().user!.otherUsers"
              :key="'ca' + otherUser.name"
              @click="changeUser(otherUser)"
            >
              <SquareImage
                :image-name="otherUser.picture"
                extra-classes="h-[35px] w-[35px] rounded-full"
                :alternate-image="blank_profile_picture"
              />
              <span>{{ otherUser.name }}</span>
            </button>
            <hr />
            <button class="navbar-dropdown-item" @click="logout">
              {{ useLanguageStore().language.messages.nav_bar__logout }}
            </button>
          </div>
        </transition>
      </div>
    </div>

    <!--- Mobile menu -->
    <div class="mx-auto flex h-12 items-center justify-between sm:hidden">
      <div class="flex h-full flex-1/2">
        <div class="flex w-full flex-col items-center">
          <template v-if="useUserStore().user === null">
            <RouterLink
              :class="
                route.name === 'home' || route.name === 'signin'
                  ? 'navbar-menu-title-selected'
                  : 'navbar-menu-title-not-selected'
              "
              to="/signin"
              >{{ useLanguageStore().language.messages.nav_bar__signin }}</RouterLink
            >
            <div
              v-if="route.name === 'home' || route.name === 'signin'"
              class="navbar-menu-line"
            ></div>
          </template>
          <template v-else>
            <RouterLink
              :class="
                route.name === 'mywishlist'
                  ? 'navbar-menu-title-selected'
                  : 'navbar-menu-title-not-selected'
              "
              to="/mywishlist"
              >{{ useLanguageStore().language.messages.nav_bar__myList }}</RouterLink
            >
            <div v-if="route.name === 'mywishlist'" class="navbar-menu-line"></div>
          </template>
        </div>
      </div>
      <div class="flex h-full basis-1/2">
        <div class="flex w-full flex-col items-center">
          <template v-if="useUserStore().user === null">
            <RouterLink
              :class="
                route.name === 'signup'
                  ? 'navbar-menu-title-selected'
                  : 'navbar-menu-title-not-selected'
              "
              to="/signup"
              >{{ useLanguageStore().language.messages.nav_bar__signup }}</RouterLink
            >
          </template>
          <template v-else>
            <RouterLink
              :class="
                route.name === 'myfriends'
                  ? 'navbar-menu-title-selected'
                  : 'navbar-menu-title-not-selected'
              "
              to="/myfriends"
              >{{ useLanguageStore().language.messages.nav_bar__myFriends }}</RouterLink
            >
          </template>
          <div
            v-if="route.name === 'signup' || route.name === 'myfriends'"
            class="navbar-menu-line"
          ></div>
        </div>
      </div>
    </div>
  </nav>
</template>
