<!--
  - Copyright (C) 2016-2026 the original author or authors
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
	<div class="Gamemodes">
		<h1 class="title has-text-weight-light is-size-1">Game Modes</h1>
		<div class="gamemodelist columns is-centered is-multiline" :class="{ 'is-loading': !gamemodesLoaded }">
			<div class="column is-full-tablet is-half-desktop" v-for="gamemode in gamemodes" :key="gamemode.name">
				<GamemodeCard :gamemode="gamemode" class="gamemodecard" />
			</div>
		</div>
	</div>
</template>

<script>
import { defineAsyncComponent } from "vue";
import { mapActions, mapState } from "vuex";
import { FETCH_GAMEMODES } from "@/components/gamemodes/gamemodes-store";

export default {
	name: "Gamemodes",
	components: {
		GamemodeCard: defineAsyncComponent(() => import("@/components/gamemodes/GamemodeCard.vue")),
	},
	mounted() {
		this.fetchGamemodes();
	},
	computed: {
		...mapState("gamemodes", {
			gamemodes: (state) => state.gamemodes,
			gamemodesLoaded: (state) => state.gamemodesLoaded,
		}),
	},
	methods: {
		...mapActions("gamemodes", {
			fetchGamemodes: FETCH_GAMEMODES,
		}),
	},
};
</script>

<style scoped lang="scss">
.Gamemodes {
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-top: 2em;
}
.gamemodelist {
	padding-right: 4em;
	padding-left: 4em;
	width: 100%;
}
.gamemodecard {
	height: 100%;
}
</style>
