<script setup lang="ts">
import { useLanguageStore } from "@/stores/language";
import type { Gift } from "./helpers/common_json";
import SquareImage from "./SquareImage.vue";
import { make_authorized_request } from "./helpers/make_request";
import { onMounted, ref, useTemplateRef } from "vue";
import { Modal } from "bootstrap";

const props = defineProps<{
  gift: Gift;
  giftUrl: string | null;
  reserved: boolean;
  friendId: number;
}>();

const modal = useTemplateRef("showGiftModal");
const bootstrapModal = ref();
const emit = defineEmits(["refresh-wishlist"]);

function truncateWebsites(whereToBuy: string): string {
  const last = whereToBuy.split("/", 3).join("/").length;
  return whereToBuy.substring(0, last) + "/...";
}

async function reserve() {
  const response = await make_authorized_request(
    `/wishlist/friend/${props.friendId}/gifts/${props.gift.id}`,
    "POST",
  );
  if (response !== null) {
    bootstrapModal.value.hide();
    emit("refresh-wishlist");
  }
}

onMounted(() => {
  bootstrapModal.value = new Modal(modal.value!);
});
</script>

<template>
  <div
    class="modal fade"
    id="showGiftModal"
    ref="showGiftModal"
    tabindex="-1"
    aria-labelledby="showGiftModalLabel"
    aria-hidden="true"
  >
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title" id="showGiftModalLabel">{{ props.gift?.name }}</h5>
          <button
            type="button"
            class="btn-close"
            data-bs-dismiss="modal"
            aria-label="Close"
          ></button>
        </div>
        <div class="modal-body d-flex flex-column align-items-center">
          <SquareImage
            :image-name="null"
            :size="300"
            :alternate-image="giftUrl!"
            :withTopRound="true"
          />
          <div class="mt-2">{{ props.gift?.description }}</div>
          <div>{{ props.gift?.price }}</div>
          <template v-if="props.gift?.where_to_buy && props.gift?.where_to_buy.startsWith('http')">
            <a :href="props.gift?.where_to_buy" target="_blank" rel="noopener noreferrer">{{
              truncateWebsites(props.gift?.where_to_buy)
            }}</a>
          </template>
          <template v-else>
            <div class="max-text text-truncate">{{ props.gift?.where_to_buy }}</div>
          </template>
        </div>
        <div v-if="props.reserved === false" class="modal-footer" @click="reserve">
          <button type="submit" class="btn btn-primary w-100">
            {{ useLanguageStore().language.messages.global__reserve }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="css">
.max-text {
  max-width: calc(var(--bs-modal-width) - 2 * var(--bs-modal-padding));
}
</style>
