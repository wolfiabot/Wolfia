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
	<div>
		<div class="is-size-1">Please select the guild you want to edit.</div>
		<div id="guildlist" class="columns is-centered is-multiline" :class="{ 'is-loading': !guildsLoaded }">
			<div class="guildlist column is-one-third-tablet is-one-fifth-desktop" v-for="guild in guilds" :key="guild.discordId">
				<GuildCard :guild="guild" class="guildcard" />
			</div>
		</div>
	</div>
</template>

<script>
import { mapActions, mapState } from "vuex";
import { FETCH_GUILDS } from "@/components/guild/guild-store";
import GuildCard from "@/components/dashboard/GuildCard";

export default {
	name: "GuildList",
	components: { GuildCard },
	mounted() {
		this.fetchGuilds();
	},
	computed: {
		...mapState("guild", {
			guilds: (state) => {
				return [...state.guilds].sort((a, b) => {
					// Guilds where the user can edit the setting shown first
					if (a.canEdit !== b.canEdit) {
						return b.canEdit - a.canEdit;
					}

					// Guilds where the bot is present next
					if (a.botPresent !== b.botPresent) {
						return b.botPresent - a.botPresent;
					}

					// Order by discord id (= age) otherwise
					return a.discordId - b.discordId;
				});
			},
			guildsLoaded: (state) => state.guildsLoaded,
		}),
	},
	methods: {
		...mapActions("guild", {
			fetchGuilds: FETCH_GUILDS,
		}),
	},
};
</script>

<style scoped>
#guildlist {
	padding: 0 6em;
	width: 100%;
	height: 100%;
	margin: 2em auto;
}
.guildcard {
	padding: 1em;
}
</style>
