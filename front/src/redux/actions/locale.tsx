import { CHANGE_LOCALE } from '../constants'

interface ChangeLocaleAction {
    type: typeof CHANGE_LOCALE,
    locale: string
}

export type ChangeLocaleActions = ChangeLocaleAction;

export function changeLocale(locale: string) : ChangeLocaleAction {
    return { type: CHANGE_LOCALE, locale: locale };
}
