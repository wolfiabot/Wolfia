<template>
	<div id="app" class="has-text-centered">
		<nav id="nav" class="navbar has-text-weight-bold">
			<div class="navbar-brand">
				<a class="navbar-item" href="/">
					<img src="https://i.imgur.com/7Ie8tB3.png" alt="Play Werewolf/Mafia in Discord" />
				</a>
				<a role="button" class="navbar-burger" data-target="navMenu" aria-label="menu" aria-expanded="false">
					<span aria-hidden="true"></span>
					<span aria-hidden="true"></span>
					<span aria-hidden="true"></span>
				</a>
			</div>
			<div class="navbar-menu" id="navMenu">
				<div class="navbar-start">
					<router-link to="/" class="navbar-item">
						Home
					</router-link>
					<hr class="navbar-divider" />
					<router-link to="/about" class="navbar-item">
						About
					</router-link>
					<router-link to="/team" class="navbar-item">
						Team
					</router-link>
					<router-link to="/dashboard" class="navbar-item">
						Dashboard
					</router-link>
				</div>
				<div class="navbar-end">
					<UserNav></UserNav>
				</div>
			</div>
		</nav>
		<router-view />
	</div>
</template>

<script>
import UserNav from "@/components/UserNav";

export default {
	components: {
		UserNav,
	},
	mounted() {
		if (process.env.NODE_ENV === "production") {
			// userreport snippet
			window._urq = window._urq || [];
			window._urq.push(["initSite", "01987d31-0d58-48c6-a4d3-96f2ae42eb14"]);
			(function () {
				const ur = document.createElement("script");
				ur.type = "text/javascript";
				ur.async = true;
				ur.src =
					document.location.protocol === "https:"
						? "https://cdn.userreport.com/userreport.js"
						: "http://cdn.userreport.com/userreport.js";
				const s = document.getElementsByTagName("script")[0];
				s.parentNode.insertBefore(ur, s);
			})();
		}

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
#app {
	font-family: "Avenir", Helvetica, Arial, sans-serif;
	-webkit-font-smoothing: antialiased;
	-moz-osx-font-smoothing: grayscale;
}

/*Source: https://github.com/jgthms/bulma/issues/847*/
@import "~bulma/sass/utilities/mixins";
.is-loading {
	position: relative;
	pointer-events: none;
	opacity: 0.5;
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
