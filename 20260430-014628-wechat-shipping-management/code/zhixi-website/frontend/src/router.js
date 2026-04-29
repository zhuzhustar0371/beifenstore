import { createRouter, createWebHistory } from "vue-router";
import HomePage from "./views/HomePage.vue";
import RulesPage from "./views/RulesPage.vue";
import UserCenterPage from "./views/UserCenterPage.vue";

const routes = [
  { path: "/", component: HomePage },
  { path: "/rules", component: RulesPage },
  { path: "/user", component: UserCenterPage },
];

export default createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, _from, savedPosition) {
    if (to.hash) return { el: to.hash, behavior: "smooth" };
    if (savedPosition) return savedPosition;
    return { top: 0 };
  },
});
