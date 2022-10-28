import { createSlice, PayloadAction } from "@reduxjs/toolkit";

import { RootState } from "../store";

interface ErrorState {
  message: String | null;
}

const defaultState: ErrorState = {
  message: null,
};

export const errorSlice = createSlice({
  name: "error",
  initialState: defaultState,
  reducers: {
    addMessage(state: ErrorState, action: PayloadAction<string>) {
      state.message = action.payload;
    },
    clearMessage(state: ErrorState) {
      state.message = null;
    },
  },
});

export const { addMessage, clearMessage } = errorSlice.actions;
export const selectErrorMessage = (state: RootState) => state.error.message;
