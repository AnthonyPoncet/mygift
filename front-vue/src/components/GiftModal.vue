<script lang="ts">
export enum GiftModalAction {
  Add = "add",
  AddSecret = "add-secret",
  Edit = "edit",
  EditSecret = "edit-secret",
}
</script>

<script setup lang="ts">
import { onMounted, ref, useTemplateRef, watch, type Ref } from "vue";
import { make_authorized_request } from "./helpers/make_request";
import { Modal } from "bootstrap";
import { useLanguageStore } from "@/stores/language";
import type { FileUpload, Gift } from "./helpers/common_json";
import { Cropper } from "vue-advanced-cropper";
import "vue-advanced-cropper/dist/style.css";

const props = defineProps<{
  action: GiftModalAction;
  category: number | null;
  gift: Gift | null;
  giftUrl: string | null;
  categories: { id: number; name: string }[];
  secretUser: number | null;
}>();

const modal = useTemplateRef("giftModal");
const form = useTemplateRef("giftModalForm");
const bootstrapModal = ref();
const emit = defineEmits(["refresh-wishlist"]);
const cropper = useTemplateRef("cropper");

const addEditing: Ref<boolean> = ref(false);

const title: Ref<string> = ref(useLanguageStore().language.messages.gift_modal__addGiftTitle);
const buttonText: Ref<string> = ref(useLanguageStore().language.messages.global__add);

const actionRef: Ref<GiftModalAction> = ref(GiftModalAction.Add);
const categoryRef: Ref<number> = ref(0);
const pictureUrl: Ref<string | null> = ref(null);
const pictureLoaded: Ref<boolean> = ref(false);
const pictureHasChanged: Ref<boolean> = ref(false);
const giftRef: Ref<Gift> = ref({
  id: 0,
  name: "",
  description: "",
  price: "",
  where_to_buy: "",
  picture: "",
  heart: false,
});
const secretUserRef: Ref<number | null> = ref(null);

watch(props, () => {
  if (props.categories.length === 0) {
    return;
  }

  if (form.value) {
    form.value.reset();
  }

  actionRef.value = props.action;
  if (actionRef.value === GiftModalAction.Add || actionRef.value === GiftModalAction.AddSecret) {
    title.value = useLanguageStore().language.messages.gift_modal__addGiftTitle;
    buttonText.value = useLanguageStore().language.messages.global__add;
    giftRef.value = {
      id: 0,
      name: "",
      description: "",
      price: "",
      where_to_buy: "",
      picture: "",
      heart: false,
    };
    categoryRef.value = props.categories[0].id;
    pictureUrl.value = null;
    secretUserRef.value = actionRef.value === GiftModalAction.AddSecret ? props.secretUser : null;
    pictureLoaded.value = false;
  } else {
    title.value = useLanguageStore().language.messages.gift_modal__updateGiftTitle;
    buttonText.value = useLanguageStore().language.messages.global__update;
    giftRef.value = props.gift!;
    categoryRef.value = props.category!;
    pictureUrl.value = props.giftUrl;
    secretUserRef.value = actionRef.value === GiftModalAction.EditSecret ? props.secretUser : null;
    pictureLoaded.value = pictureUrl.value != null;
  }
  pictureHasChanged.value = false;
});

async function clickButton(event: Event) {
  event.preventDefault();
  addEditing.value = true;

  if (!form.value!.checkValidity()) {
    form.value!.classList.add("was-validated");
    addEditing.value = false;
    return;
  }

  let picture = giftRef.value.picture;
  if (pictureUrl.value != null && pictureHasChanged.value) {
    picture = await storeImage();
    if (picture === "") {
      addEditing.value = false;
      return;
    }
  }

  let endpoint = "";
  let method = "";
  if (actionRef.value === GiftModalAction.Add) {
    endpoint = `/wishlist/categories/${categoryRef.value}/gifts`;
    method = "POST";
  } else if (actionRef.value === GiftModalAction.AddSecret) {
    endpoint = `/wishlist/friend/${secretUserRef.value}/categories/${categoryRef.value}/gifts`;
    method = "POST";
  } else if (actionRef.value === GiftModalAction.EditSecret) {
    endpoint = `/wishlist/friend/${secretUserRef.value}/categories/${categoryRef.value}/gifts/${giftRef.value.id}`;
    method = "PATCH";
  } else {
    endpoint = `/wishlist/categories/${categoryRef.value}/gifts/${giftRef.value.id}`;
    method = "PATCH";
  }

  const response = await make_authorized_request(
    endpoint,
    method,
    JSON.stringify({
      name: giftRef.value.name,
      description: giftRef.value.description,
      price: giftRef.value.price,
      where_to_buy: giftRef.value.where_to_buy,
      picture: picture,
    }),
  );
  if (response !== null) {
    bootstrapModal.value.hide();
    emit("refresh-wishlist");
    form.value!.classList.remove("was-validated");
    pictureUrl.value = null;
    form.value!.reset();
  }

  addEditing.value = false;
}

