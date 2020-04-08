import React from 'react';
import { Modal, ModalHeader, ModalBody, Button, Input, Label, FormGroup, FormFeedback } from "reactstrap";
import { Link } from 'react-router-dom';

import { connect } from 'react-redux';
import { AppState } from '../redux/store';

import Octicon, {Check, X, CircleSlash, ListUnordered} from '@primer/octicons-react';

import { MyEventsMessage } from '../translation/itrans';
import "react-datepicker/dist/react-datepicker.css";

import './style/events.css';

import { isMobile } from "react-device-detect";

import { getServerUrl } from "../ServerInformation";
let url = getServerUrl();


interface Props { token: string | null, username: String | null, myevents: MyEventsMessage };
interface State {
  eventsCreated: any[],
  eventsAsParticipant: any[],
  testDate: any,
  show: boolean, title: string, bodyRender: any, button: { text: string, fun: any }, inputs: any, errorMessage: string,
  hoverId: string
};

class Events extends React.Component<Props, State> {
    constructor(props: Props) {
        super(props);

        this.state = {
            eventsCreated: [],
            eventsAsParticipant: [],
            testDate: Date.now(),
            show: false, title: '', bodyRender: undefined, button: { text: '', fun: undefined }, inputs: { }, errorMessage: '',
            hoverId: ''
        }

        this.closeModal = this.closeModal.bind(this);
    }

    componentDidMount() {
        if (this.props.token) {
            this.getEventCreated(this.props.token);
            this.getEventAsParticipant(this.props.token); //this one should be scheduled
        }
    }

    //Hanle loggin, seems weird
    componentWillReceiveProps(nextProps: Props, nextContext: any) {
        if (nextProps.token) {
            this.getEventCreated(nextProps.token);
            this.getEventAsParticipant(nextProps.token);
        }
    }

    openAddEvent() {
        const { myevents } = this.props;
        this.setState( { show: true, title: myevents.createEventModalTitle, bodyRender: () => this.eventBodyRender(),
            button: { text: myevents.createEventModalButton, fun: () => this.createEvent() },
            inputs: { type: 'ALL_FOR_ALL', name: '', nameValidity: true, description: null, endDate: '', endDateValidity: true, target: '', targetValidity: true }, errorMessage: '' });
    }

    handleChangeType = async(event: any) => {
        const { inputs } = this.state;
        const { name, value } = event.target;
        inputs[name] = value;
        await this.setState({ inputs: inputs });
    }

    static stringIsNumber(s: string, size: number) : boolean {
        return (s !== null && s !== undefined && s.length === size && !Number.isNaN(Number(s)));
    }

    handleChange = async (event: any) => {
        const { inputs } = this.state;
        const { name, value } = event.target;
        inputs[name] = value;
        if (name === "name") {
            inputs["nameValidity"] = value.length > 0;
        } else if (name === "target") {
            inputs["targetValidity"] = value.length > 0;
        } else if (name === "endDate") {
            //Show only invalid if number of char >= length(dd/mm/yyyy)
            //Will be easier with a date picker
            //Lot of limitation like 1/2/2012 in not ok, and date like 45/56/7900 are ok. But don't want to spend time here
            //if I go in date picker
            const splitted = value.split("/");
            inputs["endDateValidity"] = (value.length < 10) || (Events.stringIsNumber(splitted[0], 2) && Events.stringIsNumber(splitted[1], 2) && Events.stringIsNumber(splitted[2], 4));
        }

        await this.setState({ inputs: inputs });
    };

    handleChangeDate = async (event: any) => {
        const { inputs } = this.state;
        inputs["endDate"] = event;
        await this.setState({ inputs: inputs });
    }

