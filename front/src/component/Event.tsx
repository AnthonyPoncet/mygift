import React from 'react';
import { withRouter } from 'react-router-dom';
import { RouteComponentProps } from "react-router";
import { Modal, ModalHeader, ModalBody, Button, Input, Label, FormGroup, FormFeedback } from "reactstrap";

import { connect } from 'react-redux';
import { AppState } from '../redux/store';

import FriendWishList from './FriendWishList';
import MyWishList from './MyWishList'

import './card-gift.css';

interface PathParam { eventId: string };
interface ConnectProps { userId: number | null, username: String | null };
interface Props extends RouteComponentProps<PathParam>, ConnectProps {}
interface State {
    event: any,
    participants: any[],
    selectedList: string | null,
    show: boolean, title: string, bodyRender: any, button: { text: string, fun: any }, inputs: any, errorMessage: string
}

class Event extends React.Component<Props, State> {
    private eventId: string;
    private friendNames: string[];

    constructor(props: Props) {
        super(props);
        this.eventId = this.props.match.params.eventId;
        this.friendNames = [];
        this.state = {event: null, participants: [], selectedList: null,
          show: false, title: '', bodyRender: null, button: { text: '', fun: null }, inputs: { }, errorMessage: ''};
    }

    componentDidMount() {
        if (this.props.userId) {
            this.getEvent(this.props.userId);
        }
    }

    openAddParticipant() {
        this.setState( { show: true, title: "Add a participant", bodyRender: () => this.modalRender(), button: { text: 'Add', fun: () => this.invite() },
            inputs: { name: '', nameValidity: true }, errorMessage: '' });
    }

    handleChange = async (event: any) => {
        await this.setState({ inputs: { name: event.target.value, nameValidity: event.target.value.length > 0 } });
    };

    modalRender() {
        return (
            <FormGroup>
                <Label>Name</Label>
                <Input name="name" placeholder="name" value={this.state.inputs.name} invalid={!this.state.inputs.nameValidity} onChange={(e) => this.handleChange(e)}/>
                <FormFeedback>Name is mandatory</FormFeedback>
            </FormGroup>);
    }

    closeModal() {
        this.setState({ show: false });
    }

    invite() {
        const {name} = this.state.inputs;
        if (name === '') {
            this.setState({inputs: {nameValidity: false}});
            return;
        }
        let participants: string[] = [];
        participants.push(name);
        const request = async () => {
          const response = await fetch('http://localhost:8080/users/' + this.props.userId + '/events/' + this.eventId + '/add-participants', {
                method: 'post',
                headers: {'Content-Type':'application/json'},
                body: JSON.stringify(participants)
            });
          if (response.status === 202) {
              if (this.props.userId !== null) this.getEvent(this.props.userId);
              this.setState({show: false});
          } else {
              const json = await response.json();
              console.log(json.error);
          }
        };
        request();
    }

    async getEvent(userId: number) {
        const response = await fetch('http://localhost:8080/users/' + userId + '/events/' + this.eventId);
        const json = await response.json();
        if (response.status === 200) {
            this.setState({ event: json, participants: json.participants });
        } else {
            console.log(json.error);
        }
    };

    async getFriend(userId: number) {
        let initiated = [];
        let received = [];
        let friends = [];

        {
          const response = await fetch('http://localhost:8080/users/' + userId + '/friend-requests/sent');
          const json = await response.json();
          if (response.status === 200) {
              initiated = json;
          } else {
              console.log(json.error);
              return;
          }
        }
        {
          const response = await fetch('http://localhost:8080/users/' + userId + '/friend-requests/received');
          const json = await response.json();
          if (response.status === 200) {
              received = json;
          } else {
              console.log(json.error);
          }
        }

        if (initiated.length > 0) {
            const ok = initiated.filter((i: any) => i.status === "ACCEPTED").map((i: any) => i.name);
            if (ok.length > 0) friends = ok;
        }
        if (received.length > 0) {
            const ok = received.filter((i: any) => i.status === "ACCEPTED").map((i: any) => i.name);
            if (ok.length > 0) friends = ok.concat(friends);
        }
    };

    renderEvent() {
      let out: any[] = [];
      if (this.state.event) {
          const {event, participants, selectedList} = this.state;
          //TODO: once develop, remove from participant me
          out.push(<div>Description: {event.description}</div>);
          out.push(<div>Creator: {event.creatorName}</div>);
          out.push(<div>End date: {event.endDate.day}/{event.endDate.month}/{event.endDate.year}</div>);
          if (event.type === "ALL_FOR_ALL") {
              //Show participants + click on participant show list + icon if already bought something to someone
              out.push(<h5>Participants</h5>);
              out.push(<Button color="link" onClick={() => this.openAddParticipant()}>Add participant</Button>);
              let participantsOut = participants.map((p, pIndex) => {
                  return <div style={{cursor: "pointer"}} onClick={() => this.setState({selectedList: p.name})}>{p.name}</div>
              });
              out.push(participantsOut);

              if (selectedList === null || selectedList === this.props.username) {
                  out.push(<h5>My List</h5>);
                  out.push(<MyWishList/>)
              } else {
                  out.push(<h5>{selectedList} List</h5>);
                  out.push(<FriendWishList friendName={selectedList}/>)
              }
          } else {
              out.push(<h5>Target is {event.target}</h5>);
              if (event.target === this.props.username) {
                  out.push(<MyWishList/>)
              } else {
                  out.push(<FriendWishList friendName={event.target}/>)
              }
          }
      }

      return (<div>{out}</div>);
    }

    render() {
        const {button} = this.state;
        let modalBody = [];
        if (this.state.bodyRender !== null) {
            modalBody.push(this.state.bodyRender());
        }

        return (
        <div>
          <h1 className="friend-wishlist-title">{this.state.event !== null ? this.state.event.name : 'Unknown event, should not happen'}</h1>
          <div>{this.renderEvent()}</div>

          <Modal isOpen={this.state.show} toggle={this.closeModal}>
              <ModalHeader toggle={this.closeModal}>{this.state.title}</ModalHeader>
              <ModalBody>
                  { this.state.errorMessage && <p className="auth-error">{this.state.errorMessage}</p> }
                  {modalBody}
                  <Button color="primary" onClick={button.fun}>{button.text}</Button>
              </ModalBody>
          </Modal>
        </div>);
    }
}

function mapStateToProps(state: AppState): ConnectProps {return { userId: state.signin.userId, username: state.signin.username };}
export default withRouter(connect(mapStateToProps)(Event));
