import { createRouter, createWebHistory } from "vue-router";
import AuditLogsPage from "./views/AuditLogsPage.vue";
import CashbacksPage from "./views/CashbacksPage.vue";
import InvitesPage from "./views/InvitesPage.vue";
import OrdersPage from "./views/OrdersPage.vue";
import OverviewPage from "./views/OverviewPage.vue";
import ProductsPage from "./views/ProductsPage.vue";
import UsersPage from "./views/UsersPage.vue";

const routes = [
  { path: "/", redirect: "/overview" },
  { path: "/overview", component: OverviewPage },
  { path: "/users", component: UsersPage },
  { path: "/products", component: ProductsPage },
  { path: "/orders", component: OrdersPage },
  { path: "/invites", component: InvitesPage },
  { path: "/cashbacks", component: CashbacksPage },
  { path: "/audit-logs", component: AuditLogsPage },
  { path: "/:pathMatch(.*)*", redirect: "/overview" }
];

export default createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 };
  }
});
