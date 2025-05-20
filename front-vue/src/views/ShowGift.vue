<script setup lang="ts">
import { make_authorized_request } from "../components/helpers/make_request.ts";
import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import type { ShowingFriendGift } from "@/components/helpers/common_json.ts";
import { useLanguageStore } from "@/stores/language.ts";
import SquareImage from "@/components/SquareImage.vue";
import blank_gift from "@/assets/images/blank_gift.png";
import { useUserStore } from "@/stores/user.ts";

const route = useRoute();
const router = useRouter();

interface GiftDetail {
  name: string;
  id: number;
}
const giftDetail = ref<GiftDetail>(route.params as unknown as GiftDetail);
const friendId = ref(-1);

const gift = ref<ShowingFriendGift | null>(null);

async function getFriendGift() {
  const response = await make_authorized_request(router, `/friends/${giftDetail.value.name}`);
  if (response !== null) {
    const friend: { id: number } = await response.json();
    friendId.value = friend.id;

    const response2 = await make_authorized_request(
      router,
      `/wishlist/friend/${friendId.value}/gifts/${giftDetail.value.id}`,
    );
    if (response2 !== null) {
      gift.value = await response2.json();
    }
  }
}

function truncateWebsites(whereToBuy: string): string {
  const last = whereToBuy.split("/", 3).join("/").length;
  return whereToBuy.substring(0, last) + "/...";
}

getFriendGift();

const reserving = ref(false);
async function reserve() {
  reserving.value = true;
  const response = await make_authorized_request(
    router,
    `/wishlist/friend/${friendId.value}/gifts/${giftDetail.value.id}`,
    gift.value?.reserved_by === null ? "POST" : "DELETE",
  );
  if (response !== null) {
    await getFriendGift();
  }
  reserving.value = false;
}
</script>

