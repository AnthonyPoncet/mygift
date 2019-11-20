import React from 'react';
import { Modal, ModalHeader, ModalBody, Button, Input, Label, FormGroup, FormFeedback } from "reactstrap";

import Octicon, {Pencil, X} from '@primer/octicons-react';

import { connect } from 'react-redux';
import { AppState } from '../redux/store';

import { MyWishListMessage } from '../translation/itrans';

import './style/card-gift.css';
import SquareImage from './SquareImage';
import blank_gift from './image/blank_gift.png';

import { isMobile } from "react-device-detect";

import { getServerUrl } from "../ServerInformation";
let url = getServerUrl();


interface Props { userId: number | null, mywishlist: MyWishListMessage };
interface State {
  catAndGifts: any[],
  show: boolean, title: string, bodyRender: any, button: { text: string, fun: any }, inputs: any, errorMessage: string,
  hoverId: string
};

class MyWishList extends React.Component<Props, State> {
    constructor(props: Props) {
        super(props);

        this.openAddGift = this.openAddGift.bind(this);
        this.openEditGift = this.openEditGift.bind(this);
        this.addGift = this.addGift.bind(this);
        this.updateGift = this.updateGift.bind(this);
        this.giftBodyRender = this.giftBodyRender.bind(this);

        this.openAddCat = this.openAddCat.bind(this);
        this.openEditCat = this.openEditCat.bind(this);
        this.addCat = this.addCat.bind(this);
        this.updateCat = this.updateCat.bind(this);
        this.catBodyRender = this.catBodyRender.bind(this);

        this.closeModal = this.closeModal.bind(this);

        this.state = {
            catAndGifts: [],
            show: false, title: '', bodyRender: null, button: { text: '', fun: null }, inputs: { }, errorMessage: '',
            hoverId: ''
        }
    }

    componentDidMount() {
        if (this.props.userId) {
            this.getGifts(this.props.userId);
        }
    }

    //Hanle loggin, seems weird
    componentWillReceiveProps(nextProps: Props, nextContext: any) {
        if (nextProps.userId) {
            this.getGifts(nextProps.userId);
        }
    }

    openAddGift() {
        const { mywishlist } = this.props;
        this.setState( { show: true, title: mywishlist.addGiftModalTitle, bodyRender: this.giftBodyRender, button: { text: mywishlist.addModalButton, fun: this.addGift },
            inputs: { name: '', nameValidity: true, description: null, price: null, whereToBuy: null, categoryId: this.state.catAndGifts[0].category.id, picture: null }, errorMessage: '' });
    }

    openEditGift(giftId: number, name: string, description: string, price: string, whereToBuy: string, categoryId: number, image: string | null) {
        const { mywishlist } = this.props;
        this.setState( { show: true, title: mywishlist.updateGiftModalTitle, bodyRender: this.giftBodyRender, button: { text: mywishlist.updateModalButton, fun: () => this.updateGift(giftId) },
            inputs: { name: name, nameValidity: true, description: description, price: price, whereToBuy: whereToBuy, categoryId: categoryId, picture: image }, errorMessage: '' });
    }

    handleChangeGift = async (event: any) => {
        const { inputs } = this.state;
        const { name, value } = event.target;
        inputs[name] = value;
        if (name === "name") {
            inputs["nameValidity"] = value.length > 0;
        }

        await this.setState({ inputs: inputs });
    };

    updateGiftModalCategory(id: string) {
        const { inputs } = this.state;
        this.setState({ inputs: { ...inputs, categoryId: id } });
    }

    changeImage(e: any) {
        const formData = new FormData();
        formData.append("0", e.target.files[0]);
        const request = async () => {
            const response = await fetch(url + '/files', {method: 'post', body: formData});
            if (response.status === 202) {
                const json = await response.json();
                const { inputs } = this.state;
                inputs["picture"] = json.name;
                this.setState({ inputs: inputs });
            } else {
                const json = await response.json();
                console.error(json);
            }
        };
        request();
    }

