/*
 * Copyright (C) 2016-2023 the original author or authors
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

import { createStore } from "vuex";
import { userStore } from "@/components/user/user-store";
import { staffStore } from "@/components/staff/staff-store";
import { guildStore } from "@/components/guild/guild-store";
import { shardStore } from "@/components/status/shard-store";
import { guildSettingsStore } from "@/components/guildsettings/guild-settings-store";

export default createStore({
	strict: import.meta.env.DEV, //see https://vuex.vuejs.org/guide/strict.html
	modules: {
		user: userStore,
		staff: staffStore,
		guild: guildStore,
		guildSettings: guildSettingsStore,
		shards: shardStore,
	},
	state: {},
	getters: {},
	mutations: {},
	actions: {},
});
