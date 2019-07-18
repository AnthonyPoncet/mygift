import { error } from './error';
import { SIGNIN, LOGOUT } from '../constants';

import { ThunkAction, ThunkDispatch } from 'redux-thunk';
import { AnyAction } from 'redux';

import { history } from '../../component/history';

interface UserSignIn {
  userId: number,
  username: String
}
interface SignIn {
  type: typeof SIGNIN,
  payload: UserSignIn
}
interface LogOut {
  type: typeof LOGOUT
}
export type UserAction = SignIn | LogOut;

export const signin = (username: String, password: String): ThunkAction<Promise<void>, {}, {}, AnyAction> => {
    return async (dispatch: ThunkDispatch<{}, {}, AnyAction>): Promise<void> => {
        return new Promise<void>(() => {
              const request = async () => {
                  const response = await fetch('http://localhost:8080/user/connect', {
                      method: 'post',
                      headers: {'Content-Type':'application/json'},
                      body: JSON.stringify({
                          "name": username,
                          "password": password
                      })
                  }).catch(err => {
                    console.error("Unexpected error: " + err.message);
                    dispatch(error("Unable to reach the server. Contact support."));
                  });

                  if (response === undefined) return;

                  const json = await response.json();
                  if (response.status === 200) {
                      localStorage.setItem('userId', json.id);
                      localStorage.setItem('username', json.name);
                      dispatch({ type: SIGNIN, payload: { userId: json.id, username: json.name } });
                      history.push('/');
                  } else {
                      dispatch(error(json.error));
                  }
              };
            request();
        })
    }
}

export function logout(){
  localStorage.removeItem('userId');
  localStorage.removeItem('username');
  return { type: LOGOUT };
}

export interface UserSignUp {
  username: String,
  password: String
}

export const signup = (user: UserSignUp): ThunkAction<Promise<void>, {}, {}, AnyAction> => {
  return async (dispatch: ThunkDispatch<{}, {}, AnyAction>): Promise<void> => {
    return new Promise<void>(() => {
      const request = async () => {
          const response = await fetch('http://localhost:8080/users', {
              method: 'put',
              headers: {'Content-Type':'application/json'},
              body: JSON.stringify({
                  "name": user.username,
                  "password": user.password
              })
          }).catch(err => {
            console.error("Unexpected error: " + err.message);
            dispatch(error("Unable to reach the server. Contact support."));
          });

          if (response === undefined) return;

          const json = await response.json();
          if (response.status === 201) {
              localStorage.setItem('userId', json.id);
              localStorage.setItem('username', json.name);
              dispatch({ type: SIGNIN, payload: { userId: json.id, username: json.name } });
              history.push('/');
          } else {
              dispatch(error(json.error));
          }
      };
      request();
    })
  }
}