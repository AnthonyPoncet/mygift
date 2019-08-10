import React from 'react';
import { RouteComponentProps } from "react-router";

import FriendWishList from './FriendWishList';

interface PathParam { friendName: string };
interface Props extends RouteComponentProps<PathParam> {};

class FriendWishListRoute extends React.Component<Props> {
    private friendName: string;

    constructor(props: Props) {
        super(props);
        this.friendName = this.props.match.params.friendName;
    }

    render() {
        return (<FriendWishList friendName={this.friendName}/>);
    }
}

export default FriendWishListRoute;
