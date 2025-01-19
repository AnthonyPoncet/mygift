<script setup lang="ts">
import { onMounted, ref, useTemplateRef, type Ref } from "vue";
import { make_authorized_request } from "./helpers/make_request";
import { Modal } from "bootstrap";
import { useLanguageStore } from "@/stores/language";

const modal = useTemplateRef("friendModal");
const form = useTemplateRef("friendModalForm");
const bootstrapModal = ref();
const emit = defineEmits(["refresh-friends"]);

const username: Ref<string> = ref("");

async function addFriend(event: Event) {
  event.preventDefault();

  if (!form.value!.checkValidity()) {
    form.value!.classList.add("was-validated");
    return;
  }

  const response = await make_authorized_request(
    "/friend-requests",
    "PUT",
    JSON.stringify({ name: username.value }),
  );
  if (response !== null) {
    bootstrapModal.value.hide();
    emit("refresh-friends");
    form.value!.classList.remove("was-validated");
    form.value!.reset();
  }
}

onMounted(() => {
  bootstrapModal.value = new Modal(modal.value!);
});
</script>

<template>
  <div
    class="modal fade"
    id="friendModal"
    ref="friendModal"
    tabindex="-1"
    aria-labelledby="friendModalLabel"
    aria-hidden="true"
  >
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title" id="friendModalLabel">
            {{ useLanguageStore().language.messages.friend_modal__title }}
          </h5>
          <button
            type="button"
            class="btn-close"
            data-bs-dismiss="modal"
            aria-label="Close"
          ></button>
        </div>
        <div class="modal-body">
          <form ref="friendModalForm">
            <div class="mb-3">
              <label for="username" class="form-label">{{
                useLanguageStore().language.messages.global__username
              }}</label>
              <input
                type="text"
                class="form-control"
                id="username"
                :placeholder="useLanguageStore().language.messages.global__username"
                v-model="username"
                required
              />
              <div class="invalid-feedback">
                {{
                  useLanguageStore().language.messages.global__form_validation_start +
                  useLanguageStore().language.messages.global__username.toLowerCase()
                }}
              </div>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button type="submit" class="btn btn-primary w-100" @click="addFriend">
            {{ useLanguageStore().language.messages.global__add }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
