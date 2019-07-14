import React from 'react';
import { Modal, ModalHeader, ModalBody, Button, Input, Label, FormGroup } from "reactstrap";

import { connect } from 'react-redux';

class HomePage extends React.Component {
  constructor(props) {
      super(props);

      this.openGiftModal = this.openGiftModal.bind(this);
      this.closeGiftModal = this.closeGiftModal.bind(this);
      this.addGift = this.addGift.bind(this);
      this.updateGift = this.updateGift.bind(this);

      this.openCatModal = this.openCatModal.bind(this);
      this.closeCatModal = this.closeCatModal.bind(this);
      this.addCat = this.addCat.bind(this);
      this.updateCat = this.updateCat.bind(this);

      this.state = {
          gifts: [], categories: [],
          giftModal: false, giftModalTitle: '', giftButton: { text: '', fun: undefined }, giftModalInput: { name: '', categoryId: '' }, giftErrorMessage: '',
          catModal: false, catModalTitle: '', catButton: { text: '', fun: undefined }, catModalInput: { name: '', categoryId: '' }, catErrorMessage: '',
      }
  }

  componentDidMount() {
      if (this.props.userId) {
          let getGifts = async () => this.getGifts();
          let getCategories = async () => this.getCategories();
          getGifts();
          getCategories();
      }
  }

  componentWillReceiveProps(nextProps, nextContext) {
      if (nextProps.userId) {
          let getGifts = async () => this.getGifts(nextProps.userId);
          let getCategories = async () => this.getCategories(nextProps.userId);
          getGifts();
          getCategories();
      }
  }

  openGiftModal() {
      let refreshCategories = async () => this.getCategories(this.props.userId);
      refreshCategories();
      this.setState( { giftModal: true, giftModalTitle: "Add a new gift", giftButton: { text: 'Add', fun: this.addGift }, giftModalInput: { name: '', categoryId: '' }, giftErrorMessage: '' });
  }

  closeGiftModal() {
      this.setState({ giftModal: false });
  }

  handleChange = async (event) => {
      const { target } = event;
      const value = target.type === 'checkbox' ? target.checked : target.value;
      const { name } = target;

      await this.setState({ giftModalInput: { name: value, categoryId: this.state.giftModalInput.categoryId } });
  };

  addGift() {
      let errorMessage = '';
      if (this.state.name === '') {
          errorMessage = "Name is mandatory";
      }

      if (errorMessage === '') {
          console.log("add " + this.state.giftModalInput.categoryId);
          let body = undefined;
          if (this.state.giftModalInput.categoryId) {
              body = JSON.stringify({"name": this.state.giftModalInput.name, "categoryId": this.state.giftModalInput.categoryId});
          } else {
              body = JSON.stringify({"name": this.state.giftModalInput.name});
          }
          const request = async () => {
              const response = await fetch('http://localhost:8080/users/' + this.props.userId + '/gifts', {
                  method: 'put',
                  headers: {'Content-Type':'application/json'},
                  body: body
              });
              if (response.status === 200) {
                  this.setState({ giftModal: false });
                  this.getGifts(this.props.userId);
              } else {
                  const json = await response.json();
                  this.setState({ giftModal: true, giftErrorMessage: json.error });
              }
          };
          request();
      } else {
          this.setState({ giftModal: true, giftErrorMessage: errorMessage })
      }
  }

  updateGift(id) {
      let errorMessage = '';
      if (this.state.name === '') {
          errorMessage = "Name is mandatory";
      }

      if (errorMessage === '') {
          const that = this;
          const request = async () => {
              const response = await fetch('http://localhost:8080/users/' + this.props.userId + '/gifts/' + id, {
                  method: 'PATCH',
                  headers: {'Content-Type':'application/json'},
                  body: JSON.stringify({
                      "name": this.state.giftModalInput.name,
                      "categoryId:": this.state.giftModalInput.categoryId
                  })
              });
              if (response.status === 200) {
                  that.setState({ giftModal: false });
                  this.getGifts(this.props.userId);
              } else {
                  const json = await response.json();
                  that.setState({ giftModal: true, giftErrorMessage: json.error });
              }
          };
          request();
      } else {
          this.setState({ giftModal: true, giftErrorMessage: errorMessage })
      }
  }

