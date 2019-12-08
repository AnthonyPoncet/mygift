import React from 'react';

import Octicon, {Checklist, Gift} from '@primer/octicons-react';

import { Modal, ModalHeader, ModalBody, ModalFooter, Button } from "reactstrap";

import { connect } from 'react-redux';
import { AppState } from '../redux/store';

import { FriendWishListMessage, MyWishListMessage, MyBuyListMessage } from '../translation/itrans';
import './style/card-gift.css';
import SquareImage from './SquareImage';
import blank_gift from './image/blank_gift.png';

import { isMobile } from "react-device-detect";

import { getServerUrl } from "../ServerInformation";
let url = getServerUrl();


interface ConnectProps { userId: number | null, username: String | null, friendwishlist: FriendWishListMessage, mywishlist: MyWishListMessage, myBuyList: MyBuyListMessage };
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

    async getBuyList(userId: number) {
        const response = await fetch(url + '/users/' + userId + '/buy-list');
        const json = await response.json();
        if (response.status === 200) {
            this.setState({ friendAndGifts: json });
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
        } else {
            console.error(json.error);
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
            console.error(json.error);
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
            console.error(json.error);
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

    _renderInsideGift(fgi: number, gi: number, gift: any) {
        if ((fgi+'-'+gi === this.state.hoverId) || isMobile) {
            return (
                <div style={{cursor: "pointer"}} onClick={() => this.showGift(gift)}>
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

    renderGifts() {
      if (this.state.friendAndGifts) {
        return this.state.friendAndGifts.map((fg, fgi) => {
            return (
            <div key={fgi}>
                <h5 style={{margin: "10px"}}>{fg.friendName}</h5>

                <div className="mycard-row">
                {fg.gifts.map((fGift: any, gi:any) => {
                  const { gift, buyActionUser } = fGift;
                  let wantToBuy: string[] = [];
                  let bought: string[] = [];
                  Object.keys(buyActionUser).forEach(key => {
                      if (buyActionUser[key] === "WANT_TO_BUY") wantToBuy.push(key);
                      if (buyActionUser[key] === "BOUGHT") bought.push(key);
                  });
                  let iWantToBuy = false;
                  let iBought = false;
                  if (this.props.username !== null) {
                      for (const [, value] of wantToBuy.entries()) { if (value === this.props.username) iWantToBuy = true; }
                      for (const [, value] of bought.entries()) { if (value === this.props.username) iBought = true; }
                  }
                  return (
                    <div className="mycard" onMouseEnter={() => this.handleEnter(fgi, gi)} onMouseLeave={() => this.handleOut()} style={{cursor: "pointer"}} onClick={() => this.showGift(fGift)}>
                        <div className="card-edit-close">
                          <div className={iWantToBuy ? "icon-selected two-icon-first" : "two-icon-first"}>
                            <span style={{cursor: "pointer"}} onClick={() => this.wantToBuy(this.props.userId, gift.id, iWantToBuy, iBought)}><Octicon icon={Checklist}/></span>{' '}
                            {wantToBuy.length !== 0 && <><span>{wantToBuy.length}</span>{' '}</>}
                          </div>
                          <div className={iBought ? "icon-selected two-icon-second" : "two-icon-second"}>
                              <span style={{cursor: "pointer"}} onClick={() => this.bought(this.props.userId, gift.id, iWantToBuy, iBought)}><Octicon icon={Gift}/></span>{' '}
                              {bought.length !== 0 && <><span>{bought.length}</span>{' '}</>}
                          </div>
                        </div>
                        <SquareImage className="card-image" imageName={gift.picture} size={150} alt="Gift" alternateImage={blank_gift}/>
                        {this._renderInsideGift(fgi, gi, gift)}
                    </div>);
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

          <DisplayGift
            username={this.props.username}
            show={this.state.showGift}
            fGift={this.state.giftToShow}
            close={() => this.setState({showGift: false, giftToShow: null})}
            friendwishlist={this.props.friendwishlist}
            mywishlist={this.props.mywishlist}
            wantToBuy={(giftId: number, iWantToBuy: boolean, iBought: boolean) => this.wantToBuy(this.props.userId, giftId, iWantToBuy, iBought)}
            bought={(giftId: number, iWantToBuy: boolean, iBought: boolean) => this.bought(this.props.userId, giftId, iWantToBuy, iBought)}
            />
        </div>);
    }
}

interface DisplayGiftProps {
    username: String | null,
    show: boolean
    fGift: any | null,
    close(): void,
    friendwishlist: FriendWishListMessage,
    mywishlist: MyWishListMessage,
    wantToBuy: Function,
    bought: Function
};

class DisplayGift extends React.Component<DisplayGiftProps> {
    render() {
        const { show, fGift, close, mywishlist, friendwishlist } = this.props;
        if (fGift === null || fGift === undefined) return <div/>;
        const { gift } = fGift;

        const isContainer = isMobile ? "" : "container";
        const padding: string = isMobile ? "0px" : "10px";

        const wtb = gift.whereToBuy.split(" ");

        //Duplicated
        const { buyActionUser } = fGift;
        let wantToBuy: string[] = [];
        let bought: string[] = [];
        Object.keys(buyActionUser).forEach(key => {
            if (buyActionUser[key] === "WANT_TO_BUY") wantToBuy.push(key);
            if (buyActionUser[key] === "BOUGHT") bought.push(key);
        });
        let iWantToBuy = false;
        let iBought = false;
        if (this.props.username !== null) {
            for (const [, value] of wantToBuy.entries()) { if (value === this.props.username) iWantToBuy = true; }
            for (const [, value] of bought.entries()) { if (value === this.props.username) iBought = true; }
        }
        console.log(this.props.username)
        console.log(fGift)
        console.log(bought)
        console.log(iBought)

        return (
          <Modal isOpen={show} toggle={() => close()} size="lg">
            <ModalHeader toggle={() => close() }>{gift.name}</ModalHeader>
            <ModalBody>
                <div className={isContainer}>
                    <SquareImage className="card-image" imageName={gift.picture} size={300} alt="Gift" alternateImage={blank_gift}/>
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
             { (bought.length === 0 || iWantToBuy || iBought) && <ModalFooter>
                  <Button color={iWantToBuy ? "primary" : "secondary"} onClick={() => this.props.wantToBuy(gift.id, iWantToBuy, iBought)}><Octicon icon={Checklist}/> {friendwishlist.iWantToBuy}</Button>
                  <Button color={iBought ? "primary" : "secondary"} onClick={() => this.props.bought(gift.id, iWantToBuy, iBought)}><Octicon icon={Gift}/> {friendwishlist.iBought}</Button>
                </ModalFooter> }
          </Modal>);
    }
}

function mapStateToProps(state: AppState): ConnectProps {return {
  userId: state.signin.userId, username: state.signin.username, friendwishlist: state.locale.messages.friendwishlist, mywishlist: state.locale.messages.mywishlist, myBuyList: state.locale.messages.myBuyList };}
export default connect(mapStateToProps)(MyBuyList);
