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
	<div class="card">
		<figure class="card-image">
			<img alt="Guild logo" class="is-square" :src="guild.iconUrl()" />
		</figure>
		<div class="card-content has-text-left">
			<div class="guildname">
				<strong class="is-size-4">{{ guild.name }}</strong>
			</div>
		</div>
		<footer class="card-footer">
			<div v-if="!guild.botPresent" class="card-footer-item">
				<a href="https://bot.wolfia.party/invite" target="_blank">Invite</a>
			</div>
			<div v-if="guild.canEdit" class="card-footer-item">
				<a :href="'/guild/' + guild.discordId">Settings</a>
			</div>
		</footer>
	</div>
</template>

<script>
import { Guild } from "@/store/guild";

export default {
	name: "Guild",
	props: {
		guild: Guild,
	},
};
</script>

<style scoped></style>
