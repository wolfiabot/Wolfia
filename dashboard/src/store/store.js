import Vue from "vue";
import Vuex from "vuex";
import {
	FETCHING_GUILDS,
	FETCHING_STAFF,
	LOAD_GUILDS,
	LOAD_STAFF,
	LOAD_USER,
	UNLOAD_USER,
} from "@/store/mutation-types";
import { FETCH_GUILDS, FETCH_STAFF, FETCH_USER, LOG_OUT } from "@/store/action-types";
import { User } from "@/store/user";
import { StaffMember } from "@/store/staffmember";
import { Guild } from "@/store/guild";

Vue.use(Vuex);

let defaultUser = new User(69, "User McUserFace", null, "0420");

const FETCH_STAFF_INTERNAL = "FETCH_STAFF_INTERNAL";
const FETCH_GUILDS_INTERNAL = "FETCH_GUILDS_INTERNAL";

export default new Vuex.Store({
	strict: process.env.NODE_ENV !== "production", //see https://vuex.vuejs.org/guide/strict.html
	state: {
		// User login
		userLoaded: false,
		user: defaultUser,
		// Staff Page
		staffLoading: false, //true each time there is a request for staff in flight
		staffLoaded: false, //true as soon as we received the data for the first time
		staff: [],
		// Guild Selection
		guildsLoading: false, //true each time there is a request for guilds in flight
		guildsLoaded: false, //true as soon as we received the data for the first time
		guilds: [],
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
			state.staffLoading = false;
			state.staff = staff;
			state.staffLoaded = true;
		},
		[FETCHING_STAFF](state) {
			state.staffLoading = true;
		},
		[LOAD_GUILDS](state, guilds) {
			state.guildsLoading = false;
			state.guilds = guilds;
			state.guildsLoaded = true;
		},
		[FETCHING_GUILDS](state) {
			state.guildsLoading = true;
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
		async [FETCH_STAFF](context) {
			if (context.state.staffLoading) {
				return;
			}
			context.commit(FETCHING_STAFF);
			context.dispatch(FETCH_STAFF_INTERNAL);
		},
		async [FETCH_STAFF_INTERNAL](context) {
			let failed = true;
			try {
				const response = await fetch("/public/staff");
				if (response.status === 200) {
					const staff = await response.json();
					let mappedStaff = staff.map((member) => {
						const user = new User(member.discordId, member.name, member.avatarId, member.discriminator);
						return new StaffMember(user, member.function, member.slogan, member.link);
					});
					context.commit(LOAD_STAFF, mappedStaff);
					failed = false;
				}
			} catch (err) {
				console.log(err);
			}
			if (failed) {
				setTimeout(() => context.dispatch(FETCH_STAFF_INTERNAL), 5000);
			}
		},
		async [FETCH_GUILDS](context) {
			if (context.state.guildsLoading) {
				return;
			}
			context.commit(FETCHING_GUILDS);
			context.dispatch(FETCH_GUILDS_INTERNAL);
		},
		async [FETCH_GUILDS_INTERNAL](context) {
			let failed = true;
			try {
				const response = await fetch("/api/guilds");
				if (response.status === 200) {
					const guildInfos = await response.json();
					console.log(guildInfos);
					let mappedGuilds = guildInfos.map((guildInfo) => {
						return new Guild(
							guildInfo.guild.id,
							guildInfo.guild.name,
							guildInfo.guild.icon,
							guildInfo.botPresent,
							guildInfo.canEdit
						);
					});
					context.commit(LOAD_GUILDS, mappedGuilds);
					failed = false;
				}
			} catch (err) {
				console.log(err);
			}
			if (failed) {
				setTimeout(() => context.dispatch(FETCH_GUILDS_INTERNAL, 5000));
			}
		},
	},
	modules: {},
});
