<script setup lang="ts">
import type {
  FriendCategory,
  FriendGift,
  FriendWishlist,
  Gift,
} from "@/components/helpers/common_json";
import { make_authorized_request } from "@/components/helpers/make_request";
import SquareImage from "@/components/SquareImage.vue";
import { useLanguageStore } from "@/stores/language";
import { ref, watch, type Ref } from "vue";
import { useRoute } from "vue-router";
import blank_gift from "@/assets/images/blank_gift.png";
import { useUserStore } from "@/stores/user";
import GiftModal, { GiftModalAction } from "@/components/GiftModal.vue";
import ShowGiftModal from "@/components/ShowGiftModal.vue";

const route = useRoute();

const friendName: Ref<string> = ref((route.params as { name: string }).name);
const friendId: Ref<number> = ref(-1);

watch(route.params, () => {
  console.log(route.params);
});

const wishList: Ref<FriendWishlist> = ref({ categories: [] });

const giftModal: Ref<Gift | null> = ref(null);
const categoryModal: Ref<number | null> = ref(null);
const giftActionModal: Ref<GiftModalAction> = ref(GiftModalAction.Add);
const giftIdToImageUrl: Ref<Record<number, string>> = ref({});
const reserved: Ref<boolean> = ref(false);

const hoveredGift: Ref<number | null> = ref(null);

async function getFriendDetails() {
  const response = await make_authorized_request(`/friends/${friendName.value}`);
  if (response !== null) {
    const friend: { id: number } = await response.json();
    friendId.value = friend.id;
  }
}

async function getGifts() {
  if (friendId.value === -1) {
    await getFriendDetails();
  }
  const response = await make_authorized_request(`/wishlist/friend/${friendId.value}`);
  if (response !== null) {
    wishList.value = await response.json();
  }
}

async function reserve(id: number, reservedBy: number | null) {
  const response = await make_authorized_request(
    `/wishlist/friend/${friendId.value}/gifts/${id}`,
    reservedBy === null ? "POST" : "DELETE",
  );
  if (response !== null) {
    getGifts();
  }
}

function getCardClasses(gift: FriendGift): string {
  let classes = "";

  if (gift.reserved_by !== null) classes += "gift-already-bought";
  if (gift.secret) classes += " gift-secret";

  return classes;
}

