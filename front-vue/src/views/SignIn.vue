<script setup lang="ts">
import { ref, useTemplateRef } from "vue";
import { getBaseUrl } from "../components/helpers/base_url";
import { useUserStore, type SignInUser } from "@/stores/user";
import { useRoute, useRouter } from "vue-router";
import { useLanguageStore } from "@/stores/language";

const username = ref<string | null>(null);
const password = ref<string | null>(null);
const showPassword = ref<boolean>(false);
const error = ref<string | null>(null);

const form = useTemplateRef<HTMLFormElement | null>("signInForm");

const router = useRouter();
const route = useRoute();

const logining = ref(false);

async function signin(e: Event) {
  e.preventDefault();
  logining.value = true;
  error.value = null;

  if (!form.value!.checkValidity()) {
    if (username.value === null) {
      username.value = "";
    }
    if (password.value === null) {
      password.value = "";
    }
    logining.value = false;
    return;
  }

  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (route.path === "/changeaccount") {
    headers["Authorization"] = `Bearer ${useUserStore().user!.token}`;
  }

  try {
    const response = await fetch(
      `${getBaseUrl()}/users/${route.path === "/changeaccount" ? "change-account" : "connect"}`,
      {
        method: "post",
        headers,
        body: JSON.stringify({
          name: username.value,
          password: password.value,
        }),
      },
    );
    if (response.ok) {
      const json: SignInUser = await response.json();
      if (route.path === "/changeaccount") {
        useUserStore().logMultiAccount(json);
      } else {
        useUserStore().updateUser(json);
      }
      error.value = null;
      await router.push({ name: "home" });
    } else if (response.status === 401) {
      error.value = useLanguageStore().language.messages.signin__errorLogin;
    }
  } catch {
    error.value = "Error while contacting the server";
  }
  logining.value = false;
}
</script>

