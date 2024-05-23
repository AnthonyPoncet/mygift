import Itrans from "./itrans";
import { Translations } from "./itrans";

class Fr implements Itrans {
  getTranslation(): Translations {
    return {
      app: {
        signin: "Se connecter",
        signup: "S'inscrire",
        myList: "Ma Liste",
        myFriends: "Mes Amis",
        myEvents: "Mes Evenements",
        myBuyList: "Ma Liste D'achat",
        logout: "Se déconnecter",
        manageAccount: "Modifier le compte",
        changeAccount: "Changer de compte",
      },
      connection: {
        signUpTitle: "S'inscrire à MyGift",
        signUpButton: "S'inscrire",
        signInTitle: "Se connecter à MyGift",
        signInButton: "Se connecter",
        username: "Pseudo",
        password: "Mot de passe",
        image: "Photo de profil",
        emptyErrorMessage: "Pseudo et Mot de passe ne peuvent pas être vide.",
        newToMygift: "Nouveau to MyGift ?",
        createAnAccount: "Créer un compte",
        listDesc: "Créer votre liste de cadeaux",
        friendsDesc: "Trouver ce que veulent vos amis et acheter le bon cadeau",
        eventsDesc: "Créer des events anniversaire, noël et autre",
      },
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
        sharedWith: "En commun avec",
        image: "Photo du cadeau",
        updateGiftModalTitle: "Modifier ce cadeau",
        updateModalButton: "Modifier",
        addCategoryButton: "Ajouter une catégorie",
        addCategoryModalTitle: "Ajouter une nouvelle catégorie",
        updateCategoryModalTitle: "Modifier cette catégorie",
        reorderButtonTitle: "Réorganiser",
        deleteGiftModalTitle: "Supprimer ce cadeau",
        deleteModalButtonReceived: "Je l'ai reçu",
        deleteModalButtonNotWanted: "Je n'en veux plus",
      },
      myfriends: {
        addFriendButton: "Ajouter un ami",
        addFriendModalTitle: "Ajouter un nouvel ami",
        addModalButton: "Ajouter",
        deleteFriendModalTitlePre: "Voulez-vous vraiment supprimer ",
        deleteFriendModalTitleSuffix: " de vos amis?",
        deleteModalButton: "Supprimer",
        name: "Pseudo",
        nameErrorMessage: "Le Pseudo est obligatoire",
        requests: "Demande d'amis",
        noPendingRequest: "Vous n'avez pas de demande d'amis en attente.",
        myRequests: "Mes demandes d'amis",
        allRequestsAccepted:
          "Toutes vos demandes d'amis ont été accepté ou refusé.",
        friends: "Amis",
      },
      friendwishlist: {
        title: "Liste de ",
        reservedByMe: "Je réserve",
        deleteModalButtonReceived: "Cadeau reçu",
        deleteModalButtonNotWanted: "Cadeau à supprimer",
      },
      myBuyList: {
        title: "Ma liste d'achat",
        received: "Reçu",
        not_wanted: "Supprimé",
        ok: "Supprimer de ma liste",
      },
      manageAccount: {
        username: "Pseudo",
        can_not_be_changed: "Ne peut pas être changé",
        profile_picture: "Photo de profil",
        save: "Sauvegarder",
      },
      imageEdition: {
        rotateLeft: "Tourner à gauche",
        rotateRight: "Tourner à droite",
      },
    };
  }
}

export default Fr;
