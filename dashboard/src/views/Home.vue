<template>
	<div class="container">
		<img v-if="userLoaded" alt="User logo" :src="user.avatarUrl()" />
		<HelloWorld
			v-if="userLoaded"
			msg="Welcome to Your Vue.js App"
			:user="user"
		/>
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
		LogIn
	},

	mounted() {
		this.fetchUser();
	},

	computed: {
		...mapState({
			userLoaded: state => state.userLoaded,
			user: state => state.user
		})
	},

	methods: {
		...mapActions({ fetchUser: FETCH_USER })
	}
};
</script>
