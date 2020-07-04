<template>
	<nav id="nav" class="navbar has-text-weight-bold">
		<div class="navbar-brand">
			<router-link class="navbar-item" to="/">
				<img src="../assets/logo.png" alt="Play Werewolf/Mafia in Discord" />
			</router-link>
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
				<router-link to="/ideas" class="navbar-item">
					Ideas
				</router-link>
				<router-link to="/togglz" class="navbar-item" v-if="isAdmin">
					Feature Flags
				</router-link>
			</div>
			<div class="navbar-end">
				<UserNav></UserNav>
			</div>
		</div>
	</nav>
</template>

<script>
import UserNav from "@/components/user/UserNav";
import { mapGetters } from "vuex";

export default {
	components: {
		UserNav,
	},
	computed: {
		...mapGetters("user", ["isAdmin"]),
	},
};
</script>

<style></style>
