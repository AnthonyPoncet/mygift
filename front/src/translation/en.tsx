import Itrans from "./itrans";
import { Translations } from "./itrans";

class En implements Itrans {
  getTranslation(): Translations {
    return {
      app: {
        signin: "Sign In",
        signup: "Sign Up",
        myList: "My List",
        myFriends: "My Friends",
        myBuyList: "My Buy List",
        logout: "Log Out",
        manageAccount: "Manage account",
        changeAccount: "Change account",
      },
      connection: {
        signUpTitle: "Sign up to MyGift",
        signUpButton: "Sign up",
        signInTitle: "Sign in to MyGift",
        signInButton: "Sign in",
        username: "Username",
        password: "Password",
        image: "Profile picture",
        emptyErrorMessage: "Username and Password could not be empty.",
        newToMygift: "New to MyGift?",
        createAnAccount: "Create an account",
        listDesc: "Create your gift list",
        friendsDesc: "Find what your friends want and bought the right gift",
        eventsDesc: "Create birthday, christmas or other events",
      },
      home: {
        year: "year",
        of: "of",
        christmas: "Christmas",
        motherDay: "Mothers day",
        fatherDay: "Fathers day",
        in: "In",
        day: "day",
        hello: "Hello",
        days_before_christmas: "days before Christmas",
        day_before_christmas: "day before Christmas"
      },
      mywishlist: {
        addGiftButton: "Add Gift",
        addGiftModalTitle: "Add a new gift",
        addModalButton: "Add",
        name: "Name",
        nameErrorMessage: "Name is mandatory",
        description: "Description",
        price: "Price",
        whereToBuy: "Where to buy",
        whereToBuyPlaceholder: "Amazon link, local shop...",
        category: "Category",
        sharedWith: "Shared with",
        image: "Picture",
        updateGiftModalTitle: "Modify gift",
        updateModalButton: "Modify",
        addCategoryButton: "Add Category",
        addCategoryModalTitle: "Add a new category",
        updateCategoryModalTitle: "Modify category",
        reorderButtonTitle: "Reorder",
        deleteGiftModalTitle: "Delete gift",
        deleteModalButtonReceived: "I have received it",
        deleteModalButtonNotWanted: "I don't want it anymore",
        downloadPdf: "Download as PDF"
      },
      myfriends: {
        addFriendButton: "Add friend",
        addFriendModalTitle: "Add a new friend",
        addModalButton: "Add",
        deleteFriendModalTitlePre: "Do you really want to remove ",
        deleteFriendModalTitleSuffix: " of your mailing list?",
        deleteModalButton: "Delete",
        name: "Username",
        nameErrorMessage: "Username is mandatory",
        requests: "Requests",
        noPendingRequest: "No pending requests.",
        myRequests: "My requests",
        allRequestsAccepted: "All requests has been accepted or declined.",
        friends: "Friends",
      },
      friendwishlist: {
        title: "Wish list of ",
        reservedByMe: "I want to reserve",
        deleteModalButtonReceived: "Gift received",
        deleteModalButtonNotWanted: "Gift to delete",
      },
      myBuyList: {
        title: "My Buy List",
        received: "Received",
        not_wanted: "Deleted",
        ok: "Delete from my list",
      },
      manageAccount: {
        username: "Username",
        usernameEmptyErrorMessage: "Username cannot be empty",
        usernameTakenErrorMessage: "Username already taken",
        dateOfBirth: "Date of birth",
        dateOfBirthDefault: "dd/mm/yyyy",
        dateOfBirthErrorMessage: "Date of birth invalid. Format: dd/mm/yyyy.",
        profilePicture: "Profile picture",
        save: "Save",
      },
      imageEdition: {
        rotateLeft: "Rotate left",
        rotateRight: "Rotate right",
      },
    };
  }
}

export default En;
