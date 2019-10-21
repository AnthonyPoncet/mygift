import React from 'react';
import { Modal, ModalHeader, ModalBody, Button, Input, Label, Form, FormGroup, FormFeedback } from "reactstrap";
import { Link } from 'react-router-dom';

import { connect } from 'react-redux';
import { AppState } from '../redux/store';

import Octicon, {Check, X, CircleSlash, ListUnordered} from '@primer/octicons-react';

import { MyFriendsMessage } from '../translation/itrans';
import './friends.css';

import blank_profile_picture from './blank_profile_picture.png';

import { getServerUrl } from "../ServerInformation";
let url = getServerUrl();


interface Props { userId: number | null, myfriends: MyFriendsMessage };
interface State {
    pendingSent: any[],
    pendingReceived: any[],
    friends: any[],
    show: boolean, title: string, bodyRender: any, button: { text: string, fun: any }, inputs: any, errorMessage: string,
    hoverId: string
};

class MyFriends extends React.Component<Props, State> {
    constructor(props: Props) {
        super(props);

        this.openAddFriend = this.openAddFriend.bind(this);
        this.addFriend = this.addFriend.bind(this);
        this.friendBodyRender = this.friendBodyRender.bind(this);

        this.closeModal = this.closeModal.bind(this);

        this.state = {
            pendingSent: [],
            pendingReceived: [],
            friends: [],
            show: false, title: '', bodyRender: undefined, button: { text: '', fun: undefined }, inputs: { }, errorMessage: '',
            hoverId: ''
        }
    }

    componentDidMount() {
        if (this.props.userId) {
            this.getPending(this.props.userId);
            this.getFriends(this.props.userId); //this one should be scheduled
        }
    }

    //Hanle loggin, seems weird
    componentWillReceiveProps(nextProps: Props, nextContext: any) {
        if (nextProps.userId) {
            this.getPending(nextProps.userId);
            this.getFriends(nextProps.userId);
        }
    }

    openAddFriend() {
        const { myfriends } = this.props;
        this.setState( { show: true, title: myfriends.addFriendModalTitle, bodyRender: this.friendBodyRender, button: { text: myfriends.addModalButton, fun: this.addFriend },
            inputs: { name: '', nameValidity: true }, errorMessage: '' });
    }

    handleChangeName = async (event: any) => {
        const { target } = event;
        const value = target.type === 'checkbox' ? target.checked : target.value;
        await this.setState({ inputs: { name: target.value, nameValidity: (value.length !== 0) } });
    };

    friendBodyRender() {
        const { myfriends } = this.props;
        return (<Form inline>
            <FormGroup className="mb-2 mr-sm-2 mb-sm-0">
                <Label className="mr-sm-2">{myfriends.name}</Label>
                <Input name="name" placeholder={myfriends.name} value={this.state.inputs.name} invalid={!this.state.inputs.nameValidity} onChange={(e) => this.handleChangeName(e)}/>
                <FormFeedback>{myfriends.nameErrorMessage}</FormFeedback>
            </FormGroup>
            <Button color="primary" onClick={this.state.button.fun}>{this.state.button.text}</Button>
        </Form>);
    }

    closeModal() {
        this.setState({ show: false });
    }

    async getPending(userId: number) {
        const response = await fetch(url + '/users/' + userId + '/friend-requests/pending');
        const json = await response.json();
        if (response.status === 200) {
            this.setState({ pendingSent: json.sent, pendingReceived: json.received });
            json.sent.forEach((req: any) => {
                if (req.otherUser.picture !== undefined) {
                    this.loadImage(req.otherUser.picture, req.otherUser.name + 'profile')
                }
            });
            json.received.forEach((req: any) => {
                if (req.otherUser.picture !== undefined) {
                    this.loadImage(req.otherUser.picture, req.otherUser.name + 'profile')
                }
            });
        } else {
            console.log(json.error);
        }
    };

    async getFriends(userId: number) {
        const response = await fetch(url + '/users/' + userId + '/friends');
        const json = await response.json();
        if (response.status === 200) {
            this.setState({ friends: json });
            json.forEach((friend: any) => {
                if (friend.picture !== undefined) {
                    this.loadImage(friend.picture, friend.name + 'profile')
                }
            });
        } else {
            console.log(json.error);
        }
    };

    generateMessage(json: any, friendName: string): string {
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
    }

    addFriend() {
        const {name} = this.state.inputs;
        if (name === undefined || name === '') {
            this.setState({inputs: {nameValidity: false}});
            return;
        }

        const request = async () => {
            const response = await fetch(url + '/users/' + this.props.userId + '/friend-requests', {
                    method: 'put',
                    headers: {'Content-Type':'application/json'},
                    body: JSON.stringify({"name": name})
                });
            if (response.status === 200) {
                this.setState({ show: false });
                this.props.userId !== null && this.getPending(this.props.userId);
            } else {
                const json = await response.json();
                this.setState({ show: true, errorMessage: this.generateMessage(json, name) });
            }
        };
        request();
    }

    cancelRequest(id: number) {
        const request = async () => {
            const response = await fetch(url + '/users/' + this.props.userId + '/friend-requests/' + id, {method: 'delete'});
            if (response.status === 202) {
                this.props.userId && this.getPending(this.props.userId);
            } else {
                const json = await response.json();
                console.log(json);
            }
        };
        request();
    };

