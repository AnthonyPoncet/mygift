import React, { useEffect, useState } from "react";

import { useParams } from "react-router-dom";

import { useAppSelector, useAppDispatch } from "../redux/store";
import { addMessage, selectErrorMessage } from "../redux/reducers/error";

import { getServerUrl } from "../ServerInformation";
let url = getServerUrl();

function isUuidValid(uuid: string, setStatus: any, appDispatch: any) {
  const request = async () => {
    const response = await fetch(url + "/passwords/reset/" + uuid, {
      method: "get",
    });
    if (response.status === 302) {
      setStatus("valid");
    } else {
      const json = await response.json();
      console.error(json.error);
      appDispatch(addMessage(json.error));
      setStatus("invalid");
    }
  };
  request();
}

function handleSubmit(
  uuid: string,
  username: string,
  password: string,
  setStatus: any,
  appDispatch: any
) {
  const request = async () => {
    const response = await fetch(url + "/passwords/reset/" + uuid, {
      method: "post",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name: username, password: password }),
    });
    if (response.status === 202) {
      setStatus("done");
    } else {
      const json = await response.json();
      console.error(json.error);
      appDispatch(addMessage(json.error));
      setStatus("failed");
    }
  };
  request();
}

function ResetPassword() {
  const params = useParams();
  const { uuid } = params;

  const errorMessage = useAppSelector(selectErrorMessage);

  const appDispatch = useAppDispatch();

  const [status, setStatus] = useState("loading");

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  useEffect(() => {
    if (uuid) {
      isUuidValid(uuid, setStatus, appDispatch);
    } else {
      setStatus("invalid");
    }
  }, [uuid, appDispatch]);

  if (status === "loading") {
    return (
      <div>
        <p>Please wait</p>
      </div>
    );
  } else if (uuid && status === "valid") {
    return (
      <div className="auth-form">
        <h1 className="auth-form-header">Reset password</h1>
        <div className="auth-form-body">
          <div className="form-group">
            <label>Username</label>
            <input
              type="text"
              name="username"
              placeholder="username"
              className="form-control"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </div>
          <div className="form-group">
            <label>Password</label>
            <input
              type="password"
              name="password"
              placeholder="password"
              className="form-control"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          <br />
          <button
            className="btn btn-primary btn-large"
            onClick={() =>
              handleSubmit(uuid, username, password, setStatus, appDispatch)
            }
          >
            Change
          </button>
        </div>
      </div>
    );
  } else if (!uuid || status === "invalid") {
    return <p>Page not valid</p>;
  } else if (status === "failed") {
    return <p className="auth-error">{errorMessage}</p>;
  } else {
    //done
    return <p>Redirect...</p>;
  }
}

export default ResetPassword;
