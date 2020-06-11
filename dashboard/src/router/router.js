import Vue from "vue";
import VueRouter from "vue-router";
import Home from "../components/Home.vue";

Vue.use(VueRouter);

const routes = [
	{
		path: "/",
		name: "home",
		component: Home,
	},
	{
		path: "/about",
		name: "about",
		// route level code-splitting
		// this generates a separate chunk (about.[hash].js) for this route
		// which is lazy-loaded when the route is visited.
		component: () => import(/* webpackChunkName: "about" */ "../components/About.vue"),
	},
	{
		path: "/team",
		name: "team",
		component: () => import(/* webpackChunkName: "team" */ "../components/staff/Staff.vue"),
	},
	{
		path: "/dashboard",
		name: "dashboard",
		component: () => import(/* webpackChunkName: "dashboard" */ "../components/dashboard/Dashboard.vue"),
	},
	{
		path: "/guild/:id",
		name: "guild settings",
		component: () => import(/* webpackChunkName: "guild" */ "../components/guild/GuildSettings.vue"),
		props: true,
	},
];

const router = new VueRouter({
	mode: "history",
	routes,
});

export default router;
