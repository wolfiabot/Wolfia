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
	<div v-if="isAdmin" class="iframe-container">
		<div class="loader-container">
			<div class="is-loading"></div>
			<iframe class="iframe-child" src="/api/togglz/index"></iframe>
		</div>
	</div>
	<GoBack v-else to="/" />
</template>

<script>
import { defineAsyncComponent } from "vue";
import { mapGetters } from "vuex";

export default {
	name: "Togglz",
	components: {
		GoBack: defineAsyncComponent(() => import("@/components/GoBack.vue")),
	},
	computed: {
		...mapGetters("user", ["isAdmin"]),
	},
};
</script>

<style scoped lang="scss">
@import "node_modules/bulmaswatch/darkly/variables";
/*This achieves a full screen size of the iframe*/
.iframe-container {
	height: 100%;
	width: 100%;
	/* The Togglz widget has a light background.
	Setting a lighter one here avoid flickering between dark and light when loading */
	background: $grey-lighter;
}
/*This achieves a loading animation behind the iframe*/
.loader-container {
	position: relative;
	height: 100%;
	.is-loading::after {
		/* Make the loader better visible on the light background */
		border-color: $grey-darker transparent transparent $grey-darker;
	}
}
/*Max out the iframe, it should take up the whole available screen space between header and footer*/
.iframe-child {
	position: absolute;
	top: 0;
	left: 0;
	height: 100%;
	width: 100%;
}
</style>
