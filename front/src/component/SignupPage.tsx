import React from 'react';

import './style/style.css';
import create_list from './image/create_list.png';
import friends from './image/friends.png';

import { Label, Form, Input, FormGroup, Button } from 'reactstrap';

import { useAppSelector, useAppDispatch } from '../redux/store';
import { selectMessages } from '../redux/reducers/locale';
import { addMessage, selectErrorMessage, clearMessage } from '../redux/reducers/error';
import { signUp } from '../redux/reducers/signin';

import { isMobile } from 'react-device-detect';

function renderText(connection: any) {
    return (
    <div>
        <div className="textContainer"><img className="small-icon" src={create_list} alt="Create list"/><p className="textAlign">{connection.listDesc}</p></div>
        <div className="textContainer"><img className="small-icon" src={friends} alt="Friends"/><p className="textAlign">{connection.friendsDesc}</p></div>
    </div>
    );
}

function renderSignUp(connection: any, errorMessage: any, appDispatch: any) {
    let onFormSubmit = (e: any) => {
        e.preventDefault();
        const username = e.target.username.value;
        const password = e.target.password.value;
        if (username && password) {
            appDispatch(clearMessage());
            appDispatch(signUp({username: username, password: password, picture: null}));
        } else {
            appDispatch(addMessage(connection.emptyErrorMessage));
        }
    }

    return(
    <div className="auth-form">
        <h1 className="auth-form-header">{connection.signUpTitle}</h1>
        { errorMessage && <p className="auth-error">{errorMessage}</p> }
        <div className="auth-form-body">
            <Form onSubmit={onFormSubmit}>
                <FormGroup>
                    <Label>{connection.username}</Label>
                    <Input type="text" name="username" id="username" placeholder={connection.username} />
                </FormGroup>
                <FormGroup>
                    <Label>{connection.password}</Label>
                    <Input type="password" name="password" id="password" placeholder={connection.password} />
                </FormGroup>
                <Button color="primary" block type="submit">{connection.signUpButton}</Button>
            </Form>
        </div>
    </div>
    );
}

function SignupPage() {
    const connection = useAppSelector(selectMessages).connection;
    const errorMessage = useAppSelector(selectErrorMessage);

    const appDispatch = useAppDispatch();

    if (isMobile) {
        return (
        <div>
            <div className="textMobile">{renderText(connection)}</div>
            <div>{renderSignUp(connection, errorMessage, appDispatch)}</div>
        </div>
        );
    } else {
        return (
        <div className="wrapper">
            <div className="left">{renderText(connection)}</div>
            <div className="right">{renderSignUp(connection, errorMessage, appDispatch)}</div>
        </div>
        );
    }
}

export default SignupPage;