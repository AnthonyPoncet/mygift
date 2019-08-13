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

export interface Translations {
    app: AppMessage,
    home: HomeMessage,
    mywishlist: MyWishListMessage
}

interface Itrans {
    getTranslation() : Translations;
}

export default Itrans;
