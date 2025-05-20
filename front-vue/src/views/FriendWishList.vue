<script setup lang="ts">
import type { FriendGift, FriendWishlist, Gift } from "@/components/helpers/common_json";
import { make_authorized_request } from "@/components/helpers/make_request";
import SquareImage from "@/components/SquareImage.vue";
import { useLanguageStore } from "@/stores/language";
import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import blank_gift from "@/assets/images/blank_gift.png";
import { useUserStore } from "@/stores/user";

const route = useRoute();
const router = useRouter();

const friendName = ref<string>((route.params as { name: string }).name);
const friendId = ref(-1);

const wishList = ref<FriendWishlist>({ categories: [] });
const hoveredGift = ref<number | null>(null);

const pdfDownloadMode = ref(false);

const giftIdToImageUrl = ref<Record<number, string>>({});

async function getFriendDetails() {
  const response = await make_authorized_request(router, `/friends/${friendName.value}`);
  if (response !== null) {
    const friend: { id: number } = await response.json();
    friendId.value = friend.id;
  }
}

async function getGifts() {
  if (friendId.value === -1) {
    await getFriendDetails();
  }
  const response = await make_authorized_request(router, `/wishlist/friend/${friendId.value}`);
  if (response !== null) {
    wishList.value = await response.json();
  }
}

async function reserve(id: number, reserve: boolean) {
  const response = await make_authorized_request(
    router,
    `/wishlist/friend/${friendId.value}/gifts/${id}`,
    reserve ? "POST" : "DELETE",
  );
  if (response !== null) {
    getGifts();
  }
}

function getCardClasses(gift: FriendGift): string {
  let classes = "";

  if (gift.reserved_by !== null) classes += "bg-zinc-200 hover:bg-zinc-300 opacity-70";
  if (gift.secret) classes += " secret-card";

  return classes;
}

const showModal = ref(false);
const modalCategory = ref<number | null>(null);
const modalGift = ref<Gift | null>(null);
function displayModal(gift: Gift, categoryId: number) {
  showModal.value = true;
  modalGift.value = gift;
  modalCategory.value = categoryId;
  window.addEventListener("click", hideModal);
}
function hideModal() {
  showModal.value = false;
  modalGift.value = null;
  modalCategory.value = null;
}
async function deleteGift() {
  const response = await make_authorized_request(
    router,
    `/wishlist/friend/${friendId.value}/categories/${modalCategory.value}/gifts/${modalGift.value!.id}`,
    "DELETE",
  );
  if (response !== null) {
    getGifts();
  }
}

