import { createStore, combineReducers, applyMiddleware } from "redux";
import thunkMiddleware from 'redux-thunk';

import { error } from './reducers/error'
import { signin } from './reducers/signin'

const rootReducer = combineReducers({error: error, signin: signin});

export type AppState = ReturnType<typeof rootReducer>

export default function configureStore() { return createStore(rootReducer, applyMiddleware(thunkMiddleware)); }
