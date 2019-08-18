import React from 'react';
import { Modal, ModalHeader, ModalBody, Button, Input, Label, Form, FormGroup, FormFeedback } from "reactstrap";
import { Link } from 'react-router-dom';

import { connect } from 'react-redux';
import { AppState } from '../redux/store';

import Octicon, {Check, X, CircleSlash, ListUnordered} from '@primer/octicons-react'

import { MyFriendsMessage } from '../translation/itrans';
import './friends.css'

class Friend {
    reqId: number;
    name: string;

    constructor(reqId: number, name: string) {
        this.reqId = reqId;
        this.name = name;
    }
}

interface Props { userId: number | null, myfriends: MyFriendsMessage }
interface State {
  initiatedRequests: any[],
  receivedRequests: any[],
  show: boolean, title: string, bodyRender: any, button: { text: string, fun: any }, inputs: any, errorMessage: string,
  hoverId: string
}

class MyFriends extends React.Component<Props, State> {
    constructor(props: Props) {
        super(props);

        this.openAddFriend = this.openAddFriend.bind(this);
        this.addFriend = this.addFriend.bind(this);
        this.friendBodyRender = this.friendBodyRender.bind(this);

        this.closeModal = this.closeModal.bind(this);

        this.state = {
            initiatedRequests: [],
            receivedRequests: [],
            show: false, title: '', bodyRender: undefined, button: { text: '', fun: undefined }, inputs: { }, errorMessage: '',
            hoverId: ''
        }
    }

    componentDidMount() {
        if (this.props.userId) {
            this.getInitiated(this.props.userId);
            this.getReceived(this.props.userId); //this one should be scheduled
        }
    }

    //Hanle loggin, seems weird
    componentWillReceiveProps(nextProps: Props, nextContext: any) {
        if (nextProps.userId) {
            this.getInitiated(nextProps.userId);
            this.getReceived(nextProps.userId);
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

    async getInitiated(userId: number) {
        const response = await fetch('http://localhost:8080/users/' + userId + '/friend-requests/sent');
        const json = await response.json();
        if (response.status === 200) {
            this.setState({ initiatedRequests: json });
        } else {
            console.log(json.error);
        }
    };

    async getReceived(userId: number) {
        const response = await fetch('http://localhost:8080/users/' + userId + '/friend-requests/received');
        const json = await response.json();
        if (response.status === 200) {
            this.setState({ receivedRequests: json });
        } else {
            console.log(json.error);
        }
    };

    generateMessage(json: any, friendName: string): string {
        console.log(json);
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
            const response = await fetch('http://localhost:8080/users/' + this.props.userId + '/friend-requests', {
                    method: 'put',
                    headers: {'Content-Type':'application/json'},
                    body: JSON.stringify({"name": name})
                });
            if (response.status === 200) {
                this.setState({ show: false });
                this.props.userId !== null && this.getInitiated(this.props.userId);
            } else {
                const json = await response.json();
                this.setState({ show: true, errorMessage: this.generateMessage(json, name) });
            }
        };
        request();
    }

    cancelRequest(id: number) {
        const request = async () => {
            const response = await fetch('http://localhost:8080/users/' + this.props.userId + '/friend-requests/' + id, {method: 'delete'});
            if (response.status === 202) {
                this.props.userId && this.getInitiated(this.props.userId);
            } else {
                const json = await response.json();
                console.log(json);
            }
        };
        request();
    };

    acceptRequest(id: number ) {
        const request = async () => {
            const response = await fetch('http://localhost:8080/users/' + this.props.userId + '/friend-requests/' + id + '/accept');
            if (response.status === 202) {
                this.props.userId && this.getReceived(this.props.userId);
            } else {
                const json = await response.json();
                console.log(json);
            }
        };
        request();
    };

    declineRequest(id: number, blockUser: boolean) {
        const request = async () => {
            console.log('http://localhost:8080/users/' + this.props.userId + '/friend-requests/' + id + '/decline?blockUser=' + blockUser)
            const response = await fetch('http://localhost:8080/users/' + this.props.userId + '/friend-requests/' + id + '/decline?blockUser=' + blockUser, {method:"post"});
            if (response.status === 202) {
                this.props.userId && this.getReceived(this.props.userId);
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

    renderRequests() {
        let friends: Friend[] = [];
        let initiated = [];
        let received = [];
        if (this.state.initiatedRequests.length > 0) {
            const ok = this.state.initiatedRequests.filter(i => i.status === "ACCEPTED").map(i => new Friend(i.id, i.to.name));
            if (ok.length > 0) friends = ok;
            initiated = this.state.initiatedRequests.filter(i => i.status === "PENDING");
        }
        if (this.state.receivedRequests.length > 0) {
            const ok = this.state.receivedRequests.filter(i => i.status === "ACCEPTED").map(i => new Friend(i.id, i.from.name));
            if (ok.length > 0) friends = ok.concat(friends);
            received = this.state.receivedRequests.filter(i => i.status === "PENDING");
        }

        const { myfriends } = this.props;

        return (<>
            <h2>{myfriends.requests}</h2>
            {received.length > 0 ?
              received.map((req, i) => { return (
                <li key={i + 'received' + req.to.name }>
                    {req.from.name}
                    {' '}
                    {req.status}
                    {' '}
                    <span style={{cursor: "pointer"}} onClick={() => this.acceptRequest(req.id)}><Octicon icon={Check}/></span>
                    {' '}
                    <span style={{cursor: "pointer"}} onClick={() => this.declineRequest(req.id, false)}><Octicon icon={X}/></span>
                    {' '}
                    <span style={{cursor: "pointer"}} onClick={() => this.declineRequest(req.id, true)}><Octicon icon={CircleSlash}/></span>
                </li>);}) :
              <span>{myfriends.noPendingRequest}</span>}

            <h2>{myfriends.myRequests}</h2>
            {initiated.length > 0 ?
              initiated.map((req, i) => { return (
                <li key={i + 'initiated' + req.to.name }>
                    {req.to.name}
                    {' '}
                    {req.status}
                    {' '}
                    <span style={{cursor: "pointer"}} onClick={() => this.cancelRequest(req.id)}><Octicon icon={X}/></span>
                </li>);}) :
              <span>{myfriends.allRequestsAccepted}</span>}

            <h2>{myfriends.friends}</h2>
            {friends.map((req, i) => {
              if (i.toString() === this.state.hoverId) {
                return (
                  <div key={i + 'friends-' + req.reqId} className="friend-card" onMouseEnter={() => this.handleEnter(i)} onMouseLeave={() => this.handleOut()}>
                    <div className="friend-image">No image</div>
                    <div className="friend-card-delete" >
                      <Link to={'/friend/' + req.name} className="btn btn-link"><Octicon icon={ListUnordered}/></Link>
                      <span style={{cursor: "pointer"}} onClick={() => this.cancelRequest(req.reqId)}><Octicon icon={X}/></span>
                    </div>
                    <div className="friend-footer">
                      <div className="friend-name">{req.name}</div>
                    </div>
                  </div>);
              } else {
                return (
                  <div key={i + 'friends-' + req.reqId} className="friend-card" onMouseEnter={() => this.handleEnter(i)} onMouseLeave={() => this.handleOut()}>
                    <div className="friend-image">No image</div>
                    <div className="friend-footer">
                      <div className="friend-name">{req.name}</div>
                    </div>
                  </div>);
              }})}
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
