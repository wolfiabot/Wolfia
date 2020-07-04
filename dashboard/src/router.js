/*
 * Copyright (C) 2016-2020 the original author or authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import Vue from "vue";
import VueRouter from "vue-router";
import Home from "@/components/Home.vue";

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
		component: () => import(/* webpackChunkName: "about" */ "@/components/About.vue"),
	},
	{
		path: "/team",
		name: "team",
		component: () => import(/* webpackChunkName: "team" */ "@/components/staff/Staff.vue"),
	},
	{
		path: "/dashboard/:guildId?",
		name: "dashboard",
		component: () => import(/* webpackChunkName: "dashboard" */ "@/components/dashboard/Dashboard.vue"),
		props: true,
	},
	{
		path: "/togglz",
		name: "togglz",
		component: () => import(/* webpackChunkName: "togglz" */ "@/components/Togglz.vue"),
	},
];

const router = new VueRouter({
	mode: "history",
	routes,
});

export default router;
