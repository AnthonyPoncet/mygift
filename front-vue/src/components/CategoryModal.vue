<script lang="ts">
export enum CategoryModalAction {
  Add = "add",
  Edit = "edit",
}
</script>

<script setup lang="ts">
import { onMounted, ref, useTemplateRef, watch, type Ref } from "vue";
import { make_authorized_request } from "./helpers/make_request";
import { Modal } from "bootstrap";
import { useLanguageStore } from "@/stores/language";
import type { Category, Friend, FriendRequest } from "./helpers/common_json";

const props = defineProps<{
  action: CategoryModalAction;
  category: Category | null;
}>();

const modal = useTemplateRef("categoryModal");
const form = useTemplateRef("categoryModalForm");
const bootstrapModal = ref();
const emit = defineEmits(["refresh-wishlist"]);

const title: Ref<string> = ref(
  useLanguageStore().language.messages.category_modal__addCategoryTitle,
);
const buttonText: Ref<string> = ref(useLanguageStore().language.messages.global__add);

const actionRef: Ref<CategoryModalAction> = ref(CategoryModalAction.Add);
const categoryRef: Ref<Category> = ref({ id: 0, name: "", share: [] });

const friends: Ref<Friend[]> = ref([]);

watch(props, () => {
  if (props.category === null) {
    categoryRef.value = { id: 0, name: "", share: [] };
  } else {
    categoryRef.value = props.category;
  }
  actionRef.value = props.action;
  if (actionRef.value === CategoryModalAction.Add) {
    title.value = useLanguageStore().language.messages.category_modal__addCategoryTitle;
    buttonText.value = useLanguageStore().language.messages.global__add;
  } else {
    title.value = useLanguageStore().language.messages.category_modal__updateCategoryTitle;
    buttonText.value = useLanguageStore().language.messages.global__update;
  }
});

async function getFriends() {
  const response = await make_authorized_request("/friends");
  if (response !== null) {
    const firendRequests: FriendRequest[] = await response.json();

    friends.value = firendRequests.map((fr) => fr.otherUser);
  }
}

getFriends();

async function clickButton(event: Event) {
  event.preventDefault();

  if (form.value === null) {
    return;
  }

  if (!form.value.checkValidity()) {
    form.value.classList.add("was-validated");
    return;
  }

  let endpoint,
    method = "";
  if (actionRef.value === CategoryModalAction.Add) {
    endpoint = "/categories";
    method = "PUT";
  } else {
    endpoint = `/categories/${categoryRef.value.id}`;
    method = "PATCH";
  }

  const response = await make_authorized_request(
    endpoint,
    method,
    JSON.stringify({
      name: categoryRef.value.name,
      share: categoryRef.value.share,
    }),
  );
  if (response !== null) {
    bootstrapModal.value.hide();
    emit("refresh-wishlist");
    form.value.classList.remove("was-validated");
    categoryRef.value = { id: 0, name: "", share: [] };
    form.value!.reset();
  }
}

onMounted(() => {
  if (modal.value != null) {
    bootstrapModal.value = new Modal(modal.value);
  }
});
</script>

<template>
  <div
    class="modal fade"
    id="categoryModal"
    ref="categoryModal"
    tabindex="-1"
    aria-labelledby="categoryModalLabel"
    aria-hidden="true"
  >
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title" id="categoryModalLabel">{{ title }}</h5>
          <button
            type="button"
            class="btn-close"
            data-bs-dismiss="modal"
            aria-label="Close"
          ></button>
        </div>
        <div class="modal-body">
          <form ref="categoryModalForm">
            <div class="mb-3">
              <label for="name" class="form-label">{{
                useLanguageStore().language.messages.global__name
              }}</label>
              <input
                type="text"
                class="form-control"
                id="name"
                :placeholder="useLanguageStore().language.messages.global__name"
                v-model="categoryRef.name"
                required
              />
              <div class="invalid-feedback">
                {{
                  useLanguageStore().language.messages.global__form_validation_start +
                  useLanguageStore().language.messages.global__name.toLowerCase()
                }}
              </div>
            </div>
            <div class="mb-3">
              <p class="form-label">
                {{ useLanguageStore().language.messages.global__share_with }}
              </p>
              <template v-for="friend in friends" :key="friend">
                <div class="form-check">
                  <input
                    class="form-check-input"
                    type="checkbox"
                    :id="friend.name"
                    :value="friend.name"
                    v-model="categoryRef.share"
                  />
                  <label class="form-check-label" :for="friend.name">{{ friend.name }}</label>
                </div>
              </template>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button type="submit" class="btn btn-primary w-100" @click="clickButton">
            {{ buttonText }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