async function getPdf() {
  pdfDownloadMode.value = true;
  const response = await make_authorized_request(router, `/wishlist/${friendId.value}/pdf`);
  if (response != null) {
    const blob = await response.blob();
    const url = window.URL.createObjectURL(new Blob([blob]));
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", `mygift_${friendName.value}.pdf`);

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
</script>

<template>
  <div>
    <div class="relative z-10" :hidden="!showModal">
      <div
        class="fixed z-20 flex min-h-full w-screen flex-col items-center justify-center bg-black/50 p-4"
      >
        <div
          class="bg-beige-300 border-beige-300 flex min-w-sm flex-col items-center gap-10 rounded-4xl border p-5 text-center shadow-md"
        >
          <div class="flex flex-row items-center justify-center">
            <h1 class="text-2xl">
              {{
                useLanguageStore().language.messages.delete_modal__pre_text +
                useLanguageStore().language.messages.delete_modal__pre_text_gift
              }}
              '{{ modalGift?.name }}' ?"
            </h1>
            <svg
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 16 16"
              class="size-[50px] flex-1/2 cursor-pointer sm:flex-auto"
              @click="hideModal"
            >
              <path
                fill="currentColor"
                d="M4.646 4.646a.5.5 0 0 1 .708 0L8 7.293l2.646-2.647a.5.5 0 0 1 .708.708L8.707 8l2.647 2.646a.5.5 0 0 1-.708.708L8 8.707l-2.646 2.647a.5.5 0 0 1-.708-.708L7.293 8 4.646 5.354a.5.5 0 0 1 0-.708"
              />
            </svg>
          </div>
          <button class="button-primary w-[80%]" @click="deleteGift">
            {{ useLanguageStore().language.messages.global__delete }}
          </button>
        </div>
      </div>
    </div>
    <div class="main-container">
      <h1 class="mb-5 text-center text-2xl font-bold sm:text-left">
        {{ useLanguageStore().language.messages.friendlist__title + friendName }}
      </h1>
      <div class="flex flex-row flex-wrap justify-center gap-5 sm:justify-start">
        <button
          type="button"
          class="button-primary button-icon"
          :disabled="pdfDownloadMode || wishList.categories.length === 0"
          @click="router.push({ name: 'friendaddgift', params: { name: friendName } })"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="size-[32px]" viewBox="0 0 16 16">
            <path
              fill="currentColor"
              fill-rule="evenodd"
              d="M 4.7113403 0 C 3.6458583 0 2.5802043 0.71049397 2.5802043 2.1311361 L 2.5802043 2.1363037 C 2.5793724 2.2773163 2.5903717 2.4177235 2.6127604 2.5569499 L 0.87539876 2.5569499 C 0.40463913 2.5569499 0.023254395 2.9388514 0.023254395 3.409611 L 0.023254395 5.1144165 C 0.023254395 5.5851761 0.40463913 5.9665609 0.87539876 5.9665609 L 0.87539876 12.359452 C 0.87539876 13.065592 1.4482512 13.638316 2.1543905 13.638444 L 7.6651652 13.638961 C 7.6651649 13.638961 7.4958801 13.263949 7.4041992 12.955282 C 7.3125181 12.646613 7.244519 12.183752 7.244519 12.183752 C 7.1594897 11.60157 7.2402354 11.317669 7.2987793 10.786938 C 7.3573229 10.256208 7.665682 9.6469564 7.665682 9.6469564 L 7.6946208 5.9665609 L 11.956376 5.9665609 L 11.959993 7.2326335 C 11.959993 7.2326335 12.080614 7.2476404 12.364103 7.2920614 C 12.647592 7.3364822 12.794051 7.3948975 12.794051 7.3948975 L 12.809037 5.9665609 C 13.279798 5.9665609 13.661182 5.5851761 13.661182 5.1144165 L 13.661182 3.409611 C 13.661182 2.9388514 13.279798 2.5569499 12.809037 2.5569499 L 11.071676 2.5569499 C 11.104066 2.3660155 11.104232 2.1959707 11.104232 2.1363037 L 11.104232 2.1311361 C 11.104232 -0.71014813 6.8424764 -0.71014813 6.8424764 2.1311361 C 6.8424764 0.71049397 5.7768224 0 4.7113403 0 z M 4.7113403 0.85214437 C 5.3506295 0.85214437 5.9898153 1.2787508 5.9898153 2.1311361 L 5.9898153 2.5569499 L 3.490743 2.5569499 L 3.471106 2.497522 C 3.434449 2.3645498 3.4328654 2.203589 3.4328654 2.1311361 C 3.4328654 1.2787508 4.0720511 0.85214437 4.7113403 0.85214437 z M 8.9730957 0.85214437 C 9.6123849 0.85214437 10.252087 1.2787508 10.252087 2.1311361 C 10.252087 2.2035889 10.24998 2.3645498 10.21333 2.497522 L 10.193693 2.5569499 L 7.6946208 2.5569499 L 7.6946208 2.1311361 C 7.6946208 1.2787508 8.3338065 0.85214437 8.9730957 0.85214437 z M 0.87539876 3.409611 L 5.9898153 3.409611 L 5.9898153 5.1144165 L 0.87539876 5.1144165 L 0.87539876 3.409611 z M 7.6946208 3.409611 L 12.809037 3.409611 L 12.809037 5.1144165 L 7.6946208 5.1144165 L 7.6946208 3.409611 z M 1.7280599 5.9665609 L 5.9898153 5.9665609 L 5.9898153 12.785783 L 2.1543905 12.785783 C 1.9190107 12.785783 1.7280599 12.594831 1.7280599 12.359452 L 1.7280599 5.9665609 z M 11.634949 7.277592 C 9.2337119 7.2775897 7.275526 9.2337116 7.2755249 11.634949 C 7.2755244 14.036186 9.2337107 15.994375 11.634949 15.994373 C 14.036185 15.994374 15.992306 14.036185 15.992306 11.634949 C 15.992304 9.2337123 14.036184 7.2775906 11.634949 7.277592 z M 11.634949 8.2775309 C 13.495745 8.2775297 14.992366 9.7741513 14.992367 11.634949 C 14.992367 13.495746 13.495746 14.993918 11.634949 14.993917 C 9.7741502 14.993918 8.2754635 13.495747 8.2754639 11.634949 C 8.2754648 9.7741508 9.7741512 8.277529 11.634949 8.2775309 z M 11.634949 8.6408162 A 0.5 0.5 0 0 0 11.134721 9.1405273 L 11.134721 11.134721 L 9.1384603 11.134721 A 0.5 0.5 0 0 0 8.6387492 11.634949 A 0.5 0.5 0 0 0 9.1384603 12.13466 L 11.134721 12.13466 L 11.134721 14.13092 A 0.5 0.5 0 0 0 11.634949 14.630632 A 0.5 0.5 0 0 0 12.13466 14.13092 L 12.13466 12.13466 L 14.128853 12.13466 A 0.5 0.5 0 0 0 14.629081 11.634949 A 0.5 0.5 0 0 0 14.128853 11.134721 L 12.13466 11.134721 L 12.13466 9.1405273 A 0.5 0.5 0 0 0 11.634949 8.6408162 z "
            />
          </svg>
          <span class="hidden sm:block">{{
            useLanguageStore().language.messages.mywishlist__addGiftButton
          }}</span>
        </button>
        <button type="button" class="button-primary button-icon" @click="getPdf">
          <template v-if="pdfDownloadMode">
            <div role="status" aria-hidden="true">
              <svg
                aria-hidden="true"
                class="text-beige-200 fill-dark-beige-800 size-[32px] animate-spin"
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
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="currentColor"
              class="size-[32px]"
              viewBox="0 0 16 16"
            >
              <path
                fill="currentColor"
                fill-rule="evenodd"
                d="M14 4.5V14a2 2 0 0 1-2 2h-1v-1h1a1 1 0 0 0 1-1V4.5h-2A1.5 1.5 0 0 1 9.5 3V1H4a1 1 0 0 0-1 1v9H2V2a2 2 0 0 1 2-2h5.5zM1.6 11.85H0v3.999h.791v-1.342h.803q.43 0 .732-.173.305-.175.463-.474a1.4 1.4 0 0 0 .161-.677q0-.375-.158-.677a1.2 1.2 0 0 0-.46-.477q-.3-.18-.732-.179m.545 1.333a.8.8 0 0 1-.085.38.57.57 0 0 1-.238.241.8.8 0 0 1-.375.082H.788V12.48h.66q.327 0 .512.181.185.183.185.522m1.217-1.333v3.999h1.46q.602 0 .998-.237a1.45 1.45 0 0 0 .595-.689q.196-.45.196-1.084 0-.63-.196-1.075a1.43 1.43 0 0 0-.589-.68q-.396-.234-1.005-.234zm.791.645h.563q.371 0 .609.152a.9.9 0 0 1 .354.454q.118.302.118.753a2.3 2.3 0 0 1-.068.592 1.1 1.1 0 0 1-.196.422.8.8 0 0 1-.334.252 1.3 1.3 0 0 1-.483.082h-.563zm3.743 1.763v1.591h-.79V11.85h2.548v.653H7.896v1.117h1.606v.638z"
              />
            </svg>
          </template>
          <span class="hidden sm:block">{{
            useLanguageStore().language.messages.mywishlist__downloadPdfButton
          }}</span>
        </button>
      </div>
      <div class="mt-4">
        <template v-for="category in wishList.categories" :key="'c' + category.id">
          <h2
            class="mt-5 mb-2 flex flex-row items-center justify-between text-2xl sm:justify-start"
          >
            {{ category.name }}
          </h2>
          <hr />
          <div class="card-grid mt-4">
            <template v-for="gift in category.gifts" :key="'g' + gift.id">
              <div
                class="group card h-[280px]"
                :class="getCardClasses(gift)"
                @mouseenter="hoveredGift = gift.id"
                @mouseleave="hoveredGift = null"
              >
                <div class="flex w-full flex-row p-2">
                  <div class="flex flex-1/3 justify-start">
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      width="16"
                      height="16"
                      viewBox="0 0 16 16"
                      class="cursor-pointer"
                      :visibility="!gift.secret && !gift.heart ? 'collapse' : 'visible'"
                      @click.stop="
                        () => {
                          if (gift.reserved_by === null) {
                            router.push({
                              name: 'friendeditgift',
                              params: { name: friendName, id: gift.id },
                            });
                          } else if (gift.secret) {
                            displayModal(gift, category.id);
                          }
                        }
                      "
                    >
                      <template v-if="gift.secret && gift.reserved_by === null">
                        <path
                          fill="var(--color-dark-beige-800)"
                          fill-rule="evenodd"
                          d="M11.013 1.427a1.75 1.75 0 012.474 0l1.086 1.086a1.75 1.75 0 010 2.474l-8.61 8.61c-.21.21-.47.364-.756.445l-3.251.93a.75.75 0 01-.927-.928l.929-3.25a1.75 1.75 0 01.445-.758l8.61-8.61zm1.414 1.06a.25.25 0 00-.354 0L10.811 3.75l1.439 1.44 1.263-1.263a.25.25 0 000-.354l-1.086-1.086zM11.189 6.25L9.75 4.81l-6.286 6.287a.25.25 0 00-.064.108l-.558 1.953 1.953-.558a.249.249 0 00.108-.064l6.286-6.286z"
                        ></path>
                      </template>
                      <template v-else-if="gift.secret">
                        <path
                          fill="var(--color-dark-beige-800)"
                          data-v-db77f6ee=""
                          fill-rule="evenodd"
                          d="M6.5 1.75a.25.25 0 01.25-.25h2.5a.25.25 0 01.25.25V3h-3V1.75zm4.5 0V3h2.25a.75.75 0 010 1.5H2.75a.75.75 0 010-1.5H5V1.75C5 .784 5.784 0 6.75 0h2.5C10.216 0 11 .784 11 1.75zM4.496 6.675a.75.75 0 10-1.492.15l.66 6.6A1.75 1.75 0 005.405 15h5.19c.9 0 1.652-.681 1.741-1.576l.66-6.6a.75.75 0 00-1.492-.149l-.66 6.6a.25.25 0 01-.249.225h-5.19a.25.25 0 01-.249-.225l-.66-6.6z"
                        ></path>
                      </template>
                      <template v-else>
                        <path
                          fill="var(--color-red-heart)"
                          fill-rule="evenodd"
                          d="M4.25 2.5c-1.336 0-2.75 1.164-2.75 3 0 2.15 1.58 4.144 3.365 5.682A20.565 20.565 0 008 13.393a20.561 20.561 0 003.135-2.211C12.92 9.644 14.5 7.65 14.5 5.5c0-1.836-1.414-3-2.75-3-1.373 0-2.609.986-3.029 2.456a.75.75 0 01-1.442 0C6.859 3.486 5.623 2.5 4.25 2.5zM8 14.25l-.345.666-.002-.001-.006-.003-.018-.01a7.643 7.643 0 01-.31-.17 22.075 22.075 0 01-3.434-2.414C2.045 10.731 0 8.35 0 5.5 0 2.836 2.086 1 4.25 1 5.797 1 7.153 1.802 8 3.02 8.847 1.802 10.203 1 11.75 1 13.914 1 16 2.836 16 5.5c0 2.85-2.045 5.231-3.885 6.818a22.08 22.08 0 01-3.744 2.584l-.018.01-.006.003h-.002L8 14.25zm0 0l.345.666a.752.752 0 01-.69 0L8 14.25z"
                        ></path>
                      </template>
                    </svg>
                  </div>
                  <div class="flex flex-1/3 justify-center">
                    <div
                      :class="
                        gift.secret
                          ? gift.reserved_by
                            ? 'bg-zinc-200 group-hover:bg-zinc-300'
                            : 'bg-beige-300 group-hover:bg-beige-200'
                          : 'hidden'
                      "
                      class="text-red-secret-text -translate-y-4 rounded-4xl px-3 pt-2 font-semibold"
                    >
                      SECRET
                    </div>
                  </div>
                  <div class="flex flex-1/3 justify-end">
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      width="16"
                      height="16"
                      viewBox="0 0 16 16"
                      class="clickable"
                      :visibility="
                        gift.reserved_by !== null && gift.reserved_by !== useUserStore().user?.id
                          ? 'collapse'
                          : 'visible'
                      "
                      @click="reserve(gift.id, gift.reserved_by === null)"
                    >
                      <path
                        :fill="
                          gift.reserved_by !== null && gift.reserved_by === useUserStore().user?.id
                            ? 'var(--color-green-reserved)'
                            : 'var(--color-dark-beige-800)'
                        "
                        fill-rule="evenodd"
                        d="M4.75 1.5a1.25 1.25 0 100 2.5h2.309c-.233-.818-.542-1.401-.878-1.793-.43-.502-.915-.707-1.431-.707zM2 2.75c0 .45.108.875.3 1.25h-.55A1.75 1.75 0 000 5.75v2c0 .698.409 1.3 1 1.582v4.918c0 .966.784 1.75 1.75 1.75h10.5A1.75 1.75 0 0015 14.25V9.332c.591-.281 1-.884 1-1.582v-2A1.75 1.75 0 0014.25 4h-.55a2.75 2.75 0 00-2.45-4c-.984 0-1.874.42-2.57 1.23A5.086 5.086 0 008 2.274a5.086 5.086 0 00-.68-1.042C6.623.42 5.733 0 4.75 0A2.75 2.75 0 002 2.75zM8.941 4h2.309a1.25 1.25 0 100-2.5c-.516 0-1 .205-1.43.707-.337.392-.646.975-.879 1.793zm-1.84 1.5H1.75a.25.25 0 00-.25.25v2c0 .138.112.25.25.25h5.5V5.5h-.149zm1.649 0V8h5.5a.25.25 0 00.25-.25v-2a.25.25 0 00-.25-.25h-5.5zm0 4h4.75v4.75a.25.25 0 01-.25.25h-4.5v-5zm-1.5 0v5h-4.5a.25.25 0 01-.25-.25V9.5h4.75z"
                      ></path>
                    </svg>
                  </div>
                </div>
                <div
                  class="cursor-pointer"
                  @click="
                    router.push({ name: 'showgift', params: { name: friendName, id: gift.id } })
                  "
                >
                  <SquareImage
                    :image-name="gift.picture"
                    extra-classes="h-[160px] w-[140px] rounded-sm"
                    :alternate-image="blank_gift"
                    @image-loaded="
                      (url) => {
                        giftIdToImageUrl[gift.id] = url;
                      }
                    "
                  />
                </div>
                <div class="mt-2 ps-2 pe-2 text-center">
                  <template
                    v-if="
                      gift.description != null &&
                      gift.description.length > 0 &&
                      hoveredGift === gift.id
                    "
                  >
                    <p class="text-gift">{{ gift.description }}</p>
                  </template>
                  <template v-else>
                    <p>{{ gift.name }}</p>
                    <p>{{ gift.price }}</p>
                  </template>
                </div>
              </div>
            </template>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style lang="css" scoped>
.text-gift {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
