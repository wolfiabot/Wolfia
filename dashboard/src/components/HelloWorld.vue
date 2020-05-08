<template>
	<div class="hello">
		<h2 class="subtitle">
			Hello there, {{ user.name }}#{{ user.discriminator }}!
		</h2>
		<h1 class="title">{{ msg }}</h1>
		<div class="button is-large" v-on:click="logout">Logout</div>
	</div>
</template>

<script>
import { LOG_OUT } from "@/store/action-types";
import { mapActions } from "vuex";

export default {
	name: "HelloWorld",
	props: {
		msg: String,
		user: Object,
	},
	methods: {
		...mapActions({ logout: LOG_OUT }),
	},
};
</script>
