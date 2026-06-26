<!--
  - Copyright (C) 2016-2026 the original author or authors
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
	<div class="Credits">
		<h1 class="title has-text-weight-light is-size-1">Special mentions</h1>
		<section class="block content">
			<ul>
				<li>
					Written in Java using the excellent
					<a href="https://github.com/DV8FromTheWorld/JDA" target="_blank" rel="noopener noreferrer"
						>JDA (Java Discord API)</a
					>. They maintain a super helpful crowd in their Discord guild.
				</li>
				<li>
					Several functions and architectural decisions inspired by and/or plain copy pasta'd from
					<a href="https://frederikam.com/" target="_blank" rel="noopener noreferrer">Frederikam's</a> music
					bot <a href="https://fredboat.com/" target="_blank" rel="noopener noreferrer">FredBoat</a>.
				</li>
				<li>
					Thanks to the folks at
					<a href="https://www.mafiauniverse.com/forums/" target="_blank" rel="noopener noreferrer"
						>Mafia Universe</a
					>
					and their Discord guild for helping testing and refining the initial version.
				</li>
				<li>
					Created with lots of <img class="coffee" src="/img/coffee.svg" alt="coffee" /> by
					<a :href="sourceCodeLink + '/graphs/contributors?all=1'" target="_blank" rel="noopener noreferrer"
						>a pack of wolves</a
					>.
				</li>
			</ul>
		</section>
	</div>
</template>

<script>
export default {
	name: "Credits",
	data: function () {
		return {
			sourceCodeLink: import.meta.env.VITE_APP_SOURCE_CODE_LINK,
		};
	},
};
</script>

<style scoped lang="scss">
.Credits {
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-top: 2em;
}

.block {
	max-width: 800px;
	width: 100%;
	text-align: left;
	padding-left: 1em;
	padding-right: 1em;

	.coffee {
		height: 1.25em;
		vertical-align: -0.3em;
	}
}
</style>
