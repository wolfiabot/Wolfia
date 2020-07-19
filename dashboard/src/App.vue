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
	<div id="app" class="has-text-centered">
		<Header class="Header" />
		<main>
			<router-view />
		</main>
		<Footer class="Footer" />
	</div>
</template>

<script>
import { ToastProgrammatic as Toast } from "buefy";

export default {
	components: {
		Header: () => import("@/components/Header"),
		Footer: () => import("@/components/Footer"),
	},
	mounted() {
		document.addEventListener("DOMContentLoaded", () => {
			// Get all "navbar-burger" elements
			const $navbarBurgers = Array.prototype.slice.call(document.querySelectorAll(".navbar-burger"), 0);

			// Check if there are any navbar burgers
			if ($navbarBurgers.length > 0) {
				// Add a click event on each of them
				$navbarBurgers.forEach((el) => {
					el.addEventListener("click", () => {
						// Get the target from the "data-target" attribute
						const target = el.dataset.target;
						const $target = document.getElementById(target);

						// Toggle the "is-active" class on both the "navbar-burger" and the "navbar-menu"
						el.classList.toggle("is-active");
						$target.classList.toggle("is-active");
					});
				});
			}
		});

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
				Toast.open({
					message: "Login Successful!",
					type: "is-success",
					duration: 3000,
				});
			} else if (loginParam === "failed") {
				Toast.open({
					message: "Looks like something went wrong with your login. Please try again!",
					type: "is-warning",
					duration: 5000,
				});
			}
		},
	},
};
</script>

<style lang="scss">
@import "node_modules/buefy/dist/buefy";
@import "node_modules/bulmaswatch/darkly/variables";
@import "node_modules/bulma/bulma";
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
//Sticky footer end

/*Source: https://github.com/jgthms/bulma/issues/847 */
@import "~bulma/sass/utilities/mixins";
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
