<script setup lang="ts">
import type { Friend, FriendRequest, PendingFriendRequest } from "@/components/helpers/common_json";
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
const pendingfriendRequests: Ref<PendingFriendRequest> = ref({ sent: [], received: [] });

async function getFriends() {
  const response = await make_authorized_request("/friends");
  if (response !== null) {
    const firendRequests: FriendRequest[] = await response.json();

    friends.value = firendRequests.map((fr) => fr.otherUser);
  }
}

async function getFriendRequests() {
  const response = await make_authorized_request("/friend-requests/pending");
  if (response !== null) {
    pendingfriendRequests.value = await response.json();
  }
}

async function acceptFriendRequests(userId: number) {
  const response = await make_authorized_request(`/friend-requests/${userId}/accept`);
  if (response !== null) {
    getFriends();
    getFriendRequests();
  }
}
async function declineFriendRequests(userId: number) {
  const response = await make_authorized_request(
    `/friend-requests/${userId}/decline?blockUser=true`,
    "post",
  );
  if (response !== null) {
    getFriendRequests();
  }
}
async function cancelFriendRequests(userId: number) {
  const response = await make_authorized_request(`/friend-requests/${userId}`, "DELETE");
  if (response !== null) {
    getFriendRequests();
  }
}

function openFriendList(username: string) {
  router.push({ name: "friend", params: { name: username } });
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
        <div class="card clickable" @click="openFriendList(friend.name)">
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
        <template v-for="friend in pendingfriendRequests.received" :key="friend.name">
          <div class="card">
            <SquareImage
              :image-name="friend.otherUser.picture === undefined ? null : friend.otherUser.picture"
              :size="150"
              :alternate-image="blank_profile_picture"
              :withTopRound="true"
            />
            <div class="card-body text-center">
              <p class="card-text">{{ friend.otherUser.name }}</p>
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
        <template v-for="friend in pendingfriendRequests.sent" :key="friend.name">
          <div class="card">
            <SquareImage
              :image-name="friend.otherUser.picture === undefined ? null : friend.otherUser.picture"
              :size="150"
              :alternate-image="blank_profile_picture"
              :withTopRound="true"
            />
            <div class="card-body text-center">
              <p class="card-text">{{ friend.otherUser.name }}</p>
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
