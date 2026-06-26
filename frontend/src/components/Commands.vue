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
	<div class="Commands">
		<h1 class="title has-text-weight-light is-size-1">Commands</h1>

		<div class="categories" :class="{ 'is-loading': !commandsLoaded }">
			<section class="category" v-for="category in categories" :key="category.name">
				<h2 class="subtitle has-text-weight-light">{{ category.name }}</h2>
				<table class="table is-fullwidth is-striped is-hoverable">
					<thead>
						<tr>
							<th>Command</th>
							<th>What it does</th>
							<th>Aliases</th>
						</tr>
					</thead>
					<tbody>
						<tr v-for="command in category.commands" :key="command.trigger">
							<td class="trigger">
								<code>{{ command.trigger }}</code>
								<span v-if="command.xmasOnly" class="tag is-info xmas">xmas only</span>
							</td>
							<td>
								<div>{{ command.description }}</div>
								<div v-if="command.examples.length > 0" class="examples">
									<code v-for="example in command.examples" :key="example">{{ example }}</code>
								</div>
							</td>
							<td class="aliases">
								<code v-for="alias in command.aliases" :key="alias">{{ alias }}</code>
								<span v-if="command.aliases.length === 0">—</span>
							</td>
						</tr>
					</tbody>
				</table>
			</section>
		</div>
	</div>
</template>

<script>
import { mapActions, mapState } from "vuex";
import { FETCH_COMMANDS } from "@/components/commands/commands-store";

export default {
	name: "Commands",
	mounted() {
		this.fetchCommands();
	},
	computed: {
		...mapState("commands", {
			categories: (state) => state.categories,
			commandsLoaded: (state) => state.commandsLoaded,
		}),
	},
	methods: {
		...mapActions("commands", {
			fetchCommands: FETCH_COMMANDS,
		}),
	},
};
</script>

<style scoped lang="scss">
.Commands {
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-top: 2em;
	padding-left: 1em;
	padding-right: 1em;
}
.categories {
	max-width: 900px;
	width: 100%;
}
.category {
	margin-bottom: 2.5em;
	text-align: left;
}
.trigger {
	white-space: nowrap;
}
.examples {
	margin-top: 0.4em;
	display: flex;
	flex-direction: column;
	align-items: flex-start;
	gap: 0.25em;
}
.aliases code {
	margin-right: 0.35em;
}
.xmas {
	margin-left: 0.5em;
}
</style>
