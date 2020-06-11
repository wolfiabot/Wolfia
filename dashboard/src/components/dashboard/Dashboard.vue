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
	<div class="container">
		<GuildList v-if="userLoaded" />
		<LogIn v-else />
	</div>
</template>

<script>
import GuildList from "@/components/dashboard/GuildList";
import LogIn from "@/components/LogIn";
import { mapActions, mapState } from "vuex";
import { FETCH_USER } from "@/store/action-types";

export default {
	name: "Dashboard",
	components: {
		GuildList,
		LogIn,
	},
	mounted() {
		this.fetchUser();
	},
	computed: {
		...mapState({
			userLoaded: (state) => state.userLoaded,
			user: (state) => state.user,
		}),
	},
	methods: {
		...mapActions({ fetchUser: FETCH_USER }),
	},
};
</script>

<style scoped></style>
