import { ref, type Ref } from "vue";
import { defineStore } from "pinia";
import { en, fr, type Messages } from "@/components/helpers/localized_text";

export enum Languages {
  Francais = "Français",
  English = "English",
}

interface State {
  language: Languages;
  messages: Messages;
}

const STORE_NAME = "language";

export const useLanguageStore = defineStore(STORE_NAME, () => {
  const language: Ref<State> = initLanguage();

  function initLanguage(): Ref<State> {
    if (localStorage.getItem(STORE_NAME)) {
      const current = localStorage.getItem(STORE_NAME) as Languages;
      return ref({ language: current, messages: getLanguage(current) });
    } else {
      const current = Languages.Francais;
      localStorage.setItem(STORE_NAME, current);
      return ref({ language: current, messages: getLanguage(current) });
    }
  }

  function getLanguage(language: Languages): Messages {
    if (language === Languages.Francais) {
      return fr;
    } else {
      return en;
    }
  }

  function updateLanguage(newLanguage: Languages) {
    language.value.language = newLanguage;
    language.value.messages = getLanguage(newLanguage);
    localStorage.setItem(STORE_NAME, language.value.language);
  }

  return { language, updateLanguage };
});
