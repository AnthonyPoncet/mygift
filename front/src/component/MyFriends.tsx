import React, { useEffect, useState } from 'react';

import { Modal, ModalHeader, ModalBody, Button, Input, Label, Form, FormGroup, FormFeedback } from 'reactstrap';
import { useNavigate } from 'react-router-dom';
import { CheckIcon, XIcon, CircleSlashIcon } from '@primer/octicons-react';

import './style/friends.css';

import SquareImage from './SquareImage';
import blank_profile_picture from './image/blank_profile_picture.png';

import { isMobile } from 'react-device-detect';

import { useAppSelector, useAppDispatch } from '../redux/store';
import { selectMessages } from '../redux/reducers/locale';
import { addMessage, selectErrorMessage, clearMessage } from '../redux/reducers/error';
import { selectSignIn, logout } from '../redux/reducers/signin';

import { getServerUrl } from '../ServerInformation';
let url = getServerUrl();

function getFriends(token: string, setFriends: any, appDispatch: any) {
    const request = async () => {
        const response = await fetch(url + '/friends', {
            method: "GET",
            headers: {'Authorization': `Bearer ${token}`}
        });
        if (response.status === 401) {
            appDispatch(logout());
        } else {
            const json = await response.json();
            if (response.status === 200) {
                setFriends(json);
            } else {
                console.error(json.error);
                appDispatch(addMessage(json.error));
            }
        }
    };
    request();
};

function getPending(token: string, setPending: any, setReceived: any, appDispatch: any) {
    const request = async () => {
        const response = await fetch(url + '/friend-requests/pending', {
            method: "GET",
            headers: {'Authorization': `Bearer ${token}`}
        });
        if (response.status === 401) {
            appDispatch(logout());
        } else {
            const json = await response.json();
            if (response.status === 200) {
                setPending(json.sent);
                setReceived(json.received);
            } else {
                console.error(json.error);
                appDispatch(addMessage(json.error));
            }
        }
    };
    request();
};

function generateMessage(json: any, friendName: string): string {
    switch(json.status) {
        case "ACCEPTED":
            return 'You are already friend with ' + friendName;
        case "PENDING":
            if (json.ownRequest) {
                return 'Your friend ' + friendName + ' has still not accepted your request';
            } else {
                return 'Your friend ' + friendName + ' has already sent you a request';
            }
        case "REJECTED":
            return 'User ' + friendName + ' blocked you. You cannot sent him request.'
    }

    return '';
};

