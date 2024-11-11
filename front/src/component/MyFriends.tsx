import React, { useEffect, useState } from "react";

import {
  Modal,
  ModalHeader,
  ModalBody,
  Button,
  Input,
  Label,
  Form,
  FormGroup,
  FormFeedback,
} from "reactstrap";
import { useNavigate } from "react-router-dom";
import { CheckIcon, TrashIcon, CircleSlashIcon } from "@primer/octicons-react";

import "./style/friends.css";

import SquareImage from "./SquareImage";
import blank_profile_picture from "./image/blank_profile_picture.png";

import { isMobile } from "react-device-detect";

import { useAppSelector, useAppDispatch } from "../redux/store";
import { selectMessages } from "../redux/reducers/locale";
import { selectSignIn, logout } from "../redux/reducers/signin";

import { getServerUrl } from "../ServerInformation";
let url = getServerUrl();

function generateMessage(json: any, friendName: string): string {
  switch (json.status) {
    case "ACCEPTED":
      return "You are already friend with " + friendName;
    case "PENDING":
      if (json.ownRequest) {
        return (
          "Your friend " + friendName + " has still not accepted your request"
        );
      } else {
        return "Your friend " + friendName + " has already sent you a request";
      }
    case "REJECTED":
      return (
        "User " + friendName + " blocked you. You cannot sent him request."
      );
  }

  return "";
}

