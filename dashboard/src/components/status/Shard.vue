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
	<div class="Shard is-size-3" :class="shardStatusClass">
		{{ this.shard.id }}
	</div>
</template>

<script>
import { Shard } from "@/components/status/shard.js";

export default {
	name: "Shard",
	props: {
		shard: Shard,
	},
	computed: {
		shardStatusClass: function () {
			return {
				online: this.shard.getShardStatus() === "Online",
				connecting: this.shard.getShardStatus() === "Connecting",
				offline: this.shard.getShardStatus() === "Offline",
			};
		},
	},
};
</script>

<style scoped lang="scss">
@import "node_modules/bulmaswatch/darkly/variables";

.Shard {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 2em;
	height: 2em;
	margin: 0.5em;
	border-radius: 0.1em;
}

.online {
	background-color: $green;
}

.connecting {
	background-color: $yellow;
}

.offline {
	background-color: $red;
}
</style>
