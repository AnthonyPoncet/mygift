import React from 'react';

import Octicon, {Heart, Checklist, Gift} from '@primer/octicons-react'

import { connect } from 'react-redux';
import { AppState } from '../redux/store';

import './card-gift.css';

interface ConnectProps { userId: number | null, username: String | null };
interface Props extends ConnectProps { friendName: string }
interface State {
    gifts: any[],
    categories: any[],
    hoverId: string
}

class FriendWishList extends React.Component<Props, State> {

    constructor(props: Props) {
        super(props);
        this.state = {gifts: [], categories: [], hoverId: ''};
    }

    componentDidMount() {
        if (this.props.userId) {
            this.getGifts(this.props.userId, this.props.friendName);
            this.getCategories(this.props.userId, this.props.friendName);
        }
    }

    async getGifts(userId: number, friendName: string) {
        const response = await fetch('http://localhost:8080/users/' + userId + '/gifts/' + friendName);
        const json = await response.json();
        if (response.status === 200) {
            this.setState({ gifts: json.friendGifts });
        } else {
            console.log(json.error);
        }
    };

    async interested(userId: number | null, giftId: number, imInterested: boolean) {
        if (userId === null) return; //Impossible

        const response = await fetch('http://localhost:8080/users/' + userId + '/gifts/' + giftId + '/interested?userId=' + userId, {method: imInterested ? "DELETE" : "POST"});
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
            response = await fetch('http://localhost:8080/users/' + userId + '/gifts/' + giftId + '/buy-action?userId=' + userId, {method: "DELETE"});
        } else {
            response = await fetch('http://localhost:8080/users/' + userId + '/gifts/' + giftId + '/buy-action?userId=' + userId + '&action=WANT_TO_BUY', {method: "POST"});
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
            response = await fetch('http://localhost:8080/users/' + userId + '/gifts/' + giftId + '/buy-action?userId=' + userId, {method: "DELETE"});
        } else {
            response = await fetch('http://localhost:8080/users/' + userId + '/gifts/' + giftId + '/buy-action?userId=' + userId + '&action=BOUGHT', {method: "POST"});
        }
        if (response.status === 202) {
            this.getGifts(userId, this.props.friendName);
        } else {
            const json = await response.json();
            console.log(json.error);
        }
    }

    async getCategories(userId: number, friendName: string)  {
        const response = await fetch('http://localhost:8080/users/' + userId + '/categories/' + friendName);
        const json = await response.json();
        if (response.status === 200) {
            this.setState({ categories: json.categories } );
        } else {
            console.log(json.error);
        }
    };

    handleEnter(cat: number, gift: number) {
      this.setState({ hoverId: cat + "-" + gift});
    }

    handleOut() {
      this.setState({ hoverId: '' });
    }

    renderGifts() {
      if (this.state.categories) {
        let out = [];
        for (const [index, value] of this.state.categories.entries()) {
          const cat = (<h5 key={index + value.name}>{value.name}</h5>);
          let filtered = this.state.gifts.filter(g => { return g.gift.categoryId === value.id });
          if (filtered.length === 0) {
              out.push(<>{cat}<p key={index + 'no_gift'}>No gift</p></>);
          }
          else {
              let giftsOut = filtered.map((fGift, gIndex) => {
                const { gift, interestedUser, buyActionUser } = fGift;
                let wantToBuy: string[] = [];
                let bought: string[] = [];
                Object.keys(buyActionUser).forEach(key => {
                    if (buyActionUser[key] === "WANT_TO_BUY") wantToBuy.push(key);
                    if (buyActionUser[key] === "BOUGHT") bought.push(key);
                });
                if (index+'-'+gIndex === this.state.hoverId) {
                  let imInterested = false;
                  let iWantToBuy = false;
                  let iBought = false;
                  if (this.props.username !== null) {
                      for (const [index, value] of interestedUser.entries()) { if (value === this.props.username) imInterested = true; }
                      for (const [index, value] of wantToBuy.entries()) { if (value === this.props.username) iWantToBuy = true; }
                      for (const [index, value] of bought.entries()) { if (value === this.props.username) iBought = true; }
                  }
                  return (
                      <div className="mycard" onMouseEnter={() => this.handleEnter(index, gIndex)} onMouseLeave={() => this.handleOut()}>
                          <div className="card-edit-close">
                            <span className={imInterested ? "text-right icon-selected" : "text-right"} style={{cursor: "pointer"}} onClick={() => this.interested(this.props.userId, gift.id, imInterested)}><Octicon icon={Heart}/></span>{' '}
                            {interestedUser.length !== 0 && <><span>{interestedUser.length}</span>{' '}</>}
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
                        <div className="mycard" onMouseEnter={() => this.handleEnter(index, gIndex)} onMouseLeave={() => this.handleOut()}>
                              <div className="card-name-only">{gift.name}</div>
                        </div>);
                  }
                });
              out.push(<>{cat}<div className="mycard-row">{giftsOut}</div></>)
          }
        }
        return (<div>{out}</div>);
      }

    }

    render() {
        return (
        <div>
          <h1 className="friend-wishlist-title">Wish list de {this.props.friendName}</h1>
          <div>{this.renderGifts()}</div>
        </div>);
    }
}

function mapStateToProps(state: AppState): ConnectProps {return { userId: state.signin.userId, username: state.signin.username };}
export default connect(mapStateToProps)(FriendWishList);
