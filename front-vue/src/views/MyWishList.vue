<script setup lang="ts">
import { ref, watch, type Ref } from "vue";
import { make_authorized_request } from "@/components/helpers/make_request";
import type { Category, Gift, Wishlist } from "@/components/helpers/common_json";
import SquareImage from "@/components/SquareImage.vue";
import blank_gift from "@/assets/images/blank_gift.png";
import CategoryModal, { CategoryModalAction } from "@/components/CategoryModal.vue";
import { useLanguageStore } from "@/stores/language";
import DeleteModal, { DeleteModalAction } from "@/components/DeleteModal.vue";
import GiftModal, { GiftModalAction } from "@/components/GiftModal.vue";
import { useUserStore } from "@/stores/user";
import draggable from "vuedraggable";
import { useRouter } from "vue-router";
import { isMobile } from "@/components/helpers/is_mobile";

const router = useRouter();

const wishList: Ref<Wishlist> = ref({ categories: [] });
const pdfDownloadMode: Ref<boolean> = ref(false);

const categoryActionModal: Ref<CategoryModalAction> = ref(CategoryModalAction.Add);
const categoryModal: Ref<Category | null> = ref(null);
const giftActionModal: Ref<GiftModalAction> = ref(GiftModalAction.Add);
const giftModal: Ref<Gift | null> = ref(null);
const deleteActionModal: Ref<DeleteModalAction> = ref(DeleteModalAction.Category);
const giftIdToImageUrl: Ref<Record<number, string>> = ref({});

const hoveredGift: Ref<number | null> = ref(null);

async function getWishlist() {
  const response = await make_authorized_request(router, "/wishlist");
  if (response !== null) {
    wishList.value = await response.json();
  }
}

async function heartGift(categoryId: number, giftId: number) {
  if (pdfDownloadMode.value) return;

  const response = await make_authorized_request(
    router,
    `/wishlist/categories/${categoryId}/gifts/${giftId}/change_like`,
  );
  if (response !== null) {
    await getWishlist();
  }
}

async function getPdf() {
  pdfDownloadMode.value = true;
  const response = await make_authorized_request(
    router,
    `/wishlist/${useUserStore().user?.id}/pdf`,
  );
  if (response != null) {
    const blob = await response.blob();
    const url = window.URL.createObjectURL(new Blob([blob]));
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", `mygift_${useUserStore().user?.name}.pdf`);

    // Append to html link element page
    document.body.appendChild(link);

    // Start download
    link.click();

    // Clean up and remove the link
    link.parentNode!.removeChild(link);
  }
  pdfDownloadMode.value = false;
}

getWishlist();

watch(
  () => useUserStore().user,
  () => {
    getWishlist();
  },
);

async function reorder_categories(e: { oldIndex: number; newIndex: number }) {
  if (e.newIndex !== e.oldIndex) {
    const [first, second] =
      e.newIndex < e.oldIndex ? [e.newIndex, e.oldIndex] : [e.oldIndex, e.newIndex];
    const toSend = wishList.value.categories
      .slice(first, second + 1)
      .map((category) => category.id);

    const response = await make_authorized_request(
      router,
      `/wishlist/categories/reorder`,
      "PATCH",
      JSON.stringify({
        starting_rank: first,
        categories: toSend,
      }),
    );
    if (response !== null) {
      await getWishlist();
    }
  }
}

async function reorder_gifts(categoryIndex: number, e: { oldIndex: number; newIndex: number }) {
  if (e.newIndex !== e.oldIndex) {
    const category = wishList.value.categories[categoryIndex];

    const [first, second] =
      e.newIndex < e.oldIndex ? [e.newIndex, e.oldIndex] : [e.oldIndex, e.newIndex];
    const toSend = category.gifts.slice(first, second + 1).map((gift) => gift.id);

    const response = await make_authorized_request(
      router,
      `/wishlist/categories/${category.id}/reorder`,
      "PATCH",
      JSON.stringify({
        starting_rank: first,
        gifts: toSend,
      }),
    );
    if (response !== null) {
      await getWishlist();
    }
  }
}
</script>