    giftBodyRender() {
        const options = this.state.catAndGifts && this.state.catAndGifts.map( (cag, index) => {
          let value = cag.category;
          if (this.state.inputs.categoryId === value.id) {
            return <option key={index} value={value.id} selected>{value.name}</option>
          } else {
            return <option key={index} value={value.id}>{value.name}</option>
          }
        });

        const { mywishlist } = this.props;

        return (<>
            <FormGroup>
                <Label>{mywishlist.name}</Label>
                <Input name="name" placeholder={mywishlist.name} value={this.state.inputs.name} invalid={!this.state.inputs.nameValidity} onChange={(e) => this.handleChangeGift(e)}/>
                <FormFeedback>{mywishlist.nameErrorMessage}</FormFeedback>
            </FormGroup>
            <FormGroup>
                <Label>{mywishlist.description}</Label>
                <Input type="textarea" name="description" placeholder={mywishlist.description} value={this.state.inputs.description} onChange={(e) => this.handleChangeGift(e)}/>
            </FormGroup>
            <FormGroup>
                <Label>{mywishlist.price}</Label>
                <Input name="price" placeholder="10" value={this.state.inputs.price} onChange={(e) => this.handleChangeGift(e)}/>
            </FormGroup>
            <FormGroup>
                <Label>{mywishlist.whereToBuy}</Label>
                <Input name="whereToBuy" placeholder={mywishlist.whereToBuyPlaceholder} value={this.state.inputs.whereToBuy} onChange={(e) => this.handleChangeGift(e)}/>
            </FormGroup>
            <FormGroup>
                <Label>{mywishlist.category}</Label>
                <Input type="select" name="categoryId" onChange={(e) => this.updateGiftModalCategory(e.target.value)}>
                    {options}
                </Input>
            </FormGroup>
            <FormGroup>
              <Label>{mywishlist.image}</Label>
              <Input type="file" onChange={(e) => this.changeImage(e)}/>
            </FormGroup>
            <SquareImage className="card-image" imageName={this.state.inputs.picture} size={150} alt="Gift" alternateImage={blank_gift}/>
            </>);
    }

    giftRestCall(url: string, method: string) {
        const {inputs} = this.state;

        if (inputs.name === '') {
            this.setState({ show: true, errorMessage: this.props.mywishlist.nameErrorMessage })
            return;
        }

        let imageName = (inputs.picture === null) ? "" : inputs.picture;
        const request = async () => {
            const response = await fetch(url, {
                method: method,
                headers: {'Content-Type':'application/json'},
                body: JSON.stringify({
                    "name": inputs.name,
                    "description" : inputs.description,
                    "price": inputs.price,
                    "whereToBuy": inputs.whereToBuy,
                    "categoryId": inputs.categoryId,
                    "picture": imageName})
            });
            if (response.status === 200) {
                this.setState({ show: false });
                this.props.userId !== null && this.getGifts(this.props.userId);
            } else {
                const json = await response.json();
                this.setState({ show: true, errorMessage: json.error });
            }
        };
        request();
    }

    addGift() { this.giftRestCall(url + '/users/' + this.props.userId + '/gifts', 'PUT'); }

    updateGift(id: number) { this.giftRestCall(url + '/users/' + this.props.userId + '/gifts/' + id, 'PATCH'); }

    deleteGift(id: number) {
        const request = async () => {
            const response = await fetch(url + '/users/' + this.props.userId + '/gifts/' + id, {method: 'delete'});
            if (response.status === 202) {
                this.props.userId && this.getGifts(this.props.userId);
            } else {
                const json = await response.json();
                console.log(json);
            }
        };
        request();
    }


    openAddCat() {
        const { mywishlist } = this.props;
        this.setState( { show: true, title: mywishlist.addCategoryModalTitle, bodyRender: this.catBodyRender, button: { text: mywishlist.addModalButton, fun: this.addCat },
            inputs: { name: '', nameValidity: true }, errorMessage: '' });
    }

    openEditCat(name: string, categoryId: number) {
        const { mywishlist } = this.props;
        this.setState( { show: true, title: mywishlist.updateCategoryModalTitle, bodyRender: this.catBodyRender, button: { text: mywishlist.updateModalButton, fun: () => this.updateCat(categoryId) },
            inputs: { name: name, nameValidity: true }, errorMessage: '' });
    }

    handleChangeCat = async (event: any) => {
        await this.setState({ inputs: { name: event.target.value, nameValidity: event.target.value.length > 0 } });
    };

    catBodyRender() {
        const { mywishlist } = this.props;
        return (
            <FormGroup>
                <Label>{mywishlist.name}</Label>
                <Input name="name" placeholder={mywishlist.name} value={this.state.inputs.name} invalid={!this.state.inputs.nameValidity} onChange={(e) => this.handleChangeCat(e)}/>
                <FormFeedback>{mywishlist.nameErrorMessage}</FormFeedback>
            </FormGroup>);
    }

    catRestCall(url: string, method: string) {
        const {inputs} = this.state;
        if (inputs.name === '') {
            this.setState({ show: true, errorMessage: this.props.mywishlist.nameErrorMessage })
            return;
        }

        const request = async () => {
            const response = await fetch(url, {
                method: method,
                headers: {'Content-Type':'application/json'},
                body: JSON.stringify({"name": inputs.name})
            });
            if (response.status === 200) {
                this.setState({ show: false });
                this.props.userId && this.getGifts(this.props.userId);
            } else {
                const json = await response.json();
                this.setState({ show: true, errorMessage: json.error });
            }
        };
        request();
    }

