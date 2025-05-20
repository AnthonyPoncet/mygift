<script setup lang="ts">
import { useLanguageStore } from "@/stores/language.ts";
import { ref, useTemplateRef } from "vue";
import { Cropper } from "vue-advanced-cropper";
import "vue-advanced-cropper/dist/style.css";
import { useRoute, useRouter } from "vue-router";
import { make_authorized_request } from "@/components/helpers/make_request.ts";
import type { FileUpload, Wishlist } from "@/components/helpers/common_json.ts";

const route = useRoute();
const router = useRouter();

const cropper = useTemplateRef("cropper");
const form = useTemplateRef<HTMLFormElement | null>("addGiftForm");

const name = ref<string | null>(null);
const description = ref<string | null>(null);
const price = ref<string | null>(null);
const whereToBuy = ref<string | null>(null);
const category = ref<number | null>(null);
const pictureUrl = ref<string | null>(null);

const pictureLoaded = ref(false);
const pictureHasChanged = ref(false);

const categories = ref<{ id: number; name: string }[]>([]);
const friendId = ref<number | null>(null);

async function getFriendDetails(friendName: string): Promise<number | null> {
  const response = await make_authorized_request(router, `/friends/${friendName}`);
  if (response !== null) {
    const friend: { id: number } = await response.json();
    return friend.id;
  }

  return null;
}
async function getCategories() {
  //This would need proper end points
  let endpoint;
  if (route.name == "mywishlistaddgift") {
    endpoint = "/wishlist";
  } else {
    friendId.value = await getFriendDetails(route.params.name as string);
    endpoint = `/wishlist/friend/${friendId.value}`;
  }

  const response = await make_authorized_request(router, endpoint);
  if (response !== null) {
    const wishlist: Wishlist = await response.json();
    for (const category of wishlist.categories) {
      categories.value.push({ id: category.id, name: category.name });
    }
    category.value = categories.value[0].id;
  }
}
getCategories();

const adding = ref(false);
async function add(e: Event) {
  e.preventDefault();
  adding.value = true;

  if (!form.value!.checkValidity()) {
    if (name.value === null) {
      name.value = "";
    }
    adding.value = false;
    return;
  }

  let picture = null;
  if (pictureUrl.value != null && pictureHasChanged.value) {
    picture = await storeImage();
    if (picture === "") {
      adding.value = false;
      return;
    }
  }

  let endpoint;
  if (route.name == "mywishlistaddgift") {
    endpoint = `/wishlist/categories/${category.value}/gifts`;
  } else {
    friendId.value = await getFriendDetails(route.params.name as string);
    endpoint = `/wishlist/friend/${friendId.value}/categories/${category.value}/gifts`;
  }

  let cleanPrice = price.value;
  if (cleanPrice !== null) {
    cleanPrice = cleanPrice.trim();
    if (cleanPrice.length === 0) {
      cleanPrice = null;
    }
  }
  let cleanWhereToBuy = whereToBuy.value;
  if (cleanWhereToBuy !== null) {
    cleanWhereToBuy = cleanWhereToBuy.trim();
    if (cleanWhereToBuy.length === 0) {
      cleanWhereToBuy = null;
    }
  }

  const response = await make_authorized_request(
    router,
    endpoint,
    "POST",
    JSON.stringify({
      name: name.value!.trim(),
      description: description.value,
      price: cleanPrice,
      where_to_buy: cleanWhereToBuy,
      picture: picture,
    }),
  );

  if (response !== null) {
    if (route.name == "mywishlistaddgift") {
      await router.push({ name: "mywishlist" });
    } else {
      await router.push({ name: "friend", params: { name: route.params.name } });
    }
  }

  adding.value = false;
}

function changeFile(event: Event) {
  const target = event.target as HTMLInputElement;
  if (target.files !== null && target.files.length > 0) {
    pictureUrl.value = window.URL.createObjectURL(target.files[0]);
    pictureHasChanged.value = true;
  }
}

function cropAllImage(imageInfo: { imageSize: { width: number; height: number } }) {
  return {
    width: imageInfo.imageSize.width,
    height: imageInfo.imageSize.height,
  };
}

function rotateImage(angle: number) {
  cropper.value!.rotate(angle);
}

