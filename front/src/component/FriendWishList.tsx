import React from 'react';
import { withRouter } from 'react-router-dom';
import { RouteComponentProps } from "react-router";

import { connect } from 'react-redux';
import { AppState } from '../redux/store';

import './card-gift.css';

interface PathParam { friendName: string };
interface ConnectProps { userId: number | null };
interface Props extends RouteComponentProps<PathParam>, ConnectProps {}
interface State {
    gifts: any[],
    categories: any[],
    hoverId: string
}

class FriendWishList extends React.Component<Props, State> {
    private friendName: string;

    constructor(props: Props) {
        super(props);
        this.friendName = this.props.match.params.friendName;
        this.state = {gifts: [], categories: [], hoverId: ''};
    }

    componentDidMount() {
        if (this.props.userId) {
            this.getGifts(this.props.userId, this.friendName);
            this.getCategories(this.props.userId, this.friendName);
        }
    }

    async getGifts(userId: number, friendName: string) {
        const response = await fetch('http://localhost:8080/users/' + userId + '/gifts/' + friendName);
        const json = await response.json();
        if (response.status === 200) {
            this.setState({ gifts: json.gifts });
        } else {
            console.log(json.error);
        }
    };

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
          const cat = (
              <h5 key={index + value.name}>{value.name}</h5>);
          let filtered = this.state.gifts.filter(g => { return g.categoryId === value.id });
          if (filtered.length === 0) {
              out.push(<>{cat}<p key={index + 'no_gift'}>No gift</p></>);
          }
          else {
              let giftsOut = filtered.map((gift, gIndex) => {
                if (index+'-'+gIndex === this.state.hoverId) {
                  return (
                      <div className="mycard" onMouseEnter={() => this.handleEnter(index, gIndex)} onMouseLeave={() => this.handleOut()}>
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
        return (<div>{this.renderGifts()}</div>);
    }
}

function mapStateToProps(state: AppState): ConnectProps {return { userId: state.signin.userId };}
export default withRouter(connect(mapStateToProps)(FriendWishList));
