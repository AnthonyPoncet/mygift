import { ERROR, CLEAR_ERROR } from '../constants'

export function error(message) {
    return { type: ERROR, message };
}

export function clearError() {
    return { type: CLEAR_ERROR };
}
