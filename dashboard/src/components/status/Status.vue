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
    <div class="is-size-1">Wolfia Shard Status</div>
		<div class="stafflist columns is-centered is-multiline" :class="{ 'is-loading': !shardsLoaded }">
      <Shard />
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
    this.fetchShards()
  },

	computed: {
		...mapState("shards", {
			shard: (state) => [...state.shard].sort((a, b) => a.shard.id - b.shard.id),
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
* {
	border: 1px solid black;
}

.stafflist {
	padding-right: 6em;
	padding-left: 6em;
	width: 100%;
	height: 100%;
}
</style>
