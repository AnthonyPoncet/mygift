import React, { useEffect, useState } from "react";

import {
  Button,
  Form,
  FormGroup,
  Label,
  Input,
  FormFeedback,
  Modal,
  ModalHeader,
  ModalBody,
  Spinner,
} from "reactstrap";
import { useNavigate } from "react-router-dom";
import {
  HeartIcon,
  PencilIcon,
  XIcon,
  ArrowDownIcon,
  ArrowUpIcon,
  ArrowLeftIcon,
  ArrowRightIcon,
} from "@primer/octicons-react";

import { useAppSelector, useAppDispatch } from "../redux/store";
import { selectMessages } from "../redux/reducers/locale";
import {
  addMessage,
  selectErrorMessage,
  clearMessage,
} from "../redux/reducers/error";
import { selectSignIn, logout } from "../redux/reducers/signin";

import SquareImage from "./SquareImage";
import blank_gift from "./image/blank_gift.png";

import { isMobile } from "react-device-detect";

import "./style/card-gift.css";

import { getServerUrl } from "../ServerInformation";
let url = getServerUrl();

function getGifts(token: string, setCategories: any, appDispatch: any) {
  const request = async () => {
    const response = await fetch(url + "/gifts", {
      method: "GET",
      headers: { Authorization: `Bearer ${token}` },
    });
    if (response.status === 401) {
      appDispatch(logout());
    } else {
      const json = await response.json();
      if (response.status === 200) {
        setCategories(json);
      } else {
        console.error(json.error);
        appDispatch(addMessage(json.error));
      }
    }
  };
  request();
}

function getFriends(token: string, setFriends: any, appDispatch: any) {
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
        console.error(json.error);
        appDispatch(addMessage(json.error));
      }
    }
  };
  request();
}

function addGiftModal(
  setModalTitle: any,
  setModalBody: any,
  setShow: any,
  mywishlist: any,
  token: string,
  appDispatch: any,
  categories: any,
  setCategories: any
) {
  appDispatch(clearMessage());

  setModalTitle(mywishlist.addGiftModalTitle);

  let onFormSubmit = (e: any) => {
    e.preventDefault();
    if (e.target.name.value === "") {
      appDispatch(addMessage(mywishlist.nameErrorMessage));
      return;
    }
    let imageName =
      e.target.picture === undefined ? "" : e.target.picture.value;
    const request = async () => {
      const response = await fetch(url + "/gifts", {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          name: e.target.name.value,
          description: e.target.description.value,
          price: e.target.price.value,
          whereToBuy: e.target.whereToBuy.value,
          categoryId: e.target.categoryId.value,
          picture: imageName,
        }),
      });
      if (response.status === 200) {
        getGifts(token, setCategories, appDispatch);
        setShow(false);
      } else if (response.status === 401) {
        appDispatch(logout());
      } else {
        const json = await response.json();
        console.error(json);
        appDispatch(addMessage(json.error));
      }
    };
    request();
  };

  let changeImage = (e: any) => {
    e.target.form[7].disabled = true;
    e.target.form[7].children[0].hidden = false;
    const formData = new FormData();
    formData.append("0", e.target.files[0]);
    const request = async () => {
      const response = await fetch(url + "/files", {
        method: "post",
        headers: { Authorization: `Bearer ${token}` },
        body: formData,
      });
      if (response.status === 401) {
        appDispatch(logout());
      } else if (response.status === 202) {
        const json = await response.json();
        e.target.form[6].value = json.name; //TODO: horrible. to replace by states
        e.target.form[7].disabled = false;
        e.target.form[7].children[0].hidden = true;
      } else {
        const json = await response.json();
        console.error(json);
        appDispatch(addMessage(json.error));
      }
    };
    request();
  };

  const options = categories.map((cag: any, index: any) => {
    let value = cag.category;
    return (
      <option key={index} value={value.id}>
        {value.name}
      </option>
    );
  });

  setModalBody(
    <Form onSubmit={onFormSubmit}>
      <FormGroup>
        <Label>{mywishlist.name}</Label>
        <Input name="name" placeholder={mywishlist.name} />
        <FormFeedback>{mywishlist.nameErrorMessage}</FormFeedback>
      </FormGroup>
      <FormGroup>
        <Label>{mywishlist.description}</Label>
        <Input
          type="textarea"
          name="description"
          placeholder={mywishlist.description}
        />
      </FormGroup>
      <FormGroup>
        <Label>{mywishlist.price}</Label>
        <Input name="price" placeholder="10" />
      </FormGroup>
      <FormGroup>
        <Label>{mywishlist.whereToBuy}</Label>
        <Input
          name="whereToBuy"
          placeholder={mywishlist.whereToBuyPlaceholder}
        />
      </FormGroup>
      <FormGroup>
        <Label>{mywishlist.category}</Label>
        <Input type="select" name="categoryId">
          {options}
        </Input>
      </FormGroup>
      <FormGroup>
        <Label>{mywishlist.image}</Label>
        <Input type="file" onChange={(e) => changeImage(e)} />
      </FormGroup>
      <FormGroup>
        <Input hidden name="picture" />
      </FormGroup>
      <Button color="primary" block type="submit">
        <Spinner hidden size="sm" /> {mywishlist.addModalButton}
      </Button>
    </Form>
  );
  setShow(true);
}

