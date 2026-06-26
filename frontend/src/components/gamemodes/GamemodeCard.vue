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
	<div class="card card-equal-height gamemode">
		<div class="card-content has-text-left">
			<h2 class="title has-text-weight-light">{{ gamemode.name }}</h2>
			<!-- description comes from our own backend and only contains role icon markup, see gameText.js -->
			<p class="description" v-html="render(gamemode.description)"></p>

			<div class="mode" v-for="mode in gamemode.modes" :key="mode.name">
				<h3 class="subtitle has-text-weight-light mode-title">
					{{ mode.name }}
					<span class="tag is-link player-count">{{ mode.playerCount }} players</span>
					<span v-if="mode.isDefault" class="tag is-success">default</span>
				</h3>
				<p class="description" v-html="render(mode.description)"></p>
			</div>
		</div>
	</div>
</template>

<script>
import { renderGameText } from "@/components/gameText";

export default {
	name: "GamemodeCard",
	props: {
		gamemode: Object,
	},
	methods: {
		render(text) {
			return renderGameText(text);
		},
	},
};
</script>

<style scoped lang="scss">
.gamemode {
	padding: 1em;
}
.description {
	white-space: pre-line;
	margin-bottom: 1em;
}
.mode {
	margin-top: 1.5em;
	padding-top: 1em;
	border-top: 1px solid rgba(255, 255, 255, 0.1);
}
.mode-title {
	display: flex;
	align-items: center;
	gap: 0.5em;
	flex-wrap: wrap;
	margin-bottom: 0.5em;
}
:deep(.role-icon) {
	height: 1em;
	width: 1em;
	vertical-align: -0.1em;
}
</style>
