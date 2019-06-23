import React from 'react';
import { Nav, Navbar, NavbarBrand, NavbarToggler, Collapse, NavItem, NavLink, Modal, ModalHeader, ModalBody, ModalFooter,
    Button, Input, Label, UncontrolledDropdown, DropdownToggle, DropdownMenu, DropdownItem,
    FormGroup } from "reactstrap";

class MyNavBar extends React.Component {
    constructor(props) {
        super(props);

        this.toggle = this.toggle.bind(this);
        this.toggleConnectModal = this.toggleConnectModal.bind(this);
        this.handleChange = this.handleChange.bind(this);
        this.connect = this.connect.bind(this);
        this.toggleCreateModal = this.toggleCreateModal.bind(this);
        this.create = this.create.bind(this);
        this.activateConnectModal = this.activateConnectModal.bind(this);
        this.activateCreateModal = this.activateCreateModal.bind(this);
        this.switchToCreate = this.switchToCreate.bind(this);
        this.state = {
            isOpen: false,
            displayConnect: true, connectModal: false, 'username': '', 'password': '', errorMessage: '', usernameInfo: {},
            createModal: false,
            displaySignUp: true };

    }

    toggle() {
        this.setState({isOpen: !this.state.isOpen});
    }

    toggleConnectModal() {
        this.setState(prevState => ({connectModal: !prevState.connectModal}));
    }
    toggleCreateModal() {
        this.setState(prevState => ({createModal: !prevState.createModal}));
    }

    activateConnectModal() {
        this.setState({connectModal: true, 'username': '', 'password': '', errorMessage: '', createModal: false})
    }

    activateCreateModal() {
        this.setState({connectModal: false, 'username': '', 'password': '', errorMessage: '', createModal: true})
    }

    switchToCreate() {
        this.setState({connectModal: false, errorMessage: '', createModal: true})
    }

    connect() {
        let errorMessage = '';
        const { validity } = this.state;
        if (this.state.username === '') {
            validity.username = 'has-danger';
            errorMessage = "Empty username";
        }

        if (this.state.password === '') {
            validity.password = 'has-danger';
            errorMessage += " empty password";
        }

        if (errorMessage === '') {
            const that = this;
            const request = async () => {
                const response = await fetch('http://localhost:8080/user/connect', {
                    method: 'post',
                    headers: {'Content-Type':'application/json'},
                    body: JSON.stringify({
                        "name": this.state.username,
                        "password": this.state.password
                    })
                });
                const json = await response.json();
                if (response.status === 200) {
                    that.setState({
                        displayConnect: false, displaySignUp: false, connectModal: false,
                        usernameInfo: { id: json.id, name: json.name}});
                    that.props.parentUpdate(json.id);
                } else {
                    that.setState({connectModal: true, usernameInfo: {}, errorMessage: json.error});
                }
            };
            request();
        } else {
            this.setState({connectModal: true, errorMessage: errorMessage})
        }
    }

    create() {
        let errorMessage = '';
        const { validity } = this.state;
        if (this.state.username === '') {
            validity.username = 'has-danger';
            errorMessage = "Empty username";
        }

        if (this.state.password === '') {
            validity.password = 'has-danger';
            errorMessage += " empty password";
        }

        if (errorMessage === '') {
            const that = this;
            const request = async () => {
                const response = await fetch('http://localhost:8080/users', {
                    method: 'put',
                    headers: {'Content-Type':'application/json'},
                    body: JSON.stringify({
                        "name": this.state.username,
                        "password": this.state.password
                    })
                });
                const json = await response.json();
                if (response.status === 201) {
                    that.setState({
                        displayConnect: false, displaySignUp: false, createModal: false,
                        usernameInfo: { id: json.id, name: json.name}});
                    that.props.parentUpdate(json.id);
                } else {
                    that.setState({createModal: true, usernameInfo: {}, errorMessage: json.error});
                }
            };
            request();
        } else {
            this.setState({createModal: true, errorMessage: errorMessage})
        }
    }

    handleChange = async (event) => {
        const { target } = event;
        const value = target.type === 'checkbox' ? target.checked : target.value;
        const { name } = target;
        await this.setState({
            [ name ]: value,
        });
    };

    disconnect() {
        this.setState({
            displayConnect: true, connectModal: false, 'username': '', 'password': '', errorMessage: '', usernameInfo: {},
            displaySignUp: true });
        this.props.parentUpdate(undefined);
    }