  editGift(giftId, name, categoryId) {
      let refreshCategories = async () => this.getCategories(this.props.userId);
      refreshCategories();
      this.setState( { giftModal: true, giftModalTitle: "Update gift", giftButton: { text: 'Update', fun: () => this.updateGift(giftId) }, giftModalInput: { name: name, categoryId: categoryId }, giftErrorMessage: '' });
  }

  deleteGift(id) {
      const request = async () => {
          const response = await fetch('http://localhost:8080/users/' + this.props.userId + '/gifts/' + id, {method: 'delete'});
          if (response.status === 202) {
              this.getGifts(this.props.userId);
          } else {
              const json = await response.json();
              console.log(json);
          }
      };
      request();
  }


  openCatModal() {
      this.setState( { catModal: true, catModalTitle: "Add a new category", catButton: { text: 'Add', fun: this.addCat }, catModalInput: { name: '' }, catErrorMessage: '' });
  }

  closeCatModal() {
      this.setState({ catModal: false });
  }

  handleChangeCat = async (event) => {
      const { target } = event;
      const value = target.type === 'checkbox' ? target.checked : target.value;
      const { name } = target;
      await this.setState({ catModalInput: {
              [ name ]: value,
          }});
  };

  addCat() {
      let errorMessage = '';
      if (this.state.name === '') {
          errorMessage = "Name is mandatory";
      }

      if (errorMessage === '') {
          const request = async () => {
              const response = await fetch('http://localhost:8080/users/' + this.props.userId + '/categories', {
                  method: 'put',
                  headers: {'Content-Type':'application/json'},
                  body: JSON.stringify({
                      "name": this.state.catModalInput.name
                  })
              });
              if (response.status === 200) {
                  this.setState({ catModal: false });
                  this.getCategories(this.props.userId);
              } else {
                  const json = await response.json();
                  this.setState({ catModal: true, catErrorMessage: json.error });
              }
          };
          request();
      } else {
          this.setState({ catModal: true, catErrorMessage: errorMessage })
      }
  }

  updateCat(id) {
      let errorMessage = '';
      if (this.state.name === '') {
          errorMessage = "Name is mandatory";
      }

      if (errorMessage === '') {
          const request = async () => {
              const response = await fetch('http://localhost:8080/users/' + this.props.userId + '/categories/' + id, {
                  method: 'PATCH',
                  headers: {'Content-Type':'application/json'},
                  body: JSON.stringify({
                      "name": this.state.catModalInput.name
                  })
              });
              if (response.status === 200) {
                  this.setState({ catModal: false });
                  this.getCategories(this.props.userId);
              } else {
                  const json = await response.json();
                  this.setState({ catModal: true, catErrorMessage: json.error });
              }
          };
          request();
      } else {
          this.setState({ catModal: true, catErrorMessage: errorMessage })
      }
  }

  editCat(name, categoryId) {
      this.setState( { catModal: true, catModalTitle: "Update category", catButton: { text: 'Update', fun: () => this.updateCat(categoryId) },
          catModalInput: { name: name }, catErrorMessage: '' });
  }

  deleteCat(id) {
      const request = async () => {
          const response = await fetch('http://localhost:8080/users/' + this.props.userId + '/categories/' + id, {method: 'delete'});
          if (response.status === 202) {
              this.getCategories(this.props.userId);
          } else {
              const json = await response.json();
              console.log(json);
          }
      };
      request();
  }

