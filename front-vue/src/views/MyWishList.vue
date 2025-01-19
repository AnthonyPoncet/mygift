<script setup lang="ts">
import { ref, watch, type Ref } from "vue";
import { make_authorized_request } from "@/components/helpers/make_request";
import type { Category, Gift, OneCategory } from "@/components/helpers/common_json";
import SquareImage from "@/components/SquareImage.vue";
import blank_gift from "@/assets/images/blank_gift.png";
import CategoryModal, { CategoryModalAction } from "@/components/CategoryModal.vue";
import { useLanguageStore } from "@/stores/language";
import DeleteModal, { DeleteModalAction } from "@/components/DeleteModal.vue";
import GiftModal, { GiftModalAction } from "@/components/GiftModal.vue";
import { useUserStore } from "@/stores/user";

const wishList: Ref<OneCategory[]> = ref([]);
const editMode: Ref<boolean> = ref(false);
const pdfDownloadMode: Ref<boolean> = ref(false);

const categoryActionModal: Ref<CategoryModalAction> = ref(CategoryModalAction.Add);
const categoryModal: Ref<Category | null> = ref(null);
const giftActionModal: Ref<GiftModalAction> = ref(GiftModalAction.Add);
const giftModal: Ref<Gift | null> = ref(null);
const deleteActionModal: Ref<DeleteModalAction> = ref(DeleteModalAction.Category);
const giftIdToImageUrl: Ref<Record<number, string>> = ref({});

const hoveredGift: Ref<number | null> = ref(null);

enum Rank {
  Down = "down",
  Up = "up",
}

enum Kind {
  Category = "categories",
  Gift = "gifts",
}

async function getGifts() {
  const response = await make_authorized_request("/gifts");
  if (response !== null) {
    wishList.value = await response.json();
  }
}

async function heartGift(id: number, currentHeartState: boolean) {
  if (pdfDownloadMode.value) return;

  const response = await make_authorized_request(
    `/gifts/${id}/heart/${currentHeartState ? "unlike" : "like"}`,
    "POST",
  );
  if (response !== null) {
    await getGifts();
  }
}

async function rank(kind: Kind, id: number, rank: Rank) {
  const response = await make_authorized_request(`/${kind}/${id}/rank-actions/${rank}`, "POST");
  if (response !== null) {
    await getGifts();
  }
}

async function getPdf() {
  pdfDownloadMode.value = true;
  const response = await make_authorized_request(`/gifts/pdf`);
  if (response != null) {
    const blob = await response.blob();
    const url = window.URL.createObjectURL(new Blob([blob]));
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", `mygift_${useUserStore().user?.username}.pdf`);

    // Append to html link element page
    document.body.appendChild(link);

    // Start download
    link.click();

    // Clean up and remove the link
    link.parentNode!.removeChild(link);
  }
  pdfDownloadMode.value = false;
}

getGifts();

watch(
  () => useUserStore().user,
  () => {
    getGifts();
  },
);
</script>

