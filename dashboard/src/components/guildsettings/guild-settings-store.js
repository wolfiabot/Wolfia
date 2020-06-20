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
import { GuildSettings } from "@/components/guildsettings/guild-settings";
import { ChannelSettings } from "@/components/guildsettings/channel-settings";
import { ToastProgrammatic as Toast } from "buefy";

export const FETCH_GUILD_SETTINGS = "FETCH_GUILD_SETTINGS";
export const SET_GAME_CHANNEL = "SET_GAME_CHANNEL";

const FETCH_GUILD_SETTINGS_INTERNAL = "FETCH_GUILD_SETTINGS_INTERNAL";

const LOAD_GUILD_SETTINGS = "LOAD_GUILD_SETTINGS";
const FETCHING_GUILD_SETTINGS = "FETCHING_GUILD_SETTINGS";

export const guildSettingsStore = {
	namespaced: true,
	modules: {},
	state: () => ({
		guildSettingsLoading: false, //true each time there is a request for guild settings in flight
		guildSettingsLoaded: false, //true as soon as we received the data for the first time
		guildSettings: {},
	}),
	getters: {},
	mutations: {
		[LOAD_GUILD_SETTINGS](state, guildSettings) {
			state.guildSettingsLoading = false;
			state.guildSettings = guildSettings;
			state.guildSettingsLoaded = true;
		},
		[FETCHING_GUILD_SETTINGS](state) {
			state.guildSettingsLoading = true;
		},
	},
	actions: {
		async [FETCH_GUILD_SETTINGS](context, guildId) {
			if (context.state.guildSettingsLoading) {
				return;
			}
			context.commit(FETCHING_GUILD_SETTINGS);
			context.dispatch(FETCH_GUILD_SETTINGS_INTERNAL, guildId);
		},
		async [FETCH_GUILD_SETTINGS_INTERNAL](context, guildId) {
			let failed = true;
			try {
				const response = await fetch(`/api/guild_settings/${guildId}`);
				if (response.status === 200) {
					await storeGuildSettings(context, response);
					failed = false;
				}
			} catch (err) {
				console.log(err);
			}
			if (failed) {
				setTimeout(() => context.dispatch(FETCH_GUILD_SETTINGS_INTERNAL, guildId), 5000);
			}
		},
		async [SET_GAME_CHANNEL](context, { guildId, body }) {
			await setGameChannel(context, guildId, body);
		},
	},
};

async function setGameChannel(context, guildId, body) {
	try {
		let csrfToken = document.cookie.replace(/(?:(?:^|.*;\s*)XSRF-TOKEN\s*=\s*([^;]*).*$)|^.*$/, "$1");
		const response = await fetch(`/api/guild_settings/${guildId}/channel_settings/game_channel`, {
			method: "POST",
			headers: {
				"X-XSRF-TOKEN": csrfToken,
				"Content-Type": "application/json",
			},
			body: JSON.stringify(body),
		});
		if (response.status === 200) {
			await storeGuildSettings(context, response);
		}
	} catch (err) {
		Toast.open({
			message: "Oh no, is your internet down?",
			type: "is-danger",
		});
		console.log(err);
	}
}

async function storeGuildSettings(context, response) {
	const guildSettings = await response.json();
	const channelSettings = guildSettings.channelSettings.map((cs) => {
		return new ChannelSettings(cs.discordId, cs.name, cs.isGameChannel);
	});
	context.commit(LOAD_GUILD_SETTINGS, new GuildSettings(guildSettings.discordId, channelSettings));
}
