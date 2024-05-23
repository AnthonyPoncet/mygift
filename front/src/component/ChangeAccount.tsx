import React from "react";

import { Label, Form, Input, FormGroup, Button } from "reactstrap";
import { useNavigate } from "react-router-dom";

import "./style/style.css";

import { useAppSelector, useAppDispatch } from "../redux/store";
import { selectMessages } from "../redux/reducers/locale";
import {
  addMessage,
  selectErrorMessage,
  clearMessage,
} from "../redux/reducers/error";
import { signIn } from "../redux/reducers/signin";

function SignInPage() {
  const connection = useAppSelector(selectMessages).connection;
  const errorMessage = useAppSelector(selectErrorMessage);

  const appDispatch = useAppDispatch();

  let navigate = useNavigate();

  let onFormSubmit = (e: any) => {
    e.preventDefault();
    const username = e.target.username.value;
    const password = e.target.password.value;
    if (username && password) {
      appDispatch(clearMessage());
      appDispatch(
        signIn({ username: username, password: password, changeAccount: true }),
      ).then(() => {
        navigate("../");
      });
    } else {
      appDispatch(addMessage(connection.emptyErrorMessage));
    }
  };

  return (
    <div className="auth-form">
      <h1 className="auth-form-header">{connection.signInTitle}</h1>
      {errorMessage && <p className="auth-error">{errorMessage}</p>}
      <div className="auth-form-body">
        <Form onSubmit={onFormSubmit}>
          <FormGroup>
            <Label>{connection.username}</Label>
            <Input
              className="form-control"
              type="text"
              name="username"
              id="username"
              placeholder={connection.username}
            />
          </FormGroup>
          <FormGroup>
            <Label>{connection.password}</Label>
            <Input
              className="form-control"
              type="password"
              name="password"
              id="password"
              placeholder={connection.password}
            />
          </FormGroup>
          <Button color="primary" block type="submit">
            {connection.signInButton}
          </Button>
        </Form>
      </div>
    </div>
  );
}

export default SignInPage;
