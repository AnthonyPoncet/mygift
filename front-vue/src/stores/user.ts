import { ref, type Ref } from "vue";
import { defineStore } from "pinia";

const STORE_NAME = "users";

interface State {
  token: string;
  username: string;
  picture: string | null;
  dateOfBirth: number | null;
  otherUsers: OtherUser[];
}

export interface OtherUser {
  token: string;
  username: string;
  picture: string | null;
  dateOfBirth: number | null;
}

export interface SignInUser {
  token: string;
  session: string;
  name: string;
  picture: string | null;
  dateOfBirth: number | null;
}

export const useUserStore = defineStore(STORE_NAME, () => {
  const user: Ref<State | null> = initState();

  function initState(): Ref<State | null> {
    const storage = localStorage.getItem(STORE_NAME);
    if (storage !== null) {
      return ref(JSON.parse(storage));
    } else {
      return ref(null);
    }
  }

  function updateUser(signedUser: SignInUser) {
    user.value = {
      token: signedUser.token,
      username: signedUser.name,
      picture: signedUser.picture,
      dateOfBirth: signedUser.dateOfBirth,
      otherUsers: [],
    };
    localStorage.setItem(STORE_NAME, JSON.stringify(user.value));
  }

  function logMultiAccount(signedUser: SignInUser) {
    const currentUser = {
      token: user.value!.token,
      username: user.value!.username,
      picture: user.value!.picture,
      dateOfBirth: user.value!.dateOfBirth,
    };
    user.value = {
      token: signedUser.token,
      username: signedUser.name,
      picture: signedUser.picture,
      dateOfBirth: signedUser.dateOfBirth,
      otherUsers: user.value!.otherUsers,
    };
    user.value.otherUsers.push(currentUser);
    localStorage.setItem(STORE_NAME, JSON.stringify(user.value));
  }

  function changeAccount(nextUser: OtherUser) {
    const currentUser = {
      token: user.value!.token,
      username: user.value!.username,
      picture: user.value!.picture,
      dateOfBirth: user.value!.dateOfBirth,
    };
    const newOtherUsers = user.value!.otherUsers.filter((u) => u.username !== nextUser.username);
    newOtherUsers.push(currentUser);

    user.value = {
      token: nextUser.token,
      username: nextUser.username,
      picture: nextUser.picture,
      dateOfBirth: nextUser.dateOfBirth,
      otherUsers: newOtherUsers,
    };
    localStorage.setItem(STORE_NAME, JSON.stringify(user.value));
  }

  function logout() {
    user.value = null;
    localStorage.removeItem(STORE_NAME);
  }

  return { user, updateUser, logMultiAccount, changeAccount, logout };
});
