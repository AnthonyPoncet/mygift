export interface AppMessage {
    signin: string,
    signup: string,
    myList: string,
    myFriends: string,
    myEvents: string,
    logout: string
}

export interface HomeMessage {
    hello: string
}

export interface MyWishListMessage {
    addGiftButton: string,
    addGiftModalTitle: string,
    addModalButton: string,
    name: string,
    nameErrorMessage: string,
    description: string,
    price: string,
    whereToBuy: string,
    whereToBuyPlaceholder: string,
    category: string,
    updateGiftModalTitle: string,
    updateModalButton: string,
    addCategoryButton: string,
    addCategoryModalTitle: string,
    updateCategoryModalTitle: string
}

export interface MyFriendsMessage {
  addFriendButton: string,
  addFriendModalTitle: string,
  addModalButton: string,
  name: string,
  nameErrorMessage: string,
  requests: string,
  noPendingRequest: string,
  myRequests: string,
  allRequestsAccepted: string,
  friends: string
}

export interface FriendWishListMessage {
  title: string
}

export interface Connection {
  signUpTitle: string,
  signUpButton: string,
  signInTitle: string,
  signInButton: string,
  username: string,
  password: string,
  emptyErrorMessage: string,
  newToMygift: string,
  createAnAccount: string
}

export interface Translations {
    app: AppMessage,
    connection: Connection,
    home: HomeMessage,
    mywishlist: MyWishListMessage,
    myfriends: MyFriendsMessage,
    friendwishlist: FriendWishListMessage
}

interface Itrans {
    getTranslation() : Translations;
}

export default Itrans;
