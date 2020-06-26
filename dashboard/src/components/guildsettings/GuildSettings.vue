<!--
  - Copyright (C) 2016-2020 Dennis Neufeld
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU Affero General Public License as published
  - by the Free Software Foundation, either version 3 of the License, or
  - (at your option) any later version.
  -
  - This program is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU Affero General Public License for more details.
  -
  - You should have received a copy of the GNU Affero General Public License
  - along with this program.  If not, see <http://www.gnu.org/licenses/>.
  -->

<template>
	<div class="guild-settings">
		<div v-if="loading" class="is-loading"></div>
		<div v-else-if="guild && guildSettings">
			<div class="box">
				<figure class="card-image">
					<img alt="Guild logo" class="is-square" :src="guild.iconUrl()" />
				</figure>
				<div class="is-size-3">
					<strong>{{ guild.name }}</strong>
				</div>
			</div>
			<div class="columns is-centered">
				<GameChannels :guild-id="guild.discordId" :guildSettings="guildSettings" class="column is-half" />
			</div>
		</div>
		<!-- TODO better fallback -->
		<div v-else>Nope</div>
	</div>
</template>

<script>
import { mapActions, mapState } from "vuex";
import { FETCH_GUILDS } from "@/components/guild/guild-store";
import GameChannels from "@/components/guildsettings/GameChannels";
import { FETCH_GUILD_SETTINGS } from "@/components/guildsettings/guild-settings-store";

export default {
	name: "GuildSettings",
	components: { GameChannels },
	props: {
		guildId: String,
	},
	mounted() {
		this.fetchGuilds();
		this.fetchGuildSettings(this.guildId);
	},
	computed: {
		...mapState("guild", {
			loadingGuild(state) {
				return !state.guildsLoaded;
			},
			guild(state) {
				return state.guilds.find((g) => g.discordId === this.guildId);
			},
		}),
		...mapState("guildSettings", {
			loadingGuildSettings(state) {
				return !state.guildSettingsLoaded;
			},
			guildSettings(state) {
				return state.guildSettings;
			},
		}),
		loading: function () {
			return this.loadingGuild || this.loadingGuildSettings;
		},
	},
	methods: {
		...mapActions("guild", {
			fetchGuilds: FETCH_GUILDS,
		}),
		...mapActions("guildSettings", {
			fetchGuildSettings: FETCH_GUILD_SETTINGS,
		}),
	},
};
</script>

<style scoped></style>
