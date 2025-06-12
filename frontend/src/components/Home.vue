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
	<div class="Home">
		<h1 class="title has-text-weight-light is-size-1-desktop">Play Werewolf & Mafia games on Discord!</h1>
		<div class="buttons">
			<div class="column is-one-quarter">
				<router-link class="button is-large is-link" id="dashboard_button" to="/dashboard"
					>Add to Discord</router-link
				>
			</div>
			<div class="column is-one-quarter">
				<router-link class="button is-large is-info" id="documentation_button" to="/commands"
					>See Commands</router-link
				>
			</div>
		</div>
	</div>
</template>

<script>
export default {
	name: "home",
};
</script>
<style scoped lang="scss">
.Home {
	display: flex;
	flex-direction: column;
	justify-content: center;
	height: 100%;

	.buttons {
		width: 100%;
		display: flex;
		justify-content: center;
	}
}
</style>
