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
      path: "/manageaccount",
      name: "manageaccount",
      component: () => import("../views/EditProfile.vue"),
    },
  ],
});

export default router;
