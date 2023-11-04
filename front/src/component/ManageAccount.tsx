import React, { useState } from "react";
import { Button, Input, Label, Form, FormGroup, FormText, Spinner } from "reactstrap";

import { useNavigate } from "react-router-dom";

import { useAppSelector, useAppDispatch } from "../redux/store";
import { addMessage, selectErrorMessage, clearMessage } from "../redux/reducers/error";
import { selectSignIn, logout, accountUpdated } from "../redux/reducers/signin";
import { selectMessages } from "../redux/reducers/locale";

import { isMobile } from "react-device-detect";

import SquareImage from "./SquareImage";
import blank_profile_picture from "./image/blank_profile_picture.png";

import { getServerUrl } from "../ServerInformation";
let url = getServerUrl();

function ManageAccount() {
  const username = useAppSelector(selectSignIn).username;
  const token = useAppSelector(selectSignIn).token;
  const picture = useAppSelector(selectSignIn).picture;

  const manageAccount = useAppSelector(selectMessages).manageAccount;

  const errorMessage = useAppSelector(selectErrorMessage);

  const appDispatch = useAppDispatch();
  let navigate = useNavigate();

  const [serverFileName, setServerFileName] = useState(
    picture === null ? "" : picture
  );

  if (token && username) {
    let changeImage = (e: any) => {
      e.target.form[2].disabled = true;
      e.target.form[2].children[0].hidden = false;
      const formData = new FormData();
      formData.append("0", e.target.files[0]);
      const request = async () => {
        const response = await fetch(url + "/files", {
          method: "post",
          headers: { Authorization: `Bearer ${token}` },
          body: formData,
        });
        if (response.status === 401) {
          appDispatch(logout());
        } else if (response.status === 202) {
          const json = await response.json();
          setServerFileName(json.name);
          e.target.form[2].disabled = false;
          e.target.form[2].children[0].hidden = true;
        } else {
          const json = await response.json();
          console.error(json);
          appDispatch(addMessage(json.error));
        }
      };
      request();
    };

    let onFormSubmit = (e: any) => {
      e.preventDefault();
      console.log(serverFileName);
      const request = async () => {
        const response = await fetch(url + "/users", {
          method: "PATCH",
          headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}`, },
          body: JSON.stringify({ name: e.target.name.value, picture: serverFileName, }),
        });
        if (response.status === 202) {
            appDispatch(clearMessage());
            appDispatch(accountUpdated({ username: e.target.name.value, picture: serverFileName }))
              .then(() => { navigate("../"); });
        } else if (response.status === 401) {
          appDispatch(logout());
        } else {
          const json = await response.json();
          console.error(json);
          appDispatch(addMessage(json.error));
        }
      };
      request();
    };

    return (
      <div style={{margin: "10px", width: isMobile ? "100%" : "25%"}}>
        {errorMessage && <p className="auth-error">{errorMessage}</p>}
        <Form onSubmit={onFormSubmit}>
          <FormGroup>
            <Label>{manageAccount.username}</Label>
            <Input name="name" defaultValue={username}/>

          </FormGroup>
          <FormGroup>
            <Label>{manageAccount.profile_picture}</Label>
            <Input type="file" onChange={(e) => changeImage(e)} />
          </FormGroup>
          <SquareImage
            token={token}
            className="profile-image"
            imageName={serverFileName}
            size={150}
            alt="Profile"
            alternateImage={blank_profile_picture}
          />
          <br />
          <br />
          <Button color="primary" type="submit"><Spinner hidden size="sm" /> {manageAccount.save}</Button>
        </Form>
      </div>
    );
  } else {
    console.log("Unauthorized... Redirecting...");
    navigate("../signin");
    return <div></div>;
  }
}

export default ManageAccount;
