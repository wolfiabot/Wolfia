import Vue from "vue";
import Vuex from "vuex";
import { LOAD_STAFF, LOAD_USER, UNLOAD_USER } from "@/store/mutation-types";
import { FETCH_STAFF, FETCH_USER, LOG_OUT } from "@/store/action-types";
import { User } from "@/store/user";
import { StaffMember } from "@/store/staffmember";

Vue.use(Vuex);

let defaultUser = new User(42, "mysterious person", null, 42);

export default new Vuex.Store({
	strict: process.env.NODE_ENV !== "production", //see https://vuex.vuejs.org/guide/strict.html
	state: {
		userLoaded: false,
		user: defaultUser,
		staff: [],
	},
	mutations: {
		[LOAD_USER](state, user) {
			state.user = user;
			state.userLoaded = true;
		},
		[UNLOAD_USER](state) {
			state.userLoaded = false;
			state.user = defaultUser;
		},
		[LOAD_STAFF](state, staff) {
			state.staff = staff;
		},
	},
	actions: {
		async [FETCH_USER](context) {
			const response = await fetch("/api/user");
			if (response.status === 200) {
				let user = await response.json();
				context.commit(LOAD_USER, new User(user.discordId, user.name, user.avatarId, user.discriminator));
			}
		},
		async [LOG_OUT](context) {
			let csrfToken = document.cookie.replace(/(?:(?:^|.*;\s*)XSRF-TOKEN\s*=\s*([^;]*).*$)|^.*$/, "$1");
			await fetch("/api/login", {
				method: "DELETE",
				headers: {
					"X-XSRF-TOKEN": csrfToken,
				},
			});
			context.commit(UNLOAD_USER);
		},
		async [FETCH_STAFF](context) {
			const response = await fetch("/api/staff");
			if (response.status === 200) {
				const staff = await response.json();
				let mappedStaff = staff.map((member) => {
					const user = new User(member.discordId, member.name, member.avatarId, member.discriminator);
					return new StaffMember(user, member.function, member.slogan, member.link);
				});
				context.commit(LOAD_STAFF, mappedStaff);
			}
		},
	},
	modules: {},
});
