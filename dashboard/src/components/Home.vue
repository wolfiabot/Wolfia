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
	<div class="container">
		<div v-if="userLoaded">
			<img alt="User logo" :src="user.avatarUrl()" />
			<HelloWorld msg="Welcome to Your Vue.js App" :user="user" />
		</div>
		<LogIn v-else />
	</div>
</template>

<script>
import HelloWorld from "@/components/HelloWorld.vue";
import LogIn from "@/components/LogIn";
import { FETCH_USER } from "@/components/user/user-store";
import { mapActions, mapState } from "vuex";

export default {
	name: "home",
	components: {
		HelloWorld,
		LogIn,
	},

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
			fetchUser: FETCH_USER,
		}),
	},
};
</script>
