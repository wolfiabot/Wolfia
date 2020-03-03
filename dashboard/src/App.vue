<template>
		<div id="app">
			<div id="nav">
				<router-link to="/">Home</router-link> |
				<router-link to="/about">About</router-link>
			</div>
			<router-view/>
		</div>
</template>

<script>
	export default {
		mounted() {
			//userreport snippet
			window._urq = window._urq || [];
			window._urq.push(['initSite', '01987d31-0d58-48c6-a4d3-96f2ae42eb14']);
			(function () {
				const ur = document.createElement('script');
				ur.type = 'text/javascript';
				ur.async = true;
				ur.src = ('https:' === document.location.protocol ? 'https://cdn.userreport.com/userreport.js' : 'http://cdn.userreport.com/userreport.js');
				const s = document.getElementsByTagName('script')[0];
				s.parentNode.insertBefore(ur, s);
			})();
		}
	}
</script>

<style>
	body {
		background-color: #001830;
		height: 100vh;
        color: beige;
	}

	#app {
		font-family: 'Avenir', Helvetica, Arial, sans-serif;
		-webkit-font-smoothing: antialiased;
		-moz-osx-font-smoothing: grayscale;
		text-align: center;
	}

	#nav {
		padding: 30px;
	}

	#nav a {
		font-weight: bold;
        color: beige;
	}

	#nav a.router-link-exact-active {
		color: khaki;
	}
</style>
