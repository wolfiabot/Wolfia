/*
 * Copyright (C) 2016-2025 the original author or authors
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

import { createRouter, createWebHistory } from "vue-router";
import Home from "@/components/Home.vue";

const routes = [
	{
		path: "/",
		name: "home",
		component: Home,
	},
	{
		path: "/commands",
		name: "commands",
		component: () => import("@/components/Commands.vue"),
	},
	{
		path: "/gamemodes",
		name: "gamemodes",
		component: () => import("@/components/Gamemodes.vue"),
	},
	{
		path: "/team",
		name: "team",
		component: () => import("@/components/staff/Staff.vue"),
	},
	{
		path: "/dashboard/:guildId?",
		name: "dashboard",
		component: () => import("@/components/dashboard/Dashboard.vue"),
		props: true,
	},
	{
		path: "/status",
		name: "status",
		component: () => import("@/components/status/Status.vue"),
	},
	{
		path: "/togglz",
		name: "togglz",
		component: () => import("@/components/Togglz.vue"),
	},
	{
		path: "/privacy",
		name: "privacy",
		component: () => import("@/components/PrivacyPolicy.vue"),
	},
];

const router = createRouter({
	history: createWebHistory(),
	// see https://router.vuejs.org/guide/advanced/scroll-behavior.html#scroll-behavior
	scrollBehavior(to, from, savedPosition) {
		if (savedPosition) {
			return savedPosition;
		}
		if (to.hash) {
			return { selector: to.hash };
		}
		return { left: 0, top: 0 };
	},
	routes,
});

export default router;
