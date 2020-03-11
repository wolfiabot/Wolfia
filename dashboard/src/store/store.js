import Vue from "vue";
import Vuex from "vuex";
import { LOAD_USER, UNLOAD_USER } from "@/store/mutation-types";
import { FETCH_USER, LOG_OUT } from "@/store/action-types";

Vue.use(Vuex);

let defaultUser = {
	discordId: 42,
	name: "mysterious person",
	avatarId: 42
};

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
				context.commit(LOAD_USER, user);
			}
		},
		async [LOG_OUT](context) {
			await fetch("/api/login", { method: "DELETE" });
			context.commit(UNLOAD_USER);
		}
	},
	modules: {}
});