<template>
  <div class="container-fluid mt-3">
    <div class="d-flex flex-row flex-wrap gap-2">
      <button
        type="button"
        class="btn btn-outline-dark me-2"
        :disabled="editMode || pdfDownloadMode || wishList.length === 0"
        data-bs-toggle="modal"
        data-bs-target="#giftModal"
        @click="
          () => {
            giftActionModal = GiftModalAction.Add;
          }
        "
      >
        {{ useLanguageStore().language.messages.mywishlist__addGiftButton }}
      </button>
      <button
        type="button"
        class="btn btn-outline-dark me-2"
        :disabled="editMode || pdfDownloadMode"
        data-bs-toggle="modal"
        data-bs-target="#categoryModal"
        @click="
          () => {
            categoryModal = null;
            categoryActionModal = CategoryModalAction.Add;
          }
        "
      >
        {{ useLanguageStore().language.messages.mywishlist__addCategoryButton }}
      </button>
      <button
        type="button"
        class="btn btn-outline-dark me-2"
        data-bs-toggle="button"
        :disabled="pdfDownloadMode"
        @click="editMode = !editMode"
      >
        {{ useLanguageStore().language.messages.mywishlist__reorderButton }}
      </button>
      <button
        type="button"
        class="btn btn-outline-dark me-2"
        :disabled="editMode || pdfDownloadMode"
        @click="getPdf"
      >
        <div class="d-flex align-items-center justify-content-center">
          {{ useLanguageStore().language.messages.mywishlist__downloadPdfButton }}
          <div
            v-if="pdfDownloadMode"
            class="spinner-border ms-2"
            role="status"
            aria-hidden="true"
          ></div>
        </div>
      </button>
    </div>
    <div class="mt-4">
      <template v-for="(category, categoryIndex) in wishList" :key="'c' + category.category.id">
        <h5 class="mt-4">
          {{ category.category.name }}
          <template v-if="editMode">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="16"
              height="16"
              fill="black"
              viewBox="0 0 16 16"
              :visibility="categoryIndex === 0 ? 'collapse' : 'visible'"
              :class="categoryIndex === 0 ? 'collapse' : ''"
              class="clickable-icon ms-1"
              @click="rank(Kind.Category, category.category.id, Rank.Down)"
            >
              <path
                fill-rule="evenodd"
                d="M3.47 7.78a.75.75 0 010-1.06l4.25-4.25a.75.75 0 011.06 0l4.25 4.25a.75.75 0 01-1.06 1.06L9 4.81v7.44a.75.75 0 01-1.5 0V4.81L4.53 7.78a.75.75 0 01-1.06 0z"
              ></path>
            </svg>
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="16"
              height="16"
              fill="black"
              viewBox="0 0 16 16"
              :visibility="categoryIndex === wishList.length - 1 ? 'hidden' : 'visible'"
              :class="categoryIndex === 0 ? 'ms-1' : 'ms-2'"
              class="clickable-icon"
              @click="rank(Kind.Category, category.category.id, Rank.Up)"
            >
              <path
                fill-rule="evenodd"
                d="M13.03 8.22a.75.75 0 010 1.06l-4.25 4.25a.75.75 0 01-1.06 0L3.47 9.28a.75.75 0 011.06-1.06l2.97 2.97V3.75a.75.75 0 011.5 0v7.44l2.97-2.97a.75.75 0 011.06 0z"
              ></path>
            </svg>
          </template>
          <template v-else>
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="16"
              height="16"
              fill="black"
              viewBox="0 0 16 16"
              class="ms-1"
              :class="pdfDownloadMode ? '' : 'clickable-icon'"
              :data-bs-toggle="pdfDownloadMode ? '' : 'modal'"
              data-bs-target="#categoryModal"
              @click="
                () => {
                  if (pdfDownloadMode) return;
                  categoryModal = category.category;
                  categoryActionModal = CategoryModalAction.Edit;
                }
              "
            >
              <path
                fill-rule="evenodd"
                d="M11.013 1.427a1.75 1.75 0 012.474 0l1.086 1.086a1.75 1.75 0 010 2.474l-8.61 8.61c-.21.21-.47.364-.756.445l-3.251.93a.75.75 0 01-.927-.928l.929-3.25a1.75 1.75 0 01.445-.758l8.61-8.61zm1.414 1.06a.25.25 0 00-.354 0L10.811 3.75l1.439 1.44 1.263-1.263a.25.25 0 000-.354l-1.086-1.086zM11.189 6.25L9.75 4.81l-6.286 6.287a.25.25 0 00-.064.108l-.558 1.953 1.953-.558a.249.249 0 00.108-.064l6.286-6.286z"
              ></path>
            </svg>
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="16"
              height="16"
              fill="black"
              viewBox="0 0 16 16"
              class="ms-2"
              :class="pdfDownloadMode ? '' : 'clickable-icon'"
              :data-bs-toggle="pdfDownloadMode ? '' : 'modal'"
              data-bs-target="#deleteModal"
              @click="
                () => {
                  if (pdfDownloadMode) return;
                  categoryModal = category.category;
                  deleteActionModal = DeleteModalAction.Category;
                }
              "
            >
              <path
                fill-rule="evenodd"
                d="M6.5 1.75a.25.25 0 01.25-.25h2.5a.25.25 0 01.25.25V3h-3V1.75zm4.5 0V3h2.25a.75.75 0 010 1.5H2.75a.75.75 0 010-1.5H5V1.75C5 .784 5.784 0 6.75 0h2.5C10.216 0 11 .784 11 1.75zM4.496 6.675a.75.75 0 10-1.492.15l.66 6.6A1.75 1.75 0 005.405 15h5.19c.9 0 1.652-.681 1.741-1.576l.66-6.6a.75.75 0 00-1.492-.149l-.66 6.6a.25.25 0 01-.249.225h-5.19a.25.25 0 01-.249-.225l-.66-6.6z"
              ></path>
            </svg>
          </template>
        </h5>
        <div class="d-flex flex-row flex-wrap gap-4">
          <template v-for="(gift, giftIndex) in category.gifts" :key="'g' + gift.id">
            <div
              class="card gift-card"
              @mouseenter="hoveredGift = gift.id"
              @mouseleave="hoveredGift = null"
            >
              <div class="p-2 d-flex flex-row justify-content-between">
                <template v-if="editMode">
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="16"
                    height="16"
                    fill="black"
                    viewBox="0 0 16 16"
                    :visibility="giftIndex === 0 ? 'hidden' : 'visible'"
                    class="clickable-icon"
                    @click="rank(Kind.Gift, gift.id, Rank.Down)"
                  >
                    <path
                      fill-rule="evenodd"
                      d="M7.78 12.53a.75.75 0 01-1.06 0L2.47 8.28a.75.75 0 010-1.06l4.25-4.25a.75.75 0 011.06 1.06L4.81 7h7.44a.75.75 0 010 1.5H4.81l2.97 2.97a.75.75 0 010 1.06z"
                    ></path>
                  </svg>
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="16"
                    height="16"
                    fill="black"
                    viewBox="0 0 16 16"
                    :visibility="giftIndex === category.gifts.length - 1 ? 'hidden' : 'visible'"
                    class="clickable-icon"
                    @click="rank(Kind.Gift, gift.id, Rank.Up)"
                  >
                    <path
                      fill-rule="evenodd"
                      d="M8.22 2.97a.75.75 0 011.06 0l4.25 4.25a.75.75 0 010 1.06l-4.25 4.25a.75.75 0 01-1.06-1.06l2.97-2.97H3.75a.75.75 0 010-1.5h7.44L8.22 4.03a.75.75 0 010-1.06z"
                    ></path>
                  </svg>
                </template>
                <template v-else>
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="16"
                    height="16"
                    :fill="gift.heart ? 'red' : 'black'"
                    viewBox="0 0 16 16"
                    :class="pdfDownloadMode ? '' : 'clickable-icon'"
                    @click="heartGift(gift.id, gift.heart)"
                  >
                    <path
                      fill-rule="evenodd"
                      d="M4.25 2.5c-1.336 0-2.75 1.164-2.75 3 0 2.15 1.58 4.144 3.365 5.682A20.565 20.565 0 008 13.393a20.561 20.561 0 003.135-2.211C12.92 9.644 14.5 7.65 14.5 5.5c0-1.836-1.414-3-2.75-3-1.373 0-2.609.986-3.029 2.456a.75.75 0 01-1.442 0C6.859 3.486 5.623 2.5 4.25 2.5zM8 14.25l-.345.666-.002-.001-.006-.003-.018-.01a7.643 7.643 0 01-.31-.17 22.075 22.075 0 01-3.434-2.414C2.045 10.731 0 8.35 0 5.5 0 2.836 2.086 1 4.25 1 5.797 1 7.153 1.802 8 3.02 8.847 1.802 10.203 1 11.75 1 13.914 1 16 2.836 16 5.5c0 2.85-2.045 5.231-3.885 6.818a22.08 22.08 0 01-3.744 2.584l-.018.01-.006.003h-.002L8 14.25zm0 0l.345.666a.752.752 0 01-.69 0L8 14.25z"
                    ></path>
                  </svg>
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="16"
                    height="16"
                    fill="black"
                    viewBox="0 0 16 16"
                    :class="pdfDownloadMode ? '' : 'clickable-icon'"
                    :data-bs-toggle="pdfDownloadMode ? '' : 'modal'"
                    data-bs-target="#deleteModal"
                    @click="
                      () => {
                        if (pdfDownloadMode) return;
                        giftModal = gift;
                        deleteActionModal = DeleteModalAction.Gift;
                      }
                    "
                  >
                    <path
                      fill-rule="evenodd"
                      d="M6.5 1.75a.25.25 0 01.25-.25h2.5a.25.25 0 01.25.25V3h-3V1.75zm4.5 0V3h2.25a.75.75 0 010 1.5H2.75a.75.75 0 010-1.5H5V1.75C5 .784 5.784 0 6.75 0h2.5C10.216 0 11 .784 11 1.75zM4.496 6.675a.75.75 0 10-1.492.15l.66 6.6A1.75 1.75 0 005.405 15h5.19c.9 0 1.652-.681 1.741-1.576l.66-6.6a.75.75 0 00-1.492-.149l-.66 6.6a.25.25 0 01-.249.225h-5.19a.25.25 0 01-.249-.225l-.66-6.6z"
                    ></path>
                  </svg>
                </template>
              </div>
              <div
                :class="editMode || pdfDownloadMode ? '' : 'clickable-icon'"
                :data-bs-toggle="editMode || pdfDownloadMode ? '' : 'modal'"
                data-bs-target="#giftModal"
                @click="
                  () => {
                    if (editMode || pdfDownloadMode) return;
                    categoryModal = category.category;
                    giftModal = gift;
                    giftActionModal = GiftModalAction.Edit;
                  }
                "
              >
                <SquareImage
                  :image-name="gift.picture"
                  :size="150"
                  :alternate-image="blank_gift"
                  @image-loaded="
                    (url) => {
                      giftIdToImageUrl[gift.id] = url;
                    }
                  "
                  :withTopRound="false"
                />
                <div class="card-body text-center">
                  <p class="text-gift">
                    {{ hoveredGift === gift.id ? gift.description : gift.name }}
                  </p>
                </div>
              </div>
            </div>
          </template>
        </div>
      </template>
    </div>
  </div>
  <CategoryModal
    @refresh-wishlist="getGifts"
    :category="categoryModal"
    :action="categoryActionModal"
  />
  <GiftModal
    @refresh-wishlist="getGifts"
    :gift="giftModal"
    :giftUrl="
      giftModal === null
        ? null
        : giftIdToImageUrl[giftModal.id] === undefined
          ? null
          : giftIdToImageUrl[giftModal.id]
    "
    :category="categoryModal === null ? null : categoryModal.id"
    :categories="wishList.map((c) => c.category)"
    :action="giftActionModal"
    :secretUser="null"
  />
  <DeleteModal
    @refresh-wishlist="getGifts"
    :action="deleteActionModal"
    :category="categoryModal"
    :gift="giftModal"
  />
</template>

<style lang="css" scoped>
.gift-card {
  height: 275px;
  width: 152px;
  border-color: black;
}

.clickable-icon {
  cursor: pointer;
}

.text-gift {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  height: calc(3em * var(--bs-body-line-height));
}
</style>
