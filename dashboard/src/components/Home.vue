<template>
	<div class="container">
		<div v-if="userLoaded">
			<img alt="User logo" :src="user.avatarUrl()" />
			<HelloWorld msg="Welcome to Your Vue.js App" :user="user" />
		</div>
		<LogIn v-else />
	</div>
</template>

<script>
import HelloWorld from "@/components/HelloWorld.vue";
import LogIn from "@/components/LogIn";
import { FETCH_USER } from "@/store/action-types";
import { mapActions, mapState } from "vuex";

export default {
	name: "home",
	components: {
		HelloWorld,
		LogIn,
	},

	mounted() {
		this.fetchUser();
	},

	computed: {
		...mapState({
			userLoaded: (state) => state.userLoaded,
			user: (state) => state.user,
		}),
	},

	methods: {
		...mapActions({ fetchUser: FETCH_USER }),
	},
};
</script>
