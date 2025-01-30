<script setup lang="ts">
import { useUserStore } from "@/stores/user";
import SignInView from "./SignIn.vue";
import { make_authorized_request } from "@/components/helpers/make_request";
import { ref, watch, type Ref } from "vue";
import blank_profile_picture from "@/assets/images/blank_profile_picture.png";
import SquareImage from "@/components/SquareImage.vue";
import { useLanguageStore } from "@/stores/language";
import { EventKind, type EventJson } from "@/components/helpers/common_json";
import { useRouter } from "vue-router";

const nextEvents: Ref<EventJson[]> = ref([]);
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
  const response = await make_authorized_request("/events");
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
</script>

<template>
  <main>
    <template v-if="useUserStore().user === null">
      <SignInView />
    </template>
    <template v-else>
      <div class="container-fluid mt-3">
        <div class="d-flex flex-row flex-wrap gap-4">
          <template
            v-for="next in nextEvents"
            :key="(next.kind === EventKind.BIRTHDAY ? next.name : 'other') + next.date"
          >
            <div
              class="card"
              :class="next.kind === EventKind.BIRTHDAY ? 'clickable' : ''"
              @click="
                () => {
                  if (next.kind === EventKind.BIRTHDAY)
                    router.push({ name: 'friend', params: { name: next.name } });
                }
              "
            >
              <div class="text-center fw-bold p-2">
                {{
                  next.name === undefined
                    ? useLanguageStore().language.messages.home__special_event[next.kind]
                    : next.name
                }}
              </div>
              <SquareImage
                :image-name="next.picture === undefined ? null : next.picture"
                :size="150"
                :alternate-image="blank_profile_picture"
                :withTopRound="false"
              />
              <div class="card-body text-center">
                <div
                  class="text-center fw-bold"
                  :class="next.kind === EventKind.BIRTHDAY ? '' : 'hidden'"
                >
                  {{
                    getYears(
                      next.date * 1000 + new Date().getTimezoneOffset() * 60000,
                      next.birth! * 1000 + new Date().getTimezoneOffset() * 60000,
                    ) +
                    " " +
                    useLanguageStore().language.messages.home__years_old
                  }}
                </div>
                <div class="text-center">
                  {{
                    new Date(
                      next.date * 1000 + new Date().getTimezoneOffset() * 60000,
                    ).toLocaleDateString()
                  }}
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>
    </template>
  </main>
</template>

<style lang="css" scoped>
.clickable {
  cursor: pointer;
}

.hidden {
  visibility: hidden;
}
</style>
