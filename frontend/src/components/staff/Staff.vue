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
	<div class="Staff">
		<h1 class="title has-text-weight-light is-size-1">The team behind Wolfia</h1>
		<div class="stafflist columns is-centered is-multiline" :class="{ 'is-loading': !staffLoaded }">
			<div
				class="column is-half-tablet is-one-third-desktop"
				v-for="member in staff"
				:key="member.user.discordId"
			>
				<StaffCard :member="member" class="staffcard" />
			</div>
		</div>
	</div>
</template>

<script>
import { defineAsyncComponent } from "vue";
import { mapActions, mapState } from "vuex";
import { FETCH_STAFF } from "@/components/staff/staff-store";

export default {
	name: "Staff",
	components: {
		StaffCard: defineAsyncComponent(() => import("@/components/staff/StaffCard")),
	},
	mounted() {
		this.fetchStaff();
	},

	computed: {
		...mapState("staff", {
			staff: (state) => [...state.staff].sort((a, b) => a.user.discordId - b.user.discordId),
			staffLoaded: (state) => state.staffLoaded,
		}),
	},
	methods: {
		...mapActions("staff", {
			fetchStaff: FETCH_STAFF,
		}),
	},
};
</script>

<style scoped lang="scss">
.Staff {
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-top: 2em;
}
.stafflist {
	padding-right: 6em;
	padding-left: 6em;
	width: 100%;
}
.staffcard {
	padding: 1em;
}
</style>
