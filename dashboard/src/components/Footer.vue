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
	<footer class="level">
		<div class="level-item">
			<a :href="sourceCodeLink" target="_blank" rel="noopener noreferrer">Source Code</a>
		</div>
		<div class="level-item">
			<p>
				Created with lots of
				<img src="https://discord.com/assets/25c09e6fde32411da2b0da00f5cb9c84.svg" alt="coffee" /> by
				<a :href="sourceCodeLink + '/graphs/contributors'" target="_blank" rel="noopener noreferrer"
					>a pack of wolves</a
				>
			</p>
		</div>
		<div class="level-item">
			<router-link to="/privacy">Privacy Policy</router-link>
		</div>
	</footer>
</template>

<script>
export default {
	name: "Footer",
	data: function () {
		return {
			sourceCodeLink: process.env.VUE_APP_PP_SOURCE_CODE_LINK,
		};
	},
};
</script>

<style scoped lang="scss">
footer {
	font-size: 1em;
	padding: 0.25em;

	img {
		height: 1.25em;
	}
}
</style>
