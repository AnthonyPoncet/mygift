<script setup lang="ts">
import type { FileUpload } from "@/components/helpers/common_json";
import { make_authorized_request } from "@/components/helpers/make_request";
import { useLanguageStore } from "@/stores/language";
import { useUserStore } from "@/stores/user";
import { ref, useTemplateRef, watch, type Ref } from "vue";
import { Cropper } from "vue-advanced-cropper";
import "vue-advanced-cropper/dist/style.css";

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
const dateOfBirthInput = useTemplateRef("dateOfBirthInput");

const modifying: Ref<boolean> = ref(false);

async function getImage() {
  const picture = useUserStore().user!.picture;
  if (picture === null) return;

  pictureLoaded.value = true;
  ready.value = false;

  const response = await make_authorized_request(`/files/${picture}`);
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
    const response = await make_authorized_request("/files", "post", formData, false);
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
    form.value!.classList.add("was-validated");
    return;
  }

  let sendingDateOfBirth = null;
  if (dateOfBirth.value) {
    const dateOfBirthSplitted = dateOfBirth.value.split("/");
    if (dateOfBirthSplitted.length !== 3) {
      //TODO: Error
      return;
    }

    const asDate = new Date(
      Number(dateOfBirthSplitted[2]),
      Number(dateOfBirthSplitted[1]) - 1,
      Number(dateOfBirthSplitted[0]),
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
    "/users",
    "PATCH",
    JSON.stringify({
      name: username.value,
      picture: picture,
      dateOfBirth: sendingDateOfBirth,
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
    pictureLoaded.value = false;
    ready.value = false;
  },
);
</script>

<template>
  <div class="container-fluid mt-3">
    <form class="form-edit" ref="editProfileForm">
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
        <label for="dateOfBirth" class="form-label">{{
          useLanguageStore().language.messages.global__dateOfBirth
        }}</label>
        <input
          type="text"
          class="form-control"
          id="dateOfBirth"
          :placeholder="useLanguageStore().language.messages.global__dateOfBirth"
          v-model="dateOfBirth"
          ref="dateOfBirthInput"
          aria-describedby="dateOfBirthFeedback"
        />
        <div class="invalid-feedback" id="dateOfBirthFeedback">
          {{
            useLanguageStore().language.messages.global__form_validation_start +
            useLanguageStore().language.messages.global__dateOfBirth.toLowerCase()
          }}
        </div>
      </div>
      <div class="mb-3">
        <label for="picture" class="form-label">{{
          useLanguageStore().language.messages.global__picture
        }}</label>
        <input type="file" class="form-control" id="picture" @change="changeFile" />
      </div>
      <div v-if="pictureUrl !== null">
        <Cropper
          ref="cropper"
          :src="pictureUrl"
          :stencil-props="{
            aspectRatio: 1,
          }"
          @ready="ready = true"
          @change="changeCanvas"
        />
        <div class="d-flex justify-content-between mt-2">
          <button type="button" class="btn btn-secondary w-25" @click="rotateImage(-90)">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="16"
              height="16"
              fill="currentColor"
              class="bi bi-arrow-counterclockwise"
              viewBox="0 0 16 16"
            >
              <path
                fill-rule="evenodd"
                d="M8 3a5 5 0 1 1-4.546 2.914.5.5 0 0 0-.908-.417A6 6 0 1 0 8 2z"
              />
              <path
                d="M8 4.466V.534a.25.25 0 0 0-.41-.192L5.23 2.308a.25.25 0 0 0 0 .384l2.36 1.966A.25.25 0 0 0 8 4.466"
              />
            </svg>
          </button>
          <button type="button" class="btn btn-secondary w-25" @click="rotateImage(90)">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="16"
              height="16"
              fill="currentColor"
              class="bi bi-arrow-clockwise"
              viewBox="0 0 16 16"
            >
              <path
                fill-rule="evenodd"
                d="M8 3a5 5 0 1 0 4.546 2.914.5.5 0 0 1 .908-.417A6 6 0 1 1 8 2z"
              />
              <path
                d="M8 4.466V.534a.25.25 0 0 1 .41-.192l2.36 1.966c.12.1.12.284 0 .384L8.41 4.658A.25.25 0 0 1 8 4.466"
              />
            </svg>
          </button>
        </div>
        <button
          type="submit"
          class="btn btn-primary w-100 mt-3"
          @click="editProfile"
          :disabled="modifying"
        >
          <div class="d-flex align-items-center justify-content-center">
            {{ useLanguageStore().language.messages.global__update }}
            <div
              v-if="modifying"
              class="spinner-border ms-2"
              role="status"
              aria-hidden="true"
            ></div>
          </div>
        </button>
      </div>
    </form>
  </div>
</template>

<style lang="css" scoped>
.form-edit {
  max-width: 500px;
}
</style>