function editGiftModal(
  gift: any,
  setModalTitle: any,
  setModalBody: any,
  setShow: any,
  mywishlist: any,
  token: string,
  appDispatch: any,
  categories: any,
  setCategories: any
) {
  appDispatch(clearMessage());

  setModalTitle(mywishlist.updateGiftModalTitle);

  let onFormSubmit = (e: any) => {
    e.preventDefault();
    if (e.target.name.value === "") {
      appDispatch(addMessage(mywishlist.nameErrorMessage));
      return;
    }
    let imageName =
      e.target.picture === undefined ? "" : e.target.picture.value;
    const request = async () => {
      const response = await fetch(url + "/gifts/" + gift.id, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          name: e.target.name.value,
          description: e.target.description.value,
          price: e.target.price.value,
          whereToBuy: e.target.whereToBuy.value,
          categoryId: e.target.categoryId.value,
          picture: imageName,
          rank: gift.rank,
        }),
      });
      if (response.status === 200) {
        getGifts(token, setCategories, appDispatch);
        setShow(false);
      } else if (response.status === 401) {
        appDispatch(logout());
      } else {
        const json = await response.json();
        console.error(json);
        appDispatch(addMessage(json.error));
      }
    };
    request();
  };

  let changeImage = (e: any) => {
    e.target.form[7].disabled = true;
    e.target.form[7].children[0].hidden = false;
    const formData = new FormData();
    formData.append("0", e.target.files[0]);
    const request = async () => {
      const response = await fetch(url + "/files", {
        method: "post",
        headers: { Authorization: `Bearer ${token}` },
        body: formData,
      });
      if (response.status === 401) {
        appDispatch(logout());
      } else if (response.status === 202) {
        const json = await response.json();
        e.target.form[6].value = json.name; //TODO: horrible. to replace by states
        e.target.form[7].disabled = false;
        e.target.form[7].children[0].hidden = true;
      } else {
        const json = await response.json();
        console.error(json);
        appDispatch(addMessage(json.error));
      }
    };
    request();
  };

  const options = categories.map((cag: any, index: any) => {
    let value = cag.category;
    if (gift.categoryId === value.id) {
      return (
        <option key={index} value={value.id} selected>
          {value.name}
        </option>
      );
    } else {
      return (
        <option key={index} value={value.id}>
          {value.name}
        </option>
      );
    }
  });

  setModalBody(
    <Form onSubmit={onFormSubmit}>
      <FormGroup>
        <Label>{mywishlist.name}</Label>
        <Input name="name" defaultValue={gift.name} />
        <FormFeedback>{mywishlist.nameErrorMessage}</FormFeedback>
      </FormGroup>
      <FormGroup>
        <Label>{mywishlist.description}</Label>
        <Input
          type="textarea"
          name="description"
          defaultValue={gift.description}
        />
      </FormGroup>
      <FormGroup>
        <Label>{mywishlist.price}</Label>
        <Input name="price" defaultValue={gift.price} />
      </FormGroup>
      <FormGroup>
        <Label>{mywishlist.whereToBuy}</Label>
        <Input name="whereToBuy" defaultValue={gift.whereToBuy} />
      </FormGroup>
      <FormGroup>
        <Label>{mywishlist.category}</Label>
        <Input type="select" name="categoryId">
          {options}
        </Input>
      </FormGroup>
      <FormGroup>
        <Label>{mywishlist.image}</Label>
        <Input type="file" onChange={(e) => changeImage(e)} />
      </FormGroup>
      <FormGroup>
        <Input hidden name="picture" defaultValue={gift.picture} />
      </FormGroup>
      <Button color="primary" block type="submit">
        <Spinner hidden size="sm" /> {mywishlist.updateModalButton}
      </Button>
    </Form>
  );
  setShow(true);
}

