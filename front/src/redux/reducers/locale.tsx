import { createSlice, PayloadAction } from "@reduxjs/toolkit";

import { RootState } from "../store";

import En from "../../translation/en";
import Fr from "../../translation/fr";
import { Translations } from "../../translation/itrans";

interface LocaleState {
  messages: Translations;
}

const defaultState: LocaleState = {
  messages:
    localStorage.getItem("locale") === null ||
    localStorage.getItem("locale") === "English"
      ? new En().getTranslation()
      : new Fr().getTranslation(),
};

export enum LocaleAvailable {
  English = "English",
  Francais = "Français",
}

export const localeSlice = createSlice({
  name: "locale",
  initialState: defaultState,
  reducers: {
    changeLocale(state: LocaleState, action: PayloadAction<LocaleAvailable>) {
      localStorage.setItem("locale", action.payload);
      if (action.payload === LocaleAvailable.English) {
        state.messages = new En().getTranslation();
      } else if (action.payload === LocaleAvailable.Francais) {
        state.messages = new Fr().getTranslation();
      }
    },
  },
});

export const { changeLocale } = localeSlice.actions;
export const selectMessages = (state: RootState) => state.locale.messages;
