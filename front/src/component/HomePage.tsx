import React from 'react';

import { useNavigate } from "react-router-dom";

import { useAppSelector } from '../redux/store';
import { selectMessages } from '../redux/reducers/locale';
import { selectSignIn } from '../redux/reducers/signin';

function HomePage()  {
    const username = useAppSelector(selectSignIn).username;
    const home = useAppSelector(selectMessages).home;

    let navigate = useNavigate();

    if (username) {
        return (
        <div>
            <h3>{home.hello} {username}</h3>
        </div>
        );
    } else {
        console.log("Unauthorized... Redirecting...")
        navigate('../signin')
        return (<div></div>);
    }
}

export default HomePage;
