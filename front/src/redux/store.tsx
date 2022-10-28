import { configureStore } from "@reduxjs/toolkit";
import { TypedUseSelectorHook, useDispatch, useSelector } from "react-redux";

import { errorSlice } from "./reducers/error";
import { localeSlice } from "./reducers/locale";
import { signInSlice } from "./reducers/signin";

const store = configureStore({
  reducer: {
    [errorSlice.name]: errorSlice.reducer,
    [signInSlice.name]: signInSlice.reducer,
    [localeSlice.name]: localeSlice.reducer,
  },
});

export { store };

// Infer the `RootState` and `AppDispatch` types from the store itself
export type RootState = ReturnType<typeof store.getState>;
// Inferred type: {posts: PostsState, comments: CommentsState, users: UsersState}
export type AppDispatch = typeof store.dispatch;

// Use throughout your app instead of plain `useDispatch` and `useSelector`
export const useAppDispatch = () => useDispatch<AppDispatch>();
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;
