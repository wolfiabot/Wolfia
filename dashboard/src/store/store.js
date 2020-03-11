import Vue from "vue";
import Vuex from "vuex";

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
		logIn(state, user) {
			state.userLoaded = true;
			state.user = user;
		},
		logOut(state) {
			state.userLoaded = false;
			state.user = defaultUser;
		}
	},
	actions: {},
	modules: {}
});