    eventBodyRender() {
        const { myevents } = this.props;
        return (<>
            <FormGroup tag="fieldset">
                <FormGroup check inline>
                    <Label check>
                      <Input type="radio" name="type" value="ALL_FOR_ALL" onChange={(e) => this.handleChange(e)} required defaultChecked/>All for All
                    </Label>
                </FormGroup>
                <FormGroup check inline>
                    <Label check>
                      <Input type="radio" name="type" value="ALL_FOR_ONE" onChange={(e) => this.handleChange(e)} required/>One For All
                    </Label>
                </FormGroup>
            </FormGroup>
            <FormGroup>
                <Label>{myevents.name}</Label>
                <Input name="name" placeholder={myevents.name} value={this.state.inputs.name} invalid={!this.state.inputs.nameValidity} onChange={(e) => this.handleChange(e)}/>
                <FormFeedback>{myevents.nameErrorMessage}</FormFeedback>
            </FormGroup>
            <FormGroup>
                <Label>{myevents.description}</Label>
                <Input name="description" placeholder={myevents.description} value={this.state.inputs.description} onChange={(e) => this.handleChange(e)}/>
            </FormGroup>
            <FormGroup>
                <Label>{myevents.endDate}</Label>
                <Input name="endDate" placeholder={myevents.endDatePlaceholder} value={this.state.inputs.endDate} invalid={!this.state.inputs.endDateValidity} onChange={(e) => this.handleChange(e)}/>
                <FormFeedback>{myevents.endDateErrorMessage}</FormFeedback>
            </FormGroup>
            {this.state.inputs.type === "ALL_FOR_ONE" && <FormGroup>
                <Label>{myevents.target}</Label>
                <Input name="target" placeholder={myevents.target} value={this.state.inputs.target} invalid={!this.state.inputs.targetValidity} onChange={(e) => this.handleChange(e)}/>
                <FormFeedback>{myevents.targetErrorMessage}</FormFeedback>
            </FormGroup>}
            </>);
    }

    closeModal() {
        this.setState({ show: false });
    }

    async getEventCreated(token: string) {
        const response = await fetch(url + '/events', {headers: {'Content-Type':'application/json', 'Authorization': `Bearer ${token}`}});
        if (response.status === 200) {
            const json = await response.json();
            this.setState({ eventsCreated: json });
        } else if (response.status === 401) {
            console.error("Unauthorized. Disconnect and redirect to connect");
        } else {
            const json = await response.json();
            console.error(json.error);
        }
    };

    async getEventAsParticipant(token: string) {
        const response = await fetch(url + '/events-as-participant', {headers: {'Content-Type':'application/json', 'Authorization': `Bearer ${token}`}});
        if (response.status === 200) {
            const json = await response.json();
            this.setState({ eventsAsParticipant: json });
        } else if (response.status === 401) {
            console.error("Unauthorized. Disconnect and redirect to connect");
        } else {
            const json = await response.json();
            console.error(json.error);
        }
    };

    createEvent() {
        const {type,name,description,endDate,target} = this.state.inputs;
        const splittedDate = endDate.split('/');

        let nameValidity = true;
        let endDateValidity = false;
        let targetValidity = true;
        if (name === '') { nameValidity = false; }
        if ((endDate.length < 10) || (Events.stringIsNumber(splittedDate[0], 2) && Events.stringIsNumber(splittedDate[1], 2) && Events.stringIsNumber(splittedDate[2], 4))) { endDateValidity = true; }
        if (type === "ALL_FOR_ONE" && target === '') { targetValidity = false; }
        if (nameValidity === false || endDateValidity === false || targetValidity === false) {
            this.setState({inputs: {nameValidity: nameValidity, endDateValidity: endDateValidity, targetValidity: targetValidity}});
            return;
        }

        let body: any = null;
        let endDateJson = JSON.stringify({"day":splittedDate[0], "month":splittedDate[1],"year": splittedDate[2]});
        if (type === "ALL_FOR_ALL") {
            body = JSON.stringify({
              "type": type,
              "name": name,
              "description": description,
              "endDate": {"day":splittedDate[0], "month":splittedDate[1],"year": splittedDate[2]}
            });
        } else {
            body = JSON.stringify({
              "type": type,
              "name": name,
              "description": description,
              "endDate": endDateJson,
              "target": target
            });
        }

        const request = async () => {
            const response = await fetch(url + '/events', {
                    method: 'put',
                    headers: {'Content-Type':'application/json', 'Authorization': `Bearer ${this.props.token}`},
                    body: body
                });
            if (response.status === 202) {
                this.setState({ show: false });
                this.props.token !== null && this.getEventCreated(this.props.token);
                this.props.token !== null && this.getEventAsParticipant(this.props.token);
            } else if (response.status === 401) {
                console.error("Unauthorized. Disconnect and redirect to connect");
            } else {
                const json = await response.json();
                this.setState({ show: true, errorMessage: json.error});
            }
        };
        request();
    }

