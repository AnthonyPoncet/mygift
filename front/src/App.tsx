import React from 'react';
import { Router, Route, Link } from 'react-router-dom';

import { connect } from 'react-redux';
import { AppState } from './redux/store'
import { clearError } from './redux/actions/error';
import { logout } from './redux/actions/user';

import FriendWishList from './component/FriendWishList'
import HomePage from './component/HomePage';
import MyFriends from './component/MyFriends';
import MyWishList from './component/MyWishList';
import SigninPage from './component/SigninPage';
import SignupPage from './component/SignupPage';
import { history } from './component/history';

interface AppProps {
    clearError: typeof clearError,
    logout: typeof logout,

    username: String | null
}

class App extends React.Component<AppProps> {
    constructor(props: AppProps) {
        super(props);
        history.listen((location, action) => {this.props.clearError()})
    }

    render() {
        const username = this.props.username;
        console.log(username)
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
                      <li className="nav-item"><Link to={'/mywishlist'} className="nav-link">My List</Link></li>
                        <li className="nav-item"><Link to={'/myfriends'} className="nav-link">My Friends</Link></li>
                      <li className="nav-item"><Link to={'/'} className="nav-link">{username}</Link></li>
                      </>}
                  </ul>
                  { username && (<form className="form-inline">
                    <button className="btn" type="button" onClick={() => this.props.logout()}>Logout</button>
                  </form>)}
                </nav>
                  {this.props.username ? <Route exact path="/" component={HomePage}/> : <Route exact path="/" component={SignupPage}/>}
                  <Route path="/signin" component={SigninPage} />
                  <Route path="/signup" component={SignupPage} />
                  <Route path="/mywishlist" component={MyWishList} />
                  <Route path="/myfriends" component={MyFriends} />
                  <Route path="/friend/:friendName?" component={FriendWishList} />
              </div>
          </Router>
        );
    }
}

function mapStateToProps(state: AppState) {
    return { username: state.signin.username };
}
export default connect(mapStateToProps, {clearError, logout})(App);