function deleteGiftModal(
  id: number,
  setModalTitle: any,
  setModalBody: any,
  setShow: any,
  mywishlist: any,
  token: string,
  appDispatch: any,
  setCategories: any
) {
  appDispatch(clearMessage());

  setModalTitle(mywishlist.deleteGiftModalTitle);

  let onFormSubmit = (e: any) => {
    e.preventDefault();
    const request = async () => {
      const response = await fetch(
        url + "/gifts/" + id + "?status=" + e.nativeEvent.submitter.value,
        { method: "delete", headers: { Authorization: `Bearer ${token}` } }
      );
      if (response.status === 202) {
        getGifts(token, setCategories, appDispatch);
        setShow(false);
      } else if (response.status === 401) {
        appDispatch(logout());
      } else {
        const json = await response.json();
        console.error(json);
        appDispatch(addMessage(json.error));
      }
    };
    request();
  };

  setModalBody(
    <Form onSubmit={onFormSubmit}>
      <Button color="primary" type="submit" value="RECEIVED">
        {mywishlist.deleteModalButtonReceived}
      </Button>{" "}
      <Button color="primary" type="submit" value="NOT_WANTED">
        {mywishlist.deleteModalButtonNotWanted}
      </Button>
    </Form>
  );
  setShow(true);
}

function rankGift(
  id: number,
  downUp: number,
  token: string,
  appDispatch: any,
  setCategories: any
) {
  //0 = down , other = up
  const val = downUp === 0 ? "up" : "down";
  const request = async () => {
    const response = await fetch(
      url + "/gifts/" + id + "/rank-actions/" + val,
      { method: "post", headers: { Authorization: `Bearer ${token}` } }
    );
    if (response.status === 202) {
      getGifts(token, setCategories, appDispatch);
    } else if (response.status === 401) {
      appDispatch(logout());
    } else {
      const json = await response.json();
      console.error(json);
      appDispatch(addMessage(json.error));
    }
  };
  request();
}

function addCategoryModal(
  setModalTitle: any,
  setModalBody: any,
  setShow: any,
  mywishlist: any,
  token: string,
  appDispatch: any,
  setCategories: any,
  friends: any
) {
  appDispatch(clearMessage());

  setModalTitle(mywishlist.addCategoryModalTitle);

  let onFormSubmit = (e: any) => {
    e.preventDefault();
    let share: string[] = [];
    for (var i = 0; i < e.target.length; i++) {
      const input = e.target[i];
      if (input.type === "checkbox" && input.checked) {
        share.push(input.name);
      }
    }
    if (e.target.name.value === "") {
      appDispatch(addMessage(mywishlist.nameErrorMessage));
      return;
    }
    const request = async () => {
      const response = await fetch(url + "/categories", {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          name: e.target.name.value,
          rank: null,
          share: share,
        }),
      });
      if (response.status === 200) {
        getGifts(token, setCategories, appDispatch);
        setShow(false);
      } else if (response.status === 401) {
        appDispatch(logout());
      } else {
        const json = await response.json();
        appDispatch(addMessage(json.error));
      }
    };
    request();
  };

  const names = friends.map((f: any, i: any) => {
    return (
      <FormGroup check>
        <Label check>
          <Input type="checkbox" id={f.id + "-" + i} name={f.otherUser.name} />
          {f.otherUser.name}
        </Label>
      </FormGroup>
    );
  });

  setModalBody(
    <Form onSubmit={onFormSubmit}>
      <FormGroup>
        <Label>{mywishlist.name}</Label>
        <Input name="name" placeholder={mywishlist.name} />
      </FormGroup>
      <FormGroup name="share">
        <Label>{mywishlist.sharedWith}</Label>
        {names}
      </FormGroup>
      <Button color="primary" block type="submit">
        {mywishlist.addModalButton}
      </Button>
    </Form>
  );
  setShow(true);
}