    render() {
        return(
            <div>
                <Navbar color="light" light expand="md">
                    <NavbarBrand href="/">MyGift</NavbarBrand>
                    <NavbarToggler onClick={this.toggle}/>
                    <Collapse isOpen={this.state.isOpen} navbar>
                        <Nav className="ml-auto" navbar>
                            {this.state.displayConnect ?
                                (<NavItem><NavLink onClick={this.activateConnectModal}>Connect</NavLink></NavItem>) :
                                (<UncontrolledDropdown nav inNavbar>
                                    <DropdownToggle nav caret>Welcome {this.state.usernameInfo.name}</DropdownToggle>
                                    <DropdownMenu right>
                                        <DropdownItem>Manage</DropdownItem>
                                        <DropdownItem divider />
                                        <DropdownItem onClick={() => this.disconnect()}>Disconnect</DropdownItem>
                                    </DropdownMenu>
                                </UncontrolledDropdown>)}
                            {this.state.displaySignUp && (<NavItem><NavLink onClick={this.activateCreateModal}>Sign Up</NavLink></NavItem>)}
                        </Nav>
                    </Collapse>
                </Navbar>

                <Modal isOpen={this.state.connectModal} toggle={this.toggleConnectModal} className={this.props.className}>
                    <ModalHeader toggle={this.toggleConnectModal}>Connect to MyGift</ModalHeader>
                    <ModalBody>
                        <FormGroup>
                            <Label>Username</Label>
                            <Input name="username" placeholder="username" value={this.state.username} onChange={(e) => this.handleChange(e)}/>
                        </FormGroup>
                        <FormGroup>
                            <Label>Password</Label>
                            <Input type="password" name="password" placeholder="********" value={this.state.password} onChange={(e) => this.handleChange(e)}/>
                        </FormGroup>
                        <Button color="primary" onClick={this.connect}>Connect</Button>{' '} {this.state.errorMessage}
                    </ModalBody>
                    <ModalFooter>
                        <Button color="secondary" onClick={this.switchToCreate}>Create an account</Button>{' '}
                    </ModalFooter>
                </Modal>

                <Modal isOpen={this.state.createModal} toggle={this.toggleCreateModal} className={this.props.className}>
                    <ModalHeader toggle={this.toggleCreateModal}>Create a new user</ModalHeader>
                    <ModalBody>
                        <FormGroup>
                            <Label>Username</Label>
                            <Input name="username" placeholder="username" value={this.state.username} onChange={(e) => this.handleChange(e)}/>
                        </FormGroup>
                        <FormGroup>
                            <Label>Password</Label>
                            <Input type="password" name="password" placeholder="********" value={this.state.password} onChange={(e) => this.handleChange(e)}/>
                        </FormGroup>
                        <Button color="primary" onClick={this.create}>Create</Button>{' '} {this.state.errorMessage}
                    </ModalBody>
                </Modal>
            </div>)
    }
}

class MyWishList extends React.Component {
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
        console.log("change " + name + " - " + value);

        await this.setState({ giftModalInput: { name: value, categoryId: this.state.giftModalInput.categoryId } });
    };

    addGift() {
        let errorMessage = '';
        if (this.state.name === '') {
            errorMessage = "Name is mandatory";
        }

        if (errorMessage === '') {
            console.log("add " + this.state.giftModalInput.categoryId);
            const request = async () => {
                const response = await fetch('http://localhost:8080/users/' + this.props.userId + '/gifts', {
                    method: 'put',
                    headers: {'Content-Type':'application/json'},
                    body: JSON.stringify({
                        "name": this.state.giftModalInput.name,
                        "categoryId": this.state.giftModalInput.categoryId,
                    })
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
        this.setState( { catModal: true, catModalTitle: "Add a new category", catButton: { text: 'Add', fun: this.addCat }, giftModalInput: { name: '' }, catErrorMessage: '' });
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
                out.push(<h2 key={index}>{value.name}</h2>);
                out.push(<Button key={index + '_edit_cat'} color="link" onClick={() => this.editCat(value.name, value.id)}>Edit</Button>);
                out.push(<Button key={index + '_delete_cat'} color="link" onClick={() => this.deleteCat(value.id)}>Delete</Button>);
                let filtered = this.state.gifts.filter(g => { return g.categoryId === value.id });
                out.push(filtered.map((gift, gIndex) => { return (
                        <li key={gIndex}>
                            {gift.name}{' '}
                            <Button color="link" onClick={() => this.editGift(gift.id, gift.name, gift.categoryId)}>Edit</Button>{' '}
                            <Button color="link" onClick={() => this.deleteGift(gift.id)}>Delete</Button>
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

class App extends React.Component {
    constructor(props) {
        super(props);

        this.updateUserId = this.updateUserId.bind(this);
        this.state = { userId: undefined }
    }

    updateUserId(id) {
        this.setState( { userId: id });
    }

    render() {
    return(
        <div>
            <MyNavBar parentUpdate={this.updateUserId}/>
            <MyWishList userId={this.state.userId}/>
        </div>)
  }
}

export default App;
