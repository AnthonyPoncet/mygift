<script lang="ts" setup>
import { useLanguageStore, Languages } from "@/stores/language";
import { useUserStore } from "@/stores/user";
import { useRouter } from "vue-router";
import SquareImage from "./SquareImage.vue";
import blank_profile_picture from "@/assets/images/blank_profile_picture.png";
import { isMobile } from "./helpers/is_mobile";
import { useTemplateRef } from "vue";

const router = useRouter();
const languageStore = useLanguageStore();

const navbarSupportedContent = useTemplateRef("navbarSupportedContent");
const navButton = useTemplateRef("navButton");

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
</script>

<template>
  <nav class="navbar navbar-expand-lg navbar-light bg-light">
    <div class="container-fluid ms-3 me-3">
      <RouterLink class="navbar-brand" to="/">Mygift</RouterLink>
      <div class="nav-item dropdown ms-auto me-2" v-if="isMobile">
        <a
          class="nav-link dropdown-toggle"
          data-bs-toggle="dropdown"
          href="#"
          role="button"
          aria-expanded="false"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="24"
            height="24"
            fill="currentColor"
            class="bi bi-globe"
            viewBox="0 0 16 16"
          >
            <path
              d="M0 8a8 8 0 1 1 16 0A8 8 0 0 1 0 8m7.5-6.923c-.67.204-1.335.82-1.887 1.855A8 8 0 0 0 5.145 4H7.5zM4.09 4a9.3 9.3 0 0 1 .64-1.539 7 7 0 0 1 .597-.933A7.03 7.03 0 0 0 2.255 4zm-.582 3.5c.03-.877.138-1.718.312-2.5H1.674a7 7 0 0 0-.656 2.5zM4.847 5a12.5 12.5 0 0 0-.338 2.5H7.5V5zM8.5 5v2.5h2.99a12.5 12.5 0 0 0-.337-2.5zM4.51 8.5a12.5 12.5 0 0 0 .337 2.5H7.5V8.5zm3.99 0V11h2.653c.187-.765.306-1.608.338-2.5zM5.145 12q.208.58.468 1.068c.552 1.035 1.218 1.65 1.887 1.855V12zm.182 2.472a7 7 0 0 1-.597-.933A9.3 9.3 0 0 1 4.09 12H2.255a7 7 0 0 0 3.072 2.472M3.82 11a13.7 13.7 0 0 1-.312-2.5h-2.49c.062.89.291 1.733.656 2.5zm6.853 3.472A7 7 0 0 0 13.745 12H11.91a9.3 9.3 0 0 1-.64 1.539 7 7 0 0 1-.597.933M8.5 12v2.923c.67-.204 1.335-.82 1.887-1.855q.26-.487.468-1.068zm3.68-1h2.146c.365-.767.594-1.61.656-2.5h-2.49a13.7 13.7 0 0 1-.312 2.5m2.802-3.5a7 7 0 0 0-.656-2.5H12.18c.174.782.282 1.623.312 2.5zM11.27 2.461c.247.464.462.98.64 1.539h1.835a7 7 0 0 0-3.072-2.472c.218.284.418.598.597.933M10.855 4a8 8 0 0 0-.468-1.068C9.835 1.897 9.17 1.282 8.5 1.077V4z"
            />
          </svg>
        </a>
        <ul class="dropdown-menu dropdown-menu-end">
          <template v-for="language in Languages" :key="language">
            <li>
              <button
                class="dropdown-item"
                type="button"
                @click="changeLanguage(language)"
                :class="useLanguageStore().language.language === language ? 'active' : ''"
              >
                {{ language }}
              </button>
            </li>
          </template>
        </ul>
      </div>
      <button
        class="navbar-toggler"
        type="button"
        data-bs-toggle="collapse"
        data-bs-target="#navbarSupportedContent"
        aria-controls="navbarSupportedContent"
        aria-expanded="false"
        aria-label="Toggle navigation"
        ref="navButton"
      >
        <span class="navbar-toggler-icon"></span>
      </button>

      <div
        class="collapse navbar-collapse"
        id="navbarSupportedContent"
        ref="navbarSupportedContent"
      >
        <ul class="navbar-nav">
          <template v-if="useUserStore().user === null">
            <li class="nav-item">
              <RouterLink class="nav-link" to="/signin" @click="collapse">{{
                useLanguageStore().language.messages.nav_bar__signin
              }}</RouterLink>
            </li>
            <li class="nav-item">
              <RouterLink class="nav-link" to="/signup" @click="collapse">{{
                useLanguageStore().language.messages.nav_bar__signup
              }}</RouterLink>
            </li>
          </template>
          <template v-else>
            <li class="nav-item">
              <RouterLink class="nav-link" to="/mywishlist" @click="collapse">{{
                useLanguageStore().language.messages.nav_bar__myList
              }}</RouterLink>
            </li>
            <li class="nav-item">
              <RouterLink class="nav-link" to="/myfriends" @click="collapse">{{
                useLanguageStore().language.messages.nav_bar__myFriends
              }}</RouterLink>
            </li>
          </template>
        </ul>
        <ul class="navbar-nav ms-auto">
          <li class="nav-item dropdown" v-if="!isMobile">
            <a
              class="nav-link dropdown-toggle"
              data-bs-toggle="dropdown"
              href="#"
              role="button"
              aria-expanded="false"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="24"
                height="24"
                fill="currentColor"
                class="bi bi-globe"
                viewBox="0 0 16 16"
              >
                <path
                  d="M0 8a8 8 0 1 1 16 0A8 8 0 0 1 0 8m7.5-6.923c-.67.204-1.335.82-1.887 1.855A8 8 0 0 0 5.145 4H7.5zM4.09 4a9.3 9.3 0 0 1 .64-1.539 7 7 0 0 1 .597-.933A7.03 7.03 0 0 0 2.255 4zm-.582 3.5c.03-.877.138-1.718.312-2.5H1.674a7 7 0 0 0-.656 2.5zM4.847 5a12.5 12.5 0 0 0-.338 2.5H7.5V5zM8.5 5v2.5h2.99a12.5 12.5 0 0 0-.337-2.5zM4.51 8.5a12.5 12.5 0 0 0 .337 2.5H7.5V8.5zm3.99 0V11h2.653c.187-.765.306-1.608.338-2.5zM5.145 12q.208.58.468 1.068c.552 1.035 1.218 1.65 1.887 1.855V12zm.182 2.472a7 7 0 0 1-.597-.933A9.3 9.3 0 0 1 4.09 12H2.255a7 7 0 0 0 3.072 2.472M3.82 11a13.7 13.7 0 0 1-.312-2.5h-2.49c.062.89.291 1.733.656 2.5zm6.853 3.472A7 7 0 0 0 13.745 12H11.91a9.3 9.3 0 0 1-.64 1.539 7 7 0 0 1-.597.933M8.5 12v2.923c.67-.204 1.335-.82 1.887-1.855q.26-.487.468-1.068zm3.68-1h2.146c.365-.767.594-1.61.656-2.5h-2.49a13.7 13.7 0 0 1-.312 2.5m2.802-3.5a7 7 0 0 0-.656-2.5H12.18c.174.782.282 1.623.312 2.5zM11.27 2.461c.247.464.462.98.64 1.539h1.835a7 7 0 0 0-3.072-2.472c.218.284.418.598.597.933M10.855 4a8 8 0 0 0-.468-1.068C9.835 1.897 9.17 1.282 8.5 1.077V4z"
                />
              </svg>
            </a>
            <ul class="dropdown-menu dropdown-menu-end">
              <template v-for="language in Languages" :key="language">
                <li>
                  <button
                    class="dropdown-item"
                    type="button"
                    @click="changeLanguage(language)"
                    :class="useLanguageStore().language.language === language ? 'active' : ''"
                  >
                    {{ language }}
                  </button>
                </li>
              </template>
            </ul>
          </li>

          <template v-if="useUserStore().user !== null">
            <li class="nav-item dropdown">
              <a
                class="nav-link dropdown-toggle"
                href="#"
                id="navbarDropdownMenuLink"
                role="button"
                data-bs-toggle="dropdown"
                aria-expanded="false"
              >
                <SquareImage
                  :image-name="useUserStore().user!.picture"
                  :size="35"
                  :alternate-image="blank_profile_picture"
                  :withTopRound="false"
                />
              </a>
              <ul class="dropdown-menu dropdown-menu-end" aria-labelledby="navbarDropdownMenuLink">
                <li>
                  <RouterLink class="dropdown-item" to="/manageaccount" @click="collapse">{{
                    useLanguageStore().language.messages.nav_bar__manageAccount
                  }}</RouterLink>
                </li>
                <li>
                  <RouterLink class="dropdown-item" to="/changeaccount" @click="collapse">{{
                    useLanguageStore().language.messages.nav_bar__changeAccount
                  }}</RouterLink>
                </li>
                <hr v-if="useUserStore().user!.otherUsers.length > 0" />
                <li>
                  <template
                    v-for="otherUser in useUserStore().user!.otherUsers"
                    :key="'ca' + otherUser.username"
                  >
                    <div
                      class="dropdown-item"
                      @click="
                        () => {
                          collapse();
                          useUserStore().changeAccount(otherUser);
                        }
                      "
                    >
                      <SquareImage
                        :image-name="otherUser.picture"
                        :size="35"
                        :alternate-image="blank_profile_picture"
                        :withTopRound="false"
                      />
                      <span class="ms-2">{{ otherUser.name }}</span>
                    </div>
                  </template>
                </li>
                <hr />
                <li>
                  <button
                    class="dropdown-item"
                    @click="
                      () => {
                        collapse();
                        logout();
                      }
                    "
                  >
                    {{ useLanguageStore().language.messages.nav_bar__logout }}
                  </button>
                </li>
              </ul>
            </li>
          </template>
        </ul>
      </div>
    </div>
  </nav>
</template>
