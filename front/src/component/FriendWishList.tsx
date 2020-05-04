import React from 'react';

import Octicon, {Heart, Checklist, Gift, Pencil, X} from '@primer/octicons-react';

import { Modal, ModalHeader, ModalBody, ModalFooter, Button, Input, Label, FormGroup, FormFeedback } from "reactstrap";

import { connect } from 'react-redux';
import { AppState } from '../redux/store';

import { FriendWishListMessage, MyWishListMessage } from '../translation/itrans';
import './style/card-gift.css';
import SquareImage from './SquareImage';
import blank_gift from './image/blank_gift.png';

import { isMobile } from "react-device-detect";

import { getServerUrl } from "../ServerInformation";
let url = getServerUrl();


interface ConnectProps { token: string | null, username: String | null, friendwishlist: FriendWishListMessage, mywishlist: MyWishListMessage };
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
        if (this.props.token) {
            this.getGifts(this.props.token, this.props.friendName);
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
            const response = await fetch(url + '/files', {method: 'post', headers: {'Authorization': `Bearer ${this.props.token}`}, body: formData });
            if (response.status === 401) {
                console.error("Unauthorized. Disconnect and redirect to connect");
            } else if (response.status === 202) {
                const json = await response.json();
                const { inputs } = this.state;
                inputs["picture"] = json.name;
                this.setState({ inputs: inputs });
            } else {
                const json = await response.json();
                console.error(json);
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
            <SquareImage token={this.props.token} className="card-image" imageName={this.state.inputs.picture} size={150} alt="Gift" alternateImage={blank_gift}/>
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
                headers: {'Content-Type':'application/json', 'Authorization': `Bearer ${this.props.token}`},
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
                this.props.token !== null && this.getGifts(this.props.token, this.props.friendName);
            } else if (response.status === 401) {
                console.error("Unauthorized. Disconnect and redirect to connect");
            } else {
                const json = await response.json();
                this.setState({ show: true, errorMessage: json.error });
            }
        };
        request();
    }

    addGift() { this.giftRestCall(url + '/gifts?forUser='+this.props.friendName, 'PUT'); }

    updateGift(id: number) { this.giftRestCall(url + '/gifts/' + id, 'PATCH'); }

    deleteGift(id: number) {
        const request = async () => {
            const response = await fetch(url + '/gifts/' + id, {method: 'delete', headers: {'Authorization': `Bearer ${this.props.token}`}});
            if (response.status === 202) {
                this.props.token && this.getGifts(this.props.token, this.props.friendName);
            } else if (response.status === 401) {
                console.error("Unauthorized. Disconnect and redirect to connect");
            } else {
                const json = await response.json();
                console.error(json);
            }
        };
        request();
    }

    closeModal() { this.setState({ show: false }); }

    async getGifts(token: string, friendName: string) {
        const response = await fetch(url + '/gifts/' + friendName, {headers: {'Authorization': `Bearer ${this.props.token}`}});
        if (response.status === 200) {
            const json = await response.json();
            this.setState({ catAndGifts: json });
            if (this.state.showGift) {
                const { id } = this.state.giftToShow.gift
                let newGiftToShow: any;
                json.forEach(function (cat: any) {
                    cat.gifts.forEach(function (fGift: any) {
                        if (fGift.gift.id === id) {
                            newGiftToShow = fGift;
                        }
                    });
                });
                this.setState({giftToShow: newGiftToShow});
            }
        } else if (response.status === 401) {
            console.error("Unauthorized. Disconnect and redirect to connect");
        } else {
            const json = await response.json();
            console.error(json.error);
        }
    };

    async interested(token: string | null, giftId: number, imInterested: boolean) {
        if (token === null) return; //Impossible

        const response = await fetch(url + '/gifts/' + giftId + '/interested', {method: imInterested ? "DELETE" : "POST", headers: {'Authorization': `Bearer ${this.props.token}`}});
        if (response.status === 202) {
            this.getGifts(token, this.props.friendName);
        } else if (response.status === 401) {
            console.error("Unauthorized. Disconnect and redirect to connect");
        } else {
            const json = await response.json();
            console.error(json.error);
        }
    }

    async wantToBuy(token: string | null, giftId: number, iWantToBuy: boolean, iBought: boolean) {
        if (token === null) return; //Impossible

        if (iBought) await this.bought(token, giftId, false, true); //Remove ibought

        let response = null;
        if (iWantToBuy === true) {
            response = await fetch(url + '/gifts/' + giftId + '/buy-action', {method: "DELETE", headers: {'Authorization': `Bearer ${this.props.token}`}});
        } else {
            response = await fetch(url + '/gifts/' + giftId + '/buy-action?action=WANT_TO_BUY', {method: "POST", headers: {'Authorization': `Bearer ${this.props.token}`}});
        }
        if (response.status === 202) {
            this.getGifts(token, this.props.friendName);
        } else if (response.status === 401) {
            console.error("Unauthorized. Disconnect and redirect to connect");
        } else {
            const json = await response.json();
            console.error(json.error);
        }
    }

    async bought(token: string | null, giftId: number, iWantToBuy: boolean, iBought: boolean) {
        if (token === null) return; //Impossible

        if (iWantToBuy) await this.wantToBuy(token, giftId, true, false); //Remove ibought

        let response = null;
        if (iBought === true) {
            response = await fetch(url + '/gifts/' + giftId + '/buy-action', {method: "DELETE", headers: {'Authorization': `Bearer ${this.props.token}`}});
        } else {
            response = await fetch(url + '/gifts/' + giftId + '/buy-action?action=BOUGHT', {method: "POST", headers: {'Authorization': `Bearer ${this.props.token}`}});
        }
        if (response.status === 202) {
            this.getGifts(token, this.props.friendName);
        } else if (response.status === 401) {
            console.error("Unauthorized. Disconnect and redirect to connect");
        } else {
            const json = await response.json();
            console.error(json.error);
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

    _renderInsideGift(cgi: number, gi: number, fGift: any) {
        const { gift, secret } = fGift;
        if ((cgi+'-'+gi === this.state.hoverId) || isMobile) {
            return (<>
                <div style={{cursor: "pointer"}} onClick={() => this.showGift(fGift)}>
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
                  const boughtClassName = (bought.length === 0) ? "" : " card-already-bought";
                  let imInterested = false;
                  let iWantToBuy = false;
                  let iBought = false;
                  if (this.props.username !== null) {
                      for (const [, value] of interestedUser.entries()) { if (value === this.props.username) imInterested = true; }
                      for (const [, value] of wantToBuy.entries()) { if (value === this.props.username) iWantToBuy = true; }
                      for (const [, value] of bought.entries()) { if (value === this.props.username) iBought = true; }
                  }

                  const secretClassName = (secret) ? " secret-border" : "";

                  return (
                      <div className={"mycard" + boughtClassName + secretClassName} onMouseEnter={() => this.handleEnter(cgi, gi)} onMouseLeave={() => this.handleOut()}>
                          {secret &&
                            <div className="card-edit-close">
                              <span className="three-icon-first" style={{cursor: "pointer"}} onClick={() => this.openEditGift(gift.id, gift.name, gift.description, gift.price, gift.whereToBuy, gift.categoryId, gift.picture === undefined ? null : gift.picture)}><Octicon icon={Pencil}/></span>
                              {' '}
                              <div className="three-icon-second secret-text">Secret</div>
                              <span className="three-icon-third" style={{cursor: "pointer"}} onClick={() => this.deleteGift(gift.id)}><Octicon icon={X}/></span>
                            </div>
                            }
                          { (bought.length === 0 || imInterested || iWantToBuy || iWantToBuy) &&
                          <div className="card-edit-close">
                              <div className={imInterested ? "icon-selected three-icon-first" : "three-icon-first"}>
                                <span style={{cursor: "pointer"}} onClick={() => this.interested(this.props.token, gift.id, imInterested)}><Octicon icon={Heart}/></span>{' '}
                                {interestedUser.length !== 0 && <><span>{interestedUser.length}</span>{' '}</>}
                              </div>
                              <div className={iWantToBuy ? "icon-selected three-icon-second" : "three-icon-second"}>
                                  <span style={{cursor: "pointer"}} onClick={() => this.wantToBuy(this.props.token, gift.id, iWantToBuy, iBought)}><Octicon icon={Checklist}/></span>{' '}
                                  {wantToBuy.length !== 0 && <><span>{wantToBuy.length}</span>{' '}</>}
                              </div>
                              <div className={iBought ? "icon-selected three-icon-third" : "three-icon-third"}>
                                  { (bought.length !== 0 && !iBought) ?
                                  <Octicon icon={Gift}/>
                                  :
                                  <span
                                      style={{cursor: "pointer"}}
                                      onClick={() => this.bought(this.props.token, gift.id, iWantToBuy, iBought)}>
                                          <Octicon icon={Gift}/>
                                  </span>
                                  }{' '}
                                  {bought.length !== 0 && <><span>{bought.length}</span>{' '}</>}
                              </div>
                          </div> }
                          <div style={{cursor: "pointer"}} onClick={() => this.showGift(fGift)}>
                              <SquareImage token={this.props.token} className="card-image" imageName={gift.picture} size={150} alt="Gift" alternateImage={blank_gift}/>
                          </div>
                          {this._renderInsideGift(cgi, gi, fGift)}
                      </div>);
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
          {this.props.token && <Button color="link" onClick={() => this.openAddGift()}>{mywishlist.addGiftButton}</Button> }
          <div>{this.renderGifts()}</div>

          <DisplayGift
            token={this.props.token}
            username={this.props.username}
            show={this.state.showGift}
            fGift={this.state.giftToShow}
            close={() => this.setState({showGift: false, giftToShow: null})}
            friendwishlist={this.props.friendwishlist}
            mywishlist={this.props.mywishlist}
            interested={(giftId: number, imInterested: boolean) => this.interested(this.props.token, giftId, imInterested)}
            wantToBuy={(giftId: number, iWantToBuy: boolean, iBought: boolean) => this.wantToBuy(this.props.token, giftId, iWantToBuy, iBought)}
            bought={(giftId: number, iWantToBuy: boolean, iBought: boolean) => this.bought(this.props.token, giftId, iWantToBuy, iBought)}/>

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
    token: string | null,
    username: String | null,
    show: boolean
    fGift: any | null,
    close(): void,
    friendwishlist: FriendWishListMessage,
    mywishlist: MyWishListMessage,
    interested: Function,
    wantToBuy: Function,
    bought: Function
};

class DisplayGift extends React.Component<DisplayGiftProps> {
    render() {
        const { show, fGift, close, friendwishlist, mywishlist } = this.props;

        if (fGift === null) return <div/>;

        const { gift } = fGift;

        const isContainer = isMobile ? "" : "container";
        const padding: string = isMobile ? "0px" : "10px";

        const wtb = gift.whereToBuy.split(" ");

        //Duplicated
        const { interestedUser, buyActionUser } = fGift;
        let wantToBuy: string[] = [];
        let bought: string[] = [];
        Object.keys(buyActionUser).forEach(key => {
            if (buyActionUser[key] === "WANT_TO_BUY") wantToBuy.push(key);
            if (buyActionUser[key] === "BOUGHT") bought.push(key);
        });
        let imInterested = false;
        let iWantToBuy = false;
        let iBought = false;
        if (this.props.username !== null) {
            for (const [, value] of interestedUser.entries()) { if (value === this.props.username) imInterested = true; }
            for (const [, value] of wantToBuy.entries()) { if (value === this.props.username) iWantToBuy = true; }
            for (const [, value] of bought.entries()) { if (value === this.props.username) iBought = true; }
        }

        return (
          <Modal isOpen={show} toggle={() => close()} size="lg">
            <ModalHeader toggle={() => close() }>{gift.name}</ModalHeader>
            <ModalBody>
                <div className={isContainer}>
                    <SquareImage token={this.props.token} className="card-image" imageName={gift.picture} size={300} alt="Gift" alternateImage={blank_gift}/>
                    <div style={{padding: padding}}>
                        {(gift.description !== "") && <>
                            <div>{mywishlist.description}: {gift.description}</div>
                            <br/>
                            </>
                        }

                        {(gift.price !== "") &&
                            <div>{mywishlist.price}: {gift.price}</div>
                        }
                        {(gift.whereToBuy !== "") &&
                            <div>{mywishlist.whereToBuy}: {wtb.map((word: string) => {
                                if (word.startsWith("http")) {
                                    let smallWord = word.length > 20 ? word.substring(0,20) + '...' : word;
                                    return <a href={word} target="_blank">{smallWord}{' '}</a>;
                                } else {
                                    return word + " ";
                                }
                            })}</div>
                        }
                    </div>
                </div>
            </ModalBody>
            { (bought.length === 0 || imInterested || iWantToBuy || iBought) && <ModalFooter>
              <Button color={imInterested ? "primary" : "secondary"} onClick={() => this.props.interested(gift.id, imInterested)}><Octicon icon={Heart}/> {friendwishlist.imInterested}</Button>{' '}
              <Button color={iWantToBuy ? "primary" : "secondary"} onClick={() => this.props.wantToBuy(gift.id, iWantToBuy, iBought)}><Octicon icon={Checklist}/> {friendwishlist.iWantToBuy}</Button>
              <Button color={iBought ? "primary" : "secondary"} onClick={() => this.props.bought(gift.id, iWantToBuy, iBought)}><Octicon icon={Gift}/> {friendwishlist.iBought}</Button>
            </ModalFooter> }
          </Modal>);
    }
}

function mapStateToProps(state: AppState): ConnectProps {return {
  token: state.signin.token, username: state.signin.username, friendwishlist: state.locale.messages.friendwishlist, mywishlist: state.locale.messages.mywishlist };}
export default connect(mapStateToProps)(FriendWishList);
