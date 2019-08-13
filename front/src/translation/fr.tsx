import Itrans from './itrans';
import { Translations } from './itrans';

class Fr implements Itrans {
    getTranslation() : Translations {
        return {
          app: { signin: "Se connecter", signup: "S'inscrire", myList: "Ma Liste", myFriends: "Mes Amis", myEvents: "Mes Evenements", logout: "Se déconnecter" },
          home: { hello: "Bonjour" },
          mywishlist: {
            addGiftButton: "Ajouter un cadeau",
            addGiftModalTitle: "Ajouter un nouveau cadeau",
            addModalButton: "Ajouter",
            name: "Nom",
            nameErrorMessage: "Le Nom est obligatoire",
            description: "Description",
            price: "Prix",
            whereToBuy: "Où acheter",
            whereToBuyPlaceholder: "Lien Amazon, magasin local...",
            category: "Catégorie",
            updateGiftModalTitle: "Modifier ce cadeau",
            updateModalButton: "Modifier",
            addCategoryButton: "Ajouter une catégorie",
            addCategoryModalTitle: "Ajouter une nouvelle catégorie",
            updateCategoryModalTitle: "Modifier cette catégorie" }
        };
    }
}

export default Fr;
