import React from 'react';
import { Button, Input, Label, Form, FormGroup, FormText } from "reactstrap";

import { connect } from 'react-redux';
import { AppState } from '../redux/store';
import { changeUserInfo, UserNameAndPicture } from '../redux/actions/user';

import { ManageAccountMessage } from '../translation/itrans';

import SquareImage from './SquareImage';
import blank_profile_picture from './image/blank_profile_picture.png';

import { getServerUrl } from "../ServerInformation";
let url = getServerUrl();

interface DispatchProps {
  changeUserInfo: (user: UserNameAndPicture) => void
};
interface StateProps {
    token: string | null,
    username: String | null,
    picture: string | null,
    manageAccount: ManageAccountMessage };
type Props = DispatchProps & StateProps;
interface State { image: string | null };

class ManageAccount extends React.Component<Props, State> {
    constructor(props: Props) {
        super(props);
        this.state = { image: this.props.picture }
    }

    _changeImage(e: any) {
      const formData = new FormData();
      formData.append("0", e.target.files[0]);
      const request = async () => {
        const response = await fetch(url + '/files', {method: 'post', body: formData});
        if (response.status === 202) {
          const json = await response.json();
          this.setState({ image: json.name });
        } else {
            const json = await response.json();
            console.error(json);
        }
      };
      request();
    }

    _save() {
        const {image} = this.state;
        let imageName = (image === null) ? "" : image;
        const request = async () => {
            const response = await fetch(url + '/users', {
                method: "PATCH",
                headers: {'Content-Type':'application/json', 'Authorization': `Bearer ${this.props.token}`},
                body: JSON.stringify({
                    "name": this.props.username,
                    "picture": imageName})
            });
            if (response.status === 401) {
                console.error("Unauthorized. Disconnect and redirect to connect");
            } else if (response.status !== 202) {
                const json = await response.json();
                console.error(json);
            }
        };
        request();

        this.props.changeUserInfo({username: (this.props.username === null ? "" : this.props.username.toString()), picture: image});
    }

    render() {
        const { image } = this.state;

        return (<div>
            <Form>
                <FormGroup>
                    <Label>Name</Label>
                    <Input value={this.props.username !== undefined ? this.props.username!.toString() : ""} disabled/>
                    <FormText>Could not be changed</FormText>
                </FormGroup>
                <FormGroup>
                    <Label>Profile picture</Label>
                    <Input type="file" onChange={(e) => this._changeImage(e)}/>
                </FormGroup>
                <SquareImage token={this.props.token} className="profile-image" imageName={image} size={150} alt="Profile" alternateImage={blank_profile_picture}/>
                <br/>
                <Button className="btn btn-primary" onClick={() => this._save()}>Save</Button>
            </Form>
        </div>);
    }
}


function mapStateToProps(state: AppState): StateProps {return { token: state.signin.token, username: state.signin.username, picture: state.signin.picture, manageAccount: state.locale.messages.manageAccount };}
export default connect(mapStateToProps, {changeUserInfo})(ManageAccount);