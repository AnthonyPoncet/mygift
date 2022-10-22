import React, { useEffect, useState }  from 'react';

import { useNavigate, useParams } from "react-router-dom";
import { HeartIcon, GiftIcon, PencilIcon, XIcon } from '@primer/octicons-react';
import { Form, Modal, ModalHeader, ModalBody, ModalFooter, Button, Input, Label, FormGroup, FormFeedback, Spinner } from 'reactstrap';


import './style/card-gift.css';
import SquareImage from './SquareImage';
import blank_gift from './image/blank_gift.png';

import { isMobile } from 'react-device-detect';

import { useAppSelector, useAppDispatch } from '../redux/store';
import { selectMessages } from '../redux/reducers/locale';
import { addMessage, selectErrorMessage, clearMessage } from '../redux/reducers/error';
import { selectSignIn, logout } from '../redux/reducers/signin';

import { getServerUrl } from '../ServerInformation';
let url = getServerUrl();

function getGifts(name: string, token: string, setCategories: any, appDispatch: any) {
    const request = async () => {
        const response = await fetch(url + '/gifts/' + name, {
            method: "GET",
            headers: {'Authorization': `Bearer ${token}`}
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
};

function reserve(giftId: number, reserve: boolean, name: string, token: string, setCategories: any, appDispatch: any) {
    const request = async () => {
        const response = await fetch(url + '/gifts/' + giftId + '/reserve', {
            method: reserve ? "DELETE" : "POST",
            headers: {'Authorization': `Bearer ${token}`}
        });
        if (response.status === 202) {
            getGifts(name, token, setCategories, appDispatch);
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

function renderInsideGift(cgi: number, gi: number, fGift: any, giftHover: string, showGiftFn: any) {
    const { gift, secret } = fGift;
    if ((cgi+'-'+gi === giftHover) || isMobile) {
        return (<>
            <div style={{cursor: "pointer"}} onClick={() => showGiftFn()}>
                <div className="card-name">{gift.name}</div>
                <div className="card-description">{gift.description}</div>
                <div className="mycard-footer">
                    <div className="card-wtb">{gift.whereToBuy}</div>
                    <div className="card-price">{gift.price}</div>
                </div>
            </div>
        </>);
    } else {
      const className = (secret) ? "card-name-only-secret" : "card-name-only";
      return (<div className={className}>{gift.name}</div>);
    }
}

function openGift(fGift: any, token: string, setShowGift: any, setModalGiftTitle: any, setModalGiftBody: any, setModalGiftFooter: any, setCategories: any, appDispatch: any, friendwishlist: any, mywishlist: any, username: string, friendName: string) {
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
            <Button color={reservedByMe ? "primary" : "secondary"} onClick={() => reserve(gift.id, reservedByMe, friendName, token, setCategories, appDispatch)}><GiftIcon/> {friendwishlist.reservedByMe}</Button>{' '}
            </>
        }
        </div>
    );
}

/** START DUPLICATE FROM MYWISHLIST **/
function addGiftModal(friendName: string, setModalTitle: any, setModalBody: any, setShow: any, mywishlist: any, token: string, appDispatch: any, categories: any, setCategories: any) {
    appDispatch(clearMessage());

    setModalTitle(mywishlist.addGiftModalTitle);

    let onFormSubmit = (e: any) => {
        e.preventDefault();
        if (e.target.name.value === '') {
            appDispatch(addMessage(mywishlist.nameErrorMessage));
            return;
        }
        let imageName = (e.target.picture === undefined) ? "" : e.target.picture.value;
        const request = async () => {
            const response = await fetch(url + "/gifts?forUser=" + friendName, {
                method: "PUT",
                headers: {'Content-Type':'application/json', 'Authorization': `Bearer ${token}`},
                body: JSON.stringify({
                    "name": e.target.name.value,
                    "description" : e.target.description.value,
                    "price": e.target.price.value,
                    "whereToBuy": e.target.whereToBuy.value,
                    "categoryId": e.target.categoryId.value,
                    "picture": imageName})
            });
            if (response.status === 200) {
                getGifts(friendName, token, setCategories, appDispatch);
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
    }

    let changeImage = (e: any) => {
        e.target.form[7].disabled = true;
        e.target.form[7].children[0].hidden = false;
        const formData = new FormData();
        formData.append("0", e.target.files[0]);
        const request = async () => {
            const response = await fetch(url + '/files', {method: 'post', headers: {'Authorization': `Bearer ${token}`}, body: formData });
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
        return <option key={index} value={value.id}>{value.name}</option>
    });

    setModalBody(
    <Form onSubmit={onFormSubmit}>
        <FormGroup>
            <Label>{mywishlist.name}</Label>
            <Input name="name" placeholder={mywishlist.name}/>
            <FormFeedback>{mywishlist.nameErrorMessage}</FormFeedback>
        </FormGroup>
        <FormGroup>
            <Label>{mywishlist.description}</Label>
            <Input type="textarea" name="description" placeholder={mywishlist.description}/>
        </FormGroup>
        <FormGroup>
            <Label>{mywishlist.price}</Label>
            <Input name="price" placeholder="10"/>
        </FormGroup>
        <FormGroup>
            <Label>{mywishlist.whereToBuy}</Label>
            <Input name="whereToBuy" placeholder={mywishlist.whereToBuyPlaceholder}/>
        </FormGroup>
        <FormGroup>
            <Label>{mywishlist.category}</Label>
            <Input type="select" name="categoryId">
                {options}
            </Input>
        </FormGroup>
        <FormGroup>
            <Label>{mywishlist.image}</Label>
            <Input type="file"onChange={(e) => changeImage(e)}/>
        </FormGroup>
        <FormGroup>
            <Input hidden name="picture"/>
        </FormGroup>
        <Button color="primary" block type="submit"><Spinner hidden size="sm"/> {mywishlist.addModalButton}</Button>
    </Form>);
    setShow(true);
}

function editGiftModal(friendName: string, gift: any, setModalTitle: any, setModalBody: any, setShow: any, mywishlist: any, token: string, appDispatch: any, categories: any, setCategories: any) {
    appDispatch(clearMessage());

    setModalTitle(mywishlist.updateGiftModalTitle);

    let onFormSubmit = (e: any) => {
        e.preventDefault();
        if (e.target.name.value === '') {
            appDispatch(addMessage(mywishlist.nameErrorMessage));
            return;
        }
        let imageName = (e.target.picture === undefined) ? "" : e.target.picture.value;
        const request = async () => {
            const response = await fetch(url + "/gifts/" + gift.id, {
                method: "PATCH",
                headers: {'Content-Type':'application/json', 'Authorization': `Bearer ${token}`},
                body: JSON.stringify({
                    "name": e.target.name.value,
                    "description" : e.target.description.value,
                    "price": e.target.price.value,
                    "whereToBuy": e.target.whereToBuy.value,
                    "categoryId": e.target.categoryId.value,
                    "picture": imageName,
                    "rank": gift.rank})
            });
            if (response.status === 200) {
                getGifts(friendName, token, setCategories, appDispatch);
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
    }

    let changeImage = (e: any) => {
        e.target.form[7].disabled = true;
        e.target.form[7].children[0].hidden = false;
        const formData = new FormData();
        formData.append("0", e.target.files[0]);
        const request = async () => {
            const response = await fetch(url + '/files', {method: 'post', headers: {'Authorization': `Bearer ${token}`}, body: formData });
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
            return <option key={index} value={value.id} selected>{value.name}</option>
        } else {
            return <option key={index} value={value.id}>{value.name}</option>
        }
    });

    setModalBody(
    <Form onSubmit={onFormSubmit}>
        <FormGroup>
            <Label>{mywishlist.name}</Label>
            <Input name="name" defaultValue={gift.name}/>
            <FormFeedback>{mywishlist.nameErrorMessage}</FormFeedback>
        </FormGroup>
        <FormGroup>
            <Label>{mywishlist.description}</Label>
            <Input type="textarea" name="description" defaultValue={gift.description}/>
        </FormGroup>
        <FormGroup>
            <Label>{mywishlist.price}</Label>
            <Input name="price" defaultValue={gift.price}/>
        </FormGroup>
        <FormGroup>
            <Label>{mywishlist.whereToBuy}</Label>
            <Input name="whereToBuy" defaultValue={gift.whereToBuy}/>
        </FormGroup>
        <FormGroup>
            <Label>{mywishlist.category}</Label>
            <Input type="select" name="categoryId">
                {options}
            </Input>
        </FormGroup>
        <FormGroup>
            <Label>{mywishlist.image}</Label>
            <Input type="file"onChange={(e) => changeImage(e)}/>
        </FormGroup>
        <FormGroup>
            <Input hidden name="picture" defaultValue={gift.picture}/>
        </FormGroup>
        <Button color="primary" block type="submit"><Spinner hidden size="sm"/> {mywishlist.updateModalButton}</Button>
    </Form>);
    setShow(true);
}

function deleteGiftModal(friendName: string, id: number, setModalTitle: any, setModalBody: any, setShow: any, friendwishlist: any, mywishlist: any, token: string, appDispatch: any, setCategories: any) {
    appDispatch(clearMessage());

    setModalTitle(mywishlist.deleteGiftModalTitle);

    let onFormSubmit = (e: any) => {
        e.preventDefault();
        const request = async () => {
            const response = await fetch(url + '/gifts/' + id + '?status=' + e.nativeEvent.submitter.value, {method: 'delete', headers: {'Authorization': `Bearer ${token}`}});
            if (response.status === 202) {
                getGifts(friendName, token, setCategories, appDispatch);
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
    }

    setModalBody(
    <Form onSubmit={onFormSubmit}>
        <Button color="primary" type="submit" value="RECEIVED">{friendwishlist.deleteModalButtonReceived}</Button> { " " }
        <Button color="primary" type="submit" value="NOT_WANTED">{friendwishlist.deleteModalButtonNotWanted}</Button>
    </Form>);
    setShow(true);
}

/** END DUPLICATE FROM MYWISHLIST **/

function FriendWishList() {
    const params: any = useParams();

    const username = useAppSelector(selectSignIn).username;
    const token = useAppSelector(selectSignIn).token;
    const friendwishlist = useAppSelector(selectMessages).friendwishlist;
    const mywishlist = useAppSelector(selectMessages).mywishlist;
    const errorMessage = useAppSelector(selectErrorMessage);

    const appDispatch = useAppDispatch();

    let navigate = useNavigate();

    const [categories, setCategories] = useState([]);
    const [giftHover, setGiftHover] = useState("");

    const [showGift, setShowGift] = useState(false);
    const handleCloseGift = () => setShowGift(false);
    const [modalGiftTitle, setModalGiftTitle] = useState("title");
    const [modalGiftBody, setModalGiftBody] = useState(<div></div>);
    const [modalGiftFooter, setModalGiftFooter] = useState(<div></div>);

    const [showAddGif, setShowAddGift] = useState(false);
    const handleCloseAddGift = () => setShowAddGift(false);
    const [modalAddGiftTitle, setModalAddGiftTitle] = useState("title");
    const [modalAddGiftBody, setModalAddGiftBody] = useState(<div></div>);

    useEffect(() => {
        if (token) {
            getGifts(params.name, token, setCategories, appDispatch);
        }
    }, [params.name, token, appDispatch]);

    if (token && username) {
        return (
        <div>
            <h1 className="friend-wishlist-title">{friendwishlist.title} {params.name}</h1>
            <Button color="link" onClick={() => addGiftModal(params.name, setModalAddGiftTitle, setModalAddGiftBody, setShowAddGift, mywishlist, token, appDispatch, categories, setCategories)}>{mywishlist.addGiftButton}</Button>
            {
            categories.map((cg: any, cgi: any) => {
                return (
                <div key={cgi}>
                    <h5 style={{margin: "10px"}}>{cg.category.name}</h5>
                    <div className="mycard-row">
                    {
                    cg.gifts.map((fGift: any, gi:any) => {
                        const { gift, reservedBy, secret } = fGift;
                        let reservedByMe = false;
                        for (const [, value] of reservedBy.entries()) { if (value === username) reservedByMe = true; }

                        const boughtClassName = (reservedBy.length === 0) ? "" : " card-already-bought";
                        const secretClassName = (secret) ? " secret-border" : "";
                        return (
                        <div className={"mycard" + boughtClassName + secretClassName} onMouseEnter={() => setGiftHover(cgi + "-" + gi)} onMouseLeave={() => setGiftHover("")}>
                            {
                            (reservedBy.length === 0 || reservedByMe) &&
                            <div className="card-edit-close">
                                {!secret &&
                                    <div className={gift.heart ? "heart-selected three-icon-first" : "three-icon-first"}>
                                        { gift.heart && <span><HeartIcon /></span>  }
                                    </div>
                                }
                                {secret &&
                                    <div className="three-icon-first">
                                        <span style={{cursor: "pointer"}} onClick={() => editGiftModal(params.name, gift, setModalAddGiftTitle, setModalAddGiftBody, setShowAddGift, mywishlist, token, appDispatch, categories, setCategories)}><PencilIcon/></span>
                                    </div>
                                }
                                <div className="three-icon-second secret-text">{secret && <>Secret</>}</div>
                                <div className={reservedByMe ? "icon-selected three-icon-third" : "three-icon-third"}>
                                    <span style={{cursor: "pointer"}} onClick={() => reserve(gift.id, reservedByMe, params.name, token, setCategories, appDispatch)}><GiftIcon/></span>
                                </div>
                                {secret &&
                                    <div className="three-icon-first">
                                        <span style={{cursor: "pointer"}} onClick={() => deleteGiftModal(params.name, gift.id, setModalAddGiftTitle, setModalAddGiftBody, setShowAddGift, friendwishlist, mywishlist, token, appDispatch, setCategories)}><XIcon/></span>
                                    </div>
                                }
                            </div>
                            }
                            <div style={{cursor: "pointer"}} onClick={() => openGift(fGift, token, setShowGift, setModalGiftTitle, setModalGiftBody, setModalGiftFooter, setCategories, appDispatch, friendwishlist, mywishlist, username, params.name)}>
                                <SquareImage token={token} className="card-image" imageName={gift.picture} size={150} alt="Gift" alternateImage={blank_gift}/>
                            </div>
                            {renderInsideGift(cgi, gi, fGift, giftHover, () => openGift(fGift, token, setShowGift, setModalGiftTitle, setModalGiftBody, setModalGiftFooter, setCategories, appDispatch, friendwishlist, mywishlist, username, params.name))}
                        </div>
                        );
                    })
                    }
                    </div>
                </div>);
            })
            }

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

            <Modal isOpen={showAddGif} toggle={handleCloseAddGift}>
                <ModalHeader toggle={handleCloseAddGift}>{modalAddGiftTitle}</ModalHeader>
                <ModalBody>
                    { errorMessage && <p className="auth-error">{errorMessage}</p> }
                    { modalAddGiftBody }
                </ModalBody>
            </Modal>
        </div>);
    } else {
        console.log("Unauthorized... Redirecting...")
        navigate('../signin')
        return (<div></div>);
    }
}


export default FriendWishList;
