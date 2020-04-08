import React from 'react';
import { Modal, ModalHeader, ModalBody, Button, Input, Label, Form, FormGroup, FormFeedback } from "reactstrap";
import { Link } from 'react-router-dom';

import { connect } from 'react-redux';
import { AppState } from '../redux/store';

import Octicon, {Check, X, CircleSlash} from '@primer/octicons-react';

import { MyFriendsMessage } from '../translation/itrans';
import './style/friends.css';

import SquareImage from './SquareImage';
import blank_profile_picture from './image/blank_profile_picture.png';

import { isMobile } from "react-device-detect";

import { getServerUrl } from "../ServerInformation";
let url = getServerUrl();


interface Props { token: string | null, myfriends: MyFriendsMessage };
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
        if (this.props.token) {
            this.getPending(this.props.token);
            this.getFriends(this.props.token); //this one should be scheduled
        }
    }

    //Hanle loggin, seems weird
    componentWillReceiveProps(nextProps: Props, nextContext: any) {
        if (nextProps.token) {
            this.getPending(nextProps.token);
            this.getFriends(nextProps.token);
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

    async getPending(token: string) {
        const response = await fetch(url + '/friend-requests/pending', {headers: {'Authorization': `Bearer ${token}`}});
        if (response.status === 200) {
            const json = await response.json();
            this.setState({ pendingSent: json.sent, pendingReceived: json.received });
        } else if (response.status === 401) {
            console.error("Unauthorized. Disconnect and redirect to connect");
        } else {
            const json = await response.json();
            console.log(json.error);
        }
    };

    async getFriends(token: string) {
        const response = await fetch(url + '/friends', {headers: {'Authorization': `Bearer ${token}`}});
        if (response.status === 200) {
            const json = await response.json();
            this.setState({ friends: json });
        } else if (response.status === 401) {
            console.error("Unauthorized. Disconnect and redirect to connect");
        } else {
            const json = await response.json();
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
            const response = await fetch(url + '/friend-requests', {
                    method: 'put',
                    headers: {'Content-Type':'application/json', 'Authorization': `Bearer ${this.props.token}`},
                    body: JSON.stringify({"name": name})
                });
            if (response.status === 200) {
                this.setState({ show: false });
                this.props.token !== null && this.getPending(this.props.token);
            } else if (response.status === 401) {
                console.error("Unauthorized. Disconnect and redirect to connect");
            } else {
                const json = await response.json();
                const errorMessage = (response.status === 409) ? this.generateMessage(json, name) : json.error;
                this.setState({ show: true, errorMessage: errorMessage });
            }
        };
        request();
    }

    cancelRequest(id: number) {
        const request = async () => {
            const response = await fetch(url + '/friend-requests/' + id, {method: 'delete', headers: {'Authorization': `Bearer ${this.props.token}`}});
            if (response.status === 202) {
                if (this.props.token !== null) {
                    this.getPending(this.props.token);
                    this.getFriends(this.props.token);
                }
            } else if (response.status === 401) {
                console.error("Unauthorized. Disconnect and redirect to connect");
            } else {
                const json = await response.json();
                console.error(json);
            }
        };
        request();
    };

    acceptRequest(id: number ) {
        const request = async () => {
            const response = await fetch(url + '/friend-requests/' + id + '/accept', {headers: {'Authorization': `Bearer ${this.props.token}`}});
            if (response.status === 202) {
                if (this.props.token !== null) {
                    this.getPending(this.props.token);
                    this.getFriends(this.props.token);
                }
            } else if (response.status === 401) {
                console.error("Unauthorized. Disconnect and redirect to connect");
            } else {
                const json = await response.json();
                console.error(json);
            }
        };
        request();
    };

    declineRequest(id: number, blockUser: boolean) {
        const request = async () => {
            const response = await fetch(url + '/friend-requests/' + id + '/decline?blockUser=' + blockUser, {method:"post", headers: {'Authorization': `Bearer ${this.props.token}`}});
            if (response.status === 202) {
                this.props.token && this.getPending(this.props.token);
            } else if (response.status === 401) {
                console.error("Unauthorized. Disconnect and redirect to connect");
            } else {
                const json = await response.json();
                console.error(json);
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

    _renderInsideFriend(i: number, req: any, user: any) {
        if ((i.toString() === this.state.hoverId) || isMobile) {
            return (<>
                <div className="friend-card-delete one-icon" >
                    <span style={{cursor: "pointer"}} onClick={() => this.cancelRequest(req.id)}><Octicon icon={X}/></span>
                </div>
                <div className="friend-footer">
                    <Link to={'/friend/' + user.name} className="friend-name">{user.name}</Link>
                </div>
            </>);
        } else {
            return (
                <div className="friend-footer">
                    <div className="friend-name">{user.name}</div>
                </div>);
        }
    }

    renderRequests() {
        const { pendingSent, pendingReceived, friends } = this.state;
        const { myfriends } = this.props;

        return (<>
            <h2 style={{margin: "10px"}}>{myfriends.friends}</h2>
            <div className="mycard-row">
                {friends.map((req, i) => {
                    const user = req.otherUser;
                    let image = <SquareImage token={this.props.token} className="profile-image" imageName={user.picture} size={150} alt="Profile" alternateImage={blank_profile_picture}/>;
                    return (
                        <div key={i + 'friends-' + req.id} className="friend-card" onMouseEnter={() => this.handleEnter(i)} onMouseLeave={() => this.handleOut()}>
                            <Link to={'/friend/' + user.name}>{image}</Link>
                            {this._renderInsideFriend(i, req, user)}
                        </div>);
                })}
            </div>

            <h2 style={{margin: "10px"}}>{myfriends.requests}</h2>
            {pendingReceived.length > 0 ?
                pendingReceived.map((req, i) => { return (
                    <li key={i + 'received' + req.otherUser.name  } style={{margin: "10px"}}>
                        {req.otherUser.name}
                        <span style={{cursor: "pointer", padding: '5px'}} onClick={() => this.acceptRequest(req.id)}><Octicon icon={Check}/></span>
                        <span style={{cursor: "pointer", padding: '5px'}} onClick={() => this.declineRequest(req.id, false)}><Octicon icon={X}/></span>
                        <span style={{cursor: "pointer", padding: '5px'}} onClick={() => this.declineRequest(req.id, true)}><Octicon icon={CircleSlash}/></span>
                    </li>);}) :
                <p style={{margin: "10px"}}>{myfriends.noPendingRequest}</p>}

            <h2 style={{margin: "10px"}}>{myfriends.myRequests}</h2>
            {pendingSent.length > 0 ?
                pendingSent.map((req, i) => { return (
                    <li key={i + 'initiated' + req.otherUser.name } style={{margin: "10px"}}>
                        {req.otherUser.name}
                        {' '}
                        <span style={{cursor: "pointer"}} onClick={() => this.cancelRequest(req.id)}><Octicon icon={X}/></span>
                    </li>);}) :
                <p style={{margin: "10px"}}>{myfriends.allRequestsAccepted}</p>}
        </>);
    }

    render() {
        let modalBody = [];
        if (this.state.bodyRender !== undefined) {
            modalBody.push(this.state.bodyRender());
        }
        return (<div>
            <div className="main-friend">
              {this.props.token && <>
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

function mapStateToProps(state: AppState): Props {return { token: state.signin.token, myfriends: state.locale.messages.myfriends };}
export default connect(mapStateToProps)(MyFriends);
