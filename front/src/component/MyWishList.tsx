import React from 'react';
import { Modal, ModalHeader, ModalBody, Button, Input, Label, FormGroup } from "reactstrap";

import { connect } from 'react-redux';
import { AppState } from '../redux/store';

interface Props {
  userId: number | null
}
interface State {
  gifts: any[],
  categories: any[],
  show: boolean, title: string, bodyRender: any, button: { text: string, fun: any }, inputs: any, errorMessage: string
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
          show: false, title: '', bodyRender: undefined, button: { text: '', fun: undefined }, inputs: { }, errorMessage: ''
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
      inputs: { name: '', categoryId: this.state.categories[0].id }, errorMessage: '' });
  }

  openEditGift(giftId: number, name: string, categoryId: number) {
      this.setState( { show: true, title: "Update gift", bodyRender: this.giftBodyRender, button: { text: 'Update', fun: () => this.updateGift(giftId) },
      inputs: { name: name, categoryId: categoryId }, errorMessage: '' });
  }

  handleChangeGift = async (event: any) => {
      const { target } = event;
      const value = target.type === 'checkbox' ? target.checked : target.value;
      await this.setState({ inputs: { name: value, categoryId: this.state.inputs.categoryId } });
  };

  updateGiftModalCategory(id: string) {
      this.setState({ inputs: { name: this.state.inputs.name, categoryId: id } });
  }

  giftBodyRender() {
    const options = this.state.categories && this.state.categories.map( (value, index) => {
        return <option key={index} value={value.id}>{value.name}</option>});

    return (<><FormGroup>
        <Label>Name</Label>
        <Input name="name" placeholder="name" value={this.state.inputs.name} onChange={(e) => this.handleChangeGift(e)}/>
    </FormGroup>
    <FormGroup>
        <Label>Category</Label>
        <Input type="select" name="categoryId" onChange={(e) => this.updateGiftModalCategory(e.target.value)}>
            {options}
        </Input>
    </FormGroup></>);
  }

  addGift() {
      const {inputs} = this.state;
      console.log(inputs);
      let errorMessage = '';
      if (inputs.name === undefined || inputs.name === '') {
          errorMessage = "Name is mandatory";
      }
      if (inputs.categoryId === undefined) {
          errorMessage = "Category is mandatory";
      }

      if (errorMessage === '') {
          const request = async () => {
              const response = await fetch('http://localhost:8080/users/' + this.props.userId + '/gifts', {
                  method: 'put',
                  headers: {'Content-Type':'application/json'},
                  body: JSON.stringify({"name": inputs.name, "categoryId": inputs.categoryId})
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
      } else {
          this.setState({ show: true, errorMessage: errorMessage })
      }
  }

  updateGift(id: number) {
      const {inputs} = this.state;
      console.log(inputs);

      let errorMessage = '';
      if (inputs.name === undefined || inputs.name === '') {
          errorMessage = "Name is mandatory";
      }
      if (inputs.categoryId === undefined) {
          errorMessage = "Category is mandatory";
      }

      if (errorMessage === '') {
          const that = this;
          const request = async () => {
              const response = await fetch('http://localhost:8080/users/' + this.props.userId + '/gifts/' + id, {
                  method: 'PATCH',
                  headers: {'Content-Type':'application/json'},
                  body: JSON.stringify({"name": inputs.name, "categoryId": inputs.categoryId})
              });
              if (response.status === 200) {
                  that.setState({ show: false });
                  this.props.userId && this.getGifts(this.props.userId);
              } else {
                  const json = await response.json();
                  that.setState({ show: true, errorMessage: json.error });
              }
          };
          request();
      } else {
          this.setState({ show: true, errorMessage: errorMessage })
      }
  }

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
        inputs: { name: '' }, errorMessage: '' });
  }

  openEditCat(name: string, categoryId: number) {
      this.setState( { show: true, title: "Update category", bodyRender: this.catBodyRender, button: { text: 'Update', fun: () => this.updateCat(categoryId) },
          inputs: { name: name }, errorMessage: '' });
  }

  handleChangeCat = async (event: any) => {
      const { target } = event;
      const value = target.type === 'checkbox' ? target.checked : target.value;
      const { name } = target;
      await this.setState({ inputs: {[ name ]: value, }});
  };

  catBodyRender() {
      return (<FormGroup>
          <Label>Name</Label>
          <Input name="name" placeholder="name" value={this.state.inputs.name} onChange={(e) => this.handleChangeCat(e)}/>
      </FormGroup>);
    }

  addCat() {
      let errorMessage = '';
      const {name} = this.state.inputs;
      if (name === undefined || name === '') {
          errorMessage = "Name is mandatory";
      }

      if (errorMessage === '') {
          const request = async () => {
              const response = await fetch('http://localhost:8080/users/' + this.props.userId + '/categories', {
                  method: 'put',
                  headers: {'Content-Type':'application/json'},
                  body: JSON.stringify({
                      "name": name
                  })
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
      } else {
          this.setState({ show: true, errorMessage: errorMessage })
      }
  }

  updateCat(id: number) {
      let errorMessage = '';
      const {name} = this.state.inputs;
      if (name === undefined || name === '') {
          errorMessage = "Name is mandatory";
      }

      if (errorMessage === '') {
          const request = async () => {
              const response = await fetch('http://localhost:8080/users/' + this.props.userId + '/categories/' + id, {
                  method: 'PATCH',
                  headers: {'Content-Type':'application/json'},
                  body: JSON.stringify({
                      "name": name
                  })
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
      } else {
          this.setState({ show: true, errorMessage: errorMessage })
      }
  }

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

  closeModal() {
      this.setState({ show: false });
  }

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

  renderGifts() {
      if (this.state.categories) {
          let out = [];
          for (const [index, value] of this.state.categories.entries()) {
              out.push(<div key={index + 'div'}>
                  <h3 key={index + value.name}>{value.name}
                  <img key={index + '_edit_cat_img'} src="edit.png" alt="Edit" width="25" height="21" onClick={() => this.openEditCat(value.name, value.id)}/>
                  {' '}
                  <img key={index + '_del_cat_img'} src="cross.png" alt="Delete" width="21" height="21" onClick={() => this.deleteCat(value.id)}/>
                  </h3>
              </div>);
              let filtered = this.state.gifts.filter(g => { return g.categoryId === value.id });
              if (filtered.length === 0)
                out.push(<p key={index + 'no_gift'}>No gift</p>);
              else
                out.push(filtered.map((gift, gIndex) => { return (
                      <li key={index + gIndex + gift.name }>
                          {gift.name}
                          {' '}
                          <img key={index + gIndex + '_edit_img'} className="d-inline" src="edit.png" alt="Edit" width="25" height="21" onClick={() => this.openEditGift(gift.id, gift.name, gift.categoryId)}/>
                          {' '}
                          <img key={index + gIndex + '_del_img'} className="d-inline" src="cross.png" alt="Delete" width="21" height="21" onClick={() => this.deleteGift(gift.id)}/>
                      </li>);}));
          }

          return (
              <div>{out}</div>
          )
      }
  }

  render() {
    const {button} = this.state;
    let modalBody = [];
    if (this.state.bodyRender !== undefined) {
      modalBody.push(this.state.bodyRender());
    }

      return (
    <div>
        {this.props.userId && <>
          <Button color="link" onClick={this.openAddGift}>Add gift</Button>
          <Button color="link" onClick={this.openAddCat}>Add Category</Button>
          {this.renderGifts()}
          </>
        }

        <Modal isOpen={this.state.show} toggle={this.closeModal}>
            <ModalHeader toggle={this.closeModal}>{this.state.title}</ModalHeader>
            <ModalBody>
                {modalBody}
                <Button color="primary" onClick={button.fun}>{button.text}</Button>{' '} {this.state.errorMessage}
            </ModalBody>
        </Modal>
    </div>
  );
  }
}

function mapStateToProps(state: AppState) : Props { return { userId: state.signin.userId }; }
export default connect(mapStateToProps)(MyWishList);
