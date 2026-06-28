<!--
  - Copyright (C) 2016-2026 the original author or authors
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
	<div class="Setup">
		<h1 class="title has-text-weight-light is-size-1">Setup</h1>

		<section class="block content">
			<h2 class="subtitle has-text-weight-light">Wolfia setup</h2>
			<ul>
				<li>Invite Wolfia to your server (see the big button on the home page).</li>
				<li>
					Pick which channels games can run in. Open your server in the
					<router-link :to="{ name: 'dashboard' }">dashboard</router-link>
					and toggle channels under
					<strong>Game Channels</strong>.
				</li>
				<li>
					Set your channel settings: <code>tag cooldown</code> is how often someone can call the tag list, and
					<code>auto-out</code> will automactally remove inactive players from the sign-up list.
				</li>
				<li>Set your game settings. See the Game Modes page for details on each game and mode.</li>
				<li>Start playing!</li>
			</ul>

			<p>A few notes:</p>
			<ul>
				<li>
					Wolfia automatically mutes and unmutes players as necessary to prevent non-players and dead players
					from talking during a game.
				</li>
				<li>
					Wolfia also runs private servers just for the Wolves/Mafia. Players receiving one of those roles
					will also get an invite to such a server in their role PM at the start of the game. You do not need
					to create any channels for Wolves/Mafia.
				</li>
				<li>
					<code>Access role</code> is for playing on private channels. You do not need to configure this
					unless Wolfia asks you to.
				</li>
			</ul>
		</section>

		<section class="block content">
			<h2 class="title has-text-weight-light is-size-3">Permissions</h2>
			<p>
				Wolfia asks for the <strong>Administrator</strong> permission when you invite it. It used to request a
				minimal set instead, but that broke often: Discord changes how permissions and channel overrides work,
				the bot library doesn't always agree with Discord, and the setup tripped up a lot of admins.
				Administrator avoids all of that.
			</p>
			<p>
				If permissions ever break, kick Wolfia and re-invite it with <strong>Administrator</strong> to fix it.
			</p>
			<p>
				You decide where Wolfia plays: use the
				<router-link :to="{ name: 'dashboard' }">dashboard</router-link>
				to choose which channels games run in (see <strong>Game Channels</strong>).
			</p>
		</section>
	</div>
</template>

<script>
export default {
	name: "Setup",
};
</script>

<style scoped lang="scss">
.Setup {
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-top: 2em;
}
.block {
	max-width: 900px;
	width: 100%;
	text-align: left;
	padding-left: 1em;
	padding-right: 1em;

	dt {
		font-weight: 600;
	}
	dd {
		margin-bottom: 0.75em;
	}
}
</style>
