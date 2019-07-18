import { SIGNIN, LOGOUT } from '../constants'
import { UserAction } from '../actions/user';

interface InternalState {
  userId: number | null,
  username: String | null
}

const defaultState : InternalState = {
  userId: Number(localStorage.getItem("userId")),
  username: localStorage.getItem('username')
};

export function signin(state = defaultState, action: UserAction) : InternalState {
  switch (action.type) {
    case SIGNIN:
      return { userId: action.payload.userId, username: action.payload.username };
    case LOGOUT:
      return { userId: null, username: null };
    default:
      return state
  }
}
