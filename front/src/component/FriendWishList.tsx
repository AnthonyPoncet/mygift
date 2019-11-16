import React from 'react';

import Octicon, {Heart, Checklist, Gift, Pencil, X} from '@primer/octicons-react';

import { Modal, ModalHeader, ModalBody, Button, Input, Label, FormGroup, FormFeedback } from "reactstrap";

import { connect } from 'react-redux';
import { AppState } from '../redux/store';

import { FriendWishListMessage, MyWishListMessage } from '../translation/itrans';
import './style/card-gift.css';
import SquareImage from './SquareImage';
import blank_gift from './image/blank_gift.png';

import { isMobile } from "react-device-detect";

import { getServerUrl } from "../ServerInformation";
let url = getServerUrl();


interface ConnectProps { userId: number | null, username: String | null, friendwishlist: FriendWishListMessage, mywishlist: MyWishListMessage };
interface Props extends ConnectProps { friendName: string };
interface State {
    catAndGifts: any[],
    hoverId: string,
    showGift: boolean,
    giftToShow: any | null,
    show: boolean, title: string, bodyRender: any, button: { text: string, fun: any }, inputs: any, errorMessage: string //to refactor, duplicate
};

class FriendWishList extends React.Component<Props, State> {
    constructor(props: Props) {
        super(props);
        this.state = {catAndGifts: [], hoverId: '', showGift: false, giftToShow: null,
                      show: false, title: '', bodyRender: null, button: { text: '', fun: null }, inputs: { }, errorMessage: ''};
    }

    componentDidMount() {
        if (this.props.userId) {
            this.getGifts(this.props.userId, this.props.friendName);
        }
    }

    openAddGift() {
        const { mywishlist } = this.props;
        this.setState( { show: true, title: mywishlist.addGiftModalTitle, bodyRender: () => this.giftBodyRender(), button: { text: mywishlist.addModalButton, fun: () => this.addGift() },
            inputs: { name: '', nameValidity: true, description: null, price: null, whereToBuy: null, categoryId: this.state.catAndGifts[0].category.id }, errorMessage: '' });
    }

    openEditGift(giftId: number, name: string, description: string, price: string, whereToBuy: string, categoryId: number, image: string | null) {
        const { mywishlist } = this.props;
        this.setState( { show: true, title: mywishlist.updateGiftModalTitle, bodyRender: () => this.giftBodyRender(), button: { text: mywishlist.updateModalButton, fun: () => this.updateGift(giftId) },
            inputs: { name: name, nameValidity: true, description: description, price: price, whereToBuy: whereToBuy, categoryId: categoryId, picture: image }, errorMessage: '' });
    }

    handleChangeGift = async (event: any) => {
        const { inputs } = this.state;
        const { name, value } = event.target;
        inputs[name] = value;
        if (name === "name") {
            inputs["nameValidity"] = value.length > 0;
        }

        await this.setState({ inputs: inputs });
    };

    updateGiftModalCategory(id: string) {
        const { inputs } = this.state;
        this.setState({ inputs: { ...inputs, categoryId: id } });
    }

    changeImage(e: any) {
        const formData = new FormData();
        formData.append("0", e.target.files[0]);
        const request = async () => {
            const response = await fetch(url + '/files', {method: 'post', body: formData});
            if (response.status === 202) {
                const json = await response.json();
                const { inputs } = this.state;
                inputs["picture"] = json.name;
                this.setState({ inputs: inputs });
            } else {
                const json = await response.json();
                console.log(json);
            }
        };
        request();
    }

