import React from "react";
import ReactDOM from "react-dom";

import { BrowserRouter, Routes, Route } from "react-router-dom";

import "./index.css";
import App from "./App";
import { Index } from "./App";
import FriendWishList from "./component/FriendWishList";
import ManageAccount from "./component/ManageAccount";
import MyBuyList from "./component/MyBuyList";
import MyFriends from "./component/MyFriends";
import MyWishList from "./component/MyWishList";
import ResetPassword from "./component/ResetPassword";
import SignInPage from "./component/SigninPage";
import SignupPage from "./component/SignupPage";

import { Provider } from "react-redux";
import { store } from "./redux/store";

import "bootstrap/dist/css/bootstrap.css";

ReactDOM.render(
  <Provider store={store}>
    <React.StrictMode>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<App />}>
            <Route index element={<Index />} />
            <Route path="/friend/:name" element={<FriendWishList />} />
            <Route path="/manageaccount" element={<ManageAccount />} />
            <Route path="/buylist" element={<MyBuyList />} />
            <Route path="/myfriends" element={<MyFriends />} />
            <Route path="/mywishlist" element={<MyWishList />} />
            ManageAccount
            <Route path="/signin" element={<SignInPage />} />
            <Route path="/signup" element={<SignupPage />} />
            <Route path="/reset-password/:uuid" element={<ResetPassword />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </React.StrictMode>
  </Provider>,
  document.getElementById("root")
);
