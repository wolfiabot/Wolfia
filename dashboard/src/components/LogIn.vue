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
	<div class="login">
		<p class="is-size-1">{{ randomGreeting() }}</p>
		<p class="is-size-2">Welcome to the Wolfia Bot Dashboard</p>
		<a class="button is-large is-link is-outlined" href="/public/login">
			Log In With Discord
		</a>
	</div>
</template>

<script>
export default {
	name: "LogIn",
	props: {},
	methods: {
		randomGreeting: function () {
			return greetings[Math.floor(Math.random() * greetings.length)];
		},
	},
};
const greetings = [
	"Ahoy!",
	"Aloha!",
	"Bonjour!",
	"Cheerio!",
	"Ciao!",
	"Good to see you!",
	"Guten Tag!",
	"Greetings!",
	"Howdy, partner!",
	"Hello, sunshine!",
	"Hello, stranger!",
	"Henlo!",
	"Hey there!",
	"Heyyy!",
	"Halo!",
	"Hi!",
	"Hiya!",
	"Hola!",
	"Konnichi wa!",
	"Long time no see!",
	"Namaste!",
	"Nice to meet you!",
	"Привет!",
	"Salutations!",
	"Salaam alaikum!",
	"Shalom!",
	"'Sup nerd!",
	"There you are!",
	"Well met!",
	"Well, look at you!",
	"Why, hello there!",
	"Yo!",
	"[Hat tip]",
];
</script>

<style scoped>
.login {
	margin: 2em;
}
.button {
	margin-top: 2em;
}
</style>