    deleteEvent(id: number) {
        const request = async () => {
            const response = await fetch(url + '/events/' + id, {method: 'delete', headers: {'Authorization': `Bearer ${this.props.token}`}});
            if (response.status === 202) {
                this.props.token !== null && this.getEventCreated(this.props.token);
                this.props.token !== null && this.getEventAsParticipant(this.props.token);
            } else if (response.status === 401) {
                console.error("Unauthorized. Disconnect and redirect to connect");
            } else {
                const json = await response.json();
                console.error(json);
            }
        };
        request();
    };

    acceptRequest(id: number) {
        const request = async () => {
            const response = await fetch(url + '/events/' + id + '/accept', {headers: {'Authorization': `Bearer ${this.props.token}`}});
            if (response.status === 202) {
                this.props.token !== null && this.getEventCreated(this.props.token);
                this.props.token !== null && this.getEventAsParticipant(this.props.token);
            } else if (response.status === 401) {
                console.error("Unauthorized. Disconnect and redirect to connect");
            } else {
                const json = await response.json();
                console.error(json);
            }
        };
        request();
    };

    declineRequest(id: number, blockEvent: boolean) {
        const request = async () => {
            const response = await fetch(url + '/events/' + id + '/decline?blockEvent=' + blockEvent, {method:"post", headers: {'Authorization': `Bearer ${this.props.token}`}});
            if (response.status === 202) {
                this.props.token !== null && this.getEventCreated(this.props.token);
                this.props.token !== null && this.getEventAsParticipant(this.props.token);
            } else if (response.status === 401) {
                console.error("Unauthorized. Disconnect and redirect to connect");
            } else {
                const json = await response.json();
                console.error(json);
            }
        };
        request();
    };

    handleEnter(type: string, event: number) {
      this.setState({ hoverId: type + "-" + event});
    }

    handleOut() {
      this.setState({ hoverId: '' });
    }

