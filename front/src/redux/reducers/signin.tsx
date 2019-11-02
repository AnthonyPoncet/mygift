import { SIGNIN, LOGOUT, CHANGE_PROFILE } from '../constants'
import { UserAction } from '../actions/user';

interface InternalState {
  userId: number | null,
  username: String | null,
  picture: string | null
}

const defaultState : InternalState = {
  userId: Number(localStorage.getItem("userId")),
  username: localStorage.getItem('username'),
  picture: localStorage.getItem('picture')
};

export function signin(state = defaultState, action: UserAction) : InternalState {
  switch (action.type) {
    case SIGNIN:
      return { userId: action.payload.userId, username: action.payload.username, picture: action.payload.picture };
    case LOGOUT:
      return { userId: null, username: null, picture: null };
    case CHANGE_PROFILE:
      return { userId: state.userId, username: action.payload.username, picture: action.payload.picture };
    default:
      return state
  }
}