    acceptRequest(id: number ) {
        const request = async () => {
            const response = await fetch(url + '/users/' + this.props.userId + '/friend-requests/' + id + '/accept');
            if (response.status === 202) {
                this.props.userId && this.getFriends(this.props.userId);
            } else {
                const json = await response.json();
                console.log(json);
            }
        };
        request();
    };

    declineRequest(id: number, blockUser: boolean) {
        const request = async () => {
            const response = await fetch(url + '/users/' + this.props.userId + '/friend-requests/' + id + '/decline?blockUser=' + blockUser, {method:"post"});
            if (response.status === 202) {
                this.props.userId && this.getPending(this.props.userId);
                this.props.userId && this.getFriends(this.props.userId);
            } else {
                const json = await response.json();
                console.log(json);
            }
        };
        request();
    };

    handleEnter(index: number) {
      this.setState({ hoverId: index.toString()});
    }

    handleOut() {
      this.setState({ hoverId: '' });
    }

    loadImage(name: string, tagName: string) {
        const request = async() => {
            const response = await fetch(url + '/files/' + name);

            response.blob().then(blob => {
                let url = window.URL.createObjectURL(blob);
                let tag = document.querySelector('#' + tagName);
                if (tag instanceof HTMLImageElement) tag.src = url;
            });
        };
        request();
    }

    renderRequests() {
        const { pendingSent, pendingReceived, friends } = this.state;
        const { myfriends } = this.props;

        return (<>
            <h2 style={{margin: "10px"}}>{myfriends.requests}</h2>
            {pendingReceived.length > 0 ?
              pendingReceived.map((req, i) => { return (
                <li key={i + 'received' + req.otherUser.name  } style={{margin: "10px"}}>
                    {req.otherUser.name}
                    <span style={{cursor: "pointer"}} onClick={() => this.acceptRequest(req.id)}><Octicon icon={Check}/></span>
                    {' '}
                    <span style={{cursor: "pointer"}} onClick={() => this.declineRequest(req.id, false)}><Octicon icon={X}/></span>
                    {' '}
                    <span style={{cursor: "pointer"}} onClick={() => this.declineRequest(req.id, true)}><Octicon icon={CircleSlash}/></span>
                </li>);}) :
              <p style={{margin: "10px"}}>{myfriends.noPendingRequest}</p>}

            <h2 style={{margin: "10px"}}>{myfriends.myRequests}</h2>
            {pendingSent.length > 0 ?
              pendingSent.map((req, i) => { return (
                <li key={i + 'initiated' + req.otherUser.name } style={{margin: "10px"}}>
                    {req.otherUser.name}
                    <span style={{cursor: "pointer"}} onClick={() => this.cancelRequest(req.id)}><Octicon icon={X}/></span>
                </li>);}) :
              <p style={{margin: "10px"}}>{myfriends.allRequestsAccepted}</p>}

            <h2 style={{margin: "10px"}}>{myfriends.friends}</h2>
            <div className="mycard-row">
              {friends.map((req, i) => {
                let image = (req.picture === undefined) ?
                  <img className="friend-image" src={blank_profile_picture} alt="Nothing"/> :
                  <img className="friend-image" id={req.name+'profile'} alt="Profile"/>;
                if (i.toString() === this.state.hoverId) {
                  return (
                    <div key={i + 'friends-' + req.reqId} className="friend-card" onMouseEnter={() => this.handleEnter(i)} onMouseLeave={() => this.handleOut()}>
                      {image}
                      <div className="friend-card-delete" >
                        <Link to={'/friend/' + req.name} className="btn btn-link" style={{ textDecoration: 'none', color: 'black' }}><Octicon icon={ListUnordered}/></Link>
                        <span style={{cursor: "pointer"}} onClick={() => this.cancelRequest(req.reqId)}><Octicon icon={X}/></span>
                      </div>
                      <div className="friend-footer">
                        <div className="friend-name">{req.name}</div>
                      </div>
                    </div>);
                } else {
                  return (
                    <div key={i + 'friends-' + req.reqId} className="friend-card" onMouseEnter={() => this.handleEnter(i)} onMouseLeave={() => this.handleOut()}>
                      {image}
                      <div className="friend-footer">
                        <div className="friend-name">{req.name}</div>
                      </div>
                    </div>);
                }})}
              </div>
        </>);
    }

    render() {
        let modalBody = [];
        if (this.state.bodyRender !== undefined) {
            modalBody.push(this.state.bodyRender());
        }
        return (<div>
            <div className="main-friend">
              {this.props.userId && <>
                  <Button color="link" onClick={this.openAddFriend}>{this.props.myfriends.addFriendButton}</Button>
                  {this.renderRequests()}
              </>}

              <Modal isOpen={this.state.show} toggle={this.closeModal}>
                  <ModalHeader toggle={this.closeModal}>{this.state.title}</ModalHeader>
                  <ModalBody>
                      { this.state.errorMessage && <p className="auth-error">{this.state.errorMessage}</p> }
                      {modalBody}
                      </ModalBody>
              </Modal>
            </div>
        </div>
        );
    }
}

function mapStateToProps(state: AppState): Props {return { userId: state.signin.userId, myfriends: state.locale.messages.myfriends };}
export default connect(mapStateToProps)(MyFriends);
