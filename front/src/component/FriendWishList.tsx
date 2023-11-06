import React, { useEffect, useState } from "react";

import { useNavigate, useParams } from "react-router-dom";
import {
  GiftIcon,
  PencilIcon,
  TrashIcon,
} from "@primer/octicons-react";
import { Modal, ModalHeader, ModalBody, ModalFooter, Button } from "reactstrap";

import "./style/card-gift.css";
import SquareImage from "./SquareImage";
import blank_gift from "./image/christmas-easter/blank_gift.png";
import knot from "./image/christmas-easter/knot.png";
import selected_tree from "./image/christmas-easter/christmas-tree-selected.png";
import sled from "./image/christmas-easter/sled.svg";
import selected_sled from "./image/christmas-easter/sled-selected.png";

import { isMobile } from "react-device-detect";

import { useAppSelector, useAppDispatch } from "../redux/store";
import { selectMessages } from "../redux/reducers/locale";
import { selectSignIn, logout } from "../redux/reducers/signin";

import AddGiftModal from "./modals/AddGiftModal";
import EditGiftModal from "./modals/EditGiftModal";
import DeleteGiftModal from "./modals/DeleteGiftModal";

import { getServerUrl } from "../ServerInformation";
let url = getServerUrl();

function FriendWishList() {
  const params: any = useParams();

  const username = useAppSelector(selectSignIn).username;
  const token = useAppSelector(selectSignIn).token;
  const friendwishlist = useAppSelector(selectMessages).friendwishlist;
  const mywishlist = useAppSelector(selectMessages).mywishlist;

  const [errorMessage, setErrorMessage] = useState("");

  const appDispatch = useAppDispatch();

  let navigate = useNavigate();

  const [categories, setCategories] = useState([]);
  const [giftHover, setGiftHover] = useState("");

  const [showGift, setShowGift] = useState(false);
  const handleCloseGift = () => setShowGift(false);
  const [modalGiftTitle, setModalGiftTitle] = useState("title");
  const [modalGiftBody, setModalGiftBody] = useState(<div></div>);
  const [modalGiftFooter, setModalGiftFooter] = useState(<div></div>);

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

  useEffect(() => {
    if (token) {
      const request = async () => {
        const response = await fetch(url + "/gifts/" + params.name, {
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
    }
  }, [params.name, token, appDispatch]);

  if (token && username) {
    const getGifts = () => {
      const request = async () => {
        const response = await fetch(url + "/gifts/" + params.name, {
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

    const reserve = (giftId: number, reserve: boolean) => {
      const request = async () => {
        const response = await fetch(url + "/gifts/" + giftId + "/reserve", {
          method: reserve ? "DELETE" : "POST",
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

    const renderInsideGift = (cgi: number, gi: number, fGift: any) => {
      const { gift, secret } = fGift;
      if (cgi + "-" + gi === giftHover || isMobile) {
        return (
          <>
            <div style={{ cursor: "pointer" }} onClick={() => openGift(fGift)}>
              <div className="card-name">{gift.name}</div>
              <div className="card-description">{gift.description}</div>
              <div className="mycard-footer">
                <div className="card-wtb">{gift.whereToBuy}</div>
                <div className="card-price">{gift.price}</div>
              </div>
            </div>
          </>
        );
      } else {
        const className = secret ? "card-name-only-secret" : "card-name-only";
        return <div className={className}>{gift.name}</div>;
      }
    };

    const openGift = (fGift: any) => {
      setShowGift(true);
      const { gift } = fGift;
      setModalGiftTitle(gift.name);

      const isContainer = isMobile ? "" : "container";
      const padding: string = isMobile ? "0px" : "10px";
      let wtb: string[] = [];
      if (gift.whereToBuy !== undefined) {
        wtb = gift.whereToBuy.split(" ");
      }

      //Duplicated
      const { reservedBy } = fGift;
      let reservedByMe = false;
      if (username !== null) {
        for (const [, value] of reservedBy.entries()) {
          if (value === username) reservedByMe = true;
        }
      }

      setModalGiftBody(
        <div className={isContainer}>
          <SquareImage
            token={token}
            className="card-image"
            imageName={gift.picture}
            size={300}
            alt="Gift"
            alternateImage={blank_gift}
          />
          <div style={{ padding: padding }}>
            {gift.description !== "" && (
              <>
                <div>
                  {mywishlist.description}: {gift.description}
                </div>
                <br />
              </>
            )}
            {gift.price !== "" && (
              <div>
                {mywishlist.price}: {gift.price}
              </div>
            )}
            {gift.whereToBuy !== "" && (
              <div>
                {mywishlist.whereToBuy}:{" "}
                {wtb.map((word: string) => {
                  if (word.startsWith("http")) {
                    let smallWord =
                      word.length > 20 ? word.substring(0, 20) + "..." : word;
                    return (
                      <a href={word} target="_blank" rel="noopener noreferrer">
                        {smallWord}{" "}
                      </a>
                    );
                  } else {
                    return word + " ";
                  }
                })}
              </div>
            )}
          </div>
        </div>,
      );
      setModalGiftFooter(
        <div>
          {(reservedBy.length === 0 || reservedByMe) && (
            <>
              <Button
                color={reservedByMe ? "primary" : "secondary"}
                onClick={() => reserve(gift.id, reservedByMe)}
              >
                <GiftIcon /> {friendwishlist.reservedByMe}
              </Button>{" "}
            </>
          )}
        </div>,
      );
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

    const deleteGiftModalOpen = (id: number) => {
      setDeleteGiftId(id);
      setShowDeleteModal(true);
    };

    return (
      <div>
        <h1 className="friend-wishlist-title">
          {friendwishlist.title} {params.name}
        </h1>
        {categories.length > 0 && (
          <Button color="link" onClick={() => setShowAddGiftModal(true)}>
            {mywishlist.addGiftButton}
          </Button>
        )}
        {categories.map((cg: any, cgi: any) => {
          return (
            <div key={cgi}>
              <h5 style={{ margin: "10px" }}>{cg.category.name}</h5>
              <div className="mycard-row">
                {cg.gifts.map((fGift: any, gi: any) => {
                  const { gift, reservedBy, secret } = fGift;
                  let reservedByMe = false;
                  for (const [, value] of reservedBy.entries()) {
                    if (value === username) reservedByMe = true;
                  }

                  const boughtClassName =
                    reservedBy.length === 0 ? "" : " card-already-bought";
                  const secretClassName = secret ? " secret-border" : "";
                  return (
                    <div
                      className={"mycard" + boughtClassName + secretClassName}
                      onMouseEnter={() => setGiftHover(cgi + "-" + gi)}
                      onMouseLeave={() => setGiftHover("")}
                    >
                      {(reservedBy.length === 0 || reservedByMe) && (
                        <div className="card-edit-close">
                          {!secret && (
                            <div
                              className={
                                gift.heart
                                  ? "heart-selected three-icon-first"
                                  : "three-icon-first"
                              }
                            >
                              {gift.heart && (
                                <span>
                                  <img className="christmas-icon" src={selected_tree}/>
                                </span>
                              )}
                            </div>
                          )}
                          {secret && (
                            <div className="three-icon-first">
                              <span
                                style={{ cursor: "pointer" }}
                                onClick={() => editGiftModalOpen(gift)}
                              >
                                <PencilIcon />
                              </span>
                            </div>
                          )}
                          <div className="three-icon-second secret-text">
                            {secret && <>Secret</>}
                          </div>
                          <div className="three-icon-third">
                            <span
                              style={{ cursor: "pointer" }}
                              onClick={() => reserve(gift.id, reservedByMe)}
                            >
                              <img className="christmas-icon" src={reservedByMe ? selected_sled : sled}/>
                            </span>
                          </div>
                          {secret && (
                            <div className="three-icon-first">
                              <span
                                style={{ cursor: "pointer" }}
                                onClick={() => deleteGiftModalOpen(gift.id)}
                              >
                                <TrashIcon />
                              </span>
                            </div>
                          )}
                        </div>
                      )}
                      <div
                        style={{ cursor: "pointer" }}
                        onClick={() => openGift(fGift)}
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
                      {renderInsideGift(cgi, gi, fGift)}
                      {reservedBy.length !== 0 && <div>
                                              <img className={(cgi + "-" + gi === giftHover || isMobile)
                                                  ? (secret ? (reservedByMe ? "knot-hover-secret" : "knot-reserved-other-hover-secret")  : (reservedByMe ? "knot-hover" : "knot-reserved-other-hover"))
                                                  : (secret ? (reservedByMe ? "knot" : "knot-reserved-other-secret") : (reservedByMe ? "knot" : "knot-reserved-other"))} src={knot}/>
                                            </div> }
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}

        <Modal isOpen={showGift} toggle={handleCloseGift} size="lg">
          <ModalHeader toggle={handleCloseGift}>{modalGiftTitle}</ModalHeader>
          <ModalBody>
            {errorMessage && <p className="auth-error">{errorMessage}</p>}
            {modalGiftBody}
          </ModalBody>
          <ModalFooter>{modalGiftFooter}</ModalFooter>
        </Modal>

        <AddGiftModal
          show={showAddGiftModal}
          closeModal={() => {
            setShowAddGiftModal(false);
            getGifts();
          }}
          categories={categories}
          friendName={params.name}
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
          received={friendwishlist.deleteModalButtonReceived}
          not_wanted={friendwishlist.deleteModalButtonNotWanted}
        />
      </div>
    );
  } else {
    console.log("Unauthorized... Redirecting...");
    navigate("../signin");
    return <div></div>;
  }
}

export default FriendWishList;
