import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";

import { RootState } from "../store";
import { addMessage } from "./error";

import { getServerUrl } from "../../ServerInformation";

interface OtherUser {
  token: string;
  username: string;
  picture: string;
  dateOfBirth: number | null;
}

interface SignInState {
  token: string | null;
  username: string | null;
  picture: string | null;
  dateOfBirth: number | null;
  otherUsers: OtherUser[];
}

const defaultState: SignInState = {
  token: localStorage.getItem("token"),
  username: localStorage.getItem("username"),
  picture: localStorage.getItem("picture"),
  dateOfBirth: Number(localStorage.getItem("dateOfBirth")),
  otherUsers: JSON.parse(localStorage.getItem("otherUsers") || "[]"),
};

interface UserSignUp {
  username: string;
  password: string;
  picture: string | null;
  dateOfBirth: number | null;
}

interface UserSignIn {
  username: string;
  password: string;
  changeAccount: boolean;
}

interface UserAccountUpdated {
  username: string;
  picture: string;
  dateOfBirth: number | null;
}

export const signUp = createAsyncThunk(
  "users/signUp",
  async (userSignUp: UserSignUp, thunkAPI: any) => {
    let imageName = userSignUp.picture === null ? "" : userSignUp.picture;
    const response = await fetch(getServerUrl() + "/users", {
      method: "put",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        name: userSignUp.username,
        password: userSignUp.password,
        picture: imageName,
        dateOfBirth: userSignUp.dateOfBirth,
      }),
    }).catch((err) => {
      console.error("Unexpected error: " + err.message);
      thunkAPI.dispatch(
        addMessage("Unable to reach the server. Contact support."),
      );
    });

    if (response === undefined) return;

    try {
      const json = await response.json();
      if (response.status === 201) {
        localStorage.setItem("token", json.token);
        localStorage.setItem("username", json.name);
        let picture =
          json.picture !== undefined && json.picture.length !== 0
            ? json.picture
            : null;
        if (picture !== null) localStorage.setItem("picture", json.picture);
        else localStorage.removeItem("picture");
        if (json.dateOfBirth !== undefined)
          localStorage.setItem("dateOfBirth", json.dateOfBirth);
        else localStorage.removeItem("dateOfBirth");

        return json;
      } else {
        thunkAPI.dispatch(addMessage(json.error));
      }
    } catch (e: any) {
      console.error("Unexpected error: " + e.message);
      thunkAPI.dispatch(
        addMessage("Unable to reach the server. Contact support."),
      );
    }
  },
);

export const signIn = createAsyncThunk(
  "users/signIn",
  async (userSignIn: UserSignIn, thunkAPI: any) => {
    let path = userSignIn.changeAccount ? "change-account" : "connect";
    const requestHeaders: HeadersInit = new Headers();
    requestHeaders.set("Content-Type", "application/json");
    if (userSignIn.changeAccount) {
      let token = localStorage.getItem("token");
      requestHeaders.set("Authorization", `Bearer ${token}`);
    }

    const response = await fetch(getServerUrl() + "/user/" + path, {
      method: "post",
      headers: requestHeaders,
      body: JSON.stringify({
        name: userSignIn.username,
        password: userSignIn.password,
      }),
    }).catch((err) => {
      console.error("Unexpected error: " + err.message);
      thunkAPI.addMessage("Unable to reach the server. Contact support.");
    });

    if (response === undefined) return;

    try {
      const json = await response.json();
      if (response.status === 200) {
        if (userSignIn.changeAccount) {
          const otherUsers = JSON.parse(
            localStorage.getItem("otherUsers") || "[]",
          );
          console.log("other: " + otherUsers + " - " + typeof otherUsers);
          const otherUser = {
            token: localStorage.getItem("token"),
            username: localStorage.getItem("username"),
            picture: localStorage.getItem("picture"),
            dateOfBirth: localStorage.getItem("dateOfBirth"),
          };
          otherUsers.push(otherUser);
          localStorage.setItem("otherUsers", JSON.stringify(otherUsers));
          json["otherUsers"] = otherUsers;
        } else {
          localStorage.setItem("otherUsers", JSON.stringify([]));
          json["otherUsers"] = [];
        }

        localStorage.setItem("token", json.token);
        localStorage.setItem("username", json.name);
        let picture =
          json.picture !== undefined && json.picture.length !== 0
            ? json.picture
            : null;
        if (picture !== null) localStorage.setItem("picture", json.picture);
        else localStorage.removeItem("picture");
        if (json.dateOfBirth !== undefined)
          localStorage.setItem("dateOfBirth", json.dateOfBirth);
        else localStorage.removeItem("dateOfBirth");

        return json;
      } else {
        thunkAPI.dispatch(addMessage(json.error));
      }
    } catch (e: any) {
      console.error("Unexpected error: " + e.message);
      thunkAPI.dispatch(
        addMessage("Unable to reach the server. Contact support."),
      );
    }
  },
);

