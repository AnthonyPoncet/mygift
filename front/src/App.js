import React from 'react';
import { Router, Route, Link } from 'react-router-dom';

import { connect } from 'react-redux';
import { clearError } from './redux/actions/error'
import { logout } from './redux/actions/user'

import HomePage from './component/HomePage'
import SigninPage from './component/SigninPage'
import SignupPage from './component/SignupPage'
import { history } from './component/history'

class App extends React.Component {
  constructor(props) {
    super(props);
    history.listen((location, action) => {this.props.dispatch(clearError())})
  }

    render() {
        const username = this.props.username;
        console.log(localStorage.getItem("username"));
        return (
          <Router history={history}>
              <div>
                <nav className="navbar navbar-expand-lg navbar-light bg-light">
                  <a className="navbar-brand" href="/">MyGift</a>
                  <ul className="navbar-nav mr-auto">
                    { !username && <>
                      <li className="nav-item"><Link to={'/signin'} className="nav-link">Sign in</Link></li>
                      <li className="nav-item"><Link to={'/signup'} className="nav-link">Sign up</Link></li>
                      </> }
                    { username && <>
                      <li className="nav-item dropdown">
                        <a className="nav-link dropdown-toggle">{username}</a>
                        <div class="dropdown-menu" aria-labelledby="navbarDropdown">
                          <a class="dropdown-item" href="#">Action</a>
                          <a class="dropdown-item" href="#">Another action</a>
                          <div class="dropdown-divider"></div>
                          <a class="dropdown-item" href="#">Something else here</a>
                        </div></li>
                      </>}
                  </ul>
                  { username && (<form class="form-inline">
                    <button class="btn" type="button" onClick={() => this.props.dispatch(logout())}>Logout</button>
                  </form>)}
                </nav>
                  <Route exact path="/" render={ props => this.props.username ? <HomePage/> : <SigninPage/>} />
                  <Route path="/signin" component={SigninPage} />
                  <Route path="/signup" component={SignupPage} />
              </div>
          </Router>
        );
    }
}

function mapStateToProps(state) {
    return { username: state.signin.username };
}
export default connect(mapStateToProps)(App);
