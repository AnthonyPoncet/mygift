import Itrans from './itrans';
import { Translations } from './itrans';

class En implements Itrans {
    getTranslation() : Translations {
        return {
          app: {
            signin: "Sign In",
            signup: "Sign Up",
            myList: "My List",
            myFriends: "My Friends",
            myEvents: "My Events",
            myBuyList: "My Buy List",
            logout: "Log Out",
            manageAccount: "Manage account"
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
            eventsDesc: "Create birthday, christmas or other events"
          },
          home: { hello: "Hello" },
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
            image: "Picture",
            updateGiftModalTitle: "Modify gift",
            updateModalButton: "Modify",
            addCategoryButton: "Add Category",
            addCategoryModalTitle: "Add a new category",
            updateCategoryModalTitle: "Modify category" },
          myfriends: {
            addFriendButton: "Add friend",
            addFriendModalTitle: "Add a new friend",
            addModalButton: "Add",
            name: "Username",
            nameErrorMessage: "Username is mandatory",
            requests: "Requests",
            noPendingRequest: "No pending requests.",
            myRequests: "My requests",
            allRequestsAccepted: "All requests has been accepted or declined.",
            friends: "Friends"
          },
          friendwishlist: { title: "Wish list of " },
          myevents: {
            createEventButton: "Create event",
            createEventModalTitle: "Create a new event",
            createEventModalButton: "Create",
            name: "Name",
            nameErrorMessage: "Name is mandatory",
            description: "Description",
            endDate: "End date",
            endDatePlaceholder: "End date in format dd/mm/yyyy",
            endDateErrorMessage: "End date is mandatory and its format is dd/mm/yyyy",
            target: "Target user",
            targetErrorMessage: "Target is mandatory",
            myEvents: "My Events",
            comingEvents: "Coming Events",
            pendingEvents: "Pending Events"
          },
          event: {
              addParticipantModalTitle: "Add a new participant",
              addParticipantModalButton: "Add",
              name: "Name",
              nameErrorMessage: "Name is mandatory",
              description: "Description",
              creator: "Organised by",
              participantsTitle: "Participants",
              targetIsTitle: "Event for",
          },
          myBuyList: { title: "My Buy List" },
          manageAccount: {}
        };
    }
}

export default En;