    giftBodyRender() {
        const options = this.state.catAndGifts && this.state.catAndGifts.map( (cag, index) => {
          let value = cag.category;
          if (this.state.inputs.categoryId === value.id) {
            return <option key={index} value={value.id} selected>{value.name}</option>
          } else {
            return <option key={index} value={value.id}>{value.name}</option>
          }
        });

        const { mywishlist } = this.props;

        return (<>
            <FormGroup>
                <Label>{mywishlist.name}</Label>
                <Input name="name" placeholder={mywishlist.name} value={this.state.inputs.name} invalid={!this.state.inputs.nameValidity} onChange={(e) => this.handleChangeGift(e)}/>
                <FormFeedback>{mywishlist.nameErrorMessage}</FormFeedback>
            </FormGroup>
            <FormGroup>
                <Label>{mywishlist.description}</Label>
                <Input type="textarea" name="description" placeholder={mywishlist.description} value={this.state.inputs.description} onChange={(e) => this.handleChangeGift(e)}/>
            </FormGroup>
            <FormGroup>
                <Label>{mywishlist.price}</Label>
                <Input name="price" placeholder="10" value={this.state.inputs.price} onChange={(e) => this.handleChangeGift(e)}/>
            </FormGroup>
            <FormGroup>
                <Label>{mywishlist.whereToBuy}</Label>
                <Input name="whereToBuy" placeholder={mywishlist.whereToBuyPlaceholder} value={this.state.inputs.whereToBuy} onChange={(e) => this.handleChangeGift(e)}/>
            </FormGroup>
            <FormGroup>
                <Label>{mywishlist.category}</Label>
                <Input type="select" name="categoryId" onChange={(e) => this.updateGiftModalCategory(e.target.value)}>
                    {options}
                </Input>
            </FormGroup>
            <FormGroup>
              <Label>{mywishlist.image}</Label>
              <Input type="file" onChange={(e) => this.changeImage(e)}/>
            </FormGroup>
            <SquareImage className="card-image" imageName={this.state.inputs.picture} size={150} alt="Gift" alternateImage={blank_gift}/>
            </>);
    }

    giftRestCall(url: string, method: string) {
        const {inputs} = this.state;

        if (inputs.name === '') {
            this.setState({ show: true, errorMessage: this.props.mywishlist.nameErrorMessage })
            return;
        }

        let imageName = (inputs.picture === null) ? "" : inputs.picture;
        const request = async () => {
            const response = await fetch(url, {
                method: method,
                headers: {'Content-Type':'application/json'},
                body: JSON.stringify({
                    "name": inputs.name,
                    "description" : inputs.description,
                    "price": inputs.price,
                    "whereToBuy": inputs.whereToBuy,
                    "categoryId": inputs.categoryId,
                    "picture": imageName})
            });
            if (response.status === 200) {
                this.setState({ show: false });
                this.props.userId !== null && this.getGifts(this.props.userId, this.props.friendName);
            } else {
                const json = await response.json();
                this.setState({ show: true, errorMessage: json.error });
            }
        };
        request();
    }

    addGift() { this.giftRestCall(url + '/users/' + this.props.userId + '/gifts?forUser='+this.props.friendName, 'PUT'); }

    updateGift(id: number) { this.giftRestCall(url + '/users/' + this.props.userId + '/gifts/' + id, 'PATCH'); }

    deleteGift(id: number) {
        const request = async () => {
            const response = await fetch(url + '/users/' + this.props.userId + '/gifts/' + id, {method: 'delete'});
            if (response.status === 202) {
                this.props.userId && this.getGifts(this.props.userId, this.props.friendName);
            } else {
                const json = await response.json();
                console.log(json);
            }
        };
        request();
    }

    closeModal() { this.setState({ show: false }); }

    async getGifts(userId: number, friendName: string) {
        const response = await fetch(url + '/users/' + userId + '/gifts/' + friendName);
        const json = await response.json();
        if (response.status === 200) {
            this.setState({ catAndGifts: json });
        } else {
            console.log(json.error);
        }
    };

    async interested(userId: number | null, giftId: number, imInterested: boolean) {
        if (userId === null) return; //Impossible

        const response = await fetch(url + '/users/' + userId + '/gifts/' + giftId + '/interested?userId=' + userId, {method: imInterested ? "DELETE" : "POST"});
        if (response.status === 202) {
            this.getGifts(userId, this.props.friendName);
        } else {
            const json = await response.json();
            console.log(json.error);
        }
    }

    async wantToBuy(userId: number | null, giftId: number, iWantToBuy: boolean, iBought: boolean) {
        if (userId === null) return; //Impossible

        if (iBought) await this.bought(userId, giftId, false, true); //Remove ibought

        let response = null;
        if (iWantToBuy === true) {
            response = await fetch(url + '/users/' + userId + '/gifts/' + giftId + '/buy-action?userId=' + userId, {method: "DELETE"});
        } else {
            response = await fetch(url + '/users/' + userId + '/gifts/' + giftId + '/buy-action?userId=' + userId + '&action=WANT_TO_BUY', {method: "POST"});
        }
        if (response.status === 202) {
            this.getGifts(userId, this.props.friendName);
        } else {
            const json = await response.json();
            console.log(json.error);
        }
    }

