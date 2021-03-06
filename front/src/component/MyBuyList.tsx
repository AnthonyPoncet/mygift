import React from 'react';

import Octicon, {Checklist, Gift} from '@primer/octicons-react';

import { Modal, ModalHeader, ModalBody, ModalFooter, Button } from "reactstrap";

import { connect } from 'react-redux';
import { ThunkDispatch } from 'redux-thunk'
import { AppState } from '../redux/store';
import { logout } from '../redux/actions/user';

import { FriendWishListMessage, MyWishListMessage, MyBuyListMessage } from '../translation/itrans';
import './style/card-gift.css';
import SquareImage from './SquareImage';
import blank_gift from './image/blank_gift.png';

import { isMobile } from "react-device-detect";

import { history } from './history';

import { getServerUrl } from "../ServerInformation";
let url = getServerUrl();


interface StateProps { token: string | null, username: String | null, friendwishlist: FriendWishListMessage, mywishlist: MyWishListMessage, myBuyList: MyBuyListMessage };
interface DispatchProps { logout: () => void };
type Props = DispatchProps & StateProps;
interface State {
    friendAndGifts: any[],
    hoverId: string,
    showGift: boolean,
    giftToShow: any | null,
    showDeletedGift: boolean
};

class MyBuyList extends React.Component<Props, State> {
    constructor(props: Props) {
        super(props);
        this.state = {friendAndGifts: [], hoverId: '', showGift: false, giftToShow: null, showDeletedGift: false};
    }

    componentDidMount() {
        if (this.props.token) {
            this.getBuyList(this.props.token);
        }
    }

    _redirect() {
        console.error("Unauthorized. Disconnect and redirect to connect");
        history.push("/signin");
        this.props.logout();
    }

    async getBuyList(token: string) {
        const response = await fetch(url + '/buy-list', {headers: {'Authorization': `Bearer ${this.props.token}`}});
        if (response.status === 200) {
            const json = await response.json();
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
        } else if (response.status === 401) {
            this._redirect()
        } else {
            const json = await response.json();
            console.error(json.error);
        }
    };

    async wantToBuy(token: string | null, giftId: number, iWantToBuy: boolean, iBought: boolean, refresh: boolean) {
        if (token === null) return; //Impossible

        if (iBought) await this.bought(token, giftId, false, true, false); //Remove ibought

        let response = null;
        if (iWantToBuy === true) {
            response = await fetch(url + '/gifts/' + giftId + '/buy-action', {method: "DELETE", headers: {'Authorization': `Bearer ${this.props.token}`}});
        } else {
            response = await fetch(url + '/gifts/' + giftId + '/buy-action?action=WANT_TO_BUY', {method: "POST", headers: {'Authorization': `Bearer ${this.props.token}`}});
        }
        if (response.status === 202) {
            if (refresh) this.getBuyList(token);
        } else if (response.status === 401) {
            this._redirect()
        } else {
            const json = await response.json();
            console.error(json.error);
        }
    }

    async bought(token: string | null, giftId: number, iWantToBuy: boolean, iBought: boolean, refresh: boolean) {
        if (token === null) return; //Impossible

        if (iWantToBuy) await this.wantToBuy(token, giftId, true, false, false); //Remove ibought

        let response = null;
        if (iBought === true) {
            response = await fetch(url + '/gifts/' + giftId + '/buy-action', {method: "DELETE", headers: {'Authorization': `Bearer ${this.props.token}`}});
        } else {
            response = await fetch(url + '/gifts/' + giftId + '/buy-action?action=BOUGHT', {method: "POST", headers: {'Authorization': `Bearer ${this.props.token}`}});
        }
        if (response.status === 202) {
            if (refresh) this.getBuyList(token);
        } else if (response.status === 401) {
            this._redirect()
        } else {
            const json = await response.json();
            console.error(json.error);
        }
    }

    async _deleteDeletedGift(token: string | null, giftId: number) {
        if (token === null) return; //Impossible

        const response = await fetch(url + '/buy-list/deleted-gifts/' + giftId, {method: "DELETE", headers: {'Authorization': `Bearer ${this.props.token}`}});
        if (response.status === 200) {
            this.setState({ showDeletedGift: false, giftToShow: null });
            this.getBuyList(token);
        } else if (response.status === 401) {
            this._redirect()
        } else {
            const json = await response.json();
            console.error(json.error);
        }
    }

