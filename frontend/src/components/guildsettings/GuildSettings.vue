<!--
  - Copyright (C) 2016-2020 the original author or authors
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
			<div class="box guild-header">
				<p>{{ guild.name }}</p>
				<img alt="Guild logo" class="is-square" :src="guild.iconUrl()" />
			</div>
			<GameChannels :guild-id="guild.discordId" :guildSettings="guildSettings" class="column is-half" />
		</div>
		<GoBack v-else to="/dashboard" />
	</div>
</template>

<script>
import { defineAsyncComponent } from "vue";
import { mapActions, mapState } from "vuex";
import { FETCH_GUILDS } from "@/components/guild/guild-store";
import { FETCH_GUILD_SETTINGS } from "@/components/guildsettings/guild-settings-store";

export default {
	name: "GuildSettings",
	components: {
		GoBack: defineAsyncComponent(() => import("@/components/GoBack.vue")),
		GameChannels: defineAsyncComponent(() => import("@/components/guildsettings/GameChannels.vue")),
	},
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

<style lang="scss" scoped>
.guild-header {
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 0.5em 0;
	img {
		height: 5em;
	}
	p {
		font-weight: bold;
		margin-right: 1em;
		font-size: 1.75em;
	}
}
</style>