    async bought(userId: number | null, giftId: number, iWantToBuy: boolean, iBought: boolean) {
        if (userId === null) return; //Impossible

        if (iWantToBuy) await this.wantToBuy(userId, giftId, true, false); //Remove ibought

        let response = null;
        if (iBought === true) {
            response = await fetch(url + '/users/' + userId + '/gifts/' + giftId + '/buy-action?userId=' + userId, {method: "DELETE"});
        } else {
            response = await fetch(url + '/users/' + userId + '/gifts/' + giftId + '/buy-action?userId=' + userId + '&action=BOUGHT', {method: "POST"});
        }
        if (response.status === 202) {
            this.getGifts(userId, this.props.friendName);
        } else {
            const json = await response.json();
            console.log(json.error);
        }
    }

    handleEnter(cat: number, gift: number) {
        this.setState({ hoverId: cat + "-" + gift});
    }

    handleOut() {
        this.setState({ hoverId: '' });
    }

    showGift(gift: any) {
        this.setState({ showGift: true, giftToShow: gift });
    }

    renderGifts() {
      if (this.state.catAndGifts) {
        return this.state.catAndGifts.map((cg, cgi) => {
            return (
            <div key={cgi}>
                <h5 style={{margin: "10px"}}>{cg.category.name}</h5>

                <div className="mycard-row">
                {cg.gifts.map((fGift: any, gi:any) => {
                  const { gift, interestedUser, buyActionUser, secret } = fGift;
                  let wantToBuy: string[] = [];
                  let bought: string[] = [];
                  Object.keys(buyActionUser).forEach(key => {
                      if (buyActionUser[key] === "WANT_TO_BUY") wantToBuy.push(key);
                      if (buyActionUser[key] === "BOUGHT") bought.push(key);
                  });
                  if ((cgi+'-'+gi === this.state.hoverId) || isMobile) {
                    let imInterested = false;
                    let iWantToBuy = false;
                    let iBought = false;
                    if (this.props.username !== null) {
                        for (const [, value] of interestedUser.entries()) { if (value === this.props.username) imInterested = true; }
                        for (const [, value] of wantToBuy.entries()) { if (value === this.props.username) iWantToBuy = true; }
                        for (const [, value] of bought.entries()) { if (value === this.props.username) iBought = true; }
                    }
                    return (
                        <div className="mycard" onMouseEnter={() => this.handleEnter(cgi, gi)} onMouseLeave={() => this.handleOut()}>
                            <div style={{cursor: "pointer"}} onClick={() => this.showGift(gift)}>
                                <SquareImage className="card-image" imageName={gift.picture} size={150} alt="Gift" alternateImage={blank_gift}/>
                            </div>
                            <div className="card-edit-close">
                              <span className={imInterested ? "text-right icon-selected" : "text-right"} style={{cursor: "pointer"}} onClick={() => this.interested(this.props.userId, gift.id, imInterested)}><Octicon icon={Heart}/></span>{' '}
                              {interestedUser.length !== 0 && <><span>{interestedUser.length}</span>{' '}</>}
                              <span className={iWantToBuy ? "icon-selected" : ""} style={{cursor: "pointer"}} onClick={() => this.wantToBuy(this.props.userId, gift.id, iWantToBuy, iBought)}><Octicon icon={Checklist}/></span>{' '}
                              {wantToBuy.length !== 0 && <><span>{wantToBuy.length}</span>{' '}</>}
                              <span className={iBought ? "icon-selected" : ""} style={{cursor: "pointer"}} onClick={() => this.bought(this.props.userId, gift.id, iWantToBuy, iBought)}><Octicon icon={Gift}/></span>{' '}
                              {bought.length !== 0 && <><span>{bought.length}</span>{' '}</>}
                              {secret && <span className="text-right" style={{cursor: "pointer"}} onClick={() => this.openEditGift(gift.id, gift.name, gift.description, gift.price, gift.whereToBuy, gift.categoryId, gift.picture === undefined ? null : gift.picture)}><Octicon icon={Pencil}/></span>}
                              {' '}
                              {secret && <span style={{cursor: "pointer"}} onClick={() => this.deleteGift(gift.id)}><Octicon icon={X}/></span>}
                            </div>
                            <div style={{cursor: "pointer"}} onClick={() => this.showGift(gift)}>
                                <div className="card-name">{gift.name}</div>
                                <div className="card-description">{gift.description}</div>
                                <div className="mycard-footer">
                                  <div className="card-wtb">{gift.whereToBuy}</div>
                                  <div className="card-price">{gift.price}</div>
                                </div>
                            </div>
                        </div>);
                    } else {
                      return (
                          <div className="mycard" onMouseEnter={() => this.handleEnter(cgi, gi)} onMouseLeave={() => this.handleOut()}>
                              <SquareImage className="card-image" imageName={gift.picture} size={150} alt="Gift" alternateImage={blank_gift}/>
                              <div className="card-name-only">{gift.name}</div>
                          </div>);
                    }
                })}
                </div>
            </div>)
        });
      }
    }

