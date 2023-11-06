import React, { useState } from "react";
import { Outlet, Link } from "react-router-dom";
import {
  Collapse,
  Dropdown,
  DropdownToggle,
  DropdownMenu,
  DropdownItem,
  Navbar,
  NavbarToggler,
  NavbarBrand,
  Nav,
  NavItem,
  NavLink,
  Form,
  Input,
  FormGroup,
} from "reactstrap";

import { useAppSelector, useAppDispatch } from "./redux/store";
import { logout, changeUser, selectSignIn } from "./redux/reducers/signin";
import {
  LocaleAvailable,
  changeLocale,
  selectMessages,
} from "./redux/reducers/locale";

import HomePage from "./component/HomePage";
import SigninPage from "./component/SigninPage";
import SquareImage from "./component/SquareImage";

import blank_profile_picture from "./component/image/blank_profile_picture.png";

import "./component/style/christmas.css";

function App() {
  //TODO: add loading image

  const username = useAppSelector(selectSignIn).username;
  const token = useAppSelector(selectSignIn).token;
  const picture = useAppSelector(selectSignIn).picture;
  const otherUsers = useAppSelector(selectSignIn).otherUsers;
  const app = useAppSelector(selectMessages).app;

  const dispatch = useAppDispatch();

  const locales: string[] = ["English", "Français"];

  //Init locale to FR if not set
  if (localStorage.getItem("locale") === null) {
    dispatch(changeLocale(LocaleAvailable["Francais"]));
  }
  const locale = localStorage.getItem("locale");

  const [collapsed, setCollapsed] = useState(true);
  const toggleNavbar = () => setCollapsed(!collapsed);

  const [dropdownState, setDropdownState] = useState(false);
  const toggleDropdown = () => setDropdownState(!dropdownState);

  return (
    <div>
      <Navbar color="light" expand="lg" light>
        <NavbarBrand href="/" className="me-auto">
          MyGift
        </NavbarBrand>

        <NavbarToggler onClick={toggleNavbar} className="me-2" />

        <Collapse isOpen={!collapsed} navbar>
          <Nav className="me-auto" navbar>
            {!username && (
              <>
                <NavItem>
                  <NavLink href="/signin">{app.signin}</NavLink>
                </NavItem>
                <NavItem>
                  <NavLink href="/signup">{app.signup}</NavLink>
                </NavItem>
              </>
            )}
            {username && (
              <>
                <NavItem>
                  <NavLink href="/mywishlist">{app.myList}</NavLink>
                </NavItem>
                <NavItem>
                  <NavLink href="/myfriends">{app.myFriends}</NavLink>
                </NavItem>
                <NavItem>
                  <NavLink href="/buylist">{app.myBuyList}</NavLink>
                </NavItem>
              </>
            )}
          </Nav>
          <Form
            inline="true"
            className="d-flex"
            style={{ alignItems: "center" }}
          >
            {username && (
              <>
                <Dropdown isOpen={dropdownState} toggle={toggleDropdown}>
                  <DropdownToggle
                    caret
                    tag="span"
                    onClick={toggleDropdown}
                    data-toggle="dropdown"
                    aria-expanded={dropdownState}
                    style={{ cursor: "pointer" }}
                  >
                    <SquareImage
                      token={token ? token : ""}
                      className="card-image"
                      imageName={picture ? picture : ""}
                      size={35}
                      alt="Profile Pic"
                      alternateImage={blank_profile_picture}
                    />
                  </DropdownToggle>
                  <DropdownMenu>
                    <DropdownItem>
                      <Link to={"/manageaccount"} className="nav-link">
                        {app.manageAccount}
                      </Link>
                    </DropdownItem>
                    {otherUsers !== undefined && otherUsers.length > 0 && (
                      <>
                        <hr />
                        {otherUsers.map((user: any) => {
                          return (
                            <DropdownItem
                              onClick={(e) => dispatch(changeUser(user))}
                            >
                              <SquareImage
                                token={token ? token : ""}
                                className="card-image"
                                imageName={user.picture ? user.picture : ""}
                                size={35}
                                alt="Profile Pic"
                                alternateImage={blank_profile_picture}
                              />
                              {" " + user.username}
                            </DropdownItem>
                          );
                        })}
                      </>
                    )}
                    <hr />
                    <DropdownItem>
                      <Link to={"/changeaccount"} className="nav-link">
                        {app.changeAccount}
                      </Link>
                    </DropdownItem>
                  </DropdownMenu>
                </Dropdown>
                <button
                  className="btn"
                  type="button"
                  onClick={() => dispatch(logout())}
                >
                  <Link to={"/"} className="nav-link">
                    {app.logout}
                  </Link>
                </button>
              </>
            )}
            <FormGroup className="mb-2 mr-sm-2 mb-sm-0">
              <Input
                type="select"
                name="select"
                value={locale ? locale : ""}
                onChange={(e) =>
                  dispatch(
                    changeLocale(
                      e.target.value === "English"
                        ? LocaleAvailable["English"]
                        : LocaleAvailable["Francais"],
                    ),
                  )
                }
              >
                {locales.map((value) => {
                  if (value === locale) {
                    return (
                      <option key={value} value={value}>
                        {value}
                      </option>
                    );
                  } else {
                    return (
                      <option key={value} value={value}>
                        {value}
                      </option>
                    );
                  }
                })}
              </Input>
            </FormGroup>
          </Form>
        </Collapse>
      </Navbar>
      <Outlet />
    </div>
  );
}

export function Index() {
  const username = useAppSelector(selectSignIn).username;
  return username ? <HomePage /> : <SigninPage />;
}

export default App;
