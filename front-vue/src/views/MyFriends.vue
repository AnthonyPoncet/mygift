<script setup lang="ts">
import type { Friend, Friends, PendingFriendRequests } from "@/components/helpers/common_json";
import { make_authorized_request } from "@/components/helpers/make_request";
import { useLanguageStore } from "@/stores/language";
import { ref, watch, type Ref } from "vue";
import blank_profile_picture from "@/assets/images/blank_profile_picture.png";
import SquareImage from "@/components/SquareImage.vue";
import FriendModal from "@/components/FriendModal.vue";
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";

const router = useRouter();

const friends: Ref<Friend[]> = ref([]);
const pendingfriendRequests: Ref<PendingFriendRequests> = ref({ sent: [], received: [] });

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
    pendingfriendRequests.value = await response.json();
  }
}

async function acceptFriendRequests(requestId: number) {
  const response = await make_authorized_request(router, `/friends/requests/${requestId}/accept`);
  if (response !== null) {
    getFriends();
    getFriendRequests();
  }
}
async function declineFriendRequests(requestId: number) {
  const response = await make_authorized_request(router, `/friends/requests/${requestId}/decline`);
  if (response !== null) {
    getFriendRequests();
  }
}

async function cancelFriendRequests(requestId: number) {
  const response = await make_authorized_request(router, `/friend/requests/${requestId}`, "DELETE");
  if (response !== null) {
    getFriendRequests();
  }
}

function openFriendList(friend: Friend) {
  router.push({ name: "friend", params: { name: friend.name } });
}

getFriends();
getFriendRequests();

watch(
  () => useUserStore().user,
  () => {
    getFriends();
    getFriendRequests();
  },
);
</script>

<template>
  <div class="container-fluid mt-3">
    <button
      type="button"
      class="btn btn-outline-dark"
      data-bs-toggle="modal"
      data-bs-target="#friendModal"
    >
      {{ useLanguageStore().language.messages.myfriends__add_friend }}
    </button>

    <h2 class="mt-2">{{ useLanguageStore().language.messages.myfriends__friend_list }}</h2>
    <div class="d-flex flex-row flex-wrap gap-4">
      <template v-for="friend in friends" :key="friend.name">
        <div class="card clickable" @click="openFriendList(friend)">
          <SquareImage
            :image-name="friend.picture === undefined ? null : friend.picture"
            :size="150"
            :alternate-image="blank_profile_picture"
            :withTopRound="true"
          />
          <div class="card-body text-center">
            {{ friend.name }}
          </div>
        </div>
      </template>
    </div>

    <template v-if="pendingfriendRequests.received.length > 0">
      <h2 class="mt-2">{{ useLanguageStore().language.messages.myfriends__friend_requests }}</h2>
      <div class="d-flex flex-row flex-wrap gap-4">
        <template v-for="friend in pendingfriendRequests.received" :key="friend.other_user.name">
          <div class="card">
            <SquareImage
              :image-name="
                friend.other_user.picture === undefined ? null : friend.other_user.picture
              "
              :size="150"
              :alternate-image="blank_profile_picture"
              :withTopRound="true"
            />
            <div class="card-body text-center">
              <p class="card-text">{{ friend.other_user.name }}</p>
              <div>
                <button
                  type="button"
                  class="btn btn-primary w-100"
                  @click="acceptFriendRequests(friend.id)"
                >
                  {{ useLanguageStore().language.messages.global__accept }}
                </button>
              </div>
              <div class="mt-1">
                <button
                  type="button"
                  class="btn btn-secondary w-100"
                  @click="declineFriendRequests(friend.id)"
                >
                  {{ useLanguageStore().language.messages.global__delete }}
                </button>
              </div>
            </div>
          </div>
        </template>
      </div>
    </template>

    <template v-if="pendingfriendRequests.sent.length > 0">
      <h2 class="mt-2">{{ useLanguageStore().language.messages.myfriends__my_friend_requests }}</h2>
      <div class="d-flex flex-row flex-wrap gap-4">
        <template v-for="friend in pendingfriendRequests.sent" :key="friend.other_user.name">
          <div class="card">
            <SquareImage
              :image-name="
                friend.other_user.picture === undefined ? null : friend.other_user.picture
              "
              :size="150"
              :alternate-image="blank_profile_picture"
              :withTopRound="true"
            />
            <div class="card-body text-center">
              <p class="card-text">{{ friend.other_user.name }}</p>
              <button
                type="button"
                class="btn btn-secondary w-100"
                @click="cancelFriendRequests(friend.id)"
              >
                {{ useLanguageStore().language.messages.global__cancel }}
              </button>
            </div>
          </div>
        </template>
      </div>
    </template>
  </div>

  <FriendModal @refresh-friends="getFriendRequests" />
</template>

<style lang="css" scoped>
.clickable {
  cursor: pointer;
}
</style>
