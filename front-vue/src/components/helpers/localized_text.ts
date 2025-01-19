import { EventKind } from "./common_json";

export interface Messages {
  global__username: string;
  global__password: string;
  global__dateOfBirth: string;
  global__name: string;
  global__description: string;
  global__price: string;
  global__whereToBuy: string;
  global__category: string;
  global__picture: string;
  global__share_with: string;
  global__form_validation_start: string;
  global__add: string;
  global__update: string;
  global__delete: string;
  global__accept: string;
  global__cancel: string;
  global__reserve: string;

  nav_bar__signin: string;
  nav_bar__signup: string;
  nav_bar__myList: string;
  nav_bar__myFriends: string;
  nav_bar__myBuyList: string;
  nav_bar__logout: string;
  nav_bar__manageAccount: string;
  nav_bar__changeAccount: string;

  signup__title: string;
  signup__button: string;
  signin__title: string;
  signin__button: string;
  signin__newAccount: string;
  signin__creatAccount: string;

  mywishlist__addGiftButton: string;
  mywishlist__addCategoryButton: string;
  mywishlist__reorderButton: string;
  mywishlist__downloadPdfButton: string;

  category_modal__addCategoryTitle: string;
  category_modal__updateCategoryTitle: string;

  gift_modal__addGiftTitle: string;
  gift_modal__updateGiftTitle: string;

  delete_modal__pre_text: string;
  delete_modal__pre_text_category: string;
  delete_modal__pre_text_gift: string;
  delete_modal__category_hint: string;
  delete_modal__post_text_friend: string;
  delete_modal__delete_gift_i_got_it: string;
  delete_modal__delete_gift_dont_want_it: string;

  myfriends__add_friend: string;
  myfriends__friend_list: string;
  myfriends__friend_requests: string;
  myfriends__my_friend_requests: string;

  friend_modal__title: string;

  friendlist__title: string;
  friendlist__addGiftButton: string;

  home__years_old: string;
  home__special_event: Record<EventKind, string>;
}

export const fr: Messages = {
  global__username: "Pseudo",
  global__password: "Mot de passe",
  global__dateOfBirth: "Date de naissance",
  global__name: "Nom",
  global__description: "Description",
  global__price: "Prix",
  global__whereToBuy: "Où acheter",
  global__category: "Catégorie",
  global__picture: "Photo",
  global__share_with: "En commun avec",
  global__form_validation_start: "Merci de renseigner le ",
  global__add: "Ajouter",
  global__update: "Modifier",
  global__delete: "Supprimer",
  global__accept: "Accepter",
  global__cancel: "Annuler",
  global__reserve: "Reserver",

  nav_bar__signin: "Se connecter",
  nav_bar__signup: "S'inscrire",
  nav_bar__myList: "Ma liste",
  nav_bar__myFriends: "Mes amis",
  nav_bar__myBuyList: "Ma liste d'achat",
  nav_bar__logout: "Se déconnecter",
  nav_bar__manageAccount: "Modifier le compte",
  nav_bar__changeAccount: "Changer de compte",

  signup__title: "S'inscrire à MyGift",
  signup__button: "S'inscrire",
  signin__title: "Se connecter à MyGift",
  signin__button: "Se connecter",
  signin__newAccount: "Nouveau sur MyGift?",
  signin__creatAccount: "Créer un compte",

  mywishlist__addGiftButton: "Ajouter un cadeau",
  mywishlist__addCategoryButton: "Ajouter une catégorie",
  mywishlist__reorderButton: "Réorganiser",
  mywishlist__downloadPdfButton: "Télécharger en PDF",

  category_modal__addCategoryTitle: "Ajouter une nouvelle catégorie",
  category_modal__updateCategoryTitle: "Modifier cette catégorie",

  gift_modal__addGiftTitle: "Ajouter un nouveau cadeau",
  gift_modal__updateGiftTitle: "Modifier ce cadeau",

  delete_modal__pre_text: "Voulez-vous vraiment supprimer ",
  delete_modal__pre_text_category: "la catégorie ",
  delete_modal__pre_text_gift: "le cadeau ",
  delete_modal__post_text_friend: "de vos amis?",
  delete_modal__category_hint: "Ceci supprimera aussi tous les cadeaux de cette catégorie.",
  delete_modal__delete_gift_i_got_it: "Je l'ai reçu",
  delete_modal__delete_gift_dont_want_it: "Je n'en veux plus",

  myfriends__add_friend: "Ajouter un ami",
  myfriends__friend_list: "Amis",
  myfriends__friend_requests: "Demande d'amis",
  myfriends__my_friend_requests: "Mes demandes d'amis",

  friend_modal__title: "Ajouter un nouvel ami",

  friendlist__title: "Liste de ",
  friendlist__addGiftButton: "Ajouter un cadeau secret",

  home__years_old: "ans",
  home__special_event: {
    [EventKind.BIRTHDAY]: "",
    [EventKind.CHRISTMAS]: "Noël",
    [EventKind.MOTHER_DAY]: "Fête des mères",
    [EventKind.FATHER_DAY]: "Fête des pères",
  },
};

