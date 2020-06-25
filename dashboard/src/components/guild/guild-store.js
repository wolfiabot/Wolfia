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
import { Guild } from "@/components/guild/guild";
import fetcher from "@/fetcher";

export const FETCH_GUILDS = "FETCH_GUILDS";

const FETCH_GUILDS_INTERNAL = "FETCH_GUILDS_INTERNAL";

const LOAD_GUILDS = "LOAD_GUILDS";
const FETCHING_GUILDS = "FETCHING_GUILDS";

export const guildStore = {
	namespaced: true,
	modules: {},
	state: () => ({
		guildsLoading: false, //true each time there is a request for guilds in flight
		guildsLoaded: false, //true as soon as we received the data for the first time
		guilds: [],
	}),
	getters: {},
	mutations: {
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
		async [FETCH_GUILDS](context) {
			if (context.state.guildsLoading) {
				return;
			}
			context.commit(FETCHING_GUILDS);
			context.dispatch(FETCH_GUILDS_INTERNAL);
		},
		async [FETCH_GUILDS_INTERNAL](context) {
			const guildInfos = await fetcher.get("/api/guilds");
			if (!guildInfos) {
				setTimeout(() => context.dispatch(FETCH_GUILDS_INTERNAL), 5000);
				return;
			}
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
		},
	},
};