//TODO: need to ask why deleted
async function deleteGift() {
  if (giftRef.value !== null) {
    const response = await make_authorized_request(
      `/wishlist/categories/${categoryRef.value}/gifts/${giftRef.value.id}`,
      "DELETE",
    );
    if (response !== null) {
      bootstrapModal.value.hide();
      emit("refresh-wishlist");
    }
  }
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
    const response = await make_authorized_request("/files", "post", formData, false);
    if (response != null) {
      const fileUpload: FileUpload = await response.json();
      return fileUpload.name;
    }
  }

  return "";
}

onMounted(() => {
  bootstrapModal.value = new Modal(modal.value!);
});
</script>

<template>
  <div
    class="modal fade"
    id="giftModal"
    ref="giftModal"
    tabindex="-1"
    aria-labelledby="giftModalLabel"
    aria-hidden="true"
  >
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title" id="giftModalLabel">{{ title }}</h5>
          <button
            type="button"
            class="btn-close"
            data-bs-dismiss="modal"
            aria-label="Close"
          ></button>
        </div>
        <div class="modal-body">
          <form ref="giftModalForm">
            <div class="mb-3">
              <label for="name" class="form-label">{{
                useLanguageStore().language.messages.global__name
              }}</label>
              <input
                type="text"
                class="form-control"
                id="name"
                :placeholder="useLanguageStore().language.messages.global__name"
                v-model="giftRef.name"
                required
              />
              <div class="invalid-feedback">
                {{
                  useLanguageStore().language.messages.global__form_validation_start +
                  useLanguageStore().language.messages.global__name.toLowerCase()
                }}
              </div>
            </div>
            <div class="mb-3">
              <label for="description" class="form-label">{{
                useLanguageStore().language.messages.global__description
              }}</label>
              <textarea
                type="textarea"
                class="form-control"
                id="description"
                :placeholder="useLanguageStore().language.messages.global__description"
                v-model="giftRef.description"
                rows="2"
              ></textarea>
            </div>
            <div class="mb-3">
              <label for="price" class="form-label">{{
                useLanguageStore().language.messages.global__price
              }}</label>
              <input
                type="text"
                class="form-control"
                id="price"
                :placeholder="useLanguageStore().language.messages.global__price"
                v-model="giftRef.price"
              />
            </div>
            <div class="mb-3">
              <label for="whereToBuy" class="form-label">{{
                useLanguageStore().language.messages.global__whereToBuy
              }}</label>
              <input
                type="text"
                class="form-control"
                id="whereToBuy"
                :placeholder="useLanguageStore().language.messages.global__whereToBuy"
                v-model="giftRef.where_to_buy"
              />
            </div>
            <div class="mb-3">
              <label for="category" class="form-label">{{
                useLanguageStore().language.messages.global__category
              }}</label>
              <select class="form-select" v-model="categoryRef">
                <template v-for="category in props.categories" :key="'gsc-' + category.id">
                  <option :value="category.id">{{ category.name }}</option>
                </template>
              </select>
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
                :default-size="cropAllImage"
                :min-width="100"
                :min-height="100"
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
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button
            type="submit"
            class="btn btn-primary w-100"
            :disabled="addEditing"
            @click="clickButton"
          >
            <div class="d-flex align-items-center justify-content-center">
              {{ buttonText }}
              <div
                v-if="addEditing"
                class="spinner-border ms-2"
                role="status"
                aria-hidden="true"
              ></div>
            </div>
          </button>
          <button type="submit" class="btn btn-secondary w-100" @click="deleteGift">
            {{ useLanguageStore().language.messages.global__delete }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
