import Vue from "vue";
import Vuex from "vuex";
import { userStore } from "@/components/user/user-store";
import { staffStore } from "@/components/staff/staff-store";
import { guildStore } from "@/components/guild/guild-store";

Vue.use(Vuex);

export default new Vuex.Store({
	strict: process.env.NODE_ENV !== "production", //see https://vuex.vuejs.org/guide/strict.html
	modules: {
		user: userStore,
		staff: staffStore,
		guild: guildStore,
	},
	state: {},
	getters: {},
	mutations: {},
	actions: {},
});
