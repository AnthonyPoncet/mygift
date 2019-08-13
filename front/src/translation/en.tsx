import Itrans from './itrans';
import { Translations } from './itrans';

class En implements Itrans {
    getTranslation() : Translations {
        return {
          app: { signin: "Sign In", signup: "Sign Up", myList: "My List", myFriends: "My Friends", myEvents: "My Events", logout: "Log Out" },
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
            updateGiftModalTitle: "Modify gift",
            updateModalButton: "Modify",
            addCategoryButton: "Add Category",
            addCategoryModalTitle: "Add a new category",
            updateCategoryModalTitle: "Modify category" }
        };
    }
}

export default En;