    handleEnter(friend: number, gift: number, type: string) {
        this.setState({ hoverId: friend + '-' + gift + '-' + type});
    }

    handleOut() {
        this.setState({ hoverId: '' });
    }

    showGift(gift: any) {
        this.setState({ showGift: true, giftToShow: gift });
    }

    showDeletedGift(gift: any) {
        this.setState({ showDeletedGift: true, giftToShow: gift });
    }

    _renderInsideGift(fgi: number, gi: number, type: string, fGift: any, showFun: Function) {
        const { gift } = fGift;
        if ((fgi+'-'+gi+'-'+type === this.state.hoverId) || isMobile) {
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

    _renderFriend(fg: any, fgi: number) {
        return (
            <div key={'friendAndGifts'+fgi}>
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
                    <div className="mycard" onMouseEnter={() => this.handleEnter(fgi, gi, 'valid')} onMouseLeave={() => this.handleOut()} style={{cursor: "pointer"}}>
                        <div className="card-edit-close">
                          <div className={iWantToBuy ? "icon-selected two-icon-first" : "two-icon-first"}>
                            <span style={{cursor: "pointer"}} onClick={() => this.wantToBuy(this.props.token, gift.id, iWantToBuy, iBought, true)}><Octicon icon={Checklist}/></span>{' '}
                            {wantToBuy.length !== 0 && <><span>{wantToBuy.length}</span>{' '}</>}
                          </div>
                          <div className={iBought ? "icon-selected two-icon-second" : "two-icon-second"}>
                              <span style={{cursor: "pointer"}} onClick={() => this.bought(this.props.token, gift.id, iWantToBuy, iBought, true)}><Octicon icon={Gift}/></span>{' '}
                              {bought.length !== 0 && <><span>{bought.length}</span>{' '}</>}
                          </div>
                        </div>
                        <div style={{cursor: "pointer"}} onClick={() => this.showGift(fGift)}>
                            <SquareImage token={this.props.token} className="card-image" imageName={gift.picture} size={150} alt="Gift" alternateImage={blank_gift}/>
                        </div>
                        {this._renderInsideGift(fgi, gi, 'valid', fGift, (gift: any) => this.showGift(gift))}
                    </div>);
                })}
                {fg.deletedGifts.map((gift: any, gi:any) => {
                    return (
                        <div className="mycard warning-border" onMouseEnter={() => this.handleEnter(fgi, gi, 'deleted')} onMouseLeave={() => this.handleOut()} style={{cursor: "pointer"}}>
                            <div className="card-edit-close warning-gift-delete">
                                <div>{gift.status === "RECEIVED" ? this.props.myBuyList.received : this.props.myBuyList.not_wanted}</div>
                            </div>
                            <div style={{cursor: "pointer"}} onClick={() => this.showDeletedGift({gift: gift})}>
                                <SquareImage token={this.props.token} className="card-image" imageName={gift.picture} size={150} alt="Gift" alternateImage={blank_gift}/>
                            </div>
                            {this._renderInsideGift(fgi, gi, 'deleted', {gift: gift}, (gift: any) => this.showDeletedGift(gift))}
                        </div>);
                    })
                }
                </div>
            </div>);
    }

    renderGifts() {
      if (this.state.friendAndGifts) {
        return (
            <div className="mycard-row">
                {this.state.friendAndGifts.map((fg, fgi) => { return(<div className="mycard-no-limit">{this._renderFriend(fg, fgi)}</div>) })}
            </div>
        );
      }
    }

    render() {
        return (
        <div>
            <h1 className="friend-wishlist-title">{this.props.myBuyList.title}</h1>
            <div>{this.renderGifts()}</div>

                <DisplayGift
                    token={this.props.token}
                    username={this.props.username}
                    show={this.state.showGift}
                    fGift={this.state.giftToShow}
                    close={() => this.setState({showGift: false, giftToShow: null})}
                    friendwishlist={this.props.friendwishlist}
                    mywishlist={this.props.mywishlist}
                    wantToBuy={(giftId: number, iWantToBuy: boolean, iBought: boolean) => this.wantToBuy(this.props.token, giftId, iWantToBuy, iBought, true)}
                    bought={(giftId: number, iWantToBuy: boolean, iBought: boolean) => this.bought(this.props.token, giftId, iWantToBuy, iBought, true)}
                    />
                <DisplayDeletedGift
                    token={this.props.token}
                    username={this.props.username}
                    show={this.state.showDeletedGift}
                    fGift={this.state.giftToShow}
                    close={() => this.setState({showDeletedGift: false, giftToShow: null})}
                    myBuyList={this.props.myBuyList}
                    mywishlist={this.props.mywishlist}
                    ok={(id: number) => this._deleteDeletedGift(this.props.token, id)}
                    />
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
    wantToBuy: Function,
    bought: Function
};

class DisplayGift extends React.Component<DisplayGiftProps> {
    render() {
        const { show, fGift, close, mywishlist, friendwishlist } = this.props;
        if (show === false || fGift === null || fGift === undefined) return <div/>;
        const { gift } = fGift;

        const isContainer = isMobile ? "" : "container";
        const padding: string = isMobile ? "0px" : "10px";

        let wtb : string[] = [];
        if (gift.whereToBuy !== undefined) {
            wtb = gift.whereToBuy.split(" ");
        }

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

        return (
          <Modal isOpen={show} toggle={() => close()} size="lg">
            <ModalHeader toggle={ () => close() }>{gift.name}</ModalHeader>
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
                                    return <a href={word} target="_blank" rel="noopener noreferrer">{smallWord}{' '}</a>;
                                } else {
                                    return word + " ";
                                }
                            })}</div>
                        }
                    </div>
                </div>
            </ModalBody>
             { (bought.length === 0 || iWantToBuy || iBought) && <ModalFooter>
                  <Button color={iWantToBuy ? "primary" : "secondary"} onClick={() => this.props.wantToBuy(gift.id, iWantToBuy, iBought)}><Octicon icon={Checklist}/> {friendwishlist.iWantToBuy}</Button>
                  <Button color={iBought ? "primary" : "secondary"} onClick={() => this.props.bought(gift.id, iWantToBuy, iBought)}><Octicon icon={Gift}/> {friendwishlist.iBought}</Button>
                </ModalFooter> }
          </Modal>);
    }
}

