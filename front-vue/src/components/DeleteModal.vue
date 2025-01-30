<script lang="ts">
export enum DeleteModalAction {
  Category = "category",
  Gift = "gift",
}
</script>

<script setup lang="ts">
import { onMounted, ref, useTemplateRef, type Ref, watch } from "vue";
import { make_authorized_request } from "./helpers/make_request";
import { Modal } from "bootstrap";
import { useLanguageStore } from "@/stores/language";
import type { Category, Gift } from "./helpers/common_json";

const props = defineProps<{
  action: DeleteModalAction;
  category: Category | null;
  gift: Gift | null;
}>();

const modal = useTemplateRef("deleteModal");
const bootstrapModal = ref();
const emit = defineEmits(["refresh-wishlist"]);

const actionRef: Ref<DeleteModalAction> = ref(DeleteModalAction.Category);
const categoryRef: Ref<Category | null> = ref(null);
const giftRef: Ref<Gift | null> = ref(null);
const title: Ref<string> = ref("");

watch(props, () => {
  actionRef.value = props.action;
  categoryRef.value = props.category;
  giftRef.value = props.gift;
  title.value = useLanguageStore().language.messages.delete_modal__pre_text;
  if (actionRef.value === DeleteModalAction.Category) {
    title.value += useLanguageStore().language.messages.delete_modal__pre_text_category;
    title.value += "'" + props.category?.name + "' ?";
  } else if (actionRef.value === DeleteModalAction.Gift) {
    title.value += useLanguageStore().language.messages.delete_modal__pre_text_gift;
    title.value += "'" + props.gift?.name + "' ?";
  }
});

onMounted(() => {
  if (modal.value != null) {
    bootstrapModal.value = new Modal(modal.value);
  }
});

async function deleteCategory() {
  if (categoryRef.value !== null) {
    const response = await make_authorized_request(
      `/wishlist/categories/${categoryRef.value.id}`,
      "DELETE",
    );
    if (response !== null) {
      bootstrapModal.value.hide();
      emit("refresh-wishlist");
    }
  }
}

async function deleteGift() {
  if (giftRef.value !== null && categoryRef.value !== null) {
    const response = await make_authorized_request(
      `/wishlist/categories/${categoryRef.value.id}/gifts/${giftRef.value.id}`,
      "DELETE",
    );
    if (response !== null) {
      bootstrapModal.value.hide();
      emit("refresh-wishlist");
    }
  }
}
</script>

<template>
  <div
    class="modal fade"
    id="deleteModal"
    ref="deleteModal"
    tabindex="-1"
    aria-labelledby="deleteModalLabel"
    aria-hidden="true"
  >
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title" id="deleteModalLabel">{{ title }}</h5>
          <button
            type="button"
            class="btn-close"
            data-bs-dismiss="modal"
            aria-label="Close"
          ></button>
        </div>
        <div v-if="actionRef === DeleteModalAction.Category" class="modal-body">
          <div class="alert alert-warning d-flex align-items-center" role="alert">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="32"
              height="32"
              fill="currentColor"
              class="bi bi-exclamation-triangle-fill flex-shrink-0 me-3"
              viewBox="0 0 16 16"
            >
              <path
                d="M8.982 1.566a1.13 1.13 0 0 0-1.96 0L.165 13.233c-.457.778.091 1.767.98 1.767h13.713c.889 0 1.438-.99.98-1.767zM8 5c.535 0 .954.462.9.995l-.35 3.507a.552.552 0 0 1-1.1 0L7.1 5.995A.905.905 0 0 1 8 5m.002 6a1 1 0 1 1 0 2 1 1 0 0 1 0-2"
              />
            </svg>
            {{
              categoryRef?.share_with.length === 0
                ? useLanguageStore().language.messages.delete_modal__category_hint
                : useLanguageStore().language.messages.delete_modal__category_shared_hint
            }}
          </div>
        </div>
        <div class="modal-footer">
          <template v-if="action === DeleteModalAction.Category">
            <button type="submit" class="btn btn-primary w-100" @click="deleteCategory">
              {{ useLanguageStore().language.messages.global__delete }}
            </button>
          </template>
          <template v-else-if="action === DeleteModalAction.Gift">
            <button type="submit" class="btn btn-primary w-100" @click="deleteGift()">
              {{ useLanguageStore().language.messages.global__delete }}
            </button>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>
