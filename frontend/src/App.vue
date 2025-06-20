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
	<div id="app" class="has-text-centered">
		<Header class="Header" />
		<main>
			<router-view />
		</main>
		<Footer class="Footer" />
	</div>
</template>

<script>
import { defineAsyncComponent } from "vue";
import { toast } from "bulma-toast";

export default {
	components: {
		Header: defineAsyncComponent(() => import("@/components/Header.vue")),
		Footer: defineAsyncComponent(() => import("@/components/Footer.vue")),
	},
	mounted() {
		this.checkLogin(this.$route);
		this.$router.afterEach((to) => {
			this.checkLogin(to);
		});
	},

	methods: {
		checkLogin: function (route) {
			const loginParam = route.query["login"];
			if (loginParam) {
				this.handleLogin(loginParam);
				this.$router.replace({
					...route,
					query: {
						login: undefined,
					},
				});
			}
		},
		handleLogin: function (loginParam) {
			if (loginParam === "success") {
				toast({
					message: "Login Successful!",
					type: "is-success",
					duration: 3000,
					position: "top-center",
				});
			} else if (loginParam === "failed") {
				toast({
					message: "Looks like something went wrong with your login. Please try again!",
					type: "is-warning",
					duration: 5000,
					position: "top-center",
				});
			} else if (loginParam === "no-consent") {
				toast({
					message: "No consent to process your data.",
					type: "is-info",
					duration: 5000,
					position: "top-center",
				});
			}
		},
	},
};
</script>

<style lang="scss">
@import "node_modules/bulmaswatch/darkly/variables";
//wtf darkly
$size-6: 1rem;
$size-7: 0.85rem;
@import "node_modules/bulma/bulma";
@import "node_modules/bulma-switch/src/sass/index";
$bulmaswatch-import-font: false;
@import "node_modules/bulmaswatch/darkly/overrides";

html,
body {
	//Generic css resets
	box-sizing: border-box;
	margin: 0;
	padding: 0;
	outline: 0;
	overflow: hidden !important;
}

@import "assets/fonts_muli.css";
#app {
	font-family: "Muli", Helvetica, Arial, sans-serif;
	-webkit-font-smoothing: antialiased;
	-moz-osx-font-smoothing: grayscale;

	overflow: auto;
	height: 100vh;

	//The main container needs to be flex, so that the footer can stick to the bottom
	display: flex;
	flex-direction: column;
}

main {
	flex: 1;
	background-color: $grey-dark;
}

/*Source: https://github.com/jgthms/bulma/issues/847 */
@import "node_modules/bulma/sass/utilities/mixins";
.is-loading {
	position: relative;
	pointer-events: none;
	opacity: 0.5;
	min-height: 10em;
	&:after {
		@include loader;
		position: absolute;
		top: calc(50% - 2.5em);
		left: calc(50% - 2.5em);
		width: 5em;
		height: 5em;
		border-width: 0.25em;
	}
}
/* Make cards have the same height when displayed as columns
   Source: https://github.com/jgthms/bulma/issues/218#issuecomment-301706143 */
.card-equal-height {
	display: flex;
	flex-direction: column;
	height: 100%;
}
.card-equal-height .card-footer {
	margin-top: auto;
}
</style>
