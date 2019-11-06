import React from 'react';
import { Link } from 'react-router-dom';
import "./style/style.css";

import { connect } from 'react-redux'
import { ThunkDispatch } from 'redux-thunk'
import { AppState } from '../redux/store'
import { error } from '../redux/actions/error'
import { signin } from '../redux/actions/user'

import { Connection } from '../translation/itrans';

interface DispatchProps {
  signin: (username: String, password: String) => void,
  error: (message: String) => void
}
interface StateProps { errorMessage: String | null, connection: Connection }
type Props = DispatchProps & StateProps

interface State {
  username: string,
  password: string
}

class SigninPage extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { username: '', password: '' };

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
    const { username, password } = this.state;
    if (username && password) {
      this.props.signin(username, password);
    } else {
      this.props.error(this.props.connection.emptyErrorMessage)
    }
  }

  render() {
    const { username, password } = this.state;
    const { connection } = this.props;
    return (
        <div className="auth-form">
          <h1 className="auth-form-header">{connection.signInTitle}</h1>
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
              <button className="btn btn-primary btn-large" onClick={this.handleSubmit}>{connection.signInButton}</button>
          </div>
          <div className="auth-bottom">
              {connection.newToMygift}<Link to="/signup" className="btn btn-link">{connection.createAnAccount}</Link>
          </div>
        </div>
    );
  }
}

function mapStateToProps(state: AppState): StateProps { return { errorMessage: state.error.message, connection: state.locale.messages.connection }; }
const mapDispatchToProps = (dispatch: ThunkDispatch<{}, {}, any>, ownProps: Props): DispatchProps => {
  return {
    signin: async (username, password) => await dispatch(signin(username, password)),
    error: (message) => dispatch(error(message))
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(SigninPage);
