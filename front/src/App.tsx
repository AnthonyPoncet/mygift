import React from 'react';
import { Router, Route, Link } from 'react-router-dom';
import { Input, FormGroup } from "reactstrap";

import { connect } from 'react-redux';
import { AppState } from './redux/store'
import { clearError } from './redux/actions/error';
import { changeLocale } from './redux/actions/locale';
import { logout } from './redux/actions/user';

import MyBuyList from './component/MyBuyList';
import Event from './component/Event';
import Events from './component/Events';
import FriendWishListRoute from './component/FriendWishListRoute'
import HomePage from './component/HomePage';
import MyFriends from './component/MyFriends';
import MyWishList from './component/MyWishList';
import SigninPage from './component/SigninPage';
import SignupPage from './component/SignupPage';
import { history } from './component/history';

import { AppMessage } from './translation/itrans';

interface AppProps {
    clearError: typeof clearError,
    changeLocale: typeof changeLocale,
    logout: typeof logout,

    username: String | null,
    app: AppMessage
}

class App extends React.Component<AppProps> {
    private locales: string[] = ['English', 'French']

    constructor(props: AppProps) {
        super(props);
        history.listen((location, action) => {this.props.clearError()});
    }

    render() {
        const username = this.props.username;
        const { app } = this.props;

        let locale  = localStorage.getItem("locale");
        return (
          <Router history={history}>
              <div>
                <nav className="navbar navbar-expand-lg navbar-light bg-light">
                  <a className="navbar-brand" href="/">MyGift</a>
                  <ul className="navbar-nav mr-auto">
                    { !username && <>
                      <li className="nav-item"><Link to={'/signin'} className="nav-link">{app.signin}</Link></li>
                      <li className="nav-item"><Link to={'/signup'} className="nav-link">{app.signup}</Link></li>
                      </> }
                    { username && <>
                      <li className="nav-item"><Link to={'/mywishlist'} className="nav-link">{app.myList}</Link></li>
                      <li className="nav-item"><Link to={'/myfriends'} className="nav-link">{app.myFriends}</Link></li>
                      <li className="nav-item"><Link to={'/events'} className="nav-link">{app.myEvents}</Link></li>
                      <li className="nav-item"><Link to={'/buy-list'} className="nav-link">{app.myBuyList}</Link></li>
                      <li className="nav-item"><Link to={'/'} className="nav-link">{username}</Link></li>
                      </>}
                  </ul>
                  <form className="form-inline">
                    { username && (<button className="btn" type="button" onClick={() => this.props.logout()}>{app.logout}</button>) }
                    <FormGroup>
                      <Input type="select" name="select" id="exampleSelect" onChange={(e) => this.props.changeLocale(e.target.value)}>
                        {this.locales.map((value) => {
                          if (value === locale) {return (<option selected>{value}</option>);}
                          else {return (<option >{value}</option>);}; })}
                      </Input>
                    </FormGroup>
                  </form>
                </nav>
                  {this.props.username ? <Route exact path="/" component={HomePage}/> : <Route exact path="/" component={SignupPage}/>}
                  <Route path="/signin" component={SigninPage} />
                  <Route path="/signup" component={SignupPage} />
                  <Route path="/mywishlist" component={MyWishList} />
                  <Route path="/myfriends" component={MyFriends} />
                  <Route path="/friend/:friendName?" component={FriendWishListRoute} />
                  <Route path="/events" component={Events} />
                  <Route path="/event/:eventId?" component={Event} />
                  <Route path="/buy-list" component={MyBuyList} />
              </div>
          </Router>
        );
    }
}

function mapStateToProps(state: AppState) { return { username: state.signin.username, app: state.locale.messages.app }; }
export default connect(mapStateToProps, {clearError, changeLocale, logout})(App);