<template>
  <div class="container-fluid mt-3">
    <div class="d-flex flex-row flex-wrap gap-2">
      <button
        type="button"
        class="btn btn-outline-dark me-2"
        :disabled="pdfDownloadMode || wishList.categories.length === 0"
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
        :disabled="pdfDownloadMode"
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
      <button type="button" class="btn btn-outline-dark me-2" @click="getPdf">
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

    <draggable
      v-model="wishList.categories"
      group="main"
      @end="(e) => reorder_categories(e)"
      item-key="id"
      class="mt-4"
      handle=".category-handle"
    >
      <template #item="{ element: category, index: categoryIndex }">
        <div>
          <h5 class="mt-4">
            <span class="category-handle">{{ category.name }}</span>
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
                  categoryModal = category;
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
                  categoryModal = category;
                  deleteActionModal = DeleteModalAction.Category;
                }
              "
            >
              <path
                fill-rule="evenodd"
                d="M6.5 1.75a.25.25 0 01.25-.25h2.5a.25.25 0 01.25.25V3h-3V1.75zm4.5 0V3h2.25a.75.75 0 010 1.5H2.75a.75.75 0 010-1.5H5V1.75C5 .784 5.784 0 6.75 0h2.5C10.216 0 11 .784 11 1.75zM4.496 6.675a.75.75 0 10-1.492.15l.66 6.6A1.75 1.75 0 005.405 15h5.19c.9 0 1.652-.681 1.741-1.576l.66-6.6a.75.75 0 00-1.492-.149l-.66 6.6a.25.25 0 01-.249.225h-5.19a.25.25 0 01-.249-.225l-.66-6.6z"
              ></path>
            </svg>
          </h5>

          <draggable
            v-model="category.gifts"
            :group="category.name"
            @end="(e) => reorder_gifts(categoryIndex, e)"
            item-key="id"
            class="d-flex flex-row flex-wrap gap-4"
            :handle="isMobile ? '.gift-handle' : ''"
          >
            <template #item="{ element: gift }">
              <div
                class="card gift-card"
                @mouseenter="hoveredGift = gift.id"
                @mouseleave="hoveredGift = null"
              >
                <div class="p-2 d-flex flex-row justify-content-between">
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="16"
                    height="16"
                    :fill="gift.heart ? 'red' : 'black'"
                    viewBox="0 0 16 16"
                    :class="pdfDownloadMode ? '' : 'clickable-icon'"
                    @click="heartGift(category.id, gift.id)"
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
                        categoryModal = category;
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
                </div>
                <div
                  :class="pdfDownloadMode ? '' : 'clickable-icon'"
                  :data-bs-toggle="pdfDownloadMode ? '' : 'modal'"
                  data-bs-target="#giftModal"
                  @click="
                    () => {
                      if (pdfDownloadMode) return;
                      categoryModal = category;
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
                    <p class="text-gift gift-handle">
                      {{ !isMobile && hoveredGift === gift.id ? gift.description : gift.name }}
                    </p>
                  </div>
                </div>
              </div>
            </template>
          </draggable>
        </div>
      </template>
    </draggable>
  </div>
  <CategoryModal
    @refresh-wishlist="getWishlist"
    :category="categoryModal"
    :action="categoryActionModal"
  />
  <GiftModal
    @refresh-wishlist="getWishlist"
    :gift="giftModal"
    :giftUrl="
      giftModal === null
        ? null
        : giftIdToImageUrl[giftModal.id] === undefined
          ? null
          : giftIdToImageUrl[giftModal.id]
    "
    :category="categoryModal === null ? null : categoryModal.id"
    :categories="wishList.categories"
    :action="giftActionModal"
    :secretUser="null"
  />
  <DeleteModal
    @refresh-wishlist="getWishlist"
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
