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
	<div class="Dashboard">
		<div v-if="loading" class="is-loading"></div>
		<LogIn v-else-if="!userLoaded" />
		<div v-else>
			<GuildList v-if="!guildId" />
			<GuildSettings v-else :guild-id="guildId" />
		</div>
	</div>
</template>

<script>
import { defineAsyncComponent } from "vue";
import { mapActions, mapState } from "vuex";
import { FETCH_USER } from "@/components/user/user-store";

export default {
	name: "Dashboard",
	components: {
		GuildList: defineAsyncComponent(() => import("@/components/dashboard/GuildList.vue")),
		GuildSettings: defineAsyncComponent(() => import("@/components/guildsettings/GuildSettings.vue")),
		LogIn: defineAsyncComponent(() => import("@/components/LogIn.vue")),
	},
	props: {
		guildId: String,
	},
	mounted() {
		this.fetchUser();
	},
	computed: {
		...mapState("user", {
			loading: (state) => !state.userLoaded && state.userLoading,
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

<style scoped lang="scss">
.Dashboard {
	height: calc(100% - 2em);
	margin-top: 2em;
}
</style>
