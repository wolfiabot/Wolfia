import Vue from "vue";
import Vuex from "vuex";
import { LOAD_USER, UNLOAD_USER } from "@/store/mutation-types";
import { FETCH_USER, LOG_OUT } from "@/store/action-types";
import { User } from "@/store/user";

Vue.use(Vuex);

let defaultUser = new User(42, "mysterious person", null, 42);

export default new Vuex.Store({
	strict: process.env.NODE_ENV !== "production", //see https://vuex.vuejs.org/guide/strict.html
	state: {
		userLoaded: false,
		user: defaultUser
	},
	mutations: {
		[LOAD_USER](state, user) {
			state.user = user;
			state.userLoaded = true;
		},
		[UNLOAD_USER](state) {
			state.userLoaded = false;
			state.user = defaultUser;
		}
	},
	actions: {
		async [FETCH_USER](context) {
			const response = await fetch("/api/user");
			if (response.status === 200) {
				let user = await response.json();
				context.commit(
					LOAD_USER,
					new User(
						user.discordId,
						user.name,
						user.avatarId,
						user.discriminator
					)
				);
			}
		},
		async [LOG_OUT](context) {
			let csrfToken = document.cookie.replace(
				/(?:(?:^|.*;\s*)XSRF-TOKEN\s*=\s*([^;]*).*$)|^.*$/,
				"$1"
			);
			await fetch("/api/login", {
				method: "DELETE",
				headers: {
					"X-XSRF-TOKEN": csrfToken
				}
			});
			context.commit(UNLOAD_USER);
		}
	},
	modules: {}
});
