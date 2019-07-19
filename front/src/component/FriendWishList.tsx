import React from 'react';
import { withRouter } from 'react-router-dom';
import { RouteComponentProps } from "react-router";

import { connect } from 'react-redux';
import { AppState } from '../redux/store';

interface PathParam { friendName: string };
interface ConnectProps { userId: number | null };
interface Props extends RouteComponentProps<PathParam>, ConnectProps {}
interface State {
    gifts: any[],
    categories: any[]
}

class FriendWishList extends React.Component<Props, State> {
    private friendName: string;

    constructor(props: Props) {
        super(props);
        this.friendName = this.props.match.params.friendName;
        this.state = {gifts: [], categories: []};
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

    renderGifts() {
        if (this.state.categories) {
            let out = [];
            for (const [index, value] of this.state.categories.entries()) {
                out.push(<h3 key={index + value.name}>{value.name}</h3>);
                let filtered = this.state.gifts.filter(g => { return g.categoryId === value.id });
                if (filtered.length === 0)
                    out.push(<p key={index + 'no_gift'}>No gift</p>);
                else
                    out.push(filtered.map((gift, gIndex) => { return (<li key={index + gIndex + gift.name }>{gift.name}</li>);}));
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
