import React from 'react';
import { Router, Route, Link } from 'react-router-dom';

import { connect } from 'react-redux';
import { clearError } from './redux/actions/error'

import HomePage from './component/HomePage'
import LoginPage from './component/LoginPage'
import RegisterPage from './component/RegisterPage'
import { history } from './component/history'

class App extends React.Component {
  constructor(props) {
    super(props);
    history.listen((location, action) => {this.props.dispatch(clearError())})
  }

    render() {
        return (
          <Router history={history}>
              <div>
                <nav className="navbar navbar-expand-lg navbar-light bg-light">
                  <ul className="navbar-nav mr-auto">
                    <li><Link to={'/'} className="nav-link">Home</Link></li>
                    <li><Link to={'/login'} className="nav-link">Login</Link></li>
                    <li><Link to={'/register'} className="nav-link">Register</Link></li>
                  </ul>
                </nav>
                  <Route exact path="/" render={props => (localStorage.getItem('username') ? <HomePage/> : <LoginPage/>)} />
                  <Route path="/register" component={RegisterPage} />
                  <Route path="/login" component={LoginPage} />
              </div>
          </Router>
        );
    }
}

function mapStateToProps(state) {
    const { error } = state;
    return {
        error
    };
}
export default connect(mapStateToProps)(App);