  async getGifts(userId) {
      const response = await fetch('http://localhost:8080/users/' + userId + '/gifts');
      const json = await response.json();
      if (response.status === 200) {
          this.setState({ gifts: json.gifts });
      } else {
          console.log(json.error);
      }
  };

  async getCategories(userId)  {
      const response = await fetch('http://localhost:8080/users/' + userId + '/categories');
      const json = await response.json();
      if (response.status === 200) {
          this.setState({ categories: json.categories } );
      } else {
          console.log(json.error);
      }
  };

  updateGiftModalCategory(id) {
      this.setState({ giftModalInput: { name: this.state.giftModalInput.name, categoryId: id } });
  }

  renderGifts() {
      if (this.state.categories) {
          let out = [];
          for (const [index, value] of this.state.categories.entries()) {
              out.push(<div>
                  <h2 key={index}>{value.name}
                  <img key={index + '_edit_cat_img'} src="edit.png" alt="Edit" width="25" height="21" onClick={() => this.editCat(value.name, value.id)}/>
                  {' '}
                  <img key={index + '_del_cat_img'} src="cross.png" alt="Delete" width="21" height="21" onClick={() => this.deleteCat(value.id)}/>
                  </h2>
              </div>);
              let filtered = this.state.gifts.filter(g => { return g.categoryId === value.id });
              out.push(filtered.map((gift, gIndex) => { return (
                      <li key={gIndex}>
                          {gift.name}
                          {' '}
                          <img key={index + '_edit_img'} className="d-inline" src="edit.png" alt="Edit" width="25" height="21" onClick={() => this.editGift(gift.id, gift.name, gift.categoryId)}/>
                          {' '}
                          <img key={index + '_del_img'} className="d-inline" src="cross.png" alt="Delete" width="21" height="21" onClick={() => this.deleteGift(gift.id)}/>
                      </li>);}));
          }

          return (
              <div>{out}</div>
          )
      }
  }

  render() {
      return (
    <div>
        {this.props.userId && <Button color="link" onClick={this.openGiftModal}>Add gift</Button>}
        {this.props.userId && <Button color="link" onClick={this.openCatModal}>Add Category</Button>}
        {this.props.userId && this.renderGifts()}

        <Modal isOpen={this.state.giftModal} toggle={this.closeGiftModal} className={this.props.className}>
            <ModalHeader toggle={this.closeGiftModal}>{this.state.giftModalTitle}</ModalHeader>
            <ModalBody>
                <FormGroup>
                    <Label>Name</Label>
                    <Input name="name" placeholder="name" value={this.state.giftModalInput.name} onChange={(e) => this.handleChange(e)}/>
                </FormGroup>
                <FormGroup>
                    <Label>Category</Label>
                    <Input type="select" name="categoryId" onChange={(e) => this.updateGiftModalCategory(e.target.value)}>
                        {this.state.categories && this.state.categories.map( (value, index) => {
                            return <option key={index} value={value.id}>{value.name}</option>})}
                    </Input>
                </FormGroup>
                <Button color="primary" onClick={this.state.giftButton.fun}>{this.state.giftButton.text}</Button>{' '} {this.state.giftErrorMessage}
            </ModalBody>
        </Modal>

        <Modal isOpen={this.state.catModal} toggle={this.closeCatModal} className={this.props.className}>
            <ModalHeader toggle={this.closeCatModal}>{this.state.catModalTitle}</ModalHeader>
            <ModalBody>
                <FormGroup>
                    <Label>Name</Label>
                    <Input name="name" placeholder="name" value={this.state.catModalInput.name} onChange={(e) => this.handleChangeCat(e)}/>
                </FormGroup>
                <Button color="primary" onClick={this.state.catButton.fun}>{this.state.catButton.text}</Button>{' '} {this.state.catErrorMessage}
            </ModalBody>
        </Modal>
    </div>
  );
  }
}

function mapStateToProps(state) {
    return { userId: state.signin.userId };
}
export default connect(mapStateToProps)(HomePage);
