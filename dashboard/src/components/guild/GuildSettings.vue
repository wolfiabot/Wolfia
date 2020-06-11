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
	<div v-if="loading">Loading</div>
	<div v-else-if="guild">
		<figure class="card-image">
			<img alt="Guild logo" class="is-square" :src="guild.iconUrl()" />
		</figure>
		<strong>Henlo, {{ guild.name }}</strong>
	</div>
	<div v-else>Nope</div>
</template>

<script>
import { mapActions, mapState } from "vuex";
import { FETCH_GUILDS } from "@/components/guild/guild-store";

export default {
	name: "GuildSettings",
	props: {
		id: String,
	},
	mounted() {
		this.fetchGuilds();
	},
	computed: {
		...mapState("guild", {
			loading(state) {
				return !state.guildsLoaded;
			},
			guild(state) {
				return state.guilds.find((g) => g.discordId === this.id);
			},
		}),
	},
	methods: {
		...mapActions("guild", {
			fetchGuilds: FETCH_GUILDS,
		}),
	},
};
</script>

<style scoped></style>