    addCat() { this.catRestCall(url + '/users/' + this.props.userId + '/categories', 'PUT'); }

    updateCat(id: number) { this.catRestCall(url + '/users/' + this.props.userId + '/categories/' + id, 'PATCH'); }

    deleteCat(id: number) {
        const request = async () => {
            const response = await fetch(url + '/users/' + this.props.userId + '/categories/' + id, {method: 'delete'});
            if (response.status === 202) {
                this.props.userId && this.getGifts(this.props.userId);
            } else {
                const json = await response.json();
                console.log(json);
            }
        };
        request();
    }

    closeModal() { this.setState({ show: false }); }

    async getGifts(userId: number) {
        const response = await fetch(url + '/users/' + userId + '/gifts');
        const json = await response.json();
        if (response.status === 200) {
            this.setState({ catAndGifts: json });
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

    _renderInsideGift(cgi: number, gi: number, gift: any) {
        const fun = () => this.openEditGift(gift.id, gift.name, gift.description, gift.price, gift.whereToBuy, gift.categoryId, gift.picture === undefined ? null : gift.picture);
        if ((cgi+'-'+gi === this.state.hoverId) || isMobile) {
            return (<>
              <div className="card-edit-close">
                <span style={{cursor: "pointer"}} onClick={() => this.deleteGift(gift.id)}><Octicon icon={X}/></span>
              </div>
              <div style={{cursor: "pointer"}} onClick={fun}>
                  <div className="card-name">{gift.name}</div>
                  <div className="card-description">{gift.description}</div>
                  <div className="mycard-footer">
                    <div className="card-wtb">{gift.whereToBuy}</div>
                    <div className="card-price">{gift.price}</div>
                  </div>
              </div>
            </>);
        } else {
            return <div className="card-name-only">{gift.name}</div>;
        }
    }

    renderGifts() {
      if (this.state.catAndGifts) {
        return this.state.catAndGifts.map((cg, cgi) => {
            return (
            <div key={cgi}>
                <h5 style={{margin: "10px"}}>{cg.category.name}
                {' '}
                <span style={{cursor: "pointer"}} onClick={() => this.openEditCat(cg.category.name, cg.category.id)}><Octicon icon={Pencil} verticalAlign='middle'/></span>
                {' '}
                <span style={{cursor: "pointer"}} onClick={() => this.deleteCat(cg.category.id)}><Octicon icon={X} verticalAlign='middle'/></span>
                </h5>

                <div className="mycard-row">
                {cg.gifts.map((gift: any, gi:any) => {
                  const fun = () => this.openEditGift(gift.id, gift.name, gift.description, gift.price, gift.whereToBuy, gift.categoryId, gift.picture === undefined ? null : gift.picture);
                  return (
                      <div className="mycard" onMouseEnter={() => this.handleEnter(cgi, gi)} onMouseLeave={() => this.handleOut()}>
                          <div style={{cursor: "pointer"}} onClick={fun}>
                            <SquareImage className="card-image" imageName={gift.picture} size={150} alt="Gift" alternateImage={blank_gift}/>
                          </div>
                          {this._renderInsideGift(cgi, gi, gift)}
                      </div>);
                })}
                </div>
            </div>)
        });
      }
    }

    render() {
        const { button } = this.state;
        const { mywishlist } = this.props;
        let modalBody = [];
        if (this.state.bodyRender !== null) {
            modalBody.push(this.state.bodyRender());
        }

        return (
            <div>
                {this.props.userId && <>
                    <Button color="link" onClick={this.openAddGift}>{mywishlist.addGiftButton}</Button>
                    <Button color="link" onClick={this.openAddCat}>{mywishlist.addCategoryButton}</Button>
                    {this.renderGifts()}</>}

                <Modal isOpen={this.state.show} toggle={this.closeModal}>
                    <ModalHeader toggle={this.closeModal}>{this.state.title}</ModalHeader>
                    <ModalBody>
                        { this.state.errorMessage && <p className="auth-error">{this.state.errorMessage}</p> }
                        {modalBody}
                        <Button color="primary" onClick={button.fun}>{button.text}</Button>
                    </ModalBody>
                </Modal>
            </div>);
    }
}

function mapStateToProps(state: AppState) : Props { return { userId: state.signin.userId, mywishlist: state.locale.messages.mywishlist }; }
export default connect(mapStateToProps)(MyWishList);
