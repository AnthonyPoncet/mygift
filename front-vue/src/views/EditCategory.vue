<script setup lang="ts">
import { useLanguageStore } from "@/stores/language.ts";
import { ref, useTemplateRef } from "vue";
import { useRoute, useRouter } from "vue-router";
import { make_authorized_request } from "@/components/helpers/make_request.ts";
import type { Friends, Wishlist } from "@/components/helpers/common_json.ts";

const route = useRoute();
const router = useRouter();

const form = useTemplateRef<HTMLFormElement | null>("addCategoryForm");

const name = ref<string | null>(null);
const isShared = ref(false);
const shareWith = ref<number[]>([]);

const friends = ref<Friends>({ friends: [] });

async function getFriends() {
  const response = await make_authorized_request(router, "/friends");
  if (response !== null) {
    friends.value = await response.json();
  }
}
getFriends();

async function getCategory() {
  //This would need proper end points
  const response = await make_authorized_request(router, "/wishlist");
  if (response !== null) {
    const categoryId = Number(route.params.id as string);
    const wishlist: Wishlist = await response.json();
    for (const category of wishlist.categories) {
      if (category.id === categoryId) {
        name.value = category.name;
        shareWith.value = category.share_with;
        isShared.value = shareWith.value.length > 0;
        return;
      }
    }
  }
}
getCategory();

const editing = ref(false);
async function edit(e: Event) {
  e.preventDefault();
  editing.value = true;

  if (!form.value!.checkValidity()) {
    if (name.value === null) {
      name.value = "";
    }
    editing.value = false;
    return;
  }

  const response = await make_authorized_request(
    router,
    `/wishlist/categories/${route.params.id}`,
    "PATCH",
    JSON.stringify({
      name: name.value!.trim(),
      share_with: shareWith.value,
    }),
  );

  if (response !== null) {
    await router.push({ name: "mywishlist" });
  }

  editing.value = false;
}

const deleting = ref(false);
async function deleteCategory() {
  deleting.value = true;
  const response = await make_authorized_request(
    router,
    `/wishlist/categories/${route.params.id}`,
    "DELETE",
  );
  if (response !== null) {
    await router.push({ name: "mywishlist" });
  }
  deleting.value = false;
}
</script>

<template>
  <div class="main-container grid grid-cols-1 justify-items-center">
    <h1 class="mb-5 text-center text-4xl font-normal">
      {{ useLanguageStore().language.messages.category_modal__updateCategoryTitle }}
    </h1>
    <form ref="addCategoryForm" class="w-full md:w-1/2">
      <div class="group relative z-0 mb-5 w-full">
        <input
          type="text"
          name="name"
          id="name"
          class="peer input-text"
          :class="{ 'input-text-invalid': name !== null }"
          placeholder=" "
          v-model="name"
          required
        />
        <label for="name" class="input-label" :class="{ 'input-label-invalid': name !== null }">{{
          useLanguageStore().language.messages.global__name
        }}</label>
        <div class="input-error" :class="{ 'peer-invalid:block': name !== null }">
          {{
            useLanguageStore().language.messages.global__form_validation_start +
            useLanguageStore().language.messages.global__name.toLowerCase()
          }}
        </div>
      </div>

      <div class="mb-5 flex w-full flex-col gap-2">
        <label for="friends">{{ useLanguageStore().language.messages.global__share_with }}</label>
        <select multiple id="friends" v-model="shareWith" class="form-select-multiple">
          <template v-for="friend in friends.friends" :key="'sf-' + friend.id">
            <option :value="friend.id">{{ friend.name }}</option>
          </template>
        </select>
      </div>

      <hr />

      <button
        type="submit"
        class="button-primary form-button mt-8"
        @click="edit"
        :disabled="editing"
      >
        <template v-if="editing">
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
          {{ useLanguageStore().language.messages.global__update }}
        </template>
      </button>
    </form>

    <div
      class="bg-beige-300 border-beige-300 mt-4 flex w-full min-w-sm flex-col gap-5 rounded-4xl border p-3 text-center shadow-md md:w-1/2"
    >
      <button
        type="button"
        class="button-primary form-button"
        @click="deleteCategory"
        :disabled="deleting"
      >
        <template v-if="deleting">
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
          {{ useLanguageStore().language.messages.global__delete }}
        </template>
      </button>
      <div class="flex flex-row items-center justify-center rounded-4xl bg-rose-950/20 px-5 py-2">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          class="me-3 size-[24px] flex-shrink-0"
          viewBox="0 0 16 16"
        >
          <path
            fill="currentColor"
            d="M8.982 1.566a1.13 1.13 0 0 0-1.96 0L.165 13.233c-.457.778.091 1.767.98 1.767h13.713c.889 0 1.438-.99.98-1.767zM8 5c.535 0 .954.462.9.995l-.35 3.507a.552.552 0 0 1-1.1 0L7.1 5.995A.905.905 0 0 1 8 5m.002 6a1 1 0 1 1 0 2 1 1 0 0 1 0-2"
          />
        </svg>
        <span>{{
          isShared
            ? useLanguageStore().language.messages.delete_modal__category_shared_hint
            : useLanguageStore().language.messages.delete_modal__category_hint
        }}</span>
      </div>
    </div>
  </div>
</template>
