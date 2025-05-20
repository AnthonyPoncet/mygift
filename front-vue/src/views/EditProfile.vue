<script setup lang="ts">
import type { FileUpload } from "@/components/helpers/common_json";
import { make_authorized_request } from "@/components/helpers/make_request";
import { useLanguageStore } from "@/stores/language";
import { useUserStore } from "@/stores/user";
import { ref, useTemplateRef, watch, type Ref } from "vue";
import { Cropper } from "vue-advanced-cropper";
import "vue-advanced-cropper/dist/style.css";
import { useRouter } from "vue-router";

const router = useRouter();

function humanReadableDateOfBirth(): string | null {
  const unixTime = useUserStore().user!.dateOfBirth;
  if (unixTime === null || unixTime === undefined) return null;
  return new Date(unixTime * 1000 + new Date().getTimezoneOffset() * 60000).toLocaleDateString();
}

const username: Ref<string> = ref(useUserStore().user!.name);
const dateOfBirth: Ref<string | null> = ref(humanReadableDateOfBirth());
const pictureUrl: Ref<string | null> = ref(null);
const pictureHasChanged: Ref<boolean> = ref(false);
const pictureLoaded: Ref<boolean> = ref(false);
const ready: Ref<boolean> = ref(false);

const cropper = useTemplateRef("cropper");
const form = useTemplateRef("editProfileForm");

const modifying: Ref<boolean> = ref(false);

async function getImage() {
  const picture = useUserStore().user!.picture;
  if (picture === null) return;

  pictureLoaded.value = true;
  ready.value = false;

  const response = await make_authorized_request(router, `/files/${picture}`);
  if (response !== null) {
    const blob = await response.blob();
    pictureUrl.value = window.URL.createObjectURL(blob);
  }
}

getImage();

function changeFile(event: Event) {
  const target = event.target as HTMLInputElement;
  if (target.files !== null && target.files.length > 0) {
    pictureUrl.value = window.URL.createObjectURL(target.files[0]);
    pictureHasChanged.value = true;
    ready.value = false;
  }
}

function rotateImage(angle: number) {
  cropper.value!.rotate(angle);
}

function changeCanvas() {
  if (pictureLoaded.value) {
    //Handle the case when the thing load previous image, this trigger change somehow
    pictureLoaded.value = false;
    return;
  } else {
    pictureHasChanged.value = true;
  }
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

async function editProfile(event: Event) {
  modifying.value = true;
  event.preventDefault();

  if (!form.value!.checkValidity()) {
    if (username.value === null) {
      username.value = "";
    }
    modifying.value = false;
    return;
  }

  let sendingDateOfBirth = null;
  if (dateOfBirth.value) {
    const dateOfBirthSplit = dateOfBirth.value.split("/");
    if (dateOfBirthSplit.length !== 3) {
      //TODO: Error
      return;
    }

    const asDate = new Date(
      Number(dateOfBirthSplit[2]),
      Number(dateOfBirthSplit[1]) - 1,
      Number(dateOfBirthSplit[0]),
    );
    sendingDateOfBirth = (asDate.getTime() - asDate.getTimezoneOffset() * 60000) / 1000;
  }

  let picture = useUserStore().user!.picture;
  if (pictureUrl.value != null && pictureHasChanged.value) {
    picture = await storeImage();
    if (picture === "") {
      //TODO: Error
      return;
    }
  }

  const response = await make_authorized_request(
    router,
    "/users",
    "PATCH",
    JSON.stringify({
      name: username.value,
      picture: picture,
      date_of_birth: sendingDateOfBirth,
    }),
  );

  if (response != null) {
    useUserStore().updateUser({
      id: useUserStore().user!.id,
      name: username.value,
      token: useUserStore().user!.token,
      picture: picture,
      date_of_birth: Number(sendingDateOfBirth),
    });
  }

  modifying.value = false;
}

watch(
  () => useUserStore().user,
  () => {
    getImage();
    username.value = useUserStore().user!.name;
    dateOfBirth.value = humanReadableDateOfBirth();
    pictureUrl.value = null;
    pictureHasChanged.value = false;
    pictureLoaded.value = useUserStore().user!.picture !== null;
    ready.value = false;
  },
);

function cropAllImage(imageInfo: { imageSize: { width: number; height: number } }) {
  return {
    width: imageInfo.imageSize.width,
    height: imageInfo.imageSize.height,
  };
}
</script>

<template>
  <div class="main-container">
    <form ref="editProfileForm" class="w-full md:w-1/2">
      <div class="group relative z-0 mb-5 w-full">
        <input
          type="text"
          name="username"
          id="username"
          class="peer input-text"
          :class="{ 'input-text-invalid': username !== null }"
          placeholder=" "
          v-model="username"
          required
        />
        <label
          for="name"
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
          type="text"
          name="dateOfBirth"
          id="dateOfBirth"
          class="peer input-text"
          :class="{ 'input-text-invalid': dateOfBirth !== null }"
          placeholder=" "
          v-model="dateOfBirth"
          required
        />
        <label
          for="name"
          class="input-label"
          :class="{ 'input-label-invalid': dateOfBirth !== null }"
          >{{ useLanguageStore().language.messages.global__dateOfBirth }}</label
        >
        <div class="input-error" :class="{ 'peer-invalid:block': dateOfBirth !== null }">
          {{
            useLanguageStore().language.messages.global__form_validation_start +
            useLanguageStore().language.messages.global__dateOfBirth.toLowerCase()
          }}
        </div>
      </div>

      <div class="mb-5 flex w-full flex-col gap-2">
        <label for="picture">{{ useLanguageStore().language.messages.global__picture }}</label>
        <input type="file" class="form-file-picker" id="picture" @change="changeFile" />
      </div>

      <div v-if="pictureUrl !== null">
        <Cropper
          ref="cropper"
          :src="pictureUrl"
          :stencil-props="{
            aspectRatio: 1,
          }"
          :default-size="cropAllImage"
          :min-width="100"
          :min-height="100"
          @ready="ready = true"
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

      <button
        type="submit"
        class="button-primary form-button mt-8"
        @click="editProfile"
        :disabled="modifying"
      >
        <template v-if="modifying">
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
  </div>
</template>

<style lang="css" scoped>
.form-edit {
  max-width: 500px;
}
</style>