<template>
  <div class="main-container flex w-full flex-col">
    <template v-if="gift !== null">
      <h1 class="mb-5 text-center text-3xl font-bold sm:text-4xl">
        {{ useLanguageStore().language.messages.showgift__gift_of }} {{ giftDetail.name }}
      </h1>
      <h2
        class="mb-5 w-full text-center text-2xl font-semibold sm:w-1/2 sm:self-center sm:text-left sm:text-3xl"
      >
        {{ useLanguageStore().language.messages.global__category }}: {{ gift.category_name }}
      </h2>
      <div class="flex w-full flex-col gap-5 sm:w-1/2 sm:flex-row sm:self-center">
        <div
          class="bg-beige-300 flex min-h-[400px] flex-1/2 justify-center rounded-2xl p-5 shadow-lg"
        >
          <SquareImage
            :image-name="gift.picture"
            extra-classes="rounded-sm"
            :alternate-image="blank_gift"
          />
        </div>
        <div
          class="bg-beige-300 flex flex-1/2 flex-col gap-2 rounded-2xl p-5 shadow-lg sm:justify-between"
        >
          <div>
            <p class="mb-2 text-center text-2xl font-bold sm:mb-5 sm:text-left sm:text-3xl">
              {{ gift.gift_name }}
            </p>
            <p class="text-center sm:text-left sm:text-xl">{{ gift.price }}</p>
          </div>
          <p
            v-if="gift.heart"
            class="text-red-heart flex flex-row items-center justify-center gap-2 font-semibold sm:justify-start sm:text-xl"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" class="w-[24px]">
              <path
                fill="var(--color-red-heart)"
                fill-rule="inherit"
                d="M4.25 2.5c-1.336 0-2.75 1.164-2.75 3 0 2.15 1.58 4.144 3.365 5.682A20.565 20.565 0 008 13.393a20.561 20.561 0 003.135-2.211C12.92 9.644 14.5 7.65 14.5 5.5c0-1.836-1.414-3-2.75-3-1.373 0-2.609.986-3.029 2.456a.75.75 0 01-1.442 0C6.859 3.486 5.623 2.5 4.25 2.5zM8 14.25l-.345.666-.002-.001-.006-.003-.018-.01a7.643 7.643 0 01-.31-.17 22.075 22.075 0 01-3.434-2.414C2.045 10.731 0 8.35 0 5.5 0 2.836 2.086 1 4.25 1 5.797 1 7.153 1.802 8 3.02 8.847 1.802 10.203 1 11.75 1 13.914 1 16 2.836 16 5.5c0 2.85-2.045 5.231-3.885 6.818a22.08 22.08 0 01-3.744 2.584l-.018.01-.006.003h-.002L8 14.25zm0 0l.345.666a.752.752 0 01-.69 0L8 14.25z"
              ></path>
            </svg>
            {{ useLanguageStore().language.messages.showgift__favorite }} {{ giftDetail.name }} !
          </p>
          <p
            v-if="gift.secret"
            class="text-red-secret flex flex-row items-center justify-center gap-2 font-semibold sm:justify-start sm:text-xl"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="w-[24px]" viewBox="0 0 16 16">
              <path
                fill="currentColor"
                fill-rule="evenodd"
                d="m4.736 1.968-.892 3.269-.014.058C2.113 5.568 1 6.006 1 6.5 1 7.328 4.134 8 8 8s7-.672 7-1.5c0-.494-1.113-.932-2.83-1.205l-.014-.058-.892-3.27c-.146-.533-.698-.849-1.239-.734C9.411 1.363 8.62 1.5 8 1.5s-1.411-.136-2.025-.267c-.541-.115-1.093.2-1.239.735m.015 3.867a.25.25 0 0 1 .274-.224c.9.092 1.91.143 2.975.143a30 30 0 0 0 2.975-.143.25.25 0 0 1 .05.498c-.918.093-1.944.145-3.025.145s-2.107-.052-3.025-.145a.25.25 0 0 1-.224-.274M3.5 10h2a.5.5 0 0 1 .5.5v1a1.5 1.5 0 0 1-3 0v-1a.5.5 0 0 1 .5-.5m-1.5.5q.001-.264.085-.5H2a.5.5 0 0 1 0-1h3.5a1.5 1.5 0 0 1 1.488 1.312 3.5 3.5 0 0 1 2.024 0A1.5 1.5 0 0 1 10.5 9H14a.5.5 0 0 1 0 1h-.085q.084.236.085.5v1a2.5 2.5 0 0 1-5 0v-.14l-.21-.07a2.5 2.5 0 0 0-1.58 0l-.21.07v.14a2.5 2.5 0 0 1-5 0zm8.5-.5h2a.5.5 0 0 1 .5.5v1a1.5 1.5 0 0 1-3 0v-1a.5.5 0 0 1 .5-.5"
              />
            </svg>
            {{ useLanguageStore().language.messages.showgift__secret }} {{ giftDetail.name }} !
          </p>
          <p class="description sm:text-xl">{{ gift.description }}</p>
          <a
            v-if="
              gift.where_to_buy !== null &&
              gift.where_to_buy !== '' &&
              gift.where_to_buy.startsWith('http')
            "
            :href="gift.where_to_buy"
            target="_blank"
            rel="noopener noreferrer"
            class="text-blue-800 underline hover:text-blue-500 sm:text-xl"
            >{{ truncateWebsites(gift.where_to_buy) }}</a
          >
          <p v-else class="sm:text-xl">{{ gift.where_to_buy }}</p>
        </div>
      </div>

      <div
        class="bg-beige-300 border-beige-300 mt-4 flex w-full min-w-sm flex-col gap-5 rounded-4xl border p-3 text-center shadow-md md:w-1/2 md:self-center"
      >
        <button
          type="button"
          class="button-primary form-button"
          @click="reserve"
          :disabled="
            reserving ||
            (gift.reserved_by !== null && gift.reserved_by !== useUserStore().user?.name)
          "
        >
          <template v-if="reserving">
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
          <template v-else-if="gift.reserved_by === null">
            {{ useLanguageStore().language.messages.global__reserve }}
          </template>
          <template v-else-if="gift.reserved_by === useUserStore().user?.name">
            {{ useLanguageStore().language.messages.showgift__cancel_reserve }}
          </template>
          <template v-else>
            {{ useLanguageStore().language.messages.showgift__already_reserved
            }}{{ gift.reserved_by }}
          </template>
        </button>
      </div>
    </template>
  </div>
</template>
