import React, { useState } from "react";
import { Button, Input, Label, Form, FormGroup, FormText } from "reactstrap";

import { useNavigate } from "react-router-dom";

import { useAppSelector, useAppDispatch } from "../redux/store";
import { addMessage } from "../redux/reducers/error";
import { selectSignIn, logout } from "../redux/reducers/signin";

import SquareImage from "./SquareImage";
import blank_profile_picture from "./image/blank_profile_picture.png";

import { getServerUrl } from "../ServerInformation";
let url = getServerUrl();

function ManageAccount() {
  const username = useAppSelector(selectSignIn).username;
  const token = useAppSelector(selectSignIn).token;
  const picture = useAppSelector(selectSignIn).picture;

  const appDispatch = useAppDispatch();
  let navigate = useNavigate();

  const [serverFileName, setServerFileName] = useState(
    picture === null ? "" : picture
  );

  if (token && username) {
    let changeImage = (e: any) => {
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
        } else {
          const json = await response.json();
          console.error(json);
          appDispatch(addMessage(json.error));
        }
      };
      request();
    };

    return (
      <div>
        <Form>
          <FormGroup>
            <Label>Name</Label>
            <Input value={username} disabled />
            <FormText>Could not be changed</FormText>
          </FormGroup>
          <FormGroup>
            <Label>Profile picture</Label>
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
          <Button className="btn btn-primary">Save</Button>
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
