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
		<div class="is-size-1 statusHeader">Wolfia Shard Status</div>
		<div class="shardList card" :class="{ 'is-loading': !shardsLoaded }">
			<Shard v-for="shard in shards" :key="shard.id" :shard="shard" />
		</div>

		<div class="statusNote">
			If you notice any issues, do not hesitate to notify staff memebers [link here]
		</div>
	</div>
</template>

<script>
import { mapActions, mapState } from "vuex";
import { FETCH_SHARDS } from "@/components/status/shard-store";
import Shard from "@/components/status/Shard.vue";

export default {
	name: "Status",
	components: {
		Shard,
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
// * {
// 	border: 1px solid black;
// }

.Status {
	display: flex;
	flex-direction: column;
	justify-content: space-around;
	align-items: center;
	height: 100%;
}

.statusHeader,
.statusNote {
	display: flex;
	align-items: center;
}

.statusHeader {
	flex-grow: 1;
}

.shardList {
	display: flex;
	flex-wrap: wrap;
	justify-content: flex-start;
	align-items: flex-start;
	flex-grow: 4;
	width: 90%;
}

.statusNote {
	flex-grow: 1;
}
</style>
