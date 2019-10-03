import React from 'react';

import Octicon, {Heart, Checklist, Gift} from '@primer/octicons-react'

import { Modal, ModalHeader, ModalBody } from "reactstrap";


import { connect } from 'react-redux';
import { AppState } from '../redux/store';

import { FriendWishListMessage, MyBuyListMessage } from '../translation/itrans';
import './card-gift.css';
import blank_gift from './blank_gift.png';

import { getServerUrl } from "../ServerInformation";
let url = getServerUrl();


interface ConnectProps { userId: number | null, username: String | null, friendwishlist: FriendWishListMessage, myBuyList: MyBuyListMessage };
interface Props extends ConnectProps { };
interface State {
    friendAndGifts: any[],
    hoverId: string,
    showGift: boolean,
    giftToShow: any | null
};

class MyBuyList extends React.Component<Props, State> {
    constructor(props: Props) {
        super(props);
        this.state = {friendAndGifts: [], hoverId: '', showGift: false, giftToShow: null};
    }

    componentDidMount() {
        if (this.props.userId) {
            this.getBuyList(this.props.userId);
        }
    }

    loadImage(name: string, tagName: string) {
        const request = async() => {
            const response = await fetch(url + '/files/' + name);
            response.blob().then(blob => {
                let url = window.URL.createObjectURL(blob);
                let tag = document.querySelector('#' + tagName);
                if (tag instanceof HTMLImageElement) tag.src = url;
            });
        };
        request();
    }

