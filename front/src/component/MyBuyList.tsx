import React, { useEffect, useState }  from 'react';

import { useNavigate } from "react-router-dom";
import { GiftIcon } from '@primer/octicons-react';
import { Modal, ModalHeader, ModalBody, ModalFooter, Button } from 'reactstrap';

import { useAppSelector, useAppDispatch } from '../redux/store';
import { selectMessages } from '../redux/reducers/locale';
import { addMessage, selectErrorMessage } from '../redux/reducers/error';
import { selectSignIn, logout } from '../redux/reducers/signin';

import './style/card-gift.css';
import SquareImage from './SquareImage';
import blank_gift from './image/blank_gift.png';

import { isMobile } from 'react-device-detect';

import { getServerUrl } from '../ServerInformation';
let url = getServerUrl();

function getBuyList(token: string, setBuyList: any, appDispatch: any) {
    const request = async () => {
        const response = await fetch(url + '/buy-list', {
            method: "GET",
            headers: {'Authorization': `Bearer ${token}`}
        });
        if (response.status === 401) {
            appDispatch(logout());
        } else {
            const json = await response.json();
            if (response.status === 200) {
                setBuyList(json);
            } else {
                console.error(json.error);
                appDispatch(addMessage(json.error));
            }
        }
    };
    request();
};

function reserve(giftId: number, reserve: boolean, token: string, setBuyList: any, appDispatch: any) {
    const request = async () => {
        const response = await fetch(url + '/gifts/' + giftId + '/reserve', {
            method: reserve ? "DELETE" : "POST",
            headers: {'Authorization': `Bearer ${token}`}
        });
        if (response.status === 202) {
            getBuyList(token, setBuyList, appDispatch);
        } else if (response.status === 401) {
            appDispatch(logout());
        } else {
            const json = await response.json();
            console.error(json.error);
            appDispatch(addMessage(json.error));
        }
    };
    request();
};

function deleteDeletedGift(giftId: number, token: string, setBuyList: any, setShowGift: any, appDispatch: any) {
    const request = async () => {
        const response = await fetch(url + '/buy-list/deleted-gifts/' + giftId, {method: "DELETE", headers: {'Authorization': `Bearer ${token}`}});
        if (response.status === 200) {
            getBuyList(token, setBuyList, appDispatch);
            setShowGift(false);
        } else if (response.status === 401) {
            appDispatch(logout());
        } else {
            const json = await response.json();
            console.error(json.error);
            appDispatch(addMessage(json.error));
        }
    };
    request();
};

function openGift(fGift: any, token: string, setShowGift: any, setModalGiftTitle: any, setModalGiftBody: any, setModalGiftFooter: any, setBuyList: any, appDispatch: any, friendwishlist: any, mywishlist: any, username: string) {
    setShowGift(true);
    const { gift } = fGift
    setModalGiftTitle(gift.name);

    const isContainer = isMobile ? "" : "container";
    const padding: string = isMobile ? "0px" : "10px";
    let wtb : string[] = [];
    if (gift.whereToBuy !== undefined) {
        wtb = gift.whereToBuy.split(" ");
    }

    //Duplicated
    const { reservedBy } = fGift;
    let reservedByMe = false;
    if (username !== null) {
        for (const [, value] of reservedBy.entries()) { if (value === username) reservedByMe = true; }
    }

    setModalGiftBody(
    <div className={isContainer}>
        <SquareImage token={token} className="card-image" imageName={gift.picture} size={300} alt="Gift" alternateImage={blank_gift}/>
        <div style={{padding: padding}}>
            {(gift.description !== "") && <><div>{mywishlist.description}: {gift.description}</div><br/></>}
            {(gift.price !== "") && <div>{mywishlist.price}: {gift.price}</div>}
            {(gift.whereToBuy !== "") &&
                <div>{mywishlist.whereToBuy}: {
                    wtb.map((word: string) => {
                        if (word.startsWith("http")) {
                            let smallWord = word.length > 20 ? word.substring(0,20) + '...' : word;
                            return <a href={word} target="_blank" rel="noopener noreferrer">{smallWord}{' '}</a>;
                        } else {
                            return word + " ";
                        }
                    })
                }
                </div>
            }
        </div>
    </div>
    );
    setModalGiftFooter(
        <div>
        { (reservedBy.length === 0 || reservedByMe) &&
            <>
            <Button color={reservedByMe ? "primary" : "secondary"} onClick={() => reserve(gift.id, reservedByMe, token, setBuyList, appDispatch)}><GiftIcon/> {friendwishlist.reservedByMe}</Button>{' '}
            </>
        }
        </div>
    );
}

