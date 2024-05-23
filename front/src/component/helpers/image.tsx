import { centerCrop, makeAspectCrop } from "react-image-crop";

export const TO_RADIANS = Math.PI / 180;
export const SCALE = 1;
export const ASPECT = 1;

export function centerAspectCrop(
  mediaWidth: number,
  mediaHeight: number,
  aspect: number,
) {
  return centerCrop(
    makeAspectCrop(
      {
        unit: "%",
        width: 100,
      },
      aspect,
      mediaWidth,
      mediaHeight,
    ),
    mediaWidth,
    mediaHeight,
  );
}
