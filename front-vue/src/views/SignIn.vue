<script setup lang="ts">
import { ref, type Ref } from "vue";
import { getBaseUrl } from "../components/helpers/base_url";
import { useUserStore, type SignInUser } from "@/stores/user";
import { useRoute, useRouter } from "vue-router";
import { useLanguageStore } from "@/stores/language";

const username: Ref<string> = ref("");
const password: Ref<string> = ref("");
const error: Ref<string | null> = ref(null);

const router = useRouter();
const route = useRoute();

const logining: Ref<boolean> = ref(false);

async function signin(e: Event) {
  e.preventDefault();
  logining.value = true;

  const form: HTMLFormElement =
    (e.target as HTMLBaseElement).tagName === "DIV"
      ? ((e.target as HTMLDivElement).parentElement?.parentElement as HTMLFormElement)
      : ((e.target as HTMLButtonElement).parentElement as HTMLFormElement);
  if (!form.checkValidity()) {
    form.classList.add("was-validated");
    logining.value = false;
    return;
  }

  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (route.path === "/changeaccount") {
    headers["Authorization"] = `Bearer ${useUserStore().user!.token}`;
  }

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
    router.push({ name: "home" });
  } else if (response.status === 401) {
    error.value = "Incorrect username or password.";
  }
  logining.value = false;
}
</script>

<template>
  <div class="container-fluid d-grid justify-content-center mt-3">
    <h1>{{ useLanguageStore().language.messages.signin__title }}</h1>
    <template v-if="error !== null">
      <div class="alert alert-danger mt-3" role="alert">{{ error }}</div>
    </template>
    <form class="mt-3">
      <div class="mb-3">
        <label for="username" class="form-label">{{
          useLanguageStore().language.messages.global__username
        }}</label>
        <input
          type="text"
          class="form-control"
          id="username"
          :placeholder="useLanguageStore().language.messages.global__username"
          v-model="username"
          autocomplete="username"
          required
        />
        <div class="invalid-feedback">
          {{
            useLanguageStore().language.messages.global__form_validation_start +
            useLanguageStore().language.messages.global__username.toLowerCase()
          }}
        </div>
      </div>
      <div class="mb-3">
        <label for="password" class="form-label">{{
          useLanguageStore().language.messages.global__password
        }}</label>
        <input
          type="password"
          class="form-control"
          id="password"
          :placeholder="useLanguageStore().language.messages.global__password"
          v-model="password"
          autocomplete="current-password"
          required
        />
        <div class="invalid-feedback">
          {{
            useLanguageStore().language.messages.global__form_validation_start +
            useLanguageStore().language.messages.global__password.toLowerCase()
          }}
        </div>
      </div>
      <button type="submit" class="btn btn-primary w-100" @click="signin" :disabled="logining">
        <div class="d-flex align-items-center justify-content-center">
          {{ useLanguageStore().language.messages.signin__button }}
          <div v-if="logining" class="spinner-border ms-2" role="status" aria-hidden="true"></div>
        </div>
      </button>
    </form>

    <div v-if="route.path !== '/changeaccount'" class="account-instead mt-2 p-3 text-center">
      <span class="me-2">{{ useLanguageStore().language.messages.signin__newAccount }}</span>
      <RouterLink to="/signup">{{
        useLanguageStore().language.messages.signin__creatAccount
      }}</RouterLink>
    </div>
  </div>
</template>

<style lang="css" scoped>
form {
  border: 1px;
  border-style: solid;
  border-radius: 5px;
  padding: 20px;
}
.account-instead {
  border: 1px;
  border-style: solid;
  border-radius: 5px;
}
</style>
