import { error } from './error';
import { SIGNIN, LOGOUT, CHANGE_PROFILE } from '../constants';

import { ThunkAction, ThunkDispatch } from 'redux-thunk';
import { AnyAction } from 'redux';

import { history } from '../../component/history';

import { getServerUrl } from "../../ServerInformation";

interface UserSignIn {
  token: string,
  username: string,
  picture: string | null
}
interface SignIn {
  type: typeof SIGNIN,
  payload: UserSignIn
}
interface LogOut {
  type: typeof LOGOUT
}
interface ChangeProfile {
  type: typeof CHANGE_PROFILE,
  payload: UserNameAndPicture
}
export type UserAction = SignIn | LogOut | ChangeProfile;

export const signin = (username: String, password: String): ThunkAction<Promise<void>, {}, {}, AnyAction> => {
    return async (dispatch: ThunkDispatch<{}, {}, AnyAction>): Promise<void> => {
        return new Promise<void>(() => {
              const request = async () => {
                  const response = await fetch(getServerUrl() + '/user/connect', {
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
                      localStorage.setItem('token', json.token);
                      localStorage.setItem('username', json.name);
                      let picture = (json.picture !== undefined && json.picture.length !== 0) ? json.picture : null;
                      if (picture !== null) localStorage.setItem('picture', json.picture);
                      else localStorage.removeItem('picture');
                      dispatch({ type: SIGNIN, payload: { token: json.token, username: json.name, picture: picture } });
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
  localStorage.removeItem('token');
  localStorage.removeItem('username');
  localStorage.removeItem('picture');
  return { type: LOGOUT };
}

export interface UserSignUp {
  username: String,
  password: String,
  image: string | null
}

export const signup = (user: UserSignUp): ThunkAction<Promise<void>, {}, {}, AnyAction> => {
  return async (dispatch: ThunkDispatch<{}, {}, AnyAction>): Promise<void> => {
    return new Promise<void>(() => {
      const request = async () => {
          let imageName = (user.image === null) ? "" : user.image;
          console.log(imageName)
          const response = await fetch(getServerUrl() + '/users', {
              method: 'put',
              headers: {'Content-Type':'application/json'},
              body: JSON.stringify({
                  "name": user.username,
                  "password": user.password,
                  "picture": imageName
              })
          }).catch(err => {
            console.error("Unexpected error: " + err.message);
            dispatch(error("Unable to reach the server. Contact support."));
          });

          if (response === undefined) return;

          const json = await response.json();
          if (response.status === 201) {
              localStorage.setItem('token', json.token);
              localStorage.setItem('username', json.name);
              let picture = (json.picture !== undefined && json.picture.length !== 0) ? json.picture : null;
              if (picture !== null) localStorage.setItem('picture', json.picture);
              else localStorage.removeItem('picture');
              dispatch({ type: SIGNIN, payload: { token: json.token, username: json.name, picture: picture } });
              history.push('/');
          } else {
              dispatch(error(json.error));
          }
      };
      request();
    })
  }
}

export interface UserNameAndPicture {
  username: string,
  picture: string | null
}

export function changeUserInfo(user: UserNameAndPicture) : UserAction {
    localStorage.setItem('username', user.username.toString());
    if (user.picture !== null) localStorage.setItem('picture', user.picture);
    else localStorage.removeItem('picture');
    return {type: CHANGE_PROFILE, payload: { username: user.username.toString(), picture: user.picture } };
}
