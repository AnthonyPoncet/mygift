<script setup lang="ts">
import { useUserStore } from "@/stores/user";
import SignInView from "./SignIn.vue";
import { make_authorized_request } from "@/components/helpers/make_request";
import { ref, watch } from "vue";
import blank_profile_picture from "@/assets/images/blank_profile_picture.png";
import tree from "@/assets/images/tree.jfif";
import SquareImage from "@/components/SquareImage.vue";
import { useLanguageStore } from "@/stores/language";
import { EventKind, type EventJson } from "@/components/helpers/common_json";
import { useRouter } from "vue-router";

const nextEvents = ref<EventJson[]>([]);
const router = useRouter();

watch(
  () => useUserStore().user,
  () => {
    if (useUserStore().user != null) {
      getEvents();
    }
  },
);

async function getEvents() {
  const response = await make_authorized_request(router, "/events");
  if (response !== null) {
    nextEvents.value = await response.json();
  }
}

if (useUserStore().user != null) {
  getEvents();
}

function getYears(eventDate: number, birthDate: number) {
  return new Date(eventDate).getFullYear() - new Date(birthDate).getFullYear();
}

function displayDate(eventDate: number): string {
  const date = new Date(eventDate * 1000);
  return (
    date.getDate() +
    " " +
    useLanguageStore().language.messages.home__months[date.getMonth()] +
    " " +
    date.getFullYear()
  );
}
</script>

<template>
  <template v-if="useUserStore().user === null">
    <SignInView />
  </template>
  <template v-else>
    <div class="main-container">
      <h1 class="mb-5 text-center text-2xl font-bold sm:text-left">
        {{ useLanguageStore().language.messages.home__next_events }}
      </h1>
      <div class="card-grid">
        <template
          v-for="next in nextEvents"
          :key="(next.kind === EventKind.BIRTHDAY ? next.name : 'other') + next.date"
        >
          <div
            class="card"
            :class="{ 'cursor-pointer': next.kind === EventKind.BIRTHDAY }"
            @click="
              () => {
                if (next.kind === EventKind.BIRTHDAY)
                  router.push({ name: 'friend', params: { name: next.name } });
              }
            "
          >
            <div class="p-2 text-center text-xl font-semibold">
              {{
                next.name === null
                  ? useLanguageStore().language.messages.home__special_event[next.kind]
                  : next.name
              }}
            </div>
            <SquareImage
              :image-name="next.picture"
              extra-classes="h-[150px] w-[150px] object-cover rounded-sm"
              :alternate-image="next.kind === EventKind.BIRTHDAY ? blank_profile_picture : tree"
            />
            <div class="flex flex-col p-2 text-center">
              <p class="text-center" :class="{ hidden: next.kind !== EventKind.BIRTHDAY }">
                {{
                  getYears(next.date * 1000, next.birth! * 1000) +
                  " " +
                  useLanguageStore().language.messages.home__years_old
                }}
              </p>
              <p class="text-center font-semibold">
                {{ displayDate(next.date) }}
              </p>
            </div>
          </div>
        </template>
      </div>
    </div>
  </template>
</template>
