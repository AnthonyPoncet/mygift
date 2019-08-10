import React from 'react';
import { Modal, ModalHeader, ModalBody, Button, Input, Label, FormGroup, FormFeedback } from "reactstrap";
import { Link } from 'react-router-dom';

import { connect } from 'react-redux';
import { AppState } from '../redux/store';

import Octicon, {Check, X, CircleSlash} from '@primer/octicons-react'

import "react-datepicker/dist/react-datepicker.css";

interface Props {userId: number | null, username: String | null}
interface State {
  eventsCreated: any[],
  eventsAsParticipant: any[],
  testDate: any,
  show: boolean, title: string, bodyRender: any, button: { text: string, fun: any }, inputs: any, errorMessage: string
}

class Events extends React.Component<Props, State> {
    constructor(props: Props) {
        super(props);

        this.state = {
            eventsCreated: [],
            eventsAsParticipant: [],
            testDate: Date.now(),
            show: false, title: '', bodyRender: undefined, button: { text: '', fun: undefined }, inputs: { }, errorMessage: ''
        }


        this.closeModal = this.closeModal.bind(this);
    }

    componentDidMount() {
        if (this.props.userId) {
            this.getEventCreated(this.props.userId);
            this.getEventAsParticipant(this.props.userId); //this one should be scheduled
        }
    }

    //Hanle loggin, seems weird
    componentWillReceiveProps(nextProps: Props, nextContext: any) {
        if (nextProps.userId) {
            this.getEventCreated(nextProps.userId);
            this.getEventAsParticipant(nextProps.userId);
        }
    }

