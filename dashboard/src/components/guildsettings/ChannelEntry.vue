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
	<div class="level is-mobile">
		<div class="level-left">
			<span class="level-item">#{{ channel.name }}</span>
		</div>
		<div class="level-right">
			<div class="level-item field">
				<b-switch :value="channel.isGameChannel" @click.prevent.native="toggle"></b-switch>
			</div>
		</div>
	</div>
</template>

<script>
import { ChannelSettings } from "@/components/guildsettings/channel-settings";

export default {
	name: "ChannelEntry",
	props: {
		channel: ChannelSettings,
	},
	methods: {
		toggle() {
			this.$emit("toggle", this.channel);
		},
	},
};
</script>

<style scoped></style>
