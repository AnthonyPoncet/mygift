import { CHANGE_LOCALE } from '../constants';
import { ChangeLocaleActions } from '../actions/locale';

import En from '../../translation/en';
import Fr from '../../translation/fr';
import { Translations } from '../../translation/itrans';

interface InternalState { messages: Translations }

const defaultState : InternalState = { messages: new En().getTranslation() };

export function changeLocale(state = defaultState, action: ChangeLocaleActions) {
  switch (action.type) {
    case CHANGE_LOCALE:
      switch(action.locale) {
          case 'English':
            return { messages: new En().getTranslation() };
            break;
          case 'French':
            return { messages: new Fr().getTranslation() };
            break;
      }
    default:
      return state
  }
}
