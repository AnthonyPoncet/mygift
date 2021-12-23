import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';

import { RootState } from '../store';
import { addMessage } from './error';

import { getServerUrl } from '../../ServerInformation';

interface SignInState {
  token: string | null,
  username: string | null,
  picture: string | null
}

const defaultState : SignInState = {
    token: localStorage.getItem("token"),
    username: localStorage.getItem('username'),
    picture: localStorage.getItem('picture')
};

interface UserSignUp {
    username: string,
    password: string,
    picture: string | null
}

interface UserSignIn {
     username: string,
     password: string
}

export const signUp = createAsyncThunk('users/signUp', async (userSignUp: UserSignUp, thunkAPI: any) => {
    let imageName = (userSignUp.picture === null) ? "" : userSignUp.picture;
    const response = await fetch(getServerUrl() + '/users', {
            method: 'put',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ 'name': userSignUp.username, 'password': userSignUp.password, 'picture': imageName })
        }).catch(err => {
            console.error("Unexpected error: " + err.message);
            thunkAPI.dispatch(addMessage("Unable to reach the server. Contact support."));
        });

    if (response === undefined) return;

    try {
        const json = await response.json();
        if (response.status === 201) {
            localStorage.setItem('token', json.token);
            localStorage.setItem('username', json.name);
            let picture = (json.picture !== undefined && json.picture.length !== 0) ? json.picture : null;
            if (picture !== null) localStorage.setItem('picture', json.picture);
            else localStorage.removeItem('picture');

            return json;
        } else {
            thunkAPI.dispatch(addMessage(json.error));
        }
    } catch(e: any) {
        console.error('Unexpected error: ' + e.message);
        thunkAPI.dispatch(addMessage('Unable to reach the server. Contact support.'));
    }

})

export const signIn = createAsyncThunk('users/signIn', async (userSignIn: UserSignIn, thunkAPI: any) => {
    const response = await fetch(getServerUrl() + '/user/connect', {
            method: 'post',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ 'name': userSignIn.username, 'password': userSignIn.password })
        })
        .catch(err => {
            console.error('Unexpected error: ' + err.message);
            thunkAPI.addMessage('Unable to reach the server. Contact support.');
        });

    if (response === undefined) return;

    try {
        const json = await response.json();
        if (response.status === 200) {
            localStorage.setItem('token', json.token);
            localStorage.setItem('username', json.name);
            let picture = (json.picture !== undefined && json.picture.length !== 0) ? json.picture : null;
            if (picture !== null) localStorage.setItem('picture', json.picture);
            else localStorage.removeItem('picture');

            return json;
        } else {
            thunkAPI.dispatch(addMessage(json.error));
        }
    } catch(e: any) {
        console.error('Unexpected error: ' + e.message);
        thunkAPI.dispatch(addMessage('Unable to reach the server. Contact support.'));
    }

})

export const signInSlice = createSlice({
    name: 'signIn',
    initialState: defaultState,
    reducers: {
        logout(state: SignInState) {
            localStorage.removeItem('token');
            localStorage.removeItem('username');
            localStorage.removeItem('picture');
            state.token = null;
            state.username = null;
            state.picture = null;
        }
    },
    extraReducers: (builder) => {
        builder
        .addCase(signUp.fulfilled, (state: SignInState, action) => {
            const json = action.payload;
            state.token = json.token;
            state.username = json.name;
            state.picture = json.picture;
        })
        .addCase(signIn.fulfilled, (state: SignInState, action) => {
            const json = action.payload;
            state.token = json.token;
            state.username = json.name;
            state.picture = json.picture;
        })
    }
});

export const { logout } = signInSlice.actions;

export const selectSignIn = (state: RootState) => state.signIn;

