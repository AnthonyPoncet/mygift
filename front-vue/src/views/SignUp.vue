<script setup lang="ts">
import { ref, type Ref } from "vue";
import { getBaseUrl } from "../components/helpers/base_url";
import { useUserStore, type SignInUser } from "@/stores/user";
import { useRouter } from "vue-router";
import { useLanguageStore } from "@/stores/language";

const username: Ref<string> = ref("");
const password: Ref<string> = ref("");
const error: Ref<string | null> = ref(null);

const router = useRouter();

const creating: Ref<boolean> = ref(false);

async function signup(e: Event) {
  e.preventDefault();
  creating.value = true;

  const form: HTMLFormElement =
    (e.target as HTMLBaseElement).tagName === "DIV"
      ? ((e.target as HTMLDivElement).parentElement?.parentElement as HTMLFormElement)
      : ((e.target as HTMLButtonElement).parentElement as HTMLFormElement);
  if (!form.checkValidity()) {
    form.classList.add("was-validated");
    creating.value = false;
    return;
  }

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
  creating.value = false;
}
</script>

<template>
  <div class="grid grid-cols-1 mt-5 justify-items-center">
    <h1 class="font-normal text-4xl mb-5">{{ useLanguageStore().language.messages.signup__title }}</h1>
    <template v-if="error !== null">
      <div class="" role="alert">{{ error }}</div>
    </template>
    <form class="border rounded-md p-5 min-w-sm">
      <div class="flex flex-col mb-3">
        <label for="username" class="mb-2">{{
          useLanguageStore().language.messages.global__username
        }}</label>
        <input
          type="text"
          class="border rounded-md p-2"
          id="username"
          :placeholder="useLanguageStore().language.messages.global__username"
          v-model="username"
          autocomplete="username"
          required
        />
        <div class="hidden">
          {{
            useLanguageStore().language.messages.global__form_validation_start +
            useLanguageStore().language.messages.global__username.toLowerCase()
          }}
        </div>
      </div>
      <div class="flex flex-col mb-10">
        <label for="password" class="mb-2">{{
          useLanguageStore().language.messages.global__password
        }}</label>
        <input
          type="password"
          class="border rounded-md p-2"
          id="password"
          :placeholder="useLanguageStore().language.messages.global__password"
          v-model="password"
          autocomplete="new-password"
          required
        />
        <div class="hidden">
          {{
            useLanguageStore().language.messages.global__form_validation_start +
            useLanguageStore().language.messages.global__password.toLowerCase()
          }}
        </div>
      </div>
      <button type="submit" class="rounded-md text-white bg-red-700 hover:bg-red-600 px-3 py-2 font-semibold shadow-xs w-full" @click="signup" :disabled="creating">
        <div class="">
          {{ useLanguageStore().language.messages.signup__button }}
          <div v-if="creating" class="spinner-border ms-2" role="status" aria-hidden="true"></div>
        </div>
      </button>
    </form>
  </div>
</template>
