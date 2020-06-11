/*
 * Copyright (C) 2016-2020 Dennis Neufeld
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
import { User } from "@/components/user/user";

export const FETCH_USER = "FETCH_USER";
export const LOG_OUT = "LOG_OUT";

const LOAD_USER = "LOAD_USER";
const UNLOAD_USER = "UNLOAD_USER";

const defaultUser = new User(69, "User McUserFace", null, "0420");

export const userStore = {
	namespaced: true,
	modules: {},
	state: () => ({
		userLoaded: false,
		user: defaultUser,
	}),
	getters: {},
	mutations: {
		[LOAD_USER](state, user) {
			state.user = user;
			state.userLoaded = true;
		},
		[UNLOAD_USER](state) {
			state.userLoaded = false;
			state.user = defaultUser;
		},
	},
	actions: {
		async [FETCH_USER](context) {
			const response = await fetch("/public/user");
			if (response.status === 200) {
				let user = await response.json();
				context.commit(LOAD_USER, new User(user.discordId, user.name, user.avatarId, user.discriminator));
			}
		},
		async [LOG_OUT](context) {
			let csrfToken = document.cookie.replace(/(?:(?:^|.*;\s*)XSRF-TOKEN\s*=\s*([^;]*).*$)|^.*$/, "$1");
			await fetch("/public/login", {
				method: "DELETE",
				headers: {
					"X-XSRF-TOKEN": csrfToken,
				},
			});
			context.commit(UNLOAD_USER);
		},
	},
};
