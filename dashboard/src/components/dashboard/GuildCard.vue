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
	<div class="card guildcard">
		<figure class="card-image">
			<img alt="Guild logo" class="is-square" :src="guild.iconUrl()" />
		</figure>
		<div class="card-content">
			<div class="guildname">
				<strong class="is-size-3">{{ guild.name }}</strong>
			</div>
		</div>
		<footer class="card-footer is-size-4">
			<a v-if="!guild.botPresent" class="card-footer-item" href="https://bot.wolfia.party/invite" target="_blank">
				Invite
			</a>
			<router-link v-if="guild.canEdit" class="card-footer-item" :to="'/dashboard/' + guild.discordId">
				Settings
			</router-link>
		</footer>
	</div>
</template>

<script>
import { Guild } from "@/components/guild/guild";

export default {
	name: "GuildCard",
	props: {
		guild: Guild,
	},
};
</script>

<style scoped lang="scss">
@import "node_modules/bulmaswatch/darkly/variables";
.card-footer-item {
	background-image: linear-gradient(to right, rgba(255, 255, 255, 0) 50%, $primary 50%);
	background-position: -0% 0;
	background-size: 200% auto;
	transition: background-position 0.5s ease-out;
}
.card-footer-item:hover {
	background-position: -99.99% 0;
}
</style>