    openAddEvent() {
        this.setState( { show: true, title: "Create a new event", bodyRender: () => this.eventBodyRender(), button: { text: 'Create', fun: () => this.createEvent() },
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
                <Label>Name</Label>
                <Input name="name" placeholder="name" value={this.state.inputs.name} invalid={!this.state.inputs.nameValidity} onChange={(e) => this.handleChange(e)}/>
                <FormFeedback>Name is mandatory</FormFeedback>
            </FormGroup>
            <FormGroup>
                <Label>Description</Label>
                <Input name="description" placeholder="description" value={this.state.inputs.description} onChange={(e) => this.handleChange(e)}/>
            </FormGroup>
            <FormGroup>
                <Label>End Date</Label>
                <Input name="endDate" placeholder="end date in format dd/mm/yyyy" value={this.state.inputs.endDate} invalid={!this.state.inputs.endDateValidity} onChange={(e) => this.handleChange(e)}/>
                <FormFeedback>End date is mandatory and its format is dd/mm/yyyy</FormFeedback>
            </FormGroup>
            {this.state.inputs.type === "ALL_FOR_ONE" && <FormGroup>
                <Label>Target</Label>
                <Input name="target" placeholder="target user" value={this.state.inputs.target} invalid={!this.state.inputs.targetValidity} onChange={(e) => this.handleChange(e)}/>
                <FormFeedback>Target is mandatory</FormFeedback>
            </FormGroup>}
            </>);
    }

    closeModal() {
        this.setState({ show: false });
    }

    async getEventCreated(userId: number) {
        const response = await fetch('http://localhost:8080/users/' + userId + '/events');
        const json = await response.json();
        if (response.status === 200) {
            this.setState({ eventsCreated: json });
        } else {
            console.log(json.error);
        }
    };

    async getEventAsParticipant(userId: number) {
        const response = await fetch('http://localhost:8080/users/' + userId + '/events-as-participant');
        const json = await response.json();
        if (response.status === 200) {
            this.setState({ eventsAsParticipant: json });
        } else {
            console.log(json.error);
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
        console.log(endDateJson);
        //let endDateJson = JSON.stringify({"year": endDate.split('/')});
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

        console.log(body);

        const request = async () => {
            const response = await fetch('http://localhost:8080/users/' + this.props.userId + '/events', {
                    method: 'put',
                    headers: {'Content-Type':'application/json'},
                    body: body
                });
            if (response.status === 202) {
                this.setState({ show: false });
                this.props.userId !== null && this.getEventCreated(this.props.userId);
                this.props.userId !== null && this.getEventAsParticipant(this.props.userId);
            } else {
                const json = await response.json();
                this.setState({ show: true, errorMessage: json.error});
            }
        };
        request();
    }

    deleteEvent(id: number) {
        const request = async () => {
            const response = await fetch('http://localhost:8080/users/' + this.props.userId + '/events/' + id, {method: 'delete'});
            if (response.status === 202) {
                this.props.userId !== null && this.getEventCreated(this.props.userId);
                this.props.userId !== null && this.getEventAsParticipant(this.props.userId);
            } else {
                const json = await response.json();
                console.log(json);
            }
        };
        request();
    };

    acceptRequest(id: number) {
        const request = async () => {
            const response = await fetch('http://localhost:8080/users/' + this.props.userId + '/events/' + id + '/accept');
            if (response.status === 202) {
                this.props.userId !== null && this.getEventCreated(this.props.userId);
                this.props.userId !== null && this.getEventAsParticipant(this.props.userId);
            } else {
                const json = await response.json();
                console.log(json);
            }
        };
        request();
    };

    declineRequest(id: number, blockEvent: boolean) {
        const request = async () => {
            const response = await fetch('http://localhost:8080/users/' + this.props.userId + '/events/' + id + '/decline?blockEvent=' + blockEvent, {method:"post"});
            if (response.status === 202) {
                this.props.userId !== null && this.getEventCreated(this.props.userId);
                this.props.userId !== null && this.getEventAsParticipant(this.props.userId);
            } else {
                const json = await response.json();
                console.log(json);
            }
        };
        request();
    };

    renderEvents() {
        let coming = [];
        let pending = [];
        if (this.state.eventsAsParticipant.length > 0) {
            console.log(this.state.eventsAsParticipant);
            const f = this.state.eventsAsParticipant.filter(i => i.participants.filter((p: any) => p.name === this.props.username)[0].status === "ACCEPTED");
            if (f.length > 0) coming = f;
            const g = this.state.eventsAsParticipant.filter(i => i.participants.filter((p: any) => p.name === this.props.username)[0].status === "PENDING");
            if (g.length > 0) pending = g;
        }

        return (<>
            <h2>My events</h2>
            {this.state.eventsCreated.map((e, i) => { return (
                <li key={i + 'created' + e.name }>
                    {'name: '}{e.name}{' - creator: '}{e.creatorName}{' - description: '}{e.description}{' - end date: '}{e.endDate.day}/{e.endDate.month}/{e.endDate.year}
                    {' '}
                    <Link to={'/event/' + e.id} className="btn btn-link">Show event</Link>
                    {' '}
                    <span style={{cursor: "pointer"}} onClick={() => this.deleteEvent(e.id)}><Octicon icon={X}/></span>
                </li>);})}
            <h2>Coming events</h2>
            {coming.map((e, i) => { return (
                <li key={i + 'participant' + e.name }>
                    {'name: '}{e.name}{' - creator: '}{e.creatorName}{' - description: '}{e.description}{' - end date: '}{e.endDate.day}/{e.endDate.month}/{e.endDate.year}
                    {' '}
                    <Link to={'/event/' + e.id} className="btn btn-link">Show event</Link>
                </li>);})}
            <h2>Pending answer events</h2>
            {pending.map((e, i) => { return (
                <li key={i + 'participant' + e.name }>
                    {'name: '}{e.name}{' - creator: '}{e.creatorName}{' - description: '}{e.description}{' - end date: '}{e.endDate.day}/{e.endDate.month}/{e.endDate.year}
                    {' '}
                    <span style={{cursor: "pointer"}} onClick={() => this.acceptRequest(e.id)}><Octicon icon={Check}/></span>
                    {' '}
                    <span style={{cursor: "pointer"}} onClick={() => this.declineRequest(e.id, false)}><Octicon icon={X}/></span>
                    {' '}
                    <span style={{cursor: "pointer"}} onClick={() => this.declineRequest(e.id, true)}><Octicon icon={CircleSlash}/></span>
                </li>);})}
        </>);
    }

    render() {
        let modalBody = [];
        if (this.state.bodyRender !== undefined) {
            modalBody.push(this.state.bodyRender());
        }
        return (<div>
            {this.props.userId && <>
                <Button color="link" onClick={() => this.openAddEvent()}>Create Event</Button>
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

function mapStateToProps(state: AppState): Props {return { userId: state.signin.userId, username: state.signin.username };}
export default connect(mapStateToProps)(Events);
