<template>
	<div id="app" class="has-text-centered">
		<nav id="nav" class="navbar has-text-weight-bold">
			<div class="navbar-brand">
				<a class="navbar-item" href="/">
					<img src="https://i.imgur.com/7Ie8tB3.png" alt="Play Werewolf/Mafia in Discord" />
				</a>
			</div>
			<div class="navbar-menu">
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
	},
};
</script>

<style>
#app {
	font-family: "Avenir", Helvetica, Arial, sans-serif;
	-webkit-font-smoothing: antialiased;
	-moz-osx-font-smoothing: grayscale;
}
</style>
