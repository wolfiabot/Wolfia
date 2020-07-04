<!--
  - Copyright (C) 2016-2020 the original author or authors
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU Affero General Public License as published
  - by the Free Software Foundation, either version 3 of the License, or
  - (at your option) any later version.
  -
  - This program is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU Affero General Public License for more details.
  -
  - You should have received a copy of the GNU Affero General Public License
  - along with this program.  If not, see <http://www.gnu.org/licenses/>.
  -->

<template>
	<div class="hello">
		<h2 class="subtitle">Hello there, {{ user.name }}#{{ user.discriminator }}!</h2>
		<h1 class="title">{{ msg }}</h1>
		<div class="button is-large" v-on:click="logout">Logout</div>
	</div>
</template>

<script>
import { LOG_OUT } from "@/components/user/user-store";
import { mapActions } from "vuex";

export default {
	name: "HelloWorld",
	props: {
		msg: String,
		user: Object,
	},
	methods: {
		...mapActions("user", {
			logout: LOG_OUT,
		}),
	},
};
</script>
