/*
 * Copyright (C) 2016-2020 the original author or authors
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
import { Shard } from "@/components/status/shard";
import fetcher from "@/fetcher";

export const FETCH_SHARDS = "FETCH_SHARDS";

const FETCH_SHARDS_INTERNAL = "FETCH_SHARDS_INTERNAL";

const LOAD_SHARDS = "LOAD_SHARDS";
const FETCHING_SHARDS = "FETCHING_SHARDS";

export const shardStore = {
	namespaced: true,
	modules: {},
	state: () => ({
		shardsLoading: false, //true each time there is a request for staff in flight
		shardsLoaded: false, //true as soon as we received the data for the first time
		shards: [],
	}),
	getters: {},
	mutations: {
		[LOAD_SHARDS](state, shards) {
			state.shardsLoading = false;
			state.shards = shards;
			state.shardsLoaded = true;
		},
		[FETCHING_SHARDS](state) {
			state.shardsLoading = true;
		},
	},
	actions: {
		async [FETCH_SHARDS](context) {
			if (context.state.shardsLoading) {
				return;
			}
			context.commit(FETCHING_SHARDS);
			context.dispatch(FETCH_SHARDS_INTERNAL);
		},
		async [FETCH_SHARDS_INTERNAL](context) {
			let shards = await fetcher.get("/public/shards");
			if (!shards) {
				setTimeout(() => context.dispatch(FETCH_SHARDS_INTERNAL), 5000);
				return;
			}

			let mappedShareds = shards.map((shard) => {
				return new Shard(shard.id, shard.status);
			});

			context.commit(LOAD_SHARDS, mappedShareds);
		},
	},
};
