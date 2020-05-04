import { SIGNIN, LOGOUT, CHANGE_PROFILE } from '../constants'
import { UserAction } from '../actions/user';

interface InternalState {
  token: string | null,
  username: String | null,
  picture: string | null
}

const defaultState : InternalState = {
  token: localStorage.getItem("token"),
  username: localStorage.getItem('username'),
  picture: localStorage.getItem('picture')
};

export function signin(state = defaultState, action: UserAction) : InternalState {
  switch (action.type) {
    case SIGNIN:
      return { token: action.payload.token, username: action.payload.username, picture: action.payload.picture };
    case LOGOUT:
      return { token: null, username: null, picture: null };
    case CHANGE_PROFILE:
      return { token: state.token, username: action.payload.username, picture: action.payload.picture };
    default:
      return state
  }
}
