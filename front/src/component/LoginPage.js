import React from 'react';
import { Link } from 'react-router-dom';
import "./style.css";

import { connect } from 'react-redux'
import { error } from '../redux/actions/error'
import { signin } from '../redux/actions/user'

class LoginPage extends React.Component {
  constructor(props) {
    super(props);
    this.state = { username: '', password: '' };

    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  handleChange(e) {
    const { name, value } = e.target;
    this.setState({ [name]: value });
  }

  handleSubmit() {
    const { username, password } = this.state;
    const { dispatch } = this.props;
    if (username && password) {
      dispatch(signin(username, password));
    } else {
      dispatch(error("Username and Password could not be empty."))
    }
  }

  render() {
    const { username, password } = this.state;
    return (
        <div className="auth-form">
          <h1 className="auth-form-header">Sign in to MyGift</h1>
          { this.props.error && <p className="auth-error">{this.props.error}</p> }
          <div className="auth-form-body">
              <div className="form-group">
                <label>Username</label>
                <input type="text" name="username" placeholder="username" className="form-control" value={username} onChange={this.handleChange} />
              </div>
              <div className="form-group">
                <label>Password</label>
                <input type="password" name="password" placeholder="password" className="form-control" value={password} onChange={this.handleChange} />
              </div>
              <button className="btn btn-primary btn-large" onClick={this.handleSubmit}>Sign in</button>
          </div>
          <div className="auth-bottom">
              New to mygift? <Link to="/register" className="btn btn-link">Register</Link>
          </div>
        </div>
    );
  }
}

function mapStateToProps(state) { return { error: state.error.message }; }
export default connect(mapStateToProps, null)(LoginPage);
