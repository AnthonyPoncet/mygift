import React from 'react';
import "./style.css";

import { connect } from 'react-redux'
import { ThunkDispatch } from 'redux-thunk'
import { AppState } from '../redux/store'
import { error } from '../redux/actions/error'
import { signup, UserSignUp } from '../redux/actions/user'

interface DispatchProps {
  signup: (user: UserSignUp) => void,
  error: (message: String) => void
}
interface StateProps {
  errorMessage: String | null
}
type Props = DispatchProps & StateProps

interface State {
  username: string,
  password: string
}

class SignupPage extends React.Component<Props, State> {
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
      this.props.signup({username: username, password: password});
    } else {
      this.props.error("Username and Password could not be empty.");
    }
  }

  render() {
    const { username, password } = this.state;
    return (
        <div className="auth-form">
          <h1 className="auth-form-header">Sign up to MyGift</h1>
          { this.props.errorMessage && <p className="auth-error">{this.props.errorMessage}</p> }
          <div className="auth-form-body">
              <div className="form-group">
                <label>Username</label>
                <input type="text" name="username" placeholder="username" className="form-control" value={username} onChange={this.handleChange} />
              </div>
              <div className="form-group">
                <label>Password</label>
                <input type="password" name="password" placeholder="password" className="form-control" value={password} onChange={this.handleChange} />
              </div>
              <button className="btn btn-primary btn-large" onClick={this.handleSubmit}>Sign up</button>
          </div>
        </div>
    );
  }
}

function mapStateToProps(state: AppState): StateProps { return { errorMessage: state.error.message }; }
const mapDispatchToProps = (dispatch: ThunkDispatch<{}, {}, any>, ownProps: Props): DispatchProps => {
  return {
    signup: async (user) => await dispatch(signup(user)),
    error: (message) => dispatch(error(message))
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(SignupPage);
