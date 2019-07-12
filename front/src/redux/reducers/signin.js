import { SIGNIN, LOGOUT } from '../constants'

const defaultState = {};

export function signin(state = defaultState, action) {
  switch (action.type) {
    case SIGNIN:
      return action.payload;
    case LOGOUT:
      return {};
    default:
      return state
  }
}
