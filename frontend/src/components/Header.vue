<!--
  - Copyright (C) 2016-2025 the original author or authors
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
	<nav id="nav" class="navbar has-text-weight-bold">
		<div class="navbar-brand">
			<router-link class="navbar-item" to="/">
				<img src="../../logo.png" class="wolfia-logo" alt="Play Werewolf & Mafia games on Discord" />
				<div>Wolfia</div>
			</router-link>
			<a
				role="button"
				class="navbar-burger"
				aria-label="menu"
				aria-expanded="false"
				@click="toggleNavbar"
				:class="{ 'is-active': showNavbar }"
			>
				<span aria-hidden="true"></span>
				<span aria-hidden="true"></span>
				<span aria-hidden="true"></span>
			</a>
		</div>
		<div class="navbar-menu" :class="{ 'is-active': showNavbar }">
			<div class="navbar-start">
				<hr class="navbar-divider" />

				<router-link to="/commands" class="navbar-item">Commands</router-link>
				<router-link to="/gamemodes" class="navbar-item"> Gamemodes</router-link>
				<router-link to="/dashboard" class="navbar-item">Dashboard</router-link>
				<router-link to="/team" class="navbar-item">Team</router-link>
				<router-link to="/status" class="navbar-item">Status</router-link>
				<a
					class="navbar-item"
					target="_blank"
					rel="noopener noreferrer"
					href="https://feedback.userreport.com/01987d31-0d58-48c6-a4d3-96f2ae42eb14#ideas"
				>
					Ideas
				</a>
				<router-link to="/togglz" class="navbar-item" v-if="isAdmin">Feature Flags</router-link>
			</div>
			<div class="navbar-end">
				<UserNav />
			</div>
		</div>
	</nav>
</template>

<script>
import { defineAsyncComponent } from "vue";
import { mapGetters } from "vuex";

export default {
	components: {
		UserNav: defineAsyncComponent(() => import("@/components/user/UserNav.vue")),
	},
	data: function () {
		return {
			showNavbar: false,
		};
	},
	computed: {
		...mapGetters("user", ["isAdmin"]),
	},
	methods: {
		toggleNavbar: function () {
			this.showNavbar = !this.showNavbar;
		},
	},
};
</script>

<style scoped lang="scss">
nav {
	padding: 0 1.5em;
	border-radius: 0em !important;
}
.wolfia-logo {
	margin: 0.5em;
}
</style>