function editCategoryModal(
  name: string,
  id: number,
  rank: number,
  setModalTitle: any,
  setModalBody: any,
  setShow: any,
  mywishlist: any,
  token: string,
  appDispatch: any,
  setCategories: any,
  friends: any
) {
  appDispatch(clearMessage());

  setModalTitle(mywishlist.updateCategoryModalTitle);

  let onFormSubmit = (e: any) => {
    e.preventDefault();
    let share: string[] = [];
    for (var i = 0; i < e.target.length; i++) {
      const input = e.target[i];
      if (input.type === "checkbox" && input.checked) {
        share.push(input.name);
      }
    }
    if (e.target.name.value === "") {
      appDispatch(addMessage(mywishlist.nameErrorMessage));
      return;
    }
    const request = async () => {
      const response = await fetch(url + "/categories/" + id, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          name: e.target.name.value,
          rank: rank,
          share: share,
        }),
      });
      if (response.status === 200) {
        getGifts(token, setCategories, appDispatch);
        setShow(false);
      } else if (response.status === 401) {
        appDispatch(logout());
      } else {
        const json = await response.json();
        appDispatch(addMessage(json.error));
      }
    };
    request();
  };

  const names = friends.map((f: any, i: any) => {
    return (
      <FormGroup check>
        <Label check>
          <Input type="checkbox" id={f.id + "-" + i} name={f.otherUser.name} />
          {f.otherUser.name}
        </Label>
      </FormGroup>
    );
  });

  setModalBody(
    <Form onSubmit={onFormSubmit}>
      <FormGroup>
        <Label>{mywishlist.name}</Label>
        <Input name="name" defaultValue={name} />
      </FormGroup>
      <FormGroup name="share">
        <Label>{mywishlist.sharedWith}</Label>
        {names}
      </FormGroup>
      <Button color="primary" block type="submit">
        {mywishlist.updateModalButton}
      </Button>
    </Form>
  );
  setShow(true);
}

function deleteCat(
  id: number,
  token: string,
  appDispatch: any,
  setCategories: any
) {
  const request = async () => {
    const response = await fetch(url + "/categories/" + id, {
      method: "delete",
      headers: { Authorization: `Bearer ${token}` },
    });
    if (response.status === 202) {
      getGifts(token, setCategories, appDispatch);
    } else if (response.status === 401) {
      appDispatch(logout());
    } else {
      const json = await response.json();
      console.error(json);
      appDispatch(addMessage(json.error));
    }
  };
  request();
}

function heartGift(
  id: number,
  heart: boolean,
  token: string,
  appDispatch: any,
  setCategories: any
) {
  const request = async () => {
    let req_heart = heart ? "unlike" : "like";
    const response = await fetch(url + "/gifts/" + id + "/heart/" + req_heart, {
      method: "post",
      headers: { Authorization: `Bearer ${token}` },
    });
    if (response.status === 202) {
      getGifts(token, setCategories, appDispatch);
    } else if (response.status === 401) {
      appDispatch(logout());
    } else {
      const json = await response.json();
      console.error(json);
      appDispatch(addMessage(json.error));
    }
  };
  request();
}

function rankCategory(
  id: number,
  downUp: number,
  token: string,
  appDispatch: any,
  setCategories: any
) {
  //0 = down , other = up
  const val = downUp === 0 ? "down" : "up";
  const request = async () => {
    const response = await fetch(
      url + "/categories/" + id + "/rank-actions/" + val,
      { method: "post", headers: { Authorization: `Bearer ${token}` } }
    );
    if (response.status === 202) {
      getGifts(token, setCategories, appDispatch);
    } else if (response.status === 401) {
      appDispatch(logout());
    } else {
      const json = await response.json();
      console.log(json);
      appDispatch(addMessage(json.error));
    }
  };
  request();
}

function renderInsideGift(
  cgi: number,
  gi: number,
  giftHover: string,
  gift: any,
  setModalTitle: any,
  setModalBody: any,
  setShow: any,
  mywishlist: any,
  token: any,
  appDispatch: any,
  categories: any,
  setCategories: any
) {
  console.log(isMobile);
  if (cgi + "-" + gi === giftHover || isMobile) {
    return (
      <div
        style={{ cursor: "pointer" }}
        onClick={() =>
          editGiftModal(
            gift,
            setModalTitle,
            setModalBody,
            setShow,
            mywishlist,
            token,
            appDispatch,
            categories,
            setCategories
          )
        }
      >
        <div className="card-name">{gift.name}</div>
        <div className="card-description">{gift.description}</div>
        <div className="mycard-footer">
          <div className="card-wtb">{gift.whereToBuy}</div>
          <div className="card-price">{gift.price}</div>
        </div>
      </div>
    );
  } else {
    return <div className="card-name-only">{gift.name}</div>;
  }
}

