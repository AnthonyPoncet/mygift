import { createStore, combineReducers, applyMiddleware } from "redux";
import thunkMiddleware from 'redux-thunk';

import { error } from './reducers/error'
import { signin } from './reducers/signin'

const rootReducer = combineReducers({error, signin});

export default createStore(rootReducer, applyMiddleware(thunkMiddleware));
