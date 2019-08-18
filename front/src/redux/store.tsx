import { createStore, combineReducers, applyMiddleware } from "redux";
import thunkMiddleware from 'redux-thunk';

import { error } from './reducers/error';
import { changeLocale } from './reducers/locale';
import { signin } from './reducers/signin';

const rootReducer = combineReducers({ error: error, signin: signin, locale: changeLocale });

export type AppState = ReturnType<typeof rootReducer>

export default function configureStore() { return createStore(rootReducer, applyMiddleware(thunkMiddleware)); }
