<script setup lang="ts">
import { ref, watchEffect } from "vue";
import { make_authorized_request } from "./helpers/make_request";

const props = defineProps<{
  imageName: string | null;
  size: number;
  alternateImage: string;
  withTopRound: boolean;
}>();

const emit = defineEmits(["image-loaded"]);

const source = ref(props.alternateImage);

watchEffect(() => {
  if (props.imageName !== null && props.imageName !== "") {
    const fetchImage = async () => {
      const response = await make_authorized_request(`/files/${props.imageName}`);
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
  <img
    :height="props.size"
    :width="props.size"
    :src="source"
    alt="Nothing"
    :class="withTopRound ? 'top-round' : ''"
  />
</template>

<style lang="css" scoped>
img {
  object-fit: scale-down;
}

.top-round {
  border-radius: 0.375rem 0.375rem 0 0;
}
</style>
