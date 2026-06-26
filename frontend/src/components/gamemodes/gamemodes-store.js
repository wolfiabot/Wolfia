/*
 * Copyright (C) 2016-2026 the original author or authors
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
import fetcher from "@/fetcher";

export const FETCH_GAMEMODES = "FETCH_GAMEMODES";

const FETCH_GAMEMODES_INTERNAL = "FETCH_GAMEMODES_INTERNAL";

const LOAD_GAMEMODES = "LOAD_GAMEMODES";
const FETCHING_GAMEMODES = "FETCHING_GAMEMODES";

export const gamemodesStore = {
	namespaced: true,
	modules: {},
	state: () => ({
		gamemodesLoading: false, //true each time there is a request for gamemodes in flight
		gamemodesLoaded: false, //true as soon as we received the data for the first time
		gamemodes: [],
	}),
	getters: {},
	mutations: {
		[LOAD_GAMEMODES](state, gamemodes) {
			state.gamemodesLoading = false;
			state.gamemodes = gamemodes;
			state.gamemodesLoaded = true;
		},
		[FETCHING_GAMEMODES](state) {
			state.gamemodesLoading = true;
		},
	},
	actions: {
		async [FETCH_GAMEMODES](context) {
			if (context.state.gamemodesLoading) {
				return;
			}
			context.commit(FETCHING_GAMEMODES);
			context.dispatch(FETCH_GAMEMODES_INTERNAL);
		},
		async [FETCH_GAMEMODES_INTERNAL](context) {
			const gamemodes = await fetcher.get("/public/gamemodes");
			if (!gamemodes) {
				setTimeout(() => context.dispatch(FETCH_GAMEMODES_INTERNAL), 5000);
				return;
			}
			context.commit(LOAD_GAMEMODES, gamemodes);
		},
	},
};
