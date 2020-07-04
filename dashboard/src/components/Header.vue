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
	<nav id="nav" class="navbar has-text-weight-bold">
		<div class="navbar-brand">
			<router-link class="navbar-item" to="/">
				<img src="../assets/logo.png" alt="Play Werewolf/Mafia in Discord" />
			</router-link>
			<a role="button" class="navbar-burger" data-target="navMenu" aria-label="menu" aria-expanded="false">
				<span aria-hidden="true"></span>
				<span aria-hidden="true"></span>
				<span aria-hidden="true"></span>
			</a>
		</div>
		<div class="navbar-menu" id="navMenu">
			<div class="navbar-start">
				<router-link to="/" class="navbar-item">
					Home
				</router-link>
				<hr class="navbar-divider" />

				<router-link to="/" class="navbar-item">
					Bot <!--Placeholder for the commands/gamemodes-->
				</router-link>
				<router-link to="/dashboard" class="navbar-item">
					Dashboard
				</router-link>
				<a
					class="navbar-item"
					target="_blank"
					rel="noopener noreferrer"
					href="https://feedback.userreport.com/01987d31-0d58-48c6-a4d3-96f2ae42eb14#ideas"
				>
					Ideas
				</a>
				<router-link to="/team" class="navbar-item">
					Team
				</router-link>
				<router-link to="/about" class="navbar-item">
					About
				</router-link>
				<router-link to="/togglz" class="navbar-item" v-if="isAdmin">
					Feature Flags
				</router-link>
			</div>
			<div class="navbar-end">
				<UserNav></UserNav>
			</div>
		</div>
	</nav>
</template>

<script>
import UserNav from "@/components/user/UserNav";
import { mapGetters } from "vuex";

export default {
	components: {
		UserNav,
	},
	computed: {
		...mapGetters("user", ["isAdmin"]),
	},
};
</script>

<style lang="scss">
	nav {
		padding: 0 1.5em;
		border-radius: 0em !important;
	}
</style>
