import React from 'react';
import "./style.css";

import { connect } from 'react-redux'
import { ThunkDispatch } from 'redux-thunk'
import { AppState } from '../redux/store'
import { error } from '../redux/actions/error'
import { signup, UserSignUp } from '../redux/actions/user'

import { Connection } from '../translation/itrans';

import create_list from './create_list.png';
import friends from './friends.png';
import events from './events.png';

import { isMobile } from "react-device-detect";

import { getServerUrl } from "../ServerInformation";
let url = getServerUrl();


interface DispatchProps {
  signup: (user: UserSignUp) => void,
  error: (message: String) => void
};
interface StateProps { errorMessage: String | null, connection: Connection };
type Props = DispatchProps & StateProps;

interface State { username: string, password: string, image: string | null, loaded: boolean };

class SignupPage extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { username: '', password: '', image: null, loaded: false };

    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  handleChange(e: any) {
      const { name, value } = e.target;
      if (name === "username") {
          this.setState({username: value, password: this.state.password});
      } else if (name === "password") {
          this.setState({username: this.state.username, password: value});
      }
  }

  handleSubmit() {
      const { username, password, image } = this.state;
      console.log(this.state);
      if (username && password) {
          this.props.signup({username: username, password: password, image: image});
      } else {
          this.props.error(this.props.connection.emptyErrorMessage);
      }
  }

  loadImage(name: string) {
      const request = async() => {
          const response = await fetch(url + '/files/' + name);

          response.blob().then(blob => {
              let url = window.URL.createObjectURL(blob);
              let tag = document.querySelector('#profile');
              if (tag instanceof HTMLImageElement) tag.src = url;
          });

          this.setState({ loaded: true });
      };
      request();
  }

  changeImage(e: any) {
      const formData = new FormData();
      formData.append("0", e.target.files[0]);
      const request = async () => {
          const response = await fetch(url + '/files', {method: 'post', body: formData});
          if (response.status === 202) {
              const json = await response.json();
              this.setState({ image: json.name, loaded: false });
              this.loadImage(json.name);
          } else {
              const json = await response.json();
              console.log(json);
          }
      };
      request();
  }

  renderSignUp() {
    const { username, password, loaded } = this.state;
    const { connection } = this.props;
    return (
        <div className="auth-form">
          <h1 className="auth-form-header">{connection.signUpTitle}</h1>
          { this.props.errorMessage && <p className="auth-error">{this.props.errorMessage}</p> }
          <div className="auth-form-body">
              <div className="form-group">
                <label>{connection.username}</label>
                <input type="text" name="username" placeholder={connection.username} className="form-control" value={username} onChange={this.handleChange} />
              </div>
              <div className="form-group">
                <label>{connection.password}</label>
                <input type="password" name="password" placeholder={connection.password} className="form-control" value={password} onChange={this.handleChange} />
              </div>
              <div className="form-group">
                <label>{connection.image}</label>
                <input type="file" onChange={(e) => this.changeImage(e)}/>
              </div>
              {loaded === true && <img id="profile" height="150" width="150" alt="Profile"/>}
              <button className="btn btn-primary btn-large" onClick={this.handleSubmit}>{connection.signUpButton}</button>
          </div>
        </div>
    );
  }

  renderText() {
    const { connection } = this.props;
    return (
      <div>
        <div className="textContainer"><img className="small-icon" src={create_list} alt="Create list"/><p className="textAlign">{connection.listDesc}</p></div>
        <div className="textContainer"><img className="small-icon" src={friends} alt="Friends"/><p className="textAlign">{connection.friendsDesc}</p></div>
        <div className="textContainer"><img className="small-icon" src={events} alt="Events"/><p className="textAlign">{connection.eventsDesc}</p></div>
      </div>
    );
  }

  render() {
    if (isMobile) {
      return (<div>
        <div className="textMobile">{this.renderText()}</div>
        <div>{this.renderSignUp()}</div>
      </div>);
    }
    return (
    <div className="wrapper">
      <div className="left">{this.renderText()}</div>
      <div className="right">{this.renderSignUp()}</div>
    </div>);
  }
}

function mapStateToProps(state: AppState): StateProps { return { errorMessage: state.error.message, connection: state.locale.messages.connection }; }
const mapDispatchToProps = (dispatch: ThunkDispatch<{}, {}, any>, ownProps: Props): DispatchProps => {
  return {
    signup: async (user) => await dispatch(signup(user)),
    error: (message) => dispatch(error(message))
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(SignupPage);
