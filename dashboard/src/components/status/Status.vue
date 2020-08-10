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
	<div class="Status">
		<h1 class="statusHeader title has-text-weight-light is-size-1">
			Wolfia Shard Status
		</h1>

		<div class="statusContent" :class="{ 'is-loading': !shardsLoaded }">
			<div class="is-size-3">
				{{ shards.filter((shard) => shard.status === "CONNECTED").length }}
				/
				{{ shards.length }}
				online
			</div>
			<div class="shardList card">
				<Shard v-for="shard in shards" :key="shard.id" :shard="shard" />
			</div>
		</div>

		<div class="statusFooter">
			<p>
				If you notice any issues, do not hesitate to reach out to us on our
				<a href="https://discord.gg/nvcfX3q" target="_blank" rel="noopener noreferrer">Discord server</a>.
			</p>
		</div>
	</div>
</template>

<script>
import { mapActions, mapState } from "vuex";
import { FETCH_SHARDS } from "@/components/status/shard-store";

export default {
	name: "Status",
	components: {
		Shard: () => import("@/components/status/Shard.vue"),
	},
	mounted() {
		this.fetchShards();
	},

	computed: {
		...mapState("shards", {
			shards: (state) => [...state.shards].sort((a, b) => a.id - b.id),
			shardsLoaded: (state) => state.shardsLoaded,
		}),
	},
	methods: {
		...mapActions("shards", {
			fetchShards: FETCH_SHARDS,
		}),
	},
};
</script>

<style scoped lang="scss">
.Status {
	display: flex;
	flex-direction: column;
	justify-content: space-between;
	align-items: center;
	margin-top: 2em;
	height: 80%;
}

.statusHeader,
.statusFooter,
.statusContent {
	display: flex;
	align-items: center;
}

.statusContent {
	display: flex;
	flex-direction: column;
}

.shardList {
	display: flex;
	flex-wrap: wrap;
	align-items: flex-start;
	width: 90%;
}
</style>
