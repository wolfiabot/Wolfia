<template>
	<div class="home">
		<img v-if="isUserLoaded" alt="User logo" :src="avatarUrl" />
		<HelloWorld
			v-if="isUserLoaded"
			msg="Welcome to Your Vue.js App"
			:user="getUser"
		/>
		<LogIn v-else />
	</div>
</template>

<script>
import HelloWorld from "@/components/HelloWorld.vue";
import LogIn from "@/components/LogIn";

export default {
	name: "home",
	components: {
		HelloWorld,
		LogIn
	},

	mounted() {
		this.loadUser();
	},

	computed: {
		isUserLoaded() {
			return this.$store.state.userLoaded;
		},
		getUser() {
			return this.$store.state.user;
		},
		avatarUrl() {
			let user = this.$store.state.user;
			const ext = user.avatarId.startsWith("a_") ? "gif" : "png";
			return `https://cdn.discordapp.com/avatars/${user.discordId}/${user.avatarId}.${ext}`;
		}
	},

	methods: {
		async loadUser() {
			const response = await fetch("/api/user");
			if (response.status === 200) {
				let user = await response.json();
				this.$store.commit("logIn", user);
			}
		}
	}
};
</script>
