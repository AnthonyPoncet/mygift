<script setup lang="ts">
import type { Friend, Friends, PendingFriendRequests } from "@/components/helpers/common_json";
import { make_authorized_request } from "@/components/helpers/make_request";
import { useLanguageStore } from "@/stores/language";
import { ref, useTemplateRef } from "vue";
import blank_profile_picture from "@/assets/images/blank_profile_picture.png";
import SquareImage from "@/components/SquareImage.vue";
import { useRouter } from "vue-router";

const router = useRouter();

const friends = ref<Friend[]>([]);
const pendingFriendRequests = ref<PendingFriendRequests>({ sent: [], received: [] });

async function getFriends() {
  const response = await make_authorized_request(router, "/friends");
  if (response !== null) {
    const friendsJson: Friends = await response.json();
    friends.value = friendsJson.friends;
  }
}

async function getFriendRequests() {
  const response = await make_authorized_request(router, "/friends/requests");
  if (response !== null) {
    pendingFriendRequests.value = await response.json();
  }
}

async function acceptFriendRequests(requestId: number) {
  const response = await make_authorized_request(router, `/friends/requests/${requestId}/accept`);
  if (response !== null) {
    await getFriends();
    await getFriendRequests();
  }
}
async function declineFriendRequests(requestId: number) {
  const response = await make_authorized_request(router, `/friends/requests/${requestId}/decline`);
  if (response !== null) {
    await getFriendRequests();
  }
}

async function cancelFriendRequests(requestId: number) {
  const response = await make_authorized_request(router, `/friend/requests/${requestId}`, "DELETE");
  if (response !== null) {
    await getFriendRequests();
  }
}

function openFriendList(friend: Friend) {
  router.push({ name: "friend", params: { name: friend.name } });
}

getFriends();
getFriendRequests();

const form = useTemplateRef<HTMLFormElement | null>("friendModalForm");
const name = ref<string | null>(null);
const adding = ref(false);
const addFriendError = ref<string | null>(null);
async function addFriend(event: Event) {
  event.preventDefault();
  adding.value = true;

  if (!form.value!.checkValidity()) {
    if (name.value === null) {
      name.value = "";
    }
    adding.value = false;
    return;
  }

  const response = await make_authorized_request(
    router,
    "/friends",
    "POST",
    JSON.stringify({ name: name.value }),
  );
  if (response !== null) {
    await getFriends();
    await getFriendRequests();
    name.value = null;
  } else {
    addFriendError.value =
      name.value + useLanguageStore().language.messages.myfriends__unknown_pseudo;
  }
  adding.value = false;
}
</script>

<template>
  <div class="main-container flex flex-col gap-10">
    <div v-if="pendingFriendRequests.received.length > 0" class="flex flex-col gap-5">
      <h1 class="text-center text-2xl font-bold sm:text-left">
        {{ useLanguageStore().language.messages.myfriends__friend_requests }}
      </h1>
      <div class="card-grid">
        <div v-for="friend in pendingFriendRequests.received" :key="friend.other_user.name">
          <div class="card pt-3">
            <SquareImage
              :image-name="
                friend.other_user.picture === undefined ? null : friend.other_user.picture
              "
              extra-classes="h-[150px] w-[150px] object-cover rounded-sm"
              :alternate-image="blank_profile_picture"
            />
            <div class="flex flex-col gap-2 p-2 text-center">
              <p>{{ friend.other_user.name }}</p>
              <button
                type="button"
                class="button-primary w-full"
                @click="acceptFriendRequests(friend.id)"
              >
                {{ useLanguageStore().language.messages.global__accept }}
              </button>
              <button
                type="button"
                class="button-primary w-full"
                @click="declineFriendRequests(friend.id)"
              >
                {{ useLanguageStore().language.messages.global__delete }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="flex flex-col gap-5">
      <h1 class="text-center text-2xl font-bold sm:text-left">
        {{ useLanguageStore().language.messages.myfriends__friend_list }}
      </h1>
      <div class="card-grid">
        <template v-for="friend in friends" :key="friend.name">
          <div class="card cursor-pointer pt-3" @click="openFriendList(friend)">
            <SquareImage
              :image-name="friend.picture === undefined ? null : friend.picture"
              extra-classes="h-[150px] w-[150px] object-cover rounded-sm"
              :alternate-image="blank_profile_picture"
            />
            <div class="p-2 text-center font-bold">
              {{ friend.name }}
            </div>
          </div>
        </template>
      </div>
    </div>

    <div v-if="pendingFriendRequests.sent.length > 0" class="flex flex-col gap-5">
      <h1 class="text-center text-2xl font-bold sm:text-left">
        {{ useLanguageStore().language.messages.myfriends__my_friend_requests }}
      </h1>
      <div class="card-grid">
        <template v-for="friend in pendingFriendRequests.sent" :key="friend.other_user.name">
          <div class="card pt-3">
            <SquareImage
              :image-name="
                friend.other_user.picture === undefined ? null : friend.other_user.picture
              "
              extra-classes="h-[150px] w-[150px] object-cover rounded-sm"
              :alternate-image="blank_profile_picture"
            />
            <div class="flex flex-col gap-2 p-2 text-center">
              <p>{{ friend.other_user.name }}</p>
              <button
                type="button"
                class="button-primary w-full"
                @click="cancelFriendRequests(friend.id)"
              >
                {{ useLanguageStore().language.messages.global__cancel }}
              </button>
            </div>
          </div>
        </template>
      </div>
    </div>

    <div class="flex flex-col gap-5">
      <h1 class="text-center text-2xl font-bold sm:text-left">
        {{ useLanguageStore().language.messages.myfriends__add_friend }}
      </h1>
      <form class="flex flex-col gap-5 sm:w-1/4" ref="friendModalForm" novalidate>
        <div
          v-if="addFriendError !== null"
          class="text-red-secret-text w-full text-center"
          role="alert"
        >
          {{ addFriendError }}
        </div>
        <div class="flex flex-row gap-5">
          <div class="group relative z-0 mb-5 flex-1/2">
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
            <label
              for="name"
              class="input-label"
              :class="{ 'input-label-invalid': name !== null }"
              >{{ useLanguageStore().language.messages.global__username }}</label
            >
            <div class="input-error" :class="{ 'peer-invalid:block': name !== null }">
              {{
                useLanguageStore().language.messages.global__form_validation_start +
                useLanguageStore().language.messages.global__username.toLowerCase()
              }}
            </div>
          </div>

          <button type="submit" class="button-primary button-icon" @click="addFriend">
            <svg xmlns="http://www.w3.org/2000/svg" class="size-[32px]" viewBox="0 0 16 16">
              <path
                fill="currentColor"
                d="M6 8a3 3 0 1 0 0-6 3 3 0 0 0 0 6m2-3a2 2 0 1 1-4 0 2 2 0 0 1 4 0m4 8c0 1-1 1-1 1H1s-1 0-1-1 1-4 6-4 6 3 6 4m-1-.004c-.001-.246-.154-.986-.832-1.664C9.516 10.68 8.289 10 6 10s-3.516.68-4.168 1.332c-.678.678-.83 1.418-.832 1.664z"
              />
              <path
                fill="currentColor"
                fill-rule="evenodd"
                d="M13.5 5a.5.5 0 0 1 .5.5V7h1.5a.5.5 0 0 1 0 1H14v1.5a.5.5 0 0 1-1 0V8h-1.5a.5.5 0 0 1 0-1H13V5.5a.5.5 0 0 1 .5-.5"
              />
            </svg>
            <span class="hidden sm:block">{{
              useLanguageStore().language.messages.myfriends__add_friend
            }}</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