export const en: Messages = {
  global__username: "Username",
  global__password: "Password",
  global__dateOfBirth: "Date of birth",
  global__name: "Name",
  global__description: "Description",
  global__price: "Price",
  global__whereToBuy: "Where to buy",
  global__category: "Category",
  global__picture: "Picture",
  global__share_with: "Share with",
  global__form_validation_start: "Please provide a ",
  global__add: "Add",
  global__update: "Modify",
  global__delete: "Delete",
  global__accept: "Accept",
  global__cancel: "Cancel",
  global__reserve: "Reserve",

  nav_bar__signin: "Sign In",
  nav_bar__signup: "Sign Up",
  nav_bar__myList: "My List",
  nav_bar__myFriends: "My Friends",
  nav_bar__myBuyList: "My Buy List",
  nav_bar__logout: "Log Out",
  nav_bar__manageAccount: "Manage account",
  nav_bar__changeAccount: "Change account",

  signup__title: "Sign up to MyGift",
  signup__button: "Sign up",
  signin__title: "Sign in to MyGift",
  signin__button: "Sign in",
  signin__newAccount: "New to MyGift?",
  signin__creatAccount: "Crate an account",

  mywishlist__addGiftButton: "Add a gift",
  mywishlist__addCategoryButton: "Add a category",
  mywishlist__reorderButton: "Reorder",
  mywishlist__downloadPdfButton: "Download as PDF",

  category_modal__addCategoryTitle: "Add a new category",
  category_modal__updateCategoryTitle: "Modify category",

  gift_modal__addGiftTitle: "Add a new gift",
  gift_modal__updateGiftTitle: "Modify gift",

  delete_modal__pre_text: "Do you really want to remove ",
  delete_modal__pre_text_category: "the category ",
  delete_modal__pre_text_gift: "the gift ",
  delete_modal__category_hint: "This will also delete all gifts of this category.",
  delete_modal__post_text_friend: " of your firend list?",
  delete_modal__delete_gift_i_got_it: "I have received it",
  delete_modal__delete_gift_dont_want_it: "I don't want it anymore",

  myfriends__add_friend: "Add friend",
  myfriends__friend_list: "Friends",
  myfriends__friend_requests: "Friend requests",
  myfriends__my_friend_requests: "My friend requests",

  friend_modal__title: "Add a new friend",

  friendlist__title: "List of ",
  friendlist__addGiftButton: "Add a secret gift",

  home__years_old: "years old",
  home__special_event: {
    [EventKind.BIRTHDAY]: "",
    [EventKind.CHRISTMAS]: "Christmas",
    [EventKind.MOTHER_DAY]: "Mothers day",
    [EventKind.FATHER_DAY]: "Fathers day",
  },
};
