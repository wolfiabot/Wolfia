<template>
	<div class="home">
		<img v-if="loaded" alt="User logo" :src="avatarUrl" />
		<img v-else alt="Vue logo" src="../assets/logo.png" />
		<HelloWorld msg="Welcome to Your Vue.js App" :user="user" />
	</div>
</template>

<script>
// @ is an alias to /src
import HelloWorld from "@/components/HelloWorld.vue";

export default {
	name: "home",
	components: {
		HelloWorld
	},
	data() {
		return {
			loaded: false,
			user: {
				discordId: 42,
				name: "mysterious person",
				avatarId: 42
			}
		};
	},

	mounted() {
		this.getUser();
	},

	computed: {
		avatarUrl() {
			const ext = this.user.avatarId.startsWith("a_") ? "gif" : "png";
			return `https://cdn.discordapp.com/avatars/${this.user.discordId}/${this.user.avatarId}.${ext}`;
		}
	},

	methods: {
		async getUser() {
			const response = await fetch("/api/user");
			this.user = await response.json();
			this.loaded = true;
		}
	}
};
</script>
