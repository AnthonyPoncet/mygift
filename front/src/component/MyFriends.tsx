import React from 'react';
import { Modal, ModalHeader, ModalBody, Button, Input, Label, Form, FormGroup, FormFeedback } from "reactstrap";

import { connect } from 'react-redux';
import { AppState } from '../redux/store';

import Octicon, {Check, X, CircleSlash} from '@primer/octicons-react'


interface Props {
  userId: number | null
}
interface State {
  initiatedRequests: any[],
  receivedRequests: any[],
  show: boolean, title: string, bodyRender: any, button: { text: string, fun: any }, inputs: any, errorMessage: string
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
          show: false, title: '', bodyRender: undefined, button: { text: '', fun: undefined }, inputs: { }, errorMessage: ''
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
    this.setState( { show: true, title: "Add a new friend", bodyRender: this.friendBodyRender, button: { text: 'Add', fun: this.addFriend },
      inputs: { name: '', nameValidity: true }, errorMessage: '' });
  }

  handleChangeName = async (event: any) => {
      const { target } = event;
      const value = target.type === 'checkbox' ? target.checked : target.value;
      await this.setState({ inputs: { name: value, nameValidity: (value.length !== 0) } });
  };

  friendBodyRender() {
    return (<Form inline>
      <FormGroup className="mb-2 mr-sm-2 mb-sm-0">
        <Label className="mr-sm-2">Name</Label>
        <Input name="name" placeholder="name" value={this.state.inputs.name} invalid={!this.state.inputs.nameValidity} onChange={(e) => this.handleChangeName(e)}/>
        <FormFeedback>Name is mandatory</FormFeedback>
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
        const response = await fetch('http://localhost:8080/users/' + this.props.userId + '/friend-requests/' + id + '/accept',
        {method: 'get'});
        if (response.status === 202) {
            this.props.userId && this.getReceived(this.props.userId);
        } else {
            const json = await response.json();
            console.log(json);
        }
    };
    request();
  };

  declineRequest(id: number ) {
    const request = async () => {
        const response = await fetch('http://localhost:8080/users/' + this.props.userId + '/friend-requests/' + id + '/decline',
        {method: 'get'});
        if (response.status === 202) {
            this.props.userId && this.getReceived(this.props.userId);
        } else {
            const json = await response.json();
            console.log(json);
        }
    };
    request();
  };

  renderRequests() {
    let friendsI = [];
    let friendsR = [];
    let initiated = [];
    let received = [];
    if (this.state.initiatedRequests.length > 0) {
      const ok = this.state.initiatedRequests.filter(i => i.status === "ACCEPTED");
      if (ok.length > 0) friendsI = ok;
      initiated = this.state.initiatedRequests.filter(i => i.status !== "ACCEPTED");
    }
    if (this.state.receivedRequests.length > 0) {
      const ok = this.state.receivedRequests.filter(i => i.status === "ACCEPTED");
      if (ok.length > 0) friendsR = ok;
      received = this.state.receivedRequests.filter(i => i.status === "PENDING");
    }

    return (<>
      <h2>My requests</h2>
      {initiated.map((req, i) => { return (
            <li key={i + 'initiated' + req.to.name }>
                {req.to.name}
                {' '}
                {req.status}
                {' '}
                <span style={{cursor: "pointer"}} onClick={() => this.cancelRequest(req.id)}><Octicon icon={X}/></span>
            </li>);})}
      <h2>Received requests</h2>
      {received.map((req, i) => { return (
            <li key={i + 'received' + req.to.name }>
                {req.from.name}
                {' '}
                {req.status}
                {' '}
                <span style={{cursor: "pointer"}} onClick={() => this.acceptRequest(req.id)}><Octicon icon={Check}/></span>
                {' '}
                <span style={{cursor: "pointer"}} onClick={() => this.declineRequest(req.id)}><Octicon icon={X}/></span>
            </li>);})}
      <h2>Friends</h2>
      {friendsI.map((req, i) => { return (<li key={i + 'friendI' + req.to.name}>{req.from.name}</li>);})}
      {friendsR.map((req, i) => { return (<li key={i + 'friendR' + req.from.name}>{req.from.name}</li>);})}
    </>);
  }

  //Next:
  //  While decline, add possibility to block people (prevent to resent) = put declined in DB or remove inline
  //  Remove friend with prevent to re-add = decline just above
  //  Manage blocked people (possibility to unblock them)

  render() {
    let modalBody = [];
    if (this.state.bodyRender !== undefined) {
        modalBody.push(this.state.bodyRender());
    }
    return (
      <div>
          {this.props.userId && <>
            <Button color="link" onClick={this.openAddFriend}>Add Friend</Button>
            {this.renderRequests()}
            </>
          }

          <Modal isOpen={this.state.show} toggle={this.closeModal}>
              <ModalHeader toggle={this.closeModal}>{this.state.title}</ModalHeader>
              <ModalBody>
                  { this.state.errorMessage && <p className="auth-error">{this.state.errorMessage}</p> }
                  {modalBody}
              </ModalBody>
          </Modal>
      </div>
    );
  }
}

function mapStateToProps(state: AppState): Props {return { userId: state.signin.userId };}
export default connect(mapStateToProps)(MyFriends);