function renderGifts(
  categories: any,
  token: string,
  appDispatch: any,
  setCategories: any,
  setModalTitle: any,
  setModalBody: any,
  setShow: any,
  mywishlist: any,
  giftHover: string,
  setGiftHover: any,
  friends: any
) {
  if (categories) {
    return categories.map((cg: any, cgi: any) => {
      return (
        <div key={cgi}>
          <h5 style={{ margin: "10px" }}>
            {cg.category.name}{" "}
            <span
              style={{ cursor: "pointer" }}
              onClick={() =>
                editCategoryModal(
                  cg.category.name,
                  cg.category.id,
                  cg.category.rank,
                  setModalTitle,
                  setModalBody,
                  setShow,
                  mywishlist,
                  token,
                  appDispatch,
                  setCategories,
                  friends
                )
              }
            >
              <PencilIcon verticalAlign="middle" />
            </span>{" "}
            <span
              style={{ cursor: "pointer" }}
              onClick={() =>
                deleteCat(cg.category.id, token, appDispatch, setCategories)
              }
            >
              <XIcon verticalAlign="middle" />
            </span>
          </h5>

          <div className="mycard-row">
            {cg.gifts.map((gift: any, gi: any) => {
              return (
                <div
                  className="mycard"
                  onMouseEnter={() => setGiftHover(cgi + "-" + gi)}
                  onMouseLeave={() => setGiftHover("")}
                  key={gi}
                >
                  <div
                    className={
                      gift.heart
                        ? "heart-selected two-icon-first"
                        : "two-icon-first"
                    }
                  >
                    <span
                      style={{ cursor: "pointer" }}
                      onClick={() =>
                        heartGift(
                          gift.id,
                          gift.heart,
                          token,
                          appDispatch,
                          setCategories
                        )
                      }
                    >
                      <HeartIcon />
                    </span>
                  </div>
                  <div className="card-edit-close two-icon-second">
                    <span
                      style={{ cursor: "pointer" }}
                      onClick={() =>
                        deleteGiftModal(
                          gift.id,
                          setModalTitle,
                          setModalBody,
                          setShow,
                          mywishlist,
                          token,
                          appDispatch,
                          setCategories
                        )
                      }
                    >
                      <XIcon />
                    </span>
                  </div>
                  <div
                    style={{ cursor: "pointer" }}
                    onClick={() =>
                      editGiftModal(
                        gift,
                        setModalTitle,
                        setModalBody,
                        setShow,
                        mywishlist,
                        token,
                        appDispatch,
                        categories,
                        setCategories
                      )
                    }
                  >
                    <SquareImage
                      token={token}
                      className="card-image"
                      imageName={gift.picture}
                      size={150}
                      alt="Gift"
                      alternateImage={blank_gift}
                    />
                  </div>
                  {renderInsideGift(
                    cgi,
                    gi,
                    giftHover,
                    gift,
                    setModalTitle,
                    setModalBody,
                    setShow,
                    mywishlist,
                    token,
                    appDispatch,
                    categories,
                    setCategories
                  )}
                </div>
              );
            })}
          </div>
        </div>
      );
    });
  }
}

