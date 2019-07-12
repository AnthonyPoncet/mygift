import { error } from './error';

import { SIGNIN, LOGOUT, SIGNUP } from '../constants'

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
                localStorage.setItem('username', JSON.stringify(json.name));
                dispatch({ type: SIGNIN, payload: { id: json.id, username: json.name } });
                history.push('/');
            } else {
                dispatch(error(json.error));
            }
        };
        request();
    };
}

export function logout() {
    localStorage.removeItem('username');
    return dispatch => dispatch({ type: LOGOUT });
}

export function signup(username) {
    return dispatch => {
        //TODO: mock for now
        if (username === "aa") {
          //OK - maybe just call signin
          localStorage.setItem('username', JSON.stringify(username));
          dispatch({ type: SIGNIN, username });
          history.push('/');
        } else {
          dispatch(error("an error"));
        }
    };

}
