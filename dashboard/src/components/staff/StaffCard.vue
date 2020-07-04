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
	<div class="card">
		<figure class="card-image">
			<img alt="Staff member logo" class="is-square avatar" :src="member.user.avatarUrl()" />
		</figure>
		<div class="card-content has-text-left">
			<div class="username">
				<strong class="is-size-4">{{ member.user.name }}</strong
				>#{{ member.user.discriminator }}
			</div>
			<div :class="staffBadgeClass">{{ member.renderStaffFunction() }}</div>
			<div v-if="member.slogan !== null" class="is-italic slogan">{{ member.slogan }}</div>
			<div v-if="member.link !== null" class="link">
				<a :href="member.link" target="_blank">{{ member.link }}</a>
			</div>
		</div>
	</div>
</template>

<script>
import { StaffMember } from "@/components/staff/staffmember";

export default {
	name: "StaffCard",
	props: {
		member: StaffMember,
	},
	computed: {
		staffBadgeClass: function () {
			return {
				tag: true,
				dev: this.member.staffFunction === "DEVELOPER",
				mod: this.member.staffFunction === "MODERATOR",
				setupman: this.member.staffFunction === "SETUP_MANAGER",
			};
		},
	},
};
</script>

<style scoped lang="scss">
.link,
.slogan,
.username {
	white-space: normal;
	word-wrap: break-word;
	height: auto;
	flex-shrink: 1;
}
.dev {
	background-color: #3498db;
}
.mod {
	background-color: #e67e22;
}
.setupman {
	background-color: #9b59b6;
}
</style>