async function deleteGift(category: FriendCategory, gift: FriendGift) {
  const response = await make_authorized_request(
    `/wishlist/friend/${friendId.value}/categories/${category.id}/gifts/${gift.id}`,
    "DELETE",
  );
  if (response !== null) {
    getGifts();
  }
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
    <h1>{{ useLanguageStore().language.messages.friendlist__title + friendName }}</h1>
    <button
      type="button"
      class="btn btn-outline-dark me-2 mt-2"
      :disabled="wishList.categories.length === 0"
      data-bs-toggle="modal"
      data-bs-target="#giftModal"
      @click="
        () => {
          giftActionModal = GiftModalAction.AddSecret;
        }
      "
    >
      {{ useLanguageStore().language.messages.friendlist__addGiftButton }}
    </button>
    <div class="mt-4">
      <template v-for="category in wishList.categories" :key="'c' + category.id">
        <h5 class="mt-4">
          {{ category.name }}
        </h5>
        <div class="d-flex flex-row flex-wrap gap-4">
          <template v-for="gift in category.gifts" :key="'g' + gift.id">
            <div
              class="card gift-card"
              :class="getCardClasses(gift)"
              @mouseenter="hoveredGift = gift.id"
              @mouseleave="hoveredGift = null"
            >
              <div class="p-2 d-flex flex-row justify-content-between align-items-center">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="16"
                  height="16"
                  :fill="gift.heart ? 'red' : 'black'"
                  viewBox="0 0 16 16"
                  class="clickable"
                  :data-bs-toggle="gift.secret && gift.reserved_by === null ? 'modal' : ''"
                  data-bs-target="#giftModal"
                  @click="
                    () => {
                      if (gift.reserved_by === null) {
                        categoryModal = category.id;
                        giftModal = gift;
                        giftActionModal = GiftModalAction.EditSecret;
                      } else {
                        deleteGift(category, gift);
                      }
                    }
                  "
                >
                  <template v-if="gift.secret && gift.reserved_by === null">
                    <path
                      fill-rule="evenodd"
                      d="M11.013 1.427a1.75 1.75 0 012.474 0l1.086 1.086a1.75 1.75 0 010 2.474l-8.61 8.61c-.21.21-.47.364-.756.445l-3.251.93a.75.75 0 01-.927-.928l.929-3.25a1.75 1.75 0 01.445-.758l8.61-8.61zm1.414 1.06a.25.25 0 00-.354 0L10.811 3.75l1.439 1.44 1.263-1.263a.25.25 0 000-.354l-1.086-1.086zM11.189 6.25L9.75 4.81l-6.286 6.287a.25.25 0 00-.064.108l-.558 1.953 1.953-.558a.249.249 0 00.108-.064l6.286-6.286z"
                    ></path>
                  </template>
                  <template v-else-if="gift.secret">
                    <path
                      data-v-db77f6ee=""
                      fill-rule="evenodd"
                      d="M6.5 1.75a.25.25 0 01.25-.25h2.5a.25.25 0 01.25.25V3h-3V1.75zm4.5 0V3h2.25a.75.75 0 010 1.5H2.75a.75.75 0 010-1.5H5V1.75C5 .784 5.784 0 6.75 0h2.5C10.216 0 11 .784 11 1.75zM4.496 6.675a.75.75 0 10-1.492.15l.66 6.6A1.75 1.75 0 005.405 15h5.19c.9 0 1.652-.681 1.741-1.576l.66-6.6a.75.75 0 00-1.492-.149l-.66 6.6a.25.25 0 01-.249.225h-5.19a.25.25 0 01-.249-.225l-.66-6.6z"
                    ></path>
                  </template>
                  <template v-else>
                    <path
                      fill-rule="evenodd"
                      d="M4.25 2.5c-1.336 0-2.75 1.164-2.75 3 0 2.15 1.58 4.144 3.365 5.682A20.565 20.565 0 008 13.393a20.561 20.561 0 003.135-2.211C12.92 9.644 14.5 7.65 14.5 5.5c0-1.836-1.414-3-2.75-3-1.373 0-2.609.986-3.029 2.456a.75.75 0 01-1.442 0C6.859 3.486 5.623 2.5 4.25 2.5zM8 14.25l-.345.666-.002-.001-.006-.003-.018-.01a7.643 7.643 0 01-.31-.17 22.075 22.075 0 01-3.434-2.414C2.045 10.731 0 8.35 0 5.5 0 2.836 2.086 1 4.25 1 5.797 1 7.153 1.802 8 3.02 8.847 1.802 10.203 1 11.75 1 13.914 1 16 2.836 16 5.5c0 2.85-2.045 5.231-3.885 6.818a22.08 22.08 0 01-3.744 2.584l-.018.01-.006.003h-.002L8 14.25zm0 0l.345.666a.752.752 0 01-.69 0L8 14.25z"
                    ></path>
                  </template>
                </svg>
                <div
                  class="gift-secret-text fw-bold fst-italic"
                  :class="gift.secret ? '' : 'hidden'"
                >
                  SECRET
                </div>
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="16"
                  height="16"
                  viewBox="0 0 16 16"
                  class="clickable"
                  :fill="
                    gift.reserved_by !== null && gift.reserved_by === useUserStore().user?.id
                      ? '#007bff'
                      : 'black'
                  "
                  :visibility="
                    gift.reserved_by !== null && gift.reserved_by !== useUserStore().user?.id
                      ? 'collapse'
                      : 'visible'
                  "
                  @click="reserve(gift.id, gift.reserved_by)"
                >
                  <path
                    fill-rule="evenodd"
                    d="M4.75 1.5a1.25 1.25 0 100 2.5h2.309c-.233-.818-.542-1.401-.878-1.793-.43-.502-.915-.707-1.431-.707zM2 2.75c0 .45.108.875.3 1.25h-.55A1.75 1.75 0 000 5.75v2c0 .698.409 1.3 1 1.582v4.918c0 .966.784 1.75 1.75 1.75h10.5A1.75 1.75 0 0015 14.25V9.332c.591-.281 1-.884 1-1.582v-2A1.75 1.75 0 0014.25 4h-.55a2.75 2.75 0 00-2.45-4c-.984 0-1.874.42-2.57 1.23A5.086 5.086 0 008 2.274a5.086 5.086 0 00-.68-1.042C6.623.42 5.733 0 4.75 0A2.75 2.75 0 002 2.75zM8.941 4h2.309a1.25 1.25 0 100-2.5c-.516 0-1 .205-1.43.707-.337.392-.646.975-.879 1.793zm-1.84 1.5H1.75a.25.25 0 00-.25.25v2c0 .138.112.25.25.25h5.5V5.5h-.149zm1.649 0V8h5.5a.25.25 0 00.25-.25v-2a.25.25 0 00-.25-.25h-5.5zm0 4h4.75v4.75a.25.25 0 01-.25.25h-4.5v-5zm-1.5 0v5h-4.5a.25.25 0 01-.25-.25V9.5h4.75z"
                  ></path>
                </svg>
              </div>
              <div
                class="clickable"
                data-bs-toggle="modal"
                data-bs-target="#showGiftModal"
                @click="
                  () => {
                    giftModal = gift;
                    reserved = gift.reserved_by !== null;
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
                  <template v-if="hoveredGift === gift.id">
                    <p class="text-gift">{{ gift.description }}</p>
                  </template>
                  <template v-else>
                    <p class="title-gift">{{ gift.name }}</p>
                    <p class="text-truncate">{{ gift.price }}</p>
                  </template>
                </div>
              </div>
            </div>
          </template>
        </div>
      </template>
    </div>
  </div>
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
    :category="categoryModal"
    :categories="wishList.categories"
    :action="giftActionModal"
    :secretUser="friendId"
  />
  <ShowGiftModal
    @refresh-wishlist="getGifts"
    :gift="giftModal!"
    :giftUrl="
      giftModal === null
        ? blank_gift
        : giftIdToImageUrl[giftModal.id] === undefined
          ? blank_gift
          : giftIdToImageUrl[giftModal.id]
    "
    :reserved="reserved"
    :friend-id="friendId"
  />
</template>

<style lang="css" scoped>
.gift-card {
  height: 300px;
  width: 152px;
  border-color: black;
}

.gift-secret {
  border-color: red;
}

.gift-secret-text {
  color: red;
}

.title-gift {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  height: calc(2em * var(--bs-body-line-height));
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

.clickable {
  cursor: pointer;
}

.gift-already-bought {
  background-color: #c0c0c0;
  opacity: 0.5;
}

.hidden {
  visibility: hidden;
}
</style>
