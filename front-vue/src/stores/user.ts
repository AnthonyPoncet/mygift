import { ref, type Ref } from "vue";
import { defineStore } from "pinia";

const STORE_NAME = "users";

interface State {
  id: number;
  name: string;
  token: string;
  picture: string | null;
  dateOfBirth: number | null;
  otherUsers: OtherUser[];
}

export interface OtherUser {
  id: number;
  name: string;
  token: string;
  picture: string | null;
  dateOfBirth: number | null;
}

export interface SignInUser {
  id: number;
  name: string;
  token: string;
  picture: string | null;
  date_of_birth: number | null;
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
      id: signedUser.id,
      name: signedUser.name,
      token: signedUser.token,
      picture: signedUser.picture,
      dateOfBirth: signedUser.date_of_birth,
      otherUsers: [],
    };
    localStorage.setItem(STORE_NAME, JSON.stringify(user.value));
  }

  function logMultiAccount(signedUser: SignInUser) {
    const currentUser = {
      id: user.value!.id,
      name: user.value!.name,
      token: user.value!.token,
      picture: user.value!.picture,
      dateOfBirth: user.value!.dateOfBirth,
    };
    user.value = {
      id: signedUser.id,
      name: signedUser.name,
      token: signedUser.token,
      picture: signedUser.picture,
      dateOfBirth: signedUser.date_of_birth,
      otherUsers: user.value!.otherUsers,
    };
    user.value.otherUsers.push(currentUser);
    localStorage.setItem(STORE_NAME, JSON.stringify(user.value));
  }

  function changeAccount(nextUser: OtherUser) {
    const currentUser = {
      id: user.value!.id,
      name: user.value!.name,
      token: user.value!.token,
      picture: user.value!.picture,
      dateOfBirth: user.value!.dateOfBirth,
    };
    const newOtherUsers = user.value!.otherUsers.filter((u) => u.name !== nextUser.name);
    newOtherUsers.push(currentUser);

    user.value = {
      id: nextUser.id,
      name: nextUser.name,
      token: nextUser.token,
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
