import React from 'react';

import Octicon, {Checklist, Gift} from '@primer/octicons-react';

import { Modal, ModalHeader, ModalBody } from "reactstrap";

import { connect } from 'react-redux';
import { AppState } from '../redux/store';

import { FriendWishListMessage, MyBuyListMessage } from '../translation/itrans';
import './style/card-gift.css';
import SquareImage from './SquareImage';
import blank_gift from './image/blank_gift.png';

import { isMobile } from "react-device-detect";

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

    async getBuyList(userId: number) {
        const response = await fetch(url + '/users/' + userId + '/buy-list');
        const json = await response.json();
        if (response.status === 200) {
            this.setState({ friendAndGifts: json });
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
                  if ((fgi+'-'+gi === this.state.hoverId) || isMobile) {
                    let iWantToBuy = false;
                    let iBought = false;
                    if (this.props.username !== null) {
                        for (const [, value] of wantToBuy.entries()) { if (value === this.props.username) iWantToBuy = true; }
                        for (const [, value] of bought.entries()) { if (value === this.props.username) iBought = true; }
                    }
                    return (
                        <div className="mycard" onMouseEnter={() => this.handleEnter(fgi, gi)} onMouseLeave={() => this.handleOut()} style={{cursor: "pointer"}} onClick={() => this.showGift(gift)}>
                            <SquareImage imageName={gift.picture} size={150} alt="Gift" alternateImage={blank_gift}/>
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
                      return (
                          <div className="mycard" onMouseEnter={() => this.handleEnter(fgi, gi)} onMouseLeave={() => this.handleOut()}>
                              <SquareImage imageName={gift.picture} size={150} alt="Gift" alternateImage={blank_gift}/>
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
    render() {
        const { show, gift, close } = this.props;

        if (gift === null) return <div/>;

        return (
          <Modal isOpen={show} toggle={() => close()}>
            <ModalHeader toggle={() => close() }>{gift.name}</ModalHeader>
            <ModalBody>
                <div>
                  <SquareImage imageName={gift.picture} size={300} alt="Gift" alternateImage={blank_gift}/>
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
