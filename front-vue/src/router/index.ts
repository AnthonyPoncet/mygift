import { createRouter, createWebHistory } from "vue-router";
import HomeView from "@/views/HomeView.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/",
      name: "home",
      component: HomeView,
    },
    {
      path: "/signin",
      name: "signin",
      component: () => import("../views/SignIn.vue"),
    },
    {
      path: "/signup",
      name: "signup",
      component: () => import("../views/SignUp.vue"),
    },
    {
      path: "/changeaccount",
      name: "changeaccount",
      component: () => import("../views/SignIn.vue"),
    },
    {
      path: "/mywishlist",
      name: "mywishlist",
      component: () => import("../views/MyWishList.vue"),
    },
    {
      path: "/mywishlist/add-gift",
      name: "mywishlistaddgift",
      component: () => import("../views/AddGift.vue"),
    },
    {
      path: "/mywishlist/add-category",
      name: "mywishlistaddcategory",
      component: () => import("../views/AddCategory.vue"),
    },
    {
      path: "/mywishlist/categories/:id/edit",
      name: "mywishlisteditcategory",
      component: () => import("../views/EditCategory.vue"),
    },
    {
      path: "/mywishlist/gifts/:id/edit",
      name: "mywishlisteditgift",
      component: () => import("../views/EditGift.vue"),
    },
    {
      path: "/myfriends",
      name: "myfriends",
      component: () => import("../views/MyFriends.vue"),
    },
    {
      path: "/friend/:name",
      name: "friend",
      component: () => import("../views/FriendWishList.vue"),
    },
    {
      path: "/friend/:name/add-gift",
      name: "friendaddgift",
      component: () => import("../views/AddGift.vue"),
    },
    {
      path: "/friend/:name/gifts/:id/edit",
      name: "friendeditgift",
      component: () => import("../views/EditGift.vue"),
    },
    {
      path: "/friend/:name/gifts/:id",
      name: "showgift",
      component: () => import("../views/ShowGift.vue"),
    },
    {
      path: "/manageaccount",
      name: "manageaccount",
      component: () => import("../views/EditProfile.vue"),
    },
  ],
  scrollBehavior(to, from, savedPosition) {
    // always scroll to top
    return { top: 0 };
  },
});

export default router;
