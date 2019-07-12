import { ERROR, CLEAR_ERROR } from '../constants';

const defaultState = {};

export function error(state = defaultState, action) {
  switch (action.type) {
    case ERROR:
      return { message: action.message };
    case CLEAR_ERROR:
      return {};
    default:
      return state
  }
}