function cancelRequest(id: number, token: string, getFriends: any, setFriends: any, getPending: any, setPending: any, setReceived: any, appDispatch: any) {
    const request = async () => {
        const response = await fetch(url + '/friend-requests/' + id, {method: 'delete', headers: {'Authorization': `Bearer ${token}`}});
        if (response.status === 202) {
            getFriends(token, setFriends);
            getPending(token, setPending, setReceived, appDispatch);
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

function acceptRequest(id: number, token: string, getFriends: any, setFriends: any, getPending: any, setPending: any, setReceived: any, appDispatch: any) {
    const request = async () => {
        const response = await fetch(url + '/friend-requests/' + id + '/accept', {headers: {'Authorization': `Bearer ${token}`}});
        if (response.status === 202) {
            getFriends(token, setFriends);
            getPending(token, setPending, setReceived, appDispatch);
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

function declineRequest(id: number, blockUser: boolean, token: string, getFriends: any, setFriends: any, getPending: any, setPending: any, setReceived: any, appDispatch: any) {
    const request = async () => {
        const response = await fetch(url + '/friend-requests/' + id + '/decline?blockUser=' + blockUser, {method:"post", headers: {'Authorization': `Bearer ${token}`}});
        if (response.status === 202) {
            getFriends(token, setFriends);
            getPending(token, setPending, setReceived, appDispatch);
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

function renderHoverFriend(i: number, req: any, user: any, userHover: number) {
    if (i === userHover || isMobile) {
        return (
        <div className="friend-card-delete one-icon">
            <span style={{cursor: "pointer"}} ><XIcon/></span>
        </div>);
    } else {
        return (<div/>);
    }
}

function MyFriends() {
    const token = useAppSelector(selectSignIn).token;
    const myfriends = useAppSelector(selectMessages).myfriends;
    const errorMessage = useAppSelector(selectErrorMessage);

    const appDispatch = useAppDispatch();

    let navigate = useNavigate();

    const [friends, setFriends] = useState([]);
    const [pending, setPending] = useState([]);
    const [received, setReceived] = useState([]);
    const [userHover, setUserHover] = useState(-1);

    const [show, setShow] = useState(false);
    const handleClose = () => setShow(false);

    useEffect(() => {
        if (token) {
            getFriends(token, setFriends, appDispatch);
            getPending(token, setPending, setReceived, appDispatch);
        }
    }, [token, appDispatch]);

    if (token) {
        let onFormSubmit = (e: any) => {
            e.preventDefault();
            if (e.target.name.value === '') {
                appDispatch(addMessage(myfriends.nameErrorMessage));
                return;
            }
            const request = async () => {
                const response = await fetch(url + "/friend-requests", {
                    method: "PUT",
                    headers: {'Content-Type':'application/json', 'Authorization': `Bearer ${token}`},
                    body: JSON.stringify({"name": e.target.name.value})
                });
                if (response.status === 200) {
                    getFriends(token, setFriends, appDispatch);
                    getPending(token, setPending, setReceived, appDispatch);
                    setShow(false);
                } else if (response.status === 401) {
                    appDispatch(logout());
                } else {
                    const json = await response.json();
                    const errorMessage = (response.status === 409) ? generateMessage(json, e.target.name.value) : json.error;
                    appDispatch(addMessage(errorMessage));
                }
            };
            request();
        }

        return (
        <div>
            <div className="main-friend">
                <Button color="link" onClick={() => {appDispatch(clearMessage()); setShow(true)}}>{myfriends.addFriendButton}</Button>
                <h2 style={{margin: "10px"}}>{myfriends.friends}</h2>
                <div className="mycard-row">
                    {friends.map((req: any, i: any) => {
                        const user = req.otherUser;
                        return (
                        <div key={i + 'friends-' + req.id} className="friend-card" onMouseEnter={() => setUserHover(i)} onMouseLeave={() => setUserHover(-1)}>
                            <div style={{cursor: "pointer"}} onClick={() => navigate("/friend/" + user.name)}>
                                <SquareImage token={token} className="profile-image" imageName={user.picture} size={150} alt="Profile" alternateImage={blank_profile_picture}/>
                            </div>
                            {renderHoverFriend(i, req, user, userHover)}
                            <div style={{cursor: "pointer"}} className="friend-footer" onClick={() => navigate("/friend/" + user.name)}>
                                <div className="friend-name">{user.name}</div>
                            </div>
                        </div>);
                    })}
                </div>

                <h2 style={{margin: "10px"}}>{myfriends.requests}</h2>
                {received.length > 0 ?
                    received.map((req: any, i: any) => { return (
                        <li key={i + 'received' + req.otherUser.name  } style={{margin: "10px"}}>
                            {req.otherUser.name}
                            <span style={{cursor: "pointer", padding: '5px'}} onClick={() => acceptRequest(req.id, token, getFriends, setFriends, getPending, setPending, setReceived, appDispatch)}><CheckIcon/></span>
                            <span style={{cursor: "pointer", padding: '5px'}} onClick={() => declineRequest(req.id, false, token, getFriends, setFriends, getPending, setPending, setReceived, appDispatch)}><XIcon/></span>
                            <span style={{cursor: "pointer", padding: '5px'}} onClick={() => declineRequest(req.id, true, token, getFriends, setFriends, getPending, setPending, setReceived, appDispatch)}><CircleSlashIcon/></span>
                        </li>);}) :
                    <p style={{margin: "10px"}}>{myfriends.noPendingRequest}</p>}

                <h2 style={{margin: "10px"}}>{myfriends.myRequests}</h2>
                {pending.length > 0 ?
                    pending.map((req: any, i: any) => { return (
                        <li key={i + 'initiated' + req.otherUser.name } style={{margin: "10px"}}>
                            {req.otherUser.name}
                            {' '}
                            <span style={{cursor: "pointer"}} onClick={() => cancelRequest(req.id, token, getFriends, setFriends, getPending, setPending, setReceived, appDispatch)}><XIcon/></span>
                        </li>);}) :
                    <p style={{margin: "10px"}}>{myfriends.allRequestsAccepted}</p>}

                <Modal isOpen={show} toggle={handleClose}>
                    <ModalHeader toggle={handleClose}>{myfriends.addFriendModalTitle}</ModalHeader>
                    <ModalBody>
                        { errorMessage && <p className="auth-error">{errorMessage}</p> }
                        <Form inline onSubmit={onFormSubmit}>
                            <FormGroup className="mb-2 mr-sm-2 mb-sm-0">
                                <Label className="mr-sm-2">{myfriends.name}</Label>
                                <Input name="name" placeholder={myfriends.name}/>
                                <FormFeedback>{myfriends.nameErrorMessage}</FormFeedback>
                            </FormGroup>
                            <br/>
                            <Button color="primary">{myfriends.addModalButton}</Button>
                        </Form>
                    </ModalBody>
                </Modal>
            </div>
        </div>
        );
    } else {
        console.log("Unauthorized... Redirecting...")
        navigate('../signin')
        return (<div></div>);
    }
}

export default MyFriends;