    render() {
          const { button } = this.state;
          const { mywishlist } = this.props;
          let modalBody = [];
          if (this.state.bodyRender !== null) {
              modalBody.push(this.state.bodyRender());
          }

        return (
        <div>
          <h1 className="friend-wishlist-title">{this.props.friendwishlist.title} {this.props.friendName}</h1>
          {this.props.userId && <Button color="link" onClick={() => this.openAddGift()}>{mywishlist.addGiftButton}</Button> }
          <div>{this.renderGifts()}</div>

          <DisplayGift
            show={this.state.showGift}
            gift={this.state.giftToShow}
            close={() => this.setState({showGift: false, giftToShow: null})}
            friendwishlist={this.props.friendwishlist}
            mywishlist={this.props.mywishlist}/>

          <Modal isOpen={this.state.show} toggle={() => this.closeModal()}>
              <ModalHeader toggle={() => this.closeModal()}>{this.state.title}</ModalHeader>
              <ModalBody>
                  { this.state.errorMessage && <p className="auth-error">{this.state.errorMessage}</p> }
                  {modalBody}
                  <Button color="primary" onClick={button.fun}>{button.text}</Button>
              </ModalBody>
          </Modal>
        </div>);
    }
}

interface DisplayGiftProps {
    show: boolean
    gift: any | null,
    close(): void,
    friendwishlist: FriendWishListMessage,
    mywishlist: MyWishListMessage
};

class DisplayGift extends React.Component<DisplayGiftProps> {
    render() {
        const { show, gift, close, friendwishlist, mywishlist } = this.props;

        if (gift === null) return <div/>;

        const isContainer = isMobile ? "" : "container";

        const wtb = gift.whereToBuy.split(" ");
        return (
          <Modal isOpen={show} toggle={() => close()} size="lg">
            <ModalHeader toggle={() => close() }>{gift.name}</ModalHeader>
            <ModalBody>
                <div className={isContainer}>
                    <SquareImage className="card-image" imageName={gift.picture} size={300} alt="Gift" alternateImage={blank_gift}/>
                    <div>
                        {(gift.description !== "") && <>
                            <div>{gift.description}</div>
                            <br/>
                            </>
                        }

                        {(gift.price !== "") &&
                            <div>{mywishlist.price}: {gift.price}</div>
                        }
                        {(gift.whereToBuy !== "") &&
                            <div>{mywishlist.whereToBuy}: {wtb.map((word: string) => {
                                if (word.startsWith("http")) {
                                    return <a href={word}>{word}{' '}</a>;
                                } else {
                                    return word + " ";
                                }
                            })}</div>
                        }
                    </div>
                </div>
            </ModalBody>
          </Modal>);
    }
}

function mapStateToProps(state: AppState): ConnectProps {return {
  userId: state.signin.userId, username: state.signin.username, friendwishlist: state.locale.messages.friendwishlist, mywishlist: state.locale.messages.mywishlist };}
export default connect(mapStateToProps)(FriendWishList);
