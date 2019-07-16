import React from 'react';
import { Modal, ModalHeader, ModalBody, Button, Input, Label, FormGroup } from "reactstrap";

import { connect } from 'react-redux';
import { AppState } from '../redux/store';

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
      inputs: { name: '' }, errorMessage: '' });
  }

  handleChangeName = async (event: any) => {
      const { target } = event;
      const value = target.type === 'checkbox' ? target.checked : target.value;
      await this.setState({ inputs: { name: value } });
  };

  friendBodyRender() {
    return (<FormGroup>
        <Label>Name</Label>
        <Input name="name" placeholder="name" value={this.state.inputs.name} onChange={(e) => this.handleChangeName(e)}/>
    </FormGroup>);
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

  addFriend() {
      let errorMessage = '';
      const {name} = this.state.inputs;
      if (name === undefined || name === '') {
          errorMessage = "Name is mandatory";
      }

      if (errorMessage === '') {
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
                  this.setState({ show: true, errorMessage: json.error });
              }
          };
          request();
      } else {
          this.setState({ show: true, errorMessage: errorMessage })
      }
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
                <img key={i + '_del_img'} className="d-inline" src="cross.png" alt="Delete" width="21" height="21" onClick={() => this.cancelRequest(req.id)}/>
            </li>);})}
      <h2>Received requests</h2>
      {received.map((req, i) => { return (
            <li key={i + 'received' + req.to.name }>
                {req.from.name}
                {' '}
                {req.status}
                {' '}
                <img key={i + '_accept_img'} className="d-inline" src="accept.png" alt="Accept" width="21" height="21" onClick={() => this.acceptRequest(req.id)}/>
                {' '}
                <img key={i + '_decline_img'} className="d-inline" src="cross.png" alt="Decline" width="21" height="21" onClick={() => this.declineRequest(req.id)}/>
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
    const {button} = this.state;
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
                  {modalBody}
                  <Button color="primary" onClick={button.fun}>{button.text}</Button>{' '} {this.state.errorMessage}
              </ModalBody>
          </Modal>
      </div>
    );
  }
}

function mapStateToProps(state: AppState): Props {return { userId: state.signin.userId };}
export default connect(mapStateToProps)(MyFriends);
