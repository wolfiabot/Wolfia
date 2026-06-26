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

export const FETCH_COMMANDS = "FETCH_COMMANDS";

const FETCH_COMMANDS_INTERNAL = "FETCH_COMMANDS_INTERNAL";

const LOAD_COMMANDS = "LOAD_COMMANDS";
const FETCHING_COMMANDS = "FETCHING_COMMANDS";

export const commandsStore = {
	namespaced: true,
	modules: {},
	state: () => ({
		commandsLoading: false, //true each time there is a request for commands in flight
		commandsLoaded: false, //true as soon as we received the data for the first time
		categories: [],
	}),
	getters: {},
	mutations: {
		[LOAD_COMMANDS](state, categories) {
			state.commandsLoading = false;
			state.categories = categories;
			state.commandsLoaded = true;
		},
		[FETCHING_COMMANDS](state) {
			state.commandsLoading = true;
		},
	},
	actions: {
		async [FETCH_COMMANDS](context) {
			if (context.state.commandsLoading) {
				return;
			}
			context.commit(FETCHING_COMMANDS);
			context.dispatch(FETCH_COMMANDS_INTERNAL);
		},
		async [FETCH_COMMANDS_INTERNAL](context) {
			const categories = await fetcher.get("/public/commands");
			if (!categories) {
				setTimeout(() => context.dispatch(FETCH_COMMANDS_INTERNAL), 5000);
				return;
			}
			context.commit(LOAD_COMMANDS, categories);
		},
	},
};
