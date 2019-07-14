import { error } from './error';

import { SIGNIN, LOGOUT } from '../constants'

import { history } from '../../component/history'

export function signin(username, password) {
    return dispatch => {
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
    };
}

export function logout() {
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    return dispatch => dispatch({ type: LOGOUT });
}

export function signup(user) {
    return dispatch => {
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
    };

}