<template>
  <div class="main-container grid grid-cols-1 justify-items-center">
    <h1 class="mb-5 text-4xl font-normal">
      {{ useLanguageStore().language.messages.signin__title }}
    </h1>
    <form ref="signInForm" class="flex flex-col gap-5">
      <div v-if="error !== null" class="text-red-secret-text mb-5 w-full text-center" role="alert">
        {{ error }}
      </div>
      <div class="group relative z-0 w-full">
        <input
          type="text"
          name="username"
          id="username"
          class="peer input-text"
          :class="{ 'input-text-invalid': username !== null }"
          placeholder=" "
          v-model="username"
          autocomplete="username"
          required
        />
        <label
          for="username"
          class="input-label"
          :class="{ 'input-label-invalid': username !== null }"
          >{{ useLanguageStore().language.messages.global__username }}</label
        >
        <div class="input-error" :class="{ 'peer-invalid:block': username !== null }">
          {{
            useLanguageStore().language.messages.global__form_validation_start +
            useLanguageStore().language.messages.global__username.toLowerCase()
          }}
        </div>
      </div>
      <div class="group relative z-0 mb-5 w-full">
        <input
          :type="showPassword ? 'text' : 'password'"
          name="password"
          id="password"
          class="peer input-text"
          :class="{ 'input-text-invalid': password !== null }"
          placeholder=" "
          v-model="password"
          autocomplete="new-password"
          required
        />
        <label
          for="password"
          class="input-label"
          :class="{ 'input-label-invalid': password !== null }"
          >{{ useLanguageStore().language.messages.global__password }}</label
        >
        <svg
          v-if="!showPassword"
          xmlns="http://www.w3.org/2000/svg"
          class="absolute end-0 z-10 size-[16px] -translate-y-5 cursor-pointer"
          viewBox="0 0 16 16"
          @click="showPassword = !showPassword"
        >
          <path
            fill="currentColor"
            d="M13.359 11.238C15.06 9.72 16 8 16 8s-3-5.5-8-5.5a7 7 0 0 0-2.79.588l.77.771A6 6 0 0 1 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13 13 0 0 1 14.828 8q-.086.13-.195.288c-.335.48-.83 1.12-1.465 1.755q-.247.248-.517.486z"
          />
          <path
            fill="currentColor"
            d="M11.297 9.176a3.5 3.5 0 0 0-4.474-4.474l.823.823a2.5 2.5 0 0 1 2.829 2.829zm-2.943 1.299.822.822a3.5 3.5 0 0 1-4.474-4.474l.823.823a2.5 2.5 0 0 0 2.829 2.829"
          />
          <path
            fill="currentColor"
            d="M3.35 5.47q-.27.24-.518.487A13 13 0 0 0 1.172 8l.195.288c.335.48.83 1.12 1.465 1.755C4.121 11.332 5.881 12.5 8 12.5c.716 0 1.39-.133 2.02-.36l.77.772A7 7 0 0 1 8 13.5C3 13.5 0 8 0 8s.939-1.721 2.641-3.238l.708.709zm10.296 8.884-12-12 .708-.708 12 12z"
          />
        </svg>
        <svg
          v-else
          xmlns="http://www.w3.org/2000/svg"
          class="absolute end-0 z-10 size-[16px] -translate-y-5 cursor-pointer"
          viewBox="0 0 16 16"
          @click="showPassword = !showPassword"
        >
          <path
            fill="currentColor"
            d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8M1.173 8a13 13 0 0 1 1.66-2.043C4.12 4.668 5.88 3.5 8 3.5s3.879 1.168 5.168 2.457A13 13 0 0 1 14.828 8q-.086.13-.195.288c-.335.48-.83 1.12-1.465 1.755C11.879 11.332 10.119 12.5 8 12.5s-3.879-1.168-5.168-2.457A13 13 0 0 1 1.172 8z"
          />
          <path
            fill="currentColor"
            d="M8 5.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5M4.5 8a3.5 3.5 0 1 1 7 0 3.5 3.5 0 0 1-7 0"
          />
        </svg>
        <div class="input-error" :class="{ 'peer-invalid:block': password !== null }">
          {{
            useLanguageStore().language.messages.global__form_validation_start +
            useLanguageStore().language.messages.global__password.toLowerCase()
          }}
        </div>
      </div>
      <button
        type="submit"
        class="button-primary flex w-full items-center justify-center"
        @click="signin"
        :disabled="logining"
      >
        <template v-if="logining">
          <div role="status" aria-hidden="true">
            <svg
              aria-hidden="true"
              class="spinner"
              viewBox="0 0 100 101"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                d="M100 50.5908C100 78.2051 77.6142 100.591 50 100.591C22.3858 100.591 0 78.2051 0 50.5908C0 22.9766 22.3858 0.59082 50 0.59082C77.6142 0.59082 100 22.9766 100 50.5908ZM9.08144 50.5908C9.08144 73.1895 27.4013 91.5094 50 91.5094C72.5987 91.5094 90.9186 73.1895 90.9186 50.5908C90.9186 27.9921 72.5987 9.67226 50 9.67226C27.4013 9.67226 9.08144 27.9921 9.08144 50.5908Z"
                fill="currentColor"
              />
              <path
                d="M93.9676 39.0409C96.393 38.4038 97.8624 35.9116 97.0079 33.5539C95.2932 28.8227 92.871 24.3692 89.8167 20.348C85.8452 15.1192 80.8826 10.7238 75.2124 7.41289C69.5422 4.10194 63.2754 1.94025 56.7698 1.05124C51.7666 0.367541 46.6976 0.446843 41.7345 1.27873C39.2613 1.69328 37.813 4.19778 38.4501 6.62326C39.0873 9.04874 41.5694 10.4717 44.0505 10.1071C47.8511 9.54855 51.7191 9.52689 55.5402 10.0491C60.8642 10.7766 65.9928 12.5457 70.6331 15.2552C75.2735 17.9648 79.3347 21.5619 82.5849 25.841C84.9175 28.9121 86.7997 32.2913 88.1811 35.8758C89.083 38.2158 91.5421 39.6781 93.9676 39.0409Z"
                fill="currentFill"
              />
            </svg>
          </div>
        </template>
        <template v-else>
          {{ useLanguageStore().language.messages.signin__button }}
        </template>
      </button>
    </form>

    <div
      v-if="route.path !== '/changeaccount'"
      class="bg-beige-300 border-beige-300 mt-4 min-w-sm rounded-4xl border p-3 text-center shadow-md"
    >
      <span class="me-2">{{ useLanguageStore().language.messages.signin__newAccount }}</span>
      <RouterLink to="/signup" class="font-semibold underline hover:text-gray-500">{{
        useLanguageStore().language.messages.signin__createAccount
      }}</RouterLink>
    </div>
  </div>
</template>
