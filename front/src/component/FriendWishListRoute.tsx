import React from 'react';
import { withRouter } from 'react-router-dom';
import { RouteComponentProps } from "react-router";

import Octicon, {Heart, Checklist, Gift} from '@primer/octicons-react'

import { connect } from 'react-redux';
import { AppState } from '../redux/store';

import FriendWishList from './FriendWishList';

import './card-gift.css';

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
