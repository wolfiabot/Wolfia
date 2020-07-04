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
	<div v-if="userLoaded" class="navbar-item has-dropdown is-hoverable">
		<a class="navbar-link">
			<div>
				<img alt="User logo" class="is-pulled-left wolfia-user-logo" :src="user.avatarUrl()" />
			</div>
			<div>Hello, {{ user.name }}!</div>
		</a>
		<div class="navbar-dropdown">
			<a class="navbar-item" v-on:click="logout">
				Logout
			</a>
		</div>
	</div>
	<div v-else class="navbar-item">
		<div class="buttons">
			<a class="button is-light" href="/public/login">
				<strong>Login</strong>
			</a>
		</div>
	</div>
</template>

<script>
import { mapActions, mapState } from "vuex";
import { FETCH_USER, LOG_OUT } from "@/components/user/user-store";

export default {
	name: "UserNav",
	props: {},

	mounted() {
		this.fetchUser();
	},

	computed: {
		...mapState("user", {
			userLoaded: (state) => state.userLoaded,
			user: (state) => state.user,
		}),
	},
	methods: {
		...mapActions("user", {
			logout: LOG_OUT,
			fetchUser: FETCH_USER,
		}),
	},
};
</script>
<style>
.wolfia-user-logo {
	margin: 0.5em;
}
</style>
