import { ERROR, CLEAR_ERROR } from '../constants';
import { ErrorActions } from '../actions/error';

interface InternalState {
    message: String | null
}

const defaultState : InternalState = {
    message: null
};

export function error(state = defaultState, action: ErrorActions) {
  switch (action.type) {
    case ERROR:
      return { message: action.message };
    case CLEAR_ERROR:
      return { message: null };
    default:
      return state
  }
}
