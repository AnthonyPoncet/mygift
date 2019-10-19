import React from 'react';
import { withRouter } from 'react-router-dom';
import { RouteComponentProps } from "react-router";
import { Modal, ModalHeader, ModalBody, Button, Input, Label, FormGroup, FormFeedback } from "reactstrap";

import { connect } from 'react-redux';
import { AppState } from '../redux/store';

import Octicon, {Plus} from '@primer/octicons-react';

import FriendWishList from './FriendWishList';
import MyWishList from './MyWishList'

import { EventMessage } from '../translation/itrans';
import './event.css';
import blank_profile_picture from './blank_profile_picture.png';

import { getServerUrl } from "../ServerInformation";
let url = getServerUrl();


interface PathParam { eventId: string };
interface ConnectProps { userId: number | null, username: String | null, eventM: EventMessage };
interface Props extends RouteComponentProps<PathParam>, ConnectProps {};
interface State {
    event: any,
    participants: any[],
    selectedList: string | null,
    show: boolean, title: string, bodyRender: any, button: { text: string, fun: any }, inputs: any, errorMessage: string
};

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
        const { eventM } = this.props;
        this.setState( { show: true, title: eventM.addParticipantModalTitle, bodyRender: () => this.modalRender(),
            button: { text: eventM.addParticipantModalButton, fun: () => this.invite() },
            inputs: { name: '', nameValidity: true }, errorMessage: '' });
    }

    handleChange = async (event: any) => {
        await this.setState({ inputs: { name: event.target.value, nameValidity: event.target.value.length > 0 } });
    };

    modalRender() {
        const { eventM } = this.props;
        return (
            <FormGroup>
                <Label>{eventM.name}</Label>
                <Input name="name" placeholder={eventM.name} value={this.state.inputs.name} invalid={!this.state.inputs.nameValidity} onChange={(e) => this.handleChange(e)}/>
                <FormFeedback>{eventM.nameErrorMessage}</FormFeedback>
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
          const response = await fetch(url + '/users/' + this.props.userId + '/events/' + this.eventId + '/add-participants', {
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
        const response = await fetch(url + '/users/' + userId + '/events/' + this.eventId);
        const json = await response.json();
        if (response.status === 200) {
            this.setState({ event: json, participants: json.participants });
        } else {
            console.log(json.error);
        }
    };

    async getFriend(userId: number) {
        const response = await fetch(url + '/users/' + userId + '/friends');
        const json = await response.json();
        if (response.status === 200) {
            //this.setState({ friends: json });
            //json.forEach((friend: any) => {
            //    if (friend.picture !== undefined) {
            //        this.loadImage(friend.picture, friend.name + 'profile')
            //    }
            //});
        } else {
            console.log(json.error);
        }
    };

    renderEvent() {
      let out: any[] = [];
      const {eventM} = this.props;
      if (this.state.event) {
          const {event, participants, selectedList} = this.state;
          //TODO: once develop, remove from participant me
          out.push(<div className="event-title-and-date">
            <h1 className="event-title">{this.state.event !== null ? this.state.event.name : 'Unknown event, should not happen'}</h1>
            <div className="event-date"><div>{event.endDate.day}/{event.endDate.month}</div><div>{event.endDate.year}</div></div>
          </div>);
          out.push(<div className="event-creator">{eventM.creator} {event.creatorName}</div>);
          { event.description && out.push(<div>{eventM.description}: {event.description}</div>); }
          if (event.type === "ALL_FOR_ALL") {
              //Show participants + click on participant show list + icon if already bought something to someone
              out.push(<div className="event-participants">
                <h5 className="event-participants-title">{eventM.participantsTitle}</h5>
                <div className="event-participants-add" style={{cursor: "pointer"}} onClick={() => this.openAddParticipant()}><Octicon icon={Plus} verticalAlign="middle"/></div>
                <div className="event-card-row">
                { participants.map((p, pIndex) => {
                    return <div className="event-card" style={{cursor: "pointer"}} onClick={() => this.setState({selectedList: p.name})}>
                      <img className="event-image" src={blank_profile_picture} alt="Nothing"/>
                      <div className="event-name">{p.name}</div>
                    </div>
                })}
                </div>
              </div>);

              out.push(<hr/>);
              if (selectedList === null || selectedList === this.props.username) {
                  out.push(<h2 style={{margin: "10px"}}>My List</h2>);
                  out.push(<MyWishList/>);
              } else {
                  out.push(<FriendWishList friendName={selectedList}/>)
              }
          } else {
              out.push(<h5>{eventM.targetIsTitle} {event.target}</h5>);
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
          <div>{this.renderEvent()}</div>

          <Modal isOpen={this.state.show} toggle={() => this.closeModal()}>
              <ModalHeader toggle={() => this.closeModal()}>{this.state.title}</ModalHeader>
              <ModalBody>
                  { this.state.errorMessage && <p className="auth-error">{this.state.errorMessage}</p> }
                  {modalBody}
                  <Button color="primary" onClick={button.fun}>{button.text}</Button>
              </ModalBody>
          </Modal>
        </div>);
    }
}

function mapStateToProps(state: AppState): ConnectProps {
  return { userId: state.signin.userId, username: state.signin.username, eventM: state.locale.messages.event };}
export default withRouter(connect(mapStateToProps)(Event));