function changeCanvas() {
  if (pictureLoaded.value) {
    //Handle the case when the thing load previous image, this trigger change somehow
    pictureLoaded.value = false;
    return;
  }
  pictureHasChanged.value = true;
}
async function storeImage(): Promise<string> {
  const { canvas } = cropper.value!.getResult();
  if (canvas) {
    const formData = new FormData();
    const blob: Blob | null = await new Promise((resolve) => canvas.toBlob(resolve, "image/png"));
    if (blob) {
      formData.append("file", blob, "image.png");
    }
    const response = await make_authorized_request(router, "/files", "post", formData, false);
    if (response != null) {
      const fileUpload: FileUpload = await response.json();
      return fileUpload.name;
    }
  }

  return "";
}
</script>

<template>
  <div class="main-container grid grid-cols-1 justify-items-center">
    <h1 class="mb-5 text-center text-4xl font-normal">
      {{
        route.name == "mywishlistaddgift"
          ? useLanguageStore().language.messages.gift_modal__addGiftTitle
          : useLanguageStore().language.messages.gift_modal__addSecretGiftTitle
      }}
    </h1>
    <form ref="addGiftForm" class="w-full md:w-1/2">
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

      <div class="group relative z-0 mb-5 w-full">
        <textarea
          type="textarea"
          rows="5"
          name="description"
          id="description"
          class="peer input-text"
          placeholder=" "
          v-model="description"
        />
        <label for="description" class="input-label">{{
          useLanguageStore().language.messages.global__description
        }}</label>
      </div>

      <div class="group relative z-0 mb-5 w-full">
        <input
          type="text"
          name="price"
          id="price"
          class="peer input-text"
          placeholder=" "
          v-model="price"
        />
        <label for="price" class="input-label">{{
          useLanguageStore().language.messages.global__price
        }}</label>
      </div>

      <div class="group relative z-0 mb-5 w-full">
        <input
          type="text"
          name="whereToBuy"
          id="whereToBuy"
          class="peer input-text"
          placeholder=" "
          v-model="whereToBuy"
        />
        <label for="whereToBuy" class="input-label">{{
          useLanguageStore().language.messages.global__whereToBuy
        }}</label>
      </div>

      <div class="mb-5 flex w-full flex-col gap-2">
        <label for="category">{{ useLanguageStore().language.messages.global__category }}</label>
        <select class="form-select" id="category" v-model="category" required>
          <template v-for="category in categories" :key="'gsc-' + category.id">
            <option :value="category.id">{{ category.name }}</option>
          </template>
        </select>
      </div>

      <div class="mb-5 flex w-full flex-col gap-2">
        <label for="picture">{{ useLanguageStore().language.messages.global__picture }}</label>
        <input type="file" class="form-file-picker" id="picture" @change="changeFile" />
      </div>

      <div v-if="pictureUrl !== null">
        <Cropper
          ref="cropper"
          :src="pictureUrl"
          :default-size="cropAllImage"
          :min-width="100"
          :min-height="100"
          @change="changeCanvas"
        />
        <div class="my-2 flex flex-row justify-between">
          <button type="button" class="button-primary mb-7 px-10 py-2" @click="rotateImage(-90)">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="16"
              height="16"
              class="bi bi-arrow-counterclockwise"
              viewBox="0 0 16 16"
            >
              <path
                fill="currentColor"
                fill-rule="evenodd"
                d="M8 3a5 5 0 1 1-4.546 2.914.5.5 0 0 0-.908-.417A6 6 0 1 0 8 2z"
              />
              <path
                fill="currentColor"
                d="M8 4.466V.534a.25.25 0 0 0-.41-.192L5.23 2.308a.25.25 0 0 0 0 .384l2.36 1.966A.25.25 0 0 0 8 4.466"
              />
            </svg>
          </button>
          <button type="button" class="button-primary mb-7 px-10 py-2" @click="rotateImage(90)">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="16"
              height="16"
              class="bi bi-arrow-clockwise"
              viewBox="0 0 16 16"
            >
              <path
                fill="currentColor"
                fill-rule="evenodd"
                d="M8 3a5 5 0 1 0 4.546 2.914.5.5 0 0 1 .908-.417A6 6 0 1 1 8 2z"
              />
              <path
                fill="currentColor"
                d="M8 4.466V.534a.25.25 0 0 1 .41-.192l2.36 1.966c.12.1.12.284 0 .384L8.41 4.658A.25.25 0 0 1 8 4.466"
              />
            </svg>
          </button>
        </div>
      </div>

      <hr />

      <button type="submit" class="button-primary form-button mt-8" @click="add" :disabled="adding">
        <template v-if="adding">
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
          {{ useLanguageStore().language.messages.global__add }}
        </template>
      </button>
    </form>
  </div>
</template>
