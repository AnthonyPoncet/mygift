import React from 'react';
import { Router, Route, Link } from 'react-router-dom';
import { Input, FormGroup } from "reactstrap";

import "../node_modules/bootstrap/dist/js/bootstrap.min.js";

import { connect } from 'react-redux';
import { AppState } from './redux/store';
import { clearError } from './redux/actions/error';
import { changeLocale } from './redux/actions/locale';
import { logout } from './redux/actions/user';

import MyBuyList from './component/MyBuyList';
import Event from './component/Event';
import Events from './component/Events';
import FriendWishListRoute from './component/FriendWishListRoute'
import HomePage from './component/HomePage';
import ManageAccount from './component/ManageAccount';
import MyFriends from './component/MyFriends';
import MyWishList from './component/MyWishList';
import SigninPage from './component/SigninPage';
import SignupPage from './component/SignupPage';
import { history } from './component/history';

import blank_profile_picture from './component/image/blank_profile_picture.png';

import { AppMessage } from './translation/itrans';

import { getServerUrl } from "./ServerInformation";
let url = getServerUrl();

interface AppProps {
    clearError: typeof clearError,
    changeLocale: typeof changeLocale,
    logout: typeof logout,

    username: String | null,
    image: string | null,
    app: AppMessage
}
interface State {
    loaded: boolean
}

class App extends React.Component<AppProps, State> {
    private locales: string[] = ['English', 'Français'];

    constructor(props: AppProps) {
        super(props);
        history.listen((location, action) => {this.props.clearError()});
        this.state = { loaded: false };

        if (this.props.image !== null) {
            this._loadImage(this.props.image);
        }
    }

    //Hanle loggin, seems weird
    componentWillReceiveProps(nextProps: AppProps, nextContext: any) {
        if (nextProps.image) {
            this._loadImage(nextProps.image);
        }
    }

    _loadImage(name: string) {
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

    render() {
        const username = this.props.username;
        const { app } = this.props;

        let locale  = localStorage.getItem("locale");
        if (locale === null) this.props.changeLocale('Français');

        return (
          <Router history={history}>
              <div>
                <nav className="navbar navbar-expand-lg navbar-light bg-light">
                  <a className="navbar-brand" href="/">MyGift</a>
                  <button className="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarTogglerDemo03" aria-controls="navbarTogglerDemo03" aria-expanded="false" aria-label="Toggle navigation">
                    <span className="navbar-toggler-icon"></span>
                  </button>
                  <div className="collapse navbar-collapse" id="navbarTogglerDemo03">
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
                      </>}
                  </ul>
                  <form className="form-inline">
                    { username && <>
                      <div className="dropdown">
                        <button className="btn btn-default dropdown-toggle" type="button" id="menu1" data-toggle="dropdown">
                          {this.state.loaded === true ? <img id="profile" height="35" width="35" alt="Profile"/>
                                           : <img height="35" width="35" src={blank_profile_picture} alt="Nothing"/>}
                        </button>
                        <div className="dropdown-menu" aria-labelledby="dropdownMenuButton">
                          <span className="dropdown-item"><Link to={'/manage-account'} className="nav-link">{app.manageAccount}</Link></span>
                        </div>
                      </div>
                      <button className="btn" type="button" onClick={() => this.props.logout()}><Link to={'/'} className="nav-link">{app.logout}</Link></button>
                      </>
                    }
                    <FormGroup>
                      <Input type="select" name="select" id="exampleSelect" onChange={(e) => this.props.changeLocale(e.target.value)}>
                        {this.locales.map((value) => {
                          if (value === locale) {return (<option selected key={value}>{value}</option>);}
                          else {return (<option key={value}>{value}</option>);}; })}
                      </Input>
                    </FormGroup>
                  </form>
                  </div>
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
                  <Route path="/manage-account" component={ManageAccount} />
              </div>
          </Router>
        );
    }
}

function mapStateToProps(state: AppState) { return { username: state.signin.username, image: state.signin.picture, app: state.locale.messages.app }; }
export default connect(mapStateToProps, {clearError, changeLocale, logout})(App);
