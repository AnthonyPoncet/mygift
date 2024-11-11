import React, { useEffect, useState } from "react";

import { Button } from "reactstrap";
import { useNavigate } from "react-router-dom";
import {
  HeartIcon,
  PencilIcon,
  TrashIcon,
  ArrowDownIcon,
  ArrowUpIcon,
  ArrowLeftIcon,
  ArrowRightIcon,
} from "@primer/octicons-react";

import AddCategoryModal from "./modals/AddCategoryModal";
import EditCategoryModal from "./modals/EditCategoryModal";
import AddGiftModal from "./modals/AddGiftModal";
import EditGiftModal from "./modals/EditGiftModal";
import DeleteGiftModal from "./modals/DeleteGiftModal";

import { useAppSelector, useAppDispatch } from "../redux/store";
import { selectMessages } from "../redux/reducers/locale";
import { selectSignIn, logout } from "../redux/reducers/signin";

import SquareImage from "./SquareImage";
import blank_gift from "./image/blank_gift.png";

import { isMobile } from "react-device-detect";

import "./style/card-gift.css";

import { getServerUrl } from "../ServerInformation";
let url = getServerUrl();

function MyWishList() {
  const token = useAppSelector(selectSignIn).token;
  const mywishlist = useAppSelector(selectMessages).mywishlist;

  const appDispatch = useAppDispatch();

  let navigate = useNavigate();

  const [errorMessage, setErrorMessage] = useState("");

  const [editMode, setEditMode] = useState(false);

  const [showAddCategoryModal, setShowAddCategoryModal] = useState(false);

  const [showEditCategoryModal, setShowEditCategoryModal] = useState(false);
  const [editCategoryId, setEditCategoryId] = useState(-1);
  const [editCategoryName, setEditCategoryName] = useState("");
  const [editCategoryRank, setEditCategoryRank] = useState(-1);

  const [showAddGiftModal, setShowAddGiftModal] = useState(false);

  const [showEditGiftModal, setShowEditGiftModal] = useState(false);
  const [editGiftName, setEditGiftName] = useState("");
  const [editGiftDescription, setEditGiftDescription] = useState("");
  const [editGiftPrice, setEditGiftPrice] = useState("");
  const [editGiftWhereToBuy, setEditGiftWhereToBuy] = useState("");
  const [editGiftCategory, setEditGiftCategory] = useState(-1);
  const [editGiftPicture, setEditGiftPicture] = useState("");
  const [editGiftId, setEditGiftId] = useState(-1);
  const [editGiftRank, setEditGiftRank] = useState(-1);

  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deleteGiftId, setDeleteGiftId] = useState(-1);

  const [categories, setCategories] = useState([]);
  const [giftHover, setGiftHover] = useState("");

  const [friends, setFriends] = useState([]);

  useEffect(() => {
    if (token) {
      const getGifts = () => {
        setErrorMessage("");
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
              setErrorMessage(json.error);
            }
          }
        };
        request();
      };

      const getFriends = () => {
        setErrorMessage("");
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

      getGifts();
      getFriends();
    }
  }, [token, appDispatch]);

  if (token) {
    const getGifts = () => {
      setErrorMessage("");
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
            setErrorMessage(json.error);
          }
        }
      };
      request();
    };

    const deleteGiftModal = (id: number) => {
      setDeleteGiftId(id);
      setShowDeleteModal(true);
    };

    const rankGift = (id: number, downUp: number) => {
      //0 = down , other = up
      const val = downUp === 0 ? "up" : "down";
      const request = async () => {
        const response = await fetch(
          url + "/gifts/" + id + "/rank-actions/" + val,
          { method: "post", headers: { Authorization: `Bearer ${token}` } },
        );
        if (response.status === 202) {
          getGifts();
        } else if (response.status === 401) {
          appDispatch(logout());
        } else {
          const json = await response.json();
          setErrorMessage(json.error);
        }
      };
      request();
    };

    const deleteCat = (id: number) => {
      const request = async () => {
        const response = await fetch(url + "/categories/" + id, {
          method: "delete",
          headers: { Authorization: `Bearer ${token}` },
        });
        if (response.status === 202) {
          getGifts();
        } else if (response.status === 401) {
          appDispatch(logout());
        } else {
          const json = await response.json();
          setErrorMessage(json.error);
        }
      };
      request();
    };

    const heartGift = (id: number, heart: boolean) => {
      const request = async () => {
        let req_heart = heart ? "unlike" : "like";
        const response = await fetch(
          url + "/gifts/" + id + "/heart/" + req_heart,
          {
            method: "post",
            headers: { Authorization: `Bearer ${token}` },
          },
        );
        if (response.status === 202) {
          getGifts();
        } else if (response.status === 401) {
          appDispatch(logout());
        } else {
          const json = await response.json();
          setErrorMessage(json.error);
        }
      };
      request();
    };

    const rankCategory = (id: number, downUp: number) => {
      //0 = down , other = up
      const val = downUp === 0 ? "down" : "up";
      const request = async () => {
        const response = await fetch(
          url + "/categories/" + id + "/rank-actions/" + val,
          { method: "post", headers: { Authorization: `Bearer ${token}` } },
        );
        if (response.status === 202) {
          getGifts();
        } else if (response.status === 401) {
          appDispatch(logout());
        } else {
          const json = await response.json();
          setErrorMessage(json.error);
        }
      };
      request();
    };

    const editGiftModalOpen = (gift: any) => {
      setEditGiftName(gift.name);
      setEditGiftDescription(gift.description);
      setEditGiftPrice(gift.price);
      setEditGiftWhereToBuy(gift.whereToBuy);
      setEditGiftCategory(gift.categoryId);
      setEditGiftPicture(gift.picture);
      setEditGiftId(gift.id);
      setEditGiftRank(gift.rank);
      setShowEditGiftModal(true);
    };

    const renderGifts = () => {
      if (categories) {
        return categories.map((cg: any, cgi: any) => {
          return (
            <div key={cgi}>
              <h5 style={{ margin: "10px" }}>
                {cg.category.name}{" "}
                <span
                  style={{ cursor: "pointer" }}
                  onClick={() => {
                    setEditCategoryId(cg.category.id);
                    setEditCategoryName(cg.category.name);
                    setEditCategoryRank(cg.category.rank);
                    setShowEditCategoryModal(true);
                  }}
                >
                  <PencilIcon verticalAlign="middle" />
                </span>{" "}
                <span
                  style={{ cursor: "pointer" }}
                  onClick={() => deleteCat(cg.category.id)}
                >
                  <TrashIcon verticalAlign="middle" />
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
                          onClick={() => heartGift(gift.id, gift.heart)}
                        >
                          <HeartIcon />
                        </span>
                      </div>
                      <div className="card-edit-close two-icon-second">
                        <span
                          style={{ cursor: "pointer" }}
                          onClick={() => deleteGiftModal(gift.id)}
                        >
                          <TrashIcon />
                        </span>
                      </div>
                      <div
                        style={{ cursor: "pointer" }}
                        onClick={() => editGiftModalOpen(gift)}
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
                      {renderInsideGift(cgi, gi, gift)}
                    </div>
                  );
                })}
              </div>
            </div>
          );
        });
      }
    };

    const renderInsideGift = (cgi: number, gi: number, gift: any) => {
      if (cgi + "-" + gi === giftHover || isMobile) {
        return (
          <div
            style={{ cursor: "pointer" }}
            onClick={() => editGiftModalOpen(gift)}
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
    };

    const renderGiftsEditMode = () => {
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
                  onClick={() => rankCategory(cg.category.id, 1)}
                >
                  <ArrowDownIcon verticalAlign="middle" />
                </span>
              )}{" "}
              {displayUp && (
                <span
                  style={{ cursor: "pointer" }}
                  onClick={() => rankCategory(cg.category.id, 0)}
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
                            onClick={() => rankGift(gift.id, 1)}
                          >
                            <ArrowLeftIcon verticalAlign="middle" />
                          </span>
                        )}
                      </div>
                      <div className="two-icon-second">
                        {displayRight && (
                          <span
                            style={{ cursor: "pointer" }}
                            onClick={() => rankGift(gift.id, 0)}
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
                    {renderInsideGift(cgi, gi, gift)}
                  </div>
                );
              })}
            </div>
          </div>
        );
      });
    };

    return (
      <div>
        {errorMessage && <p className="auth-error">{errorMessage}</p>}
        <Button
          color="link"
          disabled={editMode}
          onClick={() => setShowAddGiftModal(true)}
        >
          {mywishlist.addGiftButton}
        </Button>
        <Button
          color="link"
          disabled={editMode}
          onClick={() => setShowAddCategoryModal(true)}
        >
          {mywishlist.addCategoryButton}
        </Button>
        <Button color="link" onClick={() => setEditMode(!editMode)}>
          {mywishlist.reorderButtonTitle}
        </Button>

        {editMode ? renderGiftsEditMode() : renderGifts()}

        <AddCategoryModal
          show={showAddCategoryModal}
          closeModal={() => {
            setShowAddCategoryModal(false);
            getGifts();
          }}
          friends={friends}
        />
        <EditCategoryModal
          show={showEditCategoryModal}
          closeModal={() => {
            setShowEditCategoryModal(false);
            getGifts();
          }}
          friends={friends}
          id={editCategoryId}
          name={editCategoryName}
          setName={setEditCategoryName}
          rank={editCategoryRank}
        />
        <AddGiftModal
          show={showAddGiftModal}
          closeModal={() => {
            setShowAddGiftModal(false);
            getGifts();
          }}
          categories={categories}
          friendName={null}
        />
        <EditGiftModal
          show={showEditGiftModal}
          closeModal={() => {
            setShowEditGiftModal(false);
            getGifts();
          }}
          categories={categories}
          name={editGiftName}
          setName={setEditGiftName}
          description={editGiftDescription}
          setDescription={setEditGiftDescription}
          price={editGiftPrice}
          setPrice={setEditGiftPrice}
          whereToBuy={editGiftWhereToBuy}
          setWhereToBuy={setEditGiftWhereToBuy}
          categoryId={editGiftCategory}
          picture={editGiftPicture}
          giftId={editGiftId}
          rank={editGiftRank}
        />
        <DeleteGiftModal
          show={showDeleteModal}
          closeModal={() => {
            setShowDeleteModal(false);
            getGifts();
          }}
          giftId={deleteGiftId}
          received={mywishlist.deleteModalButtonReceived}
          not_wanted={mywishlist.deleteModalButtonNotWanted}
        />
      </div>
    );
  } else {
    console.log("Unauthorized... Redirecting...");
    navigate("../signin");
    return <div></div>;
  }
}

export default MyWishList;
