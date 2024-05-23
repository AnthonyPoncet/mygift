import { centerCrop, makeAspectCrop } from "react-image-crop";

export const TO_RADIANS = Math.PI / 180;
export const SCALE = 1;

export function centerAspectCrop(mediaWidth: number, mediaHeight: number) {
  return centerCrop(
    makeAspectCrop(
      {
        unit: "%",
        width: 100,
      },
      mediaWidth / mediaHeight,
      mediaWidth,
      mediaHeight,
    ),
    mediaWidth,
    mediaHeight,
  );
}
