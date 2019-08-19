import Itrans from './itrans';
import { Translations } from './itrans';

class Fr implements Itrans {
    getTranslation() : Translations {
        return {
          app: { signin: "Se connecter", signup: "S'inscrire", myList: "Ma Liste", myFriends: "Mes Amis", myEvents: "Mes Evenements", logout: "Se déconnecter" },
          connection: {
            signUpTitle: "S'inscrire à MyGift",
            signUpButton: "S'inscrire",
            signInTitle: "Se connecter à MyGift",
            signInButton: "Se connecter",
            username: "Username",
            password: "Mot de passe",
            image: "Photo de profil",
            emptyErrorMessage: "Username et Mot de passe ne peuvent pas être vide.",
            newToMygift: "Nouveau to MyGift ?",
            createAnAccount: "Créer un compte"
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
            updateGiftModalTitle: "Modifier ce cadeau",
            updateModalButton: "Modifier",
            addCategoryButton: "Ajouter une catégorie",
            addCategoryModalTitle: "Ajouter une nouvelle catégorie",
            updateCategoryModalTitle: "Modifier cette catégorie" },
          myfriends: {
            addFriendButton: "Ajouter un ami",
            addFriendModalTitle: "Ajouter un nouvel ami",
            addModalButton: "Ajouter",
            name: "Nom",
            nameErrorMessage: "Le Nom est obligatoire",
            requests: "Demande d'amis",
            noPendingRequest: "Vous n'avez pas de demande d'amis en attente.",
            myRequests: "Mes demandes d'amis",
            allRequestsAccepted: "Toutes vos demandes d'amis ont été accepté ou refusé.",
            friends: "Amis"
          },
          friendwishlist: { title: "Liste de " },
          myevents: {
            createEventButton: "Créer un évènement",
            createEventModalTitle: "Créer un nouveau évènement",
            createEventModalButton: "Créer",
            name: "Nom",
            nameErrorMessage: "Le Nom est obligatoire",
            description: "Description",
            endDate: "Date de fin",
            endDatePlaceholder: "Date de fin au format jj/mm/aaaa",
            endDateErrorMessage: "Date de fin est obligatoire et doit être au format jj/mm/aaaa",
            target: "Utilisateur concerné",
            targetErrorMessage: "Utilisateur est obligatoire",
            myEvents: "Mes Evènements",
            comingEvents: "Evènements à venir",
            pendingEvents: "Evènements en attente"
          },
          event: {
            addParticipantButton: "Ajouter un participant",
            addParticipantModalTitle: "Ajouter un nouveau participant",
            addParticipantModalButton: "Ajouter",
            name: "Nom",
            nameErrorMessage: "Le nom est obligatoire",
            description: "Description",
            creator: "Createur",
            endDate: "Date de fin",
            participantsTitle: "Participants",
            targetIsTitle: "Utilisateur concerné",
          }
        };
    }
}

export default Fr;
