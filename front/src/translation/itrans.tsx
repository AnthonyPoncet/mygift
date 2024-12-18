export interface AppMessage {
  signin: string;
  signup: string;
  myList: string;
  myFriends: string;
  myBuyList: string;
  logout: string;
  manageAccount: string;
  changeAccount: string;
}

export interface HomeMessage {
  year: string;
  of: string;
  christmas: string;
  motherDay: string;
  fatherDay: string;
  in: string;
  day: string;
}

export interface MyWishListMessage {
  addGiftButton: string;
  addGiftModalTitle: string;
  addModalButton: string;
  name: string;
  nameErrorMessage: string;
  description: string;
  price: string;
  whereToBuy: string;
  whereToBuyPlaceholder: string;
  category: string;
  sharedWith: string;
  image: string;
  updateGiftModalTitle: string;
  updateModalButton: string;
  addCategoryButton: string;
  addCategoryModalTitle: string;
  updateCategoryModalTitle: string;
  reorderButtonTitle: string;
  deleteGiftModalTitle: string;
  deleteModalButtonReceived: string;
  deleteModalButtonNotWanted: string;
  downloadPdf: string;
}

export interface MyFriendsMessage {
  addFriendButton: string;
  addFriendModalTitle: string;
  addModalButton: string;
  deleteFriendModalTitlePre: string;
  deleteFriendModalTitleSuffix: string;
  deleteModalButton: string;
  name: string;
  nameErrorMessage: string;
  requests: string;
  noPendingRequest: string;
  myRequests: string;
  allRequestsAccepted: string;
  friends: string;
}

export interface FriendWishListMessage {
  title: string;
  reservedByMe: string;
  deleteModalButtonReceived: string;
  deleteModalButtonNotWanted: string;
}

export interface Connection {
  signUpTitle: string;
  signUpButton: string;
  signInTitle: string;
  signInButton: string;
  username: string;
  password: string;
  image: string;
  emptyErrorMessage: string;
  newToMygift: string;
  createAnAccount: string;
  listDesc: string;
  friendsDesc: string;
  eventsDesc: string;
}

export interface MyBuyListMessage {
  title: string;
  received: string;
  not_wanted: string;
  ok: string;
}

export interface ManageAccountMessage {
  username: string;
  usernameEmptyErrorMessage: string;
  usernameTakenErrorMessage: string;
  dateOfBirth: string;
  dateOfBirthDefault: string;
  dateOfBirthErrorMessage: string;
  profilePicture: string;
  save: string;
}

export interface ImageEdition {
  rotateLeft: string;
  rotateRight: string;
}

export interface Translations {
  app: AppMessage;
  connection: Connection;
  home: HomeMessage;
  mywishlist: MyWishListMessage;
  myfriends: MyFriendsMessage;
  friendwishlist: FriendWishListMessage;
  myBuyList: MyBuyListMessage;
  manageAccount: ManageAccountMessage;
  imageEdition: ImageEdition;
}

interface Itrans {
  getTranslation(): Translations;
}

export default Itrans;
