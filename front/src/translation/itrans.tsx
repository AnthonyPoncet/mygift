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
    image: string,
    emptyErrorMessage: string,
    newToMygift: string,
    createAnAccount: string
}

export interface MyEventsMessage {
    createEventButton: string,
    createEventModalTitle: string,
    createEventModalButton: string,
    name: string,
    nameErrorMessage: string,
    description: string,
    endDate: string,
    endDatePlaceholder: string,
    endDateErrorMessage: string,
    target: string,
    targetErrorMessage: string,
    myEvents: string,
    comingEvents: string,
    pendingEvents: string
}

export interface EventMessage {
    addParticipantButton: string,
    addParticipantModalTitle: string,
    addParticipantModalButton: string,
    name: string,
    nameErrorMessage: string,
    description: string,
    creator: string,
    endDate: string,
    participantsTitle: string,
    targetIsTitle: string,
}

export interface Translations {
    app: AppMessage,
    connection: Connection,
    home: HomeMessage,
    mywishlist: MyWishListMessage,
    myfriends: MyFriendsMessage,
    friendwishlist: FriendWishListMessage,
    myevents: MyEventsMessage,
    event: EventMessage
}

interface Itrans {
    getTranslation() : Translations;
}

export default Itrans;