function openDeletedGift(gift: any, token: string, setShowGift: any, setModalGiftTitle: any, setModalGiftBody: any, setModalGiftFooter: any, setBuyList: any, appDispatch: any, myBuyList: any, mywishlist: any, username: string) {
    setShowGift(true);
    setModalGiftTitle(gift.name);

    const isContainer = isMobile ? "" : "container";
    const padding: string = isMobile ? "0px" : "10px";
    let wtb : string[] = [];
    if (gift.whereToBuy !== undefined) {
        wtb = gift.whereToBuy.split(" ");
    }

    setModalGiftBody(
    <div className={isContainer}>
        <SquareImage token={token} className="card-image" imageName={gift.picture} size={300} alt="Gift" alternateImage={blank_gift}/>
        <div style={{padding: padding}}>
            {(gift.description !== "") && <><div>{mywishlist.description}: {gift.description}</div><br/></>}
            {(gift.price !== "") && <div>{mywishlist.price}: {gift.price}</div>}
            {(gift.whereToBuy !== "") &&
                <div>{mywishlist.whereToBuy}: {
                    wtb.map((word: string) => {
                        if (word.startsWith("http")) {
                            let smallWord = word.length > 20 ? word.substring(0,20) + '...' : word;
                            return <a href={word} target="_blank" rel="noopener noreferrer">{smallWord}{' '}</a>;
                        } else {
                            return word + " ";
                        }
                    })
                }
                </div>
            }
        </div>
    </div>
    );
    setModalGiftFooter(
        <Button color="primary" onClick={() => deleteDeletedGift(gift.id, token, setBuyList, setShowGift, appDispatch)}>{myBuyList.ok}</Button>
    );
}

function renderInsideGift(fgi: number, gi: number, type: string, fGift: any, giftHover: any, showFun: Function) {
    const { gift } = fGift;
    if ((fgi+'-'+gi+'-'+type === giftHover) || isMobile) {
        return (
        <div style={{cursor: "pointer"}} onClick={() => showFun(fGift)}>
            <div className="card-name">{gift.name}</div>
            <div className="card-description">{gift.description}</div>
            <div className="mycard-footer">
                <div className="card-wtb">{gift.whereToBuy}</div>
                <div className="card-price">{gift.price}</div>
            </div>
        </div>);
    } else {
        return (<div className="card-name-only">{gift.name}</div>);
    }
}

