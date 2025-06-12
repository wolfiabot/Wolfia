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
	<div class="has-text-left">
		<div v-for="channel in channels" :key="channel.discordId" class="channel">
			<ChannelEntry :channel="channel" @toggle="toggle" class="channel"></ChannelEntry>
		</div>
	</div>
</template>

<script>
import { defineAsyncComponent } from "vue";

export default {
	name: "ChannelList",
	components: {
		ChannelEntry: defineAsyncComponent(() => import("@/components/guildsettings/ChannelEntry.vue")),
	},
	props: {
		channels: Array,
	},
	methods: {
		toggle: function (channel) {
			this.$emit("toggle", channel);
		},
	},
};
</script>

<style scoped>
.channel {
	margin: 0.5em;
}
</style>