export const logout = createAsyncThunk("users/logout", async () => {
  let token = localStorage.getItem("token");
  await fetch(getServerUrl() + "/user/logout", {
    method: "get",
    headers: { Authorization: `Bearer ${token}` },
  }).catch((err) => {
    localStorage.removeItem("token");
    localStorage.removeItem("username");
    localStorage.removeItem("picture");
    localStorage.removeItem("dateOfBirth");
    localStorage.removeItem("otherUsers");
    console.error("Unexpected error: " + err.message);
  });

  localStorage.removeItem("token");
  localStorage.removeItem("username");
  localStorage.removeItem("picture");
  localStorage.removeItem("dateOfBirth");
  localStorage.removeItem("otherUsers");
});

export const changeUser = createAsyncThunk(
  "users/changeUser",
  async (otherUser: OtherUser) => {
    const otherUsers = JSON.parse(localStorage.getItem("otherUsers") || "[]");
    const currentUser = {
      token: localStorage.getItem("token"),
      username: localStorage.getItem("username"),
      picture: localStorage.getItem("picture"),
      dateOfBirth: localStorage.getItem("dateOfBirth"),
    };

    let newOtherUsers = otherUsers.filter(
      (u: OtherUser) => u.username !== otherUser.username,
    );
    newOtherUsers.push(currentUser);

    localStorage.setItem("token", otherUser.token);
    localStorage.setItem("username", otherUser.username);
    localStorage.setItem("picture", otherUser.picture);
    if (otherUser.dateOfBirth !== null)
      localStorage.setItem("dateOfBirth", otherUser.dateOfBirth.toString());
    else localStorage.removeItem("dateOfBirth");
    localStorage.setItem("otherUsers", JSON.stringify(newOtherUsers));

    return {
      token: otherUser.token,
      username: otherUser.username,
      picture: otherUser.picture,
      dateOfBirth: otherUser.dateOfBirth,
      otherUsers: newOtherUsers,
    };
  },
);

export const accountUpdated = createAsyncThunk(
  "users/accountUpdated",
  async (userAccountUpdated: UserAccountUpdated) => {
    localStorage.setItem("username", userAccountUpdated.username);
    localStorage.setItem("picture", userAccountUpdated.picture);
    if (userAccountUpdated.dateOfBirth !== null)
      localStorage.setItem(
        "dateOfBirth",
        userAccountUpdated.dateOfBirth.toString(),
      );
    else localStorage.removeItem("dateOfBirth");
    return userAccountUpdated;
  },
);

export const signInSlice = createSlice({
  name: "signIn",
  initialState: defaultState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(signUp.fulfilled, (state: SignInState, action) => {
        const json = action.payload;
        if (json !== undefined) {
          state.token = json.token;
          state.username = json.name;
          state.picture = json.picture;
          state.dateOfBirth =
            json.dateOfBirth === undefined ? null : json.dateOfBirth;
          state.otherUsers = [];
        }
      })
      .addCase(signIn.fulfilled, (state: SignInState, action) => {
        const json = action.payload;
        if (json !== undefined) {
          state.token = json.token;
          state.username = json.name;
          state.picture = json.picture;
          state.dateOfBirth =
            json.dateOfBirth === undefined ? null : json.dateOfBirth;
          state.otherUsers = json.otherUsers;
        }
      })
      .addCase(logout.fulfilled, (state: SignInState) => {
        state.token = null;
        state.username = null;
        state.picture = null;
        state.dateOfBirth = null;
        state.otherUsers = [];
      })
      .addCase(changeUser.fulfilled, (state: SignInState, action) => {
        const json = action.payload;
        if (json !== undefined) {
          state.token = json.token;
          state.username = json.username;
          state.picture = json.picture;
          state.dateOfBirth =
            json.dateOfBirth === undefined ? null : json.dateOfBirth;
          state.otherUsers = json.otherUsers;
        }
      })
      .addCase(accountUpdated.fulfilled, (state: SignInState, action) => {
        const userAccountUpdated = action.payload;
        if (userAccountUpdated !== undefined) {
          state.username = userAccountUpdated.username;
          state.picture = userAccountUpdated.picture;
          state.dateOfBirth =
            userAccountUpdated.dateOfBirth === undefined
              ? null
              : userAccountUpdated.dateOfBirth;
        }
      });
  },
});

export const selectSignIn = (state: RootState) => state.signIn;
