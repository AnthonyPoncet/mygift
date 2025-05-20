<script setup lang="ts">
import { ref, watchEffect } from "vue";
import { make_authorized_request } from "./helpers/make_request";
import { useRouter } from "vue-router";

const props = defineProps<{
  imageName: string | null;
  extraClasses: string;
  alternateImage: string;
}>();

const router = useRouter();

const emit = defineEmits(["image-loaded"]);

const source = ref(props.alternateImage);

watchEffect(() => {
  if (props.imageName !== null && props.imageName !== "") {
    const fetchImage = async () => {
      const response = await make_authorized_request(router, `/files/${props.imageName}`);
      if (response !== null) {
        const blob = await response.blob();
        source.value = window.URL.createObjectURL(blob);
        emit("image-loaded", source.value);
      }
    };
    fetchImage();
  }

  source.value = props.alternateImage;
});
</script>

<template>
  <img :src="source" alt="Nothing" class="object-contain" :class="extraClasses" />
</template>
