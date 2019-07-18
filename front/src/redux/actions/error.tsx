import { ERROR, CLEAR_ERROR } from '../constants'

interface ErrorAction {
  type: typeof ERROR,
  message: String
}

interface ClearErrorAction {
  type: typeof CLEAR_ERROR
}

export type ErrorActions = ErrorAction | ClearErrorAction;

export function error(message: String) : ErrorAction {
    return { type: ERROR, message: message };
}

export function clearError() : ClearErrorAction {
    return { type: CLEAR_ERROR };
}
