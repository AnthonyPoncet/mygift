import React from 'react';
import { RouteComponentProps } from "react-router";

import FriendWishList from './FriendWishList';

import { connect } from 'react-redux';
import { ThunkDispatch } from 'redux-thunk'
import { logout } from '../redux/actions/user';

interface PathParam { friendName: string };
interface StateProps extends RouteComponentProps<PathParam> {};
interface DispatchProps { logout: () => void };
type Props = DispatchProps & StateProps;

class FriendWishListRoute extends React.Component<Props> {
    private friendName: string;

    constructor(props: Props) {
        super(props);
        this.friendName = this.props.match.params.friendName;
    }

    render() {
        return (<FriendWishList friendName={this.friendName} logout={this.props.logout}/>);
    }
}

const mapDispatchToProps = (dispatch: ThunkDispatch<{}, {}, any>, ownProps: Props): DispatchProps => {
    return { logout: async () => await dispatch(logout()) }
}
export default connect(null, mapDispatchToProps)(FriendWishListRoute);