interface DisplayDeletedGiftProps {
    token: string | null,
    username: String | null,
    show: boolean
    fGift: any | null,
    close(): void,
    mywishlist: MyWishListMessage,
    myBuyList: MyBuyListMessage,
    ok: Function
};

class DisplayDeletedGift extends React.Component<DisplayDeletedGiftProps> {
    render() {
        const { show, fGift, close, mywishlist, myBuyList } = this.props;
        if (show === false || fGift === null || fGift === undefined) return <div/>;
        const { gift } = fGift;

        const isContainer = isMobile ? "" : "container";
        const padding: string = isMobile ? "0px" : "10px";

        let wtb : string[] = [];
        if (gift.whereToBuy !== undefined) {
            wtb = gift.whereToBuy.split(" ");
        }

        return (
          <Modal isOpen={show} toggle={() => close()} size="lg">
            <ModalHeader toggle={ () => close() }>{gift.name}</ModalHeader>
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
                                    return <a href={word} target="_blank" rel="noopener noreferrer">{smallWord}{' '}</a>;
                                } else {
                                    return word + " ";
                                }
                            })}</div>
                        }
                    </div>
                </div>
            </ModalBody>
            <ModalFooter>
                <Button color="primary" onClick={() => this.props.ok(gift.id)}>{myBuyList.ok}</Button>
            </ModalFooter>
          </Modal>);
    }
}

function mapStateToProps(state: AppState): StateProps {
    return { token: state.signin.token,
              username: state.signin.username,
              friendwishlist: state.locale.messages.friendwishlist,
              mywishlist: state.locale.messages.mywishlist,
              myBuyList: state.locale.messages.myBuyList };
}
const mapDispatchToProps = (dispatch: ThunkDispatch<{}, {}, any>, ownProps: Props): DispatchProps => {
   return { logout: async () => await dispatch(logout()) }
}
export default connect(mapStateToProps, mapDispatchToProps)(MyBuyList);