function renderGiftsEditMode(
  categories: any,
  token: string,
  appDispatch: any,
  setCategories: any,
  setModalTitle: any,
  setModalBody: any,
  setShow: any,
  mywishlist: any,
  giftHover: string,
  setGiftHover: any
) {
  return categories.map((cg: any, cgi: any) => {
    let displayDown = cgi !== categories.length - 1;
    let displayUp = cgi !== 0;
    return (
      <div key={cgi}>
        <h5 style={{ margin: "10px" }}>
          {cg.category.name} - {cg.category.rank}{" "}
          {displayDown && (
            <span
              style={{ cursor: "pointer" }}
              onClick={() =>
                rankCategory(
                  cg.category.id,
                  1,
                  token,
                  appDispatch,
                  setCategories
                )
              }
            >
              <ArrowDownIcon verticalAlign="middle" />
            </span>
          )}{" "}
          {displayUp && (
            <span
              style={{ cursor: "pointer" }}
              onClick={() =>
                rankCategory(
                  cg.category.id,
                  0,
                  token,
                  appDispatch,
                  setCategories
                )
              }
            >
              <ArrowUpIcon verticalAlign="middle" />
            </span>
          )}
        </h5>

        <div className="mycard-row">
          {cg.gifts.map((gift: any, gi: any) => {
            let displayLeft = gi !== 0;
            let displayRight = gi !== cg.gifts.length - 1;
            return (
              <div className="mycard">
                <div className="card-edit-close">
                  <div className="two-icon-first">
                    {displayLeft && (
                      <span
                        style={{ cursor: "pointer" }}
                        onClick={() =>
                          rankGift(
                            gift.id,
                            1,
                            token,
                            appDispatch,
                            setCategories
                          )
                        }
                      >
                        <ArrowLeftIcon verticalAlign="middle" />
                      </span>
                    )}
                  </div>
                  <div className="two-icon-second">
                    {displayRight && (
                      <span
                        style={{ cursor: "pointer" }}
                        onClick={() =>
                          rankGift(
                            gift.id,
                            0,
                            token,
                            appDispatch,
                            setCategories
                          )
                        }
                      >
                        <ArrowRightIcon verticalAlign="middle" />
                      </span>
                    )}
                  </div>
                </div>
                <SquareImage
                  token={token}
                  className="card-image"
                  imageName={gift.picture}
                  size={150}
                  alt="Gift"
                  alternateImage={blank_gift}
                />
                <div></div>
                {renderInsideGift(
                  cgi,
                  gi,
                  giftHover,
                  gift,
                  setModalTitle,
                  setModalBody,
                  setShow,
                  mywishlist,
                  token,
                  appDispatch,
                  categories,
                  setCategories
                )}
              </div>
            );
          })}
        </div>
      </div>
    );
  });
}

function MyWishList() {
  const token = useAppSelector(selectSignIn).token;
  const mywishlist = useAppSelector(selectMessages).mywishlist;
  const errorMessage = useAppSelector(selectErrorMessage);

  const appDispatch = useAppDispatch();

  let navigate = useNavigate();

  const [editMode, setEditMode] = useState(false);
  const [show, setShow] = useState(false);
  const handleClose = () => setShow(false);

  const [modalTitle, setModalTitle] = useState("title");
  const [modalBody, setModalBody] = useState(<div></div>);

  const [categories, setCategories] = useState([]);
  const [giftHover, setGiftHover] = useState("");

  const [friends, setFriends] = useState([]);

  useEffect(() => {
    if (token) {
      getGifts(token, setCategories, appDispatch);
      getFriends(token, setFriends, appDispatch);
    }
  }, [token, appDispatch]);

  if (token) {
    return (
      <div>
        <Button
          color="link"
          disabled={editMode}
          onClick={() =>
            addGiftModal(
              setModalTitle,
              setModalBody,
              setShow,
              mywishlist,
              token,
              appDispatch,
              categories,
              setCategories
            )
          }
        >
          {mywishlist.addGiftButton}
        </Button>
        <Button
          color="link"
          disabled={editMode}
          onClick={() =>
            addCategoryModal(
              setModalTitle,
              setModalBody,
              setShow,
              mywishlist,
              token,
              appDispatch,
              setCategories,
              friends
            )
          }
        >
          {mywishlist.addCategoryButton}
        </Button>
        <Button
          color="link"
          onClick={() => {
            setEditMode(!editMode);
          }}
        >
          {mywishlist.reorderButtonTitle}
        </Button>

        {editMode
          ? renderGiftsEditMode(
              categories,
              token,
              appDispatch,
              setCategories,
              setModalTitle,
              setModalBody,
              setShow,
              mywishlist,
              giftHover,
              setGiftHover
            )
          : renderGifts(
              categories,
              token,
              appDispatch,
              setCategories,
              setModalTitle,
              setModalBody,
              setShow,
              mywishlist,
              giftHover,
              setGiftHover,
              friends
            )}

        <Modal isOpen={show} toggle={handleClose}>
          <ModalHeader toggle={handleClose}>{modalTitle}</ModalHeader>
          <ModalBody>
            {errorMessage && <p className="auth-error">{errorMessage}</p>}
            {modalBody}
          </ModalBody>
        </Modal>
      </div>
    );
  } else {
    console.log("Unauthorized... Redirecting...");
    navigate("../signin");
    return <div></div>;
  }
}

export default MyWishList;