    async getBuyList(userId: number) {
        const response = await fetch(url + '/users/' + userId + '/buy-list');
        const json = await response.json();
        if (response.status === 200) {
            this.setState({ friendAndGifts: json });
            json.forEach((friendAndGift: any) => {
                friendAndGift.gifts.forEach((gift: any) => {
                  if (gift.gift.picture !== undefined) this.loadImage(gift.gift.picture, 'gift-'+gift.gift.id);
                });
            });
        } else {
            console.log(json.error);
        }
    };

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
            this.getBuyList(userId);
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
            this.getBuyList(userId);
        } else {
            const json = await response.json();
            console.log(json.error);
        }
    }

    handleEnter(friend: number, gift: number) {
        this.setState({ hoverId: friend + "-" + gift});
    }

    handleOut() {
        this.setState({ hoverId: '' });
    }

    showGift(gift: any) {
        this.setState({ showGift: true, giftToShow: gift });
    }

    renderGifts() {
      if (this.state.friendAndGifts) {
        return this.state.friendAndGifts.map((fg, fgi) => {
            return (
            <div key={fgi}>
                <h5>{fg.friendName}</h5>

                <div className="mycard-row">
                {fg.gifts.map((fGift: any, gi:any) => {
                   console.log(fGift);
                  const { gift, buyActionUser } = fGift;
                  let wantToBuy: string[] = [];
                  let bought: string[] = [];
                  Object.keys(buyActionUser).forEach(key => {
                      if (buyActionUser[key] === "WANT_TO_BUY") wantToBuy.push(key);
                      if (buyActionUser[key] === "BOUGHT") bought.push(key);
                  });
                  if (fgi+'-'+gi === this.state.hoverId) {
                    let iWantToBuy = false;
                    let iBought = false;
                    if (this.props.username !== null) {
                        for (const [, value] of wantToBuy.entries()) { if (value === this.props.username) iWantToBuy = true; }
                        for (const [, value] of bought.entries()) { if (value === this.props.username) iBought = true; }
                    }
                    let imageFull = (gift.picture === undefined) ?
                      <img className="gift-image-full" style={{cursor: "pointer"}} onClick={() => this.showGift(gift)} src={blank_gift} alt="Nothing"/> :
                      <img className="gift-image-full" style={{cursor: "pointer"}} onClick={() => this.showGift(gift)} id={'gift-'+gift.id} alt="Gift"/>;
                    return (
                        <div className="mycard" onMouseEnter={() => this.handleEnter(fgi, gi)} onMouseLeave={() => this.handleOut()}>
                            {imageFull}
                            <div className="card-edit-close">
                              <span className={iWantToBuy ? "icon-selected" : ""} style={{cursor: "pointer"}} onClick={() => this.wantToBuy(this.props.userId, gift.id, iWantToBuy, iBought)}><Octicon icon={Checklist}/></span>{' '}
                              {wantToBuy.length !== 0 && <><span>{wantToBuy.length}</span>{' '}</>}
                              <span className={iBought ? "icon-selected" : ""} style={{cursor: "pointer"}} onClick={() => this.bought(this.props.userId, gift.id, iWantToBuy, iBought)}><Octicon icon={Gift}/></span>{' '}
                              {bought.length !== 0 && <><span>{bought.length}</span>{' '}</>}
                            </div>
                            <div className="card-name">{gift.name}</div>
                            <div className="card-description">{gift.description}</div>
                            <div className="mycard-footer">
                              <div className="card-wtb">{gift.whereToBuy}</div>
                              <div className="card-price">{gift.price}</div>
                            </div>
                        </div>);
                    } else {
                      let imageOnly = (gift.picture === undefined) ?
                        <img className="gift-image-name-only" src={blank_gift} alt="Nothing"/> :
                        <img className="gift-image-name-only" id={'gift-'+gift.id} alt="Gift"/>;
                      return (
                          <div className="mycard" onMouseEnter={() => this.handleEnter(fgi, gi)} onMouseLeave={() => this.handleOut()}>
                              {imageOnly}
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
        return (
        <div>
          <h1 className="friend-wishlist-title">{this.props.myBuyList.title}</h1>
          <div>{this.renderGifts()}</div>

          <DisplayGift show={this.state.showGift} gift={this.state.giftToShow} close={() => this.setState({showGift: false, giftToShow: null})}/>
        </div>);
    }
}

interface DisplayGiftProps {
    show: boolean
    gift: any | null,
    close(): void
};

class DisplayGift extends React.Component<DisplayGiftProps> {
    private oldGift: any | null = null;
    private url: any | null = null;

    componentWillReceiveProps(nextProps: any) {
        // You don't have to do this check first, but it can help prevent an unneeded render
        if (nextProps.gift !== this.oldGift) {
            this.oldGift = nextProps.gift;
            if (nextProps.gift !== null && nextProps.gift.picture !== undefined) {
                this.url = null;
                this.loadImage(nextProps.gift.picture, 'gift-'+nextProps.gift.id);
            }
        }
    }

    loadImage(name: string, tagName: string) {
        console.log("load " + name + " in " + tagName)

        const request = async() => {
            const response = await fetch(url + '/files/' + name);
            response.blob().then(blob => {
                let url = window.URL.createObjectURL(blob);
                this.url = url;
            });
        };
        request();
    }

    render() {
        const { show, gift, close } = this.props;

        if (gift === null) return <div/>;

        console.log(gift)

        return (
          <Modal isOpen={show} toggle={() => close()}>
            <ModalHeader toggle={() => close() }>{gift.name}</ModalHeader>
            <ModalBody>
                <div>
                  {
                    (gift.picture === undefined) ?
                    <img className="dg-image" src={blank_gift} alt="Nothing"/> :
                    <img className="dg-image" src={this.url} alt="Gift"/>
                  }
                  {gift.description !== "" && gift.description}
                </div>
                <div>{gift.price}</div>
                <div>{gift.whereToBuy}</div>
            </ModalBody>
          </Modal>);
    }
}

function mapStateToProps(state: AppState): ConnectProps {return {
  userId: state.signin.userId, username: state.signin.username, friendwishlist: state.locale.messages.friendwishlist, myBuyList: state.locale.messages.myBuyList };}
export default connect(mapStateToProps)(MyBuyList);
