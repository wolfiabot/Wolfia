<template>
	<div id="app" class="has-text-centered">
		<nav id="nav" class="navbar has-text-weight-bold">
			<div class="navbar-brand"></div>
			<div class="navbar-menu">
				<div class="navbar-start">
					<router-link to="/" class="navbar-item">
						Home
					</router-link>
					<hr class="navbar-divider" />
					<router-link to="/about" class="navbar-item">
						About
					</router-link>
				</div>
			</div>
		</nav>
		<router-view />
	</div>
</template>

<script>
export default {
	mounted() {
		if (process.env.NODE_ENV === "production") {
			// userreport snippet
			window._urq = window._urq || [];
			window._urq.push([
				"initSite",
				"01987d31-0d58-48c6-a4d3-96f2ae42eb14",
			]);
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

#nav {
	padding: 30px;
}
</style>
