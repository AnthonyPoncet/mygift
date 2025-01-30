export interface ErrorJson {
  error: string;
}

export interface Wishlist {
  categories: Category[];
}

export interface Category {
  id: number;
  name: string;
  share_with: number[];
  gifts: Gift[];
}

export interface Gift {
  id: number;
  name: string;
  description: string | null;
  price: string | null;
  where_to_buy: string | null;
  picture: string | null;
  heart: boolean;
}

export interface FriendWishlist {
  categories: FriendCategory[];
}

export interface FriendCategory {
  id: number;
  name: string;
  gifts: FriendGift[];
}

export interface FriendGift {
  id: number;
  name: string;
  description: string | null;
  price: string | null;
  where_to_buy: string | null;
  picture: string | null;
  heart: boolean;
  secret: boolean;
  reserved_by: number | null;
}

export interface Friends {
  friends: Friend[];
}

export interface Friend {
  id: number;
  name: string;
  picture: string | null;
  date_of_birth: number | null;
}

export interface PendingFriendRequests {
  sent: FriendRequest[];
  received: FriendRequest[];
}

export interface FriendRequest {
  id: number;
  other_user: Friend;
}

export interface FileUpload {
  name: string;
}

export enum EventKind {
  BIRTHDAY = "Birthday",
  CHRISTMAS = "Christmas",
}

export interface EventJson {
  kind: EventKind;
  date: number;
  name: string | null;
  picture: string | null;
  birth: number | null;
}