function MyFriends() {
  const token = useAppSelector(selectSignIn).token;
  const myfriends = useAppSelector(selectMessages).myfriends;

  const appDispatch = useAppDispatch();

  let navigate = useNavigate();

  const [errorMessage, setErrorMessage] = useState("");

  const [friends, setFriends] = useState([]);
  const [pending, setPending] = useState([]);
  const [received, setReceived] = useState([]);
  const [userHover, setUserHover] = useState(-1);

  const [showAddFriendModal, setShowAddFriendModal] = useState(false);
  const [name, setName] = useState("");
  const [shouldCheckName, setShouldCheckName] = useState(false);
  const [errorMessageAddFriendModal, setErrorMessageAddFriendModal] =
    useState("");

  const [showDeleteFriendModal, setShowDeleteFriendModal] = useState(false);
  const [deleteFriendId, setDeleteFriendId] = useState(-1);
  const [deleteFriendName, setDeleteFriendName] = useState("");
  const [errorMessageDeleteFriendModal, setErrorMessageDeleteFriendModal] =
    useState("");

  useEffect(() => {
    if (token) {
      const getFriends = () => {
        const request = async () => {
          const response = await fetch(url + "/friends", {
            method: "GET",
            headers: { Authorization: `Bearer ${token}` },
          });
          if (response.status === 401) {
            appDispatch(logout());
          } else {
            const json = await response.json();
            if (response.status === 200) {
              setFriends(json);
            } else {
              setErrorMessage(json.error);
            }
          }
        };
        request();
      };

      const getPending = () => {
        const request = async () => {
          const response = await fetch(url + "/friend-requests/pending", {
            method: "GET",
            headers: { Authorization: `Bearer ${token}` },
          });
          if (response.status === 401) {
            appDispatch(logout());
          } else {
            const json = await response.json();
            if (response.status === 200) {
              setPending(json.sent);
              setReceived(json.received);
            } else {
              setErrorMessage(json.error);
            }
          }
        };
        request();
      };

      getFriends();
      getPending();
    }
  }, [token, appDispatch]);

  if (token) {
    const getFriends = () => {
      const request = async () => {
        const response = await fetch(url + "/friends", {
          method: "GET",
          headers: { Authorization: `Bearer ${token}` },
        });
        if (response.status === 401) {
          appDispatch(logout());
        } else {
          const json = await response.json();
          if (response.status === 200) {
            setFriends(json);
          } else {
            setErrorMessage(json.error);
          }
        }
      };
      request();
    };

    const getPending = () => {
      const request = async () => {
        const response = await fetch(url + "/friend-requests/pending", {
          method: "GET",
          headers: { Authorization: `Bearer ${token}` },
        });
        if (response.status === 401) {
          appDispatch(logout());
        } else {
          const json = await response.json();
          if (response.status === 200) {
            setPending(json.sent);
            setReceived(json.received);
          } else {
            setErrorMessage(json.error);
          }
        }
      };
      request();
    };

    const cancelRequest = (id: number) => {
      const request = async () => {
        const response = await fetch(url + "/friend-requests/" + id, {
          method: "delete",
          headers: { Authorization: `Bearer ${token}` },
        });
        if (response.status === 202) {
          getFriends();
          getPending();
        } else if (response.status === 401) {
          appDispatch(logout());
        } else {
          const json = await response.json();
          setErrorMessage(json.error);
        }
      };
      request();
    };

    const acceptRequest = (id: number) => {
      const request = async () => {
        const response = await fetch(
          url + "/friend-requests/" + id + "/accept",
          {
            headers: { Authorization: `Bearer ${token}` },
          },
        );
        if (response.status === 202) {
          getFriends();
          getPending();
        } else if (response.status === 401) {
          appDispatch(logout());
        } else {
          const json = await response.json();
          setErrorMessage(json.error);
        }
      };
      request();
    };

    const declineRequest = (id: number, blockUser: boolean) => {
      const request = async () => {
        const response = await fetch(
          url + "/friend-requests/" + id + "/decline?blockUser=" + blockUser,
          {
            method: "post",
            headers: { Authorization: `Bearer ${token}` },
          },
        );
        if (response.status === 202) {
          getFriends();
          getPending();
        } else if (response.status === 401) {
          appDispatch(logout());
        } else {
          const json = await response.json();
          setErrorMessage(json.error);
        }
      };
      request();
    };

    const renderHoverFriend = (i: number, req: any, user: any) => {
      if (i === userHover || isMobile) {
        return (
          <div className="friend-card-delete one-icon">
            <span
              style={{ cursor: "pointer" }}
              onClick={() => {
                setDeleteFriendName(user.name);
                setDeleteFriendId(req.id);
                setErrorMessageDeleteFriendModal("");
                setShowDeleteFriendModal(true);
              }}
            >
              <TrashIcon />
            </span>
          </div>
        );
      } else {
        return <div />;
      }
    };

    const onAddFriendFormSubmit = (e: any) => {
      e.preventDefault();
      if (name === "") {
        setShouldCheckName(true);
        return;
      }
      const request = async () => {
        const response = await fetch(url + "/friend-requests", {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({ name: e.target.name.value }),
        });
        if (response.status === 200) {
          getFriends();
          getPending();
          setShouldCheckName(false);
          setName("");
          setShowAddFriendModal(false);
        } else if (response.status === 401) {
          appDispatch(logout());
        } else {
          const json = await response.json();
          const errorMessage =
            response.status === 409
              ? generateMessage(json, e.target.name.value)
              : json.error;
          setErrorMessageAddFriendModal(errorMessage);
        }
      };
      request();
    };

    const onDeleteFriendFormSubmit = (e: any) => {
      e.preventDefault();
      const request = async () => {
        const response = await fetch(
          url + "/friend-requests/" + deleteFriendId,
          {
            method: "delete",
            headers: { Authorization: `Bearer ${token}` },
          },
        );
        if (response.status === 202) {
          getFriends();
          getPending();
          setShowDeleteFriendModal(false);
        } else if (response.status === 401) {
          appDispatch(logout());
        } else {
          const json = await response.json();
          setErrorMessageDeleteFriendModal(json.error);
        }
      };
      request();
    };

    return (
      <div>
        <div className="main-friend">
          <Button
            color="link"
            onClick={() => {
              setShouldCheckName(false);
              setName("");
              setErrorMessageAddFriendModal("");
              setShowAddFriendModal(true);
            }}
          >
            {myfriends.addFriendButton}
          </Button>
          <h2 style={{ margin: "10px" }}>{myfriends.friends}</h2>
          <div className="mycard-row">
            {friends.map((req: any, i: any) => {
              const user = req.otherUser;
              return (
                <div
                  key={i + "friends-" + req.id}
                  className="friend-card"
                  onMouseEnter={() => setUserHover(i)}
                  onMouseLeave={() => setUserHover(-1)}
                >
                  <div
                    style={{ cursor: "pointer" }}
                    onClick={() => navigate("/friend/" + user.name)}
                  >
                    <SquareImage
                      token={token}
                      className="profile-image"
                      imageName={user.picture}
                      size={150}
                      alt="Profile"
                      alternateImage={blank_profile_picture}
                    />
                  </div>
                  {renderHoverFriend(i, req, user)}
                  <div
                    style={{ cursor: "pointer" }}
                    className="friend-footer"
                    onClick={() => navigate("/friend/" + user.name)}
                  >
                    <div className="friend-name">{user.name}</div>
                  </div>
                </div>
              );
            })}
          </div>

          <h2 style={{ margin: "10px" }}>{myfriends.requests}</h2>
          {received.length > 0 ? (
            received.map((req: any, i: any) => {
              return (
                <li
                  key={i + "received" + req.otherUser.name}
                  style={{ margin: "10px" }}
                >
                  {req.otherUser.name}
                  <span
                    style={{ cursor: "pointer", padding: "5px" }}
                    onClick={() => acceptRequest(req.id)}
                  >
                    <CheckIcon />
                  </span>
                  <span
                    style={{ cursor: "pointer", padding: "5px" }}
                    onClick={() => declineRequest(req.id, false)}
                  >
                    <TrashIcon />
                  </span>
                  <span
                    style={{ cursor: "pointer", padding: "5px" }}
                    onClick={() => declineRequest(req.id, true)}
                  >
                    <CircleSlashIcon />
                  </span>
                </li>
              );
            })
          ) : (
            <p style={{ margin: "10px" }}>{myfriends.noPendingRequest}</p>
          )}

          <h2 style={{ margin: "10px" }}>{myfriends.myRequests}</h2>
          {pending.length > 0 ? (
            pending.map((req: any, i: any) => {
              return (
                <li
                  key={i + "initiated" + req.otherUser.name}
                  style={{ margin: "10px" }}
                >
                  {req.otherUser.name}{" "}
                  <span
                    style={{ cursor: "pointer" }}
                    onClick={() => cancelRequest(req.id)}
                  >
                    <TrashIcon />
                  </span>
                </li>
              );
            })
          ) : (
            <p style={{ margin: "10px" }}>{myfriends.allRequestsAccepted}</p>
          )}

          <Modal
            isOpen={showAddFriendModal}
            toggle={() => setShowAddFriendModal(false)}
          >
            <ModalHeader toggle={() => setShowAddFriendModal(false)}>
              {myfriends.addFriendModalTitle}
            </ModalHeader>
            <ModalBody>
              {errorMessageAddFriendModal && (
                <p className="auth-error">{errorMessageAddFriendModal}</p>
              )}
              <Form inline="true" onSubmit={onAddFriendFormSubmit}>
                <FormGroup className="mb-2 mr-sm-2 mb-sm-0">
                  <Label className="mr-sm-2">{myfriends.name}</Label>
                  <Input
                    invalid={shouldCheckName && name.length === 0}
                    name="name"
                    placeholder={myfriends.name}
                    value={name}
                    onChange={(e) => {
                      setShouldCheckName(true);
                      setName(e.target.value);
                    }}
                  />
                  <FormFeedback>{myfriends.nameErrorMessage}</FormFeedback>
                </FormGroup>
                <br />
                <Button
                  disabled={shouldCheckName && name.length === 0}
                  color="primary"
                >
                  {myfriends.addModalButton}
                </Button>
              </Form>
            </ModalBody>
          </Modal>

          <Modal
            isOpen={showDeleteFriendModal}
            toggle={() => setShowDeleteFriendModal(false)}
          >
            <ModalHeader toggle={() => setShowDeleteFriendModal(false)}>
              {myfriends.deleteFriendModalTitlePre}
              {deleteFriendName}
              {myfriends.deleteFriendModalTitleSuffix}
            </ModalHeader>
            <ModalBody>
              {errorMessageDeleteFriendModal && (
                <p className="auth-error">{errorMessageDeleteFriendModal}</p>
              )}
              <Form inline="true" onSubmit={onDeleteFriendFormSubmit}>
                <Button color="primary">{myfriends.deleteModalButton}</Button>
              </Form>
            </ModalBody>
          </Modal>
        </div>
      </div>
    );
  } else {
    console.log("Unauthorized... Redirecting...");
    navigate("../signin");
    return <div></div>;
  }
}

export default MyFriends;
