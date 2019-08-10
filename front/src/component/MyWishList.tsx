import React from 'react';
import { Modal, ModalHeader, ModalBody, Button, Input, Label, FormGroup, FormFeedback } from "reactstrap";

import Octicon, {Pencil, X} from '@primer/octicons-react'

import { connect } from 'react-redux';
import { AppState } from '../redux/store';

import './card-gift.css';

interface Props {
  userId: number | null
}
interface State {
  gifts: any[],
  categories: any[],
  show: boolean, title: string, bodyRender: any, button: { text: string, fun: any }, inputs: any, errorMessage: string,
  hoverId: string
}

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
            gifts: [],
            categories: [],
            show: false, title: '', bodyRender: null, button: { text: '', fun: null }, inputs: { }, errorMessage: '',
            hoverId: ''
        }
    }

    componentDidMount() {
        if (this.props.userId) {
            this.getGifts(this.props.userId);
            this.getCategories(this.props.userId);
        }
    }

    //Hanle loggin, seems weird
    componentWillReceiveProps(nextProps: Props, nextContext: any) {
        if (nextProps.userId) {
            this.getGifts(nextProps.userId);
            this.getCategories(nextProps.userId);
        }
    }

    openAddGift() {
        this.setState( { show: true, title: "Add a new gift", bodyRender: this.giftBodyRender, button: { text: 'Add', fun: this.addGift },
            inputs: { name: '', nameValidity: true, description: null, price: null, whereToBuy: null, categoryId: this.state.categories[0].id }, errorMessage: '' });
    }

    openEditGift(giftId: number, name: string, description: string, price: string, whereToBuy: string, categoryId: number) {
        this.setState( { show: true, title: "Update gift", bodyRender: this.giftBodyRender, button: { text: 'Update', fun: () => this.updateGift(giftId) },
            inputs: { name: name, nameValidity: true, description: description, price: price, whereToBuy: whereToBuy, categoryId: categoryId }, errorMessage: '' });
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

    giftBodyRender() {
        const options = this.state.categories && this.state.categories.map( (value, index) => {
          if (this.state.inputs.categoryId === value.id) {
            return <option key={index} value={value.id} selected>{value.name}</option>
          } else {
            return <option key={index} value={value.id}>{value.name}</option>
          }
        });

        return (<>
            <FormGroup>
                <Label>Name</Label>
                <Input name="name" placeholder="name" value={this.state.inputs.name} invalid={!this.state.inputs.nameValidity} onChange={(e) => this.handleChangeGift(e)}/>
                <FormFeedback>Name is mandatory</FormFeedback>
            </FormGroup>
            <FormGroup>
                <Label>Description</Label>
                <Input name="description" placeholder="description" value={this.state.inputs.description} onChange={(e) => this.handleChangeGift(e)}/>
            </FormGroup>
            <FormGroup>
                <Label>Price</Label>
                <Input name="price" placeholder="10" value={this.state.inputs.price} onChange={(e) => this.handleChangeGift(e)}/>
            </FormGroup>
            <FormGroup>
                <Label>Where to buy</Label>
                <Input name="whereToBuy" placeholder="Amazon link, local shop..." value={this.state.inputs.whereToBuy} onChange={(e) => this.handleChangeGift(e)}/>
            </FormGroup>
            <FormGroup>
                <Label>Category</Label>
                <Input type="select" name="categoryId" onChange={(e) => this.updateGiftModalCategory(e.target.value)}>
                    {options}
                </Input>
            </FormGroup></>);
    }

    giftRestCall(url: string, method: string) {
      console.log(url);
        const {inputs} = this.state;

        if (inputs.name === '') {
            this.setState({ show: true, errorMessage: "Name is mandatory." })
            return;
        }

        const request = async () => {
            const response = await fetch(url, {
                method: method,
                headers: {'Content-Type':'application/json'},
                body: JSON.stringify({
                    "name": inputs.name,
                    "description" : inputs.description,
                    "price": inputs.price,
                    "whereToBuy": inputs.whereToBuy,
                    "categoryId": inputs.categoryId})
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

    addGift() { this.giftRestCall('http://localhost:8080/users/' + this.props.userId + '/gifts', 'PUT'); }

    updateGift(id: number) { this.giftRestCall('http://localhost:8080/users/' + this.props.userId + '/gifts/' + id, 'PATCH'); }

    deleteGift(id: number) {
        const request = async () => {
            const response = await fetch('http://localhost:8080/users/' + this.props.userId + '/gifts/' + id, {method: 'delete'});
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
        this.setState( { show: true, title: "Add a new category", bodyRender: this.catBodyRender, button: { text: 'Add', fun: this.addCat },
            inputs: { name: '', nameValidity: true }, errorMessage: '' });
    }

    openEditCat(name: string, categoryId: number) {
        this.setState( { show: true, title: "Update category", bodyRender: this.catBodyRender, button: { text: 'Update', fun: () => this.updateCat(categoryId) },
            inputs: { name: name, nameValidity: true }, errorMessage: '' });
    }

    handleChangeCat = async (event: any) => {
        await this.setState({ inputs: { name: event.target.value, nameValidity: event.target.value.length > 0 } });
    };

    catBodyRender() {
        return (
            <FormGroup>
                <Label>Name</Label>
                <Input name="name" placeholder="name" value={this.state.inputs.name} invalid={!this.state.inputs.nameValidity} onChange={(e) => this.handleChangeCat(e)}/>
                <FormFeedback>Name is mandatory</FormFeedback>
            </FormGroup>);
    }

    catRestCall(url: string, method: string) {
        const {inputs} = this.state;
        if (inputs.name === '') {
            this.setState({ show: true, errorMessage: "Name is mandatory." })
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
                this.props.userId && this.getCategories(this.props.userId);
            } else {
                const json = await response.json();
                this.setState({ show: true, errorMessage: json.error });
            }
        };
        request();
    }

    addCat() { this.catRestCall('http://localhost:8080/users/' + this.props.userId + '/categories', 'PUT'); }

    updateCat(id: number) { this.catRestCall('http://localhost:8080/users/' + this.props.userId + '/categories/' + id, 'PATCH'); }

    deleteCat(id: number) {
        const request = async () => {
            const response = await fetch('http://localhost:8080/users/' + this.props.userId + '/categories/' + id, {method: 'delete'});
            if (response.status === 202) {
                this.props.userId && this.getCategories(this.props.userId);
            } else {
                const json = await response.json();
                console.log(json);
            }
        };
        request();
    }

    closeModal() { this.setState({ show: false }); }

    async getGifts(userId: number) {
        const response = await fetch('http://localhost:8080/users/' + userId + '/gifts');
        const json = await response.json();
        if (response.status === 200) {
            this.setState({ gifts: json.gifts });
        } else {
            console.log(json.error);
        }
    };

    async getCategories(userId: number)  {
        const response = await fetch('http://localhost:8080/users/' + userId + '/categories');
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
                    <h5 key={index + value.name}>{value.name}
                    {' '}
                    <span style={{cursor: "pointer"}} onClick={() => this.openEditCat(value.name, value.id)}><Octicon icon={Pencil}/></span>
                    {' '}
                    <span style={{cursor: "pointer"}} onClick={() => this.deleteCat(value.id)}><Octicon icon={X}/></span>
                    </h5>);
                let filtered = this.state.gifts.filter(g => { return g.categoryId === value.id });
                if (filtered.length === 0) {
                    out.push(<>{cat}<p key={index + 'no_gift'}>No gift</p></>);
                }
                else {
                    let giftsOut = filtered.map((gift, gIndex) => {
                      if (index+'-'+gIndex === this.state.hoverId) {
                        return (
                            <div className="mycard" onMouseEnter={() => this.handleEnter(index, gIndex)} onMouseLeave={() => this.handleOut()}>
                                <div className="card-edit-close">
                                  <span className="text-right" style={{cursor: "pointer"}} onClick={() => this.openEditGift(gift.id, gift.name, gift.description, gift.price, gift.whereToBuy, gift.categoryId)}><Octicon icon={Pencil}/></span>{' '}
                                  <span style={{cursor: "pointer"}} onClick={() => this.deleteGift(gift.id)}><Octicon icon={X}/></span>
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
        const {button} = this.state;
        let modalBody = [];
        if (this.state.bodyRender !== null) {
            modalBody.push(this.state.bodyRender());
        }

        return (
            <div>
                {this.props.userId && <>
                    <Button color="link" onClick={this.openAddGift}>Add gift</Button>
                    <Button color="link" onClick={this.openAddCat}>Add Category</Button>
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

function mapStateToProps(state: AppState) : Props { return { userId: state.signin.userId }; }
export default connect(mapStateToProps)(MyWishList);