    renderEvents() {
        let coming = [];
        let pending = [];
        if (this.state.eventsAsParticipant.length > 0) {
            const f = this.state.eventsAsParticipant.filter(i => i.participants.filter((p: any) => p.name === this.props.username)[0].status === "ACCEPTED");
            if (f.length > 0) coming = f;
            const g = this.state.eventsAsParticipant.filter(i => i.participants.filter((p: any) => p.name === this.props.username)[0].status === "PENDING");
            if (g.length > 0) pending = g;
        }

        const { myevents } = this.props;
        return (<>
            <h2 style={{margin: "10px"}}>{myevents.myEvents}</h2>
            <div className="events-card-row">
              {this.state.eventsCreated.map((e, i) => {
                  if (('my-'+i === this.state.hoverId) || isMobile) {
                    return (
                        <div className="events-card" onMouseEnter={() => this.handleEnter("my", i)} onMouseLeave={() => this.handleOut()}>
                            <div className="events-edit-close">
                              <Link to={'/event/' + e.id} className="btn btn-link"><Octicon icon={ListUnordered}/></Link>
                              <span style={{cursor: "pointer"}} onClick={() => this.deleteEvent(e.id)}><Octicon icon={X}/></span>
                            </div>
                            <div className="events-card-name">{e.name}</div>
                            <div className="events-card-description">{e.description}</div>
                            <div className="events-card-footer">
                              <div className="events-card-wtb">{e.endDate.day}/{e.endDate.month}/{e.endDate.year}</div>
                              <div className="events-card-price">{e.type === "ALL_FOR_ALL" ? "All" : e.target}</div>
                            </div>
                        </div>);
                    } else {
                      return (
                          <div className="events-card" onMouseEnter={() => this.handleEnter("my", i)} onMouseLeave={() => this.handleOut()}>
                                <div className="events-card-name-only">{e.name}</div>
                          </div>);
                    }
              })}
            </div>
            <h2 style={{margin: "10px"}}>{myevents.comingEvents}</h2>
            <div className="events-card-row">
              {coming.map((e, i) => {
                  if (('coming-'+i === this.state.hoverId) || isMobile) {
                    return (
                        <div className="events-card" onMouseEnter={() => this.handleEnter("coming", i)} onMouseLeave={() => this.handleOut()}>
                            <div className="events-edit-close">
                              <Link to={'/event/' + e.id} className="btn btn-link" style={{ textDecoration: 'none', color: 'black' }}><Octicon icon={ListUnordered}/></Link>
                            </div>
                            <div className="events-card-name">{e.name}</div>
                            <div className="events-card-description">{e.description}</div>
                            <div className="events-card-footer">
                              <div className="events-card-wtb">{e.endDate.day}/{e.endDate.month}/{e.endDate.year}</div>
                              <div className="events-card-price">{e.type === "ALL_FOR_ALL" ? "All" : e.target}</div>
                            </div>
                        </div>);
                    } else {
                      return (
                          <div className="events-card" onMouseEnter={() => this.handleEnter("coming", i)} onMouseLeave={() => this.handleOut()}>
                                <div className="events-card-name-only">{e.name}</div>
                          </div>);
                    }
              })}
            </div>
            <h2 style={{margin: "10px"}}>{myevents.pendingEvents}</h2>
            <div className="events-card-row">
              {pending.map((e, i) => {
                if (('my-'+i === this.state.hoverId) || isMobile) {
                  return (
                      <div className="events-card" onMouseEnter={() => this.handleEnter("my", i)} onMouseLeave={() => this.handleOut()}>
                          <div className="events-edit-close">
                            <span style={{cursor: "pointer"}} onClick={() => this.acceptRequest(e.id)}><Octicon icon={Check}/></span>
                            {' '}
                            <span style={{cursor: "pointer"}} onClick={() => this.declineRequest(e.id, false)}><Octicon icon={X}/></span>
                            {' '}
                            <span style={{cursor: "pointer"}} onClick={() => this.declineRequest(e.id, true)}><Octicon icon={CircleSlash}/></span>
                          </div>
                          <div className="events-card-name">{e.name}</div>
                          <div className="events-card-description">{e.description}</div>
                          <div className="events-card-footer">
                            <div className="events-card-wtb">{e.endDate.day}/{e.endDate.month}/{e.endDate.year}</div>
                            <div className="events-card-price">{e.type === "ALL_FOR_ALL" ? "All" : e.target}</div>
                          </div>
                      </div>);
                  } else {
                    return (
                        <div className="events-card" onMouseEnter={() => this.handleEnter("my", i)} onMouseLeave={() => this.handleOut()}>
                              <div className="events-card-name-only">{e.name}</div>
                        </div>);
                  }
              })}
            </div>
        </>);
    }

    render() {
        let modalBody = [];
        if (this.state.bodyRender !== undefined) {
            modalBody.push(this.state.bodyRender());
        }
        return (<div>
            {this.props.token && <>
                <Button color="link" onClick={() => this.openAddEvent()}>{this.props.myevents.createEventButton}</Button>
                {this.renderEvents()}
            </>}

            <Modal isOpen={this.state.show} toggle={this.closeModal}>
                <ModalHeader toggle={() => this.closeModal()}>{this.state.title}</ModalHeader>
                <ModalBody>
                    { this.state.errorMessage && <p className="auth-error">{this.state.errorMessage}</p> }
                    {modalBody}
                    <Button color="primary" onClick={this.state.button.fun}>{this.state.button.text}</Button>
                </ModalBody>
            </Modal>
        </div>
        );
    }
}

function mapStateToProps(state: AppState): Props {
  return { token: state.signin.token, username: state.signin.username, myevents: state.locale.messages.myevents };}
export default connect(mapStateToProps)(Events);
