import React from 'react';
import { withRouter } from 'react-router-dom';
import { RouteComponentProps } from "react-router";

import Octicon, {Heart, Checklist, Gift} from '@primer/octicons-react'

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
    selectedList: string | null
}

class Event extends React.Component<Props, State> {
    private eventId: string;
    private friendNames: string[];

    constructor(props: Props) {
        super(props);
        this.eventId = this.props.match.params.eventId;
        this.friendNames = [];
        this.state = {event: null, participants: [], selectedList: null};
    }

    componentDidMount() {
        if (this.props.userId) {
            this.getEvent(this.props.userId);
        }
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
        return (
        <div>
          <h1 className="friend-wishlist-title">{this.state.event !== null ? this.state.event.name : 'Unknown event, should not happen'}</h1>
          <div>{this.renderEvent()}</div>
        </div>);
    }
}

function mapStateToProps(state: AppState): ConnectProps {return { userId: state.signin.userId, username: state.signin.username };}
export default withRouter(connect(mapStateToProps)(Event));
