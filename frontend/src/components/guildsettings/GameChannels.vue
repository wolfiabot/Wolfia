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
	<div class="box container">
		<span class="is-size-2">Game Channels</span>
		<div class="field">
			<input
				id="all"
				type="checkbox"
				class="switch is-rounded is-info"
				:checked="isEveryChannelAGameChannel"
				@input="handleAllToggle"
			/>
			<label for="all">Enable All</label>
		</div>
		<ChannelList :channels="guildSettings.channelSettings" @toggle="handleToggle" />
	</div>
</template>

<script>
import { defineAsyncComponent } from "vue";
import { mapActions } from "vuex";
import { SET_GAME_CHANNEL } from "@/components/guildsettings/guild-settings-store";
import { GuildSettings } from "@/components/guildsettings/guild-settings";

export default {
	name: "GameChannels",
	components: {
		ChannelList: defineAsyncComponent(() => import("@/components/guildsettings/ChannelList.vue")),
	},
	props: {
		guildSettings: GuildSettings,
	},
	computed: {
		isEveryChannelAGameChannel: function () {
			return this.guildSettings.channelSettings.every((channel) => channel.isGameChannel);
		},
	},
	methods: {
		...mapActions("guildSettings", {
			setGameChannel: SET_GAME_CHANNEL,
		}),
		handleAllToggle: function (event) {
			const checked = event.target.checked;
			const body = this.guildSettings.channelSettings
				.filter((channel) => channel.isGameChannel !== checked)
				.map((channel) => {
					return {
						channelId: channel.discordId,
						isGameChannel: checked,
					};
				});
			this.setGameChannel({
				guildId: this.guildSettings.discordId,
				body,
			});
		},
		handleToggle: function (channel) {
			this.setGameChannel({
				guildId: this.guildSettings.discordId,
				body: [
					{
						channelId: channel.discordId,
						isGameChannel: !channel.isGameChannel,
					},
				],
			});
		},
	},
};
</script>

<style scoped></style>
