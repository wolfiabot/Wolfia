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
import Header from "@/components/Header";
import Footer from "@/components/Footer";

export default {
	components: {
		Header,
		Footer,
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
	},
};
</script>

<style lang="scss">
html, body {
	//Generic css resets
	box-sizing: border-box;
	margin: 0;
	padding: 0;
	outline: 0;
	overflow: hidden !important;
}

#app {
	font-family: "Avenir", Helvetica, Arial, sans-serif;
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
</style>
