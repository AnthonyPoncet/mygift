export interface ErrorJson {
  error: string;
}

export interface OneCategory {
  category: Category;
  gifts: Gift[];
}

export interface OneFriendCategory {
  category: Category;
  gifts: FriendGift[];
}

export interface Category {
  id: number;
  name: string;
  share: string[];
}

export interface Gift {
  id: number;
  name: string;
  description: string;
  price: string;
  whereToBuy: string;
  picture: string;
  heart: boolean;
}

export interface FriendGift {
  gift: Gift;
  reservedBy: string[]; //TODO: make no sense that it returns a reserve list...
  secret: boolean;
}

export interface FriendRequest {
  id: number;
  otherUser: Friend;
}

export interface Friend {
  name: string;
  picture: string | null;
  dateOfBirth: number | null;
}

export interface PendingFriendRequest {
  sent: FriendRequest[];
  received: FriendRequest[];
}

export interface FileUpload {
  name: string;
}

export enum EventKind {
  BIRTHDAY = "BIRTHDAY",
  CHRISTMAS = "CHRISTMAS",
  MOTHER_DAY = "MOTHER_DAY",
  FATHER_DAY = "FATHER_DAY",
}

export interface EventJson {
  kind: EventKind;
  date: number;
  name: string | null;
  picture: string | null;
  birth: number | null;
}