function MyBuyList() {
    const username = useAppSelector(selectSignIn).username;
    const token = useAppSelector(selectSignIn).token;
    const friendwishlist = useAppSelector(selectMessages).friendwishlist;
    const myBuyList = useAppSelector(selectMessages).myBuyList;
    const mywishlist = useAppSelector(selectMessages).mywishlist;
    const errorMessage = useAppSelector(selectErrorMessage);

    const appDispatch = useAppDispatch();
    let navigate = useNavigate();

    const [buyList, setBuyList] = useState([]);
    const [giftHover, setGiftHover] = useState("");

    const [showGift, setShowGift] = useState(false);
    const handleCloseGift = () => setShowGift(false);
    const [modalGiftTitle, setModalGiftTitle] = useState("title");
    const [modalGiftBody, setModalGiftBody] = useState(<div></div>);
    const [modalGiftFooter, setModalGiftFooter] = useState(<div></div>);

    useEffect(() => {
        if (token) {
            getBuyList(token, setBuyList, appDispatch);
        }
    }, [token, appDispatch]);

    if (token && username) {
        return (
        <div>
            <h1 className="friend-wishlist-title">{myBuyList.title}</h1>
            <div className="mycard-row">
                { buyList.map((fg: any, fgi: number) => { return(
                <div className="mycard-no-limit">
                    <div key={'friendAndGifts'+fgi}>
                        <h5 style={{margin: "10px"}}>{fg.friendName}</h5>
                        <div className="mycard-row">
                            {
                            fg.gifts.map((fGift: any, gi:any) => {
                                const { gift, reservedBy } = fGift;
                                let reservedByMe = false;
                                for (const [, value] of reservedBy.entries()) { if (value === username) reservedByMe = true; }
                                return (
                                <div className="mycard" onMouseEnter={() => setGiftHover(fgi + '-' + gi + '-valid')} onMouseLeave={() => setGiftHover("")} style={{cursor: "pointer"}}>
                                    <div className="card-edit-close">
                                        <div className="icon-selected two-icon-first">
                                            <span style={{cursor: "pointer"}} onClick={() => reserve(gift.id, reservedByMe, token, setBuyList, appDispatch)}><GiftIcon/></span>
                                        </div>
                                    </div>
                                    <div style={{cursor: "pointer"}} onClick={() => openGift(fGift, token, setShowGift, setModalGiftTitle, setModalGiftBody, setModalGiftFooter, setBuyList, appDispatch, friendwishlist, mywishlist, username)}>
                                        <SquareImage token={token} className="card-image" imageName={gift.picture} size={150} alt="Gift" alternateImage={blank_gift}/>
                                    </div>
                                    { renderInsideGift(fgi, gi, 'valid', fGift, giftHover, (gift: any) => openGift(gift, token, setShowGift, setModalGiftTitle, setModalGiftBody, setModalGiftFooter, setBuyList, appDispatch, friendwishlist, mywishlist, username)) }
                                </div>
                                )
                            })
                            }
                            {
                            fg.deletedGifts.map((gift: any, gi:any) => {
                                return (
                                <div className="mycard warning-border" onMouseEnter={() => setGiftHover(fgi + '-' + gi + '-deleted')} onMouseLeave={() => setGiftHover("")} style={{cursor: "pointer"}}>
                                    <div className="card-edit-close warning-gift-delete">
                                        <div>{gift.status === "RECEIVED" ? myBuyList.received : myBuyList.not_wanted}</div>
                                    </div>
                                    <div style={{cursor: "pointer"}} onClick={() => openDeletedGift(gift, token, setShowGift, setModalGiftTitle, setModalGiftBody, setModalGiftFooter, setBuyList, appDispatch, myBuyList, mywishlist, username)}>
                                        <SquareImage token={token} className="card-image" imageName={gift.picture} size={150} alt="Gift" alternateImage={blank_gift}/>
                                    </div>
                                    { renderInsideGift(fgi, gi, 'deleted', {gift: gift}, giftHover, (gift: any) => openDeletedGift(gift, token, setShowGift, setModalGiftTitle, setModalGiftBody, setModalGiftFooter, setBuyList, appDispatch, myBuyList, mywishlist, username)) }
                                </div>);
                                })
                            }
                        </div>
                    </div>
                </div>) }) }
            </div>

            <Modal isOpen={showGift} toggle={handleCloseGift} size="lg">
                <ModalHeader toggle={handleCloseGift}>{modalGiftTitle}</ModalHeader>
                <ModalBody>
                    { errorMessage && <p className="auth-error">{errorMessage}</p> }
                    { modalGiftBody }
                </ModalBody>
                <ModalFooter>
                    { modalGiftFooter}
                </ModalFooter>
            </Modal>
        </div>);
    } else {
        console.log("Unauthorized... Redirecting...")
        navigate('../signin')
        return (<div></div>);
    }
}

export default MyBuyList;
