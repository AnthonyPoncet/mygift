<script setup lang="ts">
import { ref, useTemplateRef } from "vue";
import { getBaseUrl } from "../components/helpers/base_url";
import { useUserStore, type SignInUser } from "@/stores/user";
import { useRouter } from "vue-router";
import { useLanguageStore } from "@/stores/language";

const username = ref<string | null>(null);
const password = ref<string | null>(null);
const error = ref<string | null>(null);

const form = useTemplateRef<HTMLFormElement | null>("signUpForm");

const router = useRouter();

const creating = ref(false);

async function signup(e: Event) {
  e.preventDefault();
  creating.value = true;
  error.value = null;

  if (!form.value!.checkValidity()) {
    if (username.value === null) {
      username.value = "";
    }
    if (password.value === null) {
      password.value = "";
    }
    creating.value = false;
    return;
  }

  try {
    const response = await fetch(`${getBaseUrl()}/users`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        name: username.value,
        password: password.value,
      }),
    });
    if (response.ok) {
      const json: SignInUser = await response.json();
      useUserStore().updateUser(json);
      error.value = null;
      router.push({ name: "home" });
    }
  } catch {
    error.value = "Error while contacting the server";
  }
  creating.value = false;
}
</script>

<template>
  <div class="main-container grid grid-cols-1 justify-items-center">
    <h1 class="mb-5 text-4xl font-normal">
      {{ useLanguageStore().language.messages.signup__title }}
    </h1>
    <form ref="signUpForm">
      <div class="group relative z-0 mb-5 w-full">
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
      <div class="group relative z-0 mb-10 w-full">
        <input
          type="password"
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
        <div class="input-error" :class="{ 'peer-invalid:block': password !== null }">
          {{
            useLanguageStore().language.messages.global__form_validation_start +
            useLanguageStore().language.messages.global__password.toLowerCase()
          }}
        </div>
      </div>
      <template v-if="error !== null">
        <div class="mb-5 flex justify-center rounded-4xl bg-red-300 py-2 text-black" role="alert">
          {{ error }}
        </div>
      </template>
      <button type="submit" class="button-primary form-button" @click="signup" :disabled="creating">
        <template v-if="creating">
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
          {{ useLanguageStore().language.messages.signup__button }}
        </template>
      </button>
    </form>

    <!-- TODO -->
    <div
      class="bg-beige-300 border-beige-300 mt-4 min-w-sm rounded-4xl border p-3 text-center shadow-md"
    >
      <span class="me-2">{{ useLanguageStore().language.messages.signup__hasAccount }}</span>
      <RouterLink to="/signin" class="font-semibold underline hover:text-gray-500">{{
        useLanguageStore().language.messages.signup__connect
      }}</RouterLink>
    </div>
  </div>
</template>
