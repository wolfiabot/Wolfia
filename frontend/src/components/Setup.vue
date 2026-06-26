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
				<li>Revoke Wolfia's write permission on channels where games should not be played.</li>
				<li>
					Set your channel settings: <code>tag cooldown</code> is how often someone can call the tag list,
					and <code>auto-out</code> will automactally remove inactive players from the sign-up list.
				</li>
				<li>Set your game settings. See the Game Modes page for details on each game and mode.</li>
				<li>Start playing!</li>
			</ul>

			<p>A few notes:</p>
			<ul>
				<li>
					Wolfia automatically mutes and unmutes players as necessary to prevent non-players and dead
					players from talking during a game.
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
				Wolfia requires some permissions to run games flawlessly. It will try to automatically set itself up
				with what it needs, or ask you to give it the required permissions to do so.
			</p>
			<p>
				Using the official invite link, or by running the <code>w.help</code> command to invite Wolfia to your
				server, will have it request the required permissions. If permissions on your server are broken for
				Wolfia or the required ones have been updated, kicking and reinviting should restore permissions to a
				working state.
			</p>
			<p>
				Nevertheless, and also for the control freaks among us, here is a comprehensive list of what is
				required and why:
			</p>

			<h3 class="subtitle has-text-weight-light is-size-5">Required</h3>
			<dl>
				<dt>Read Message History</dt>
				<dd>Edit it's own messages after they have been sent</dd>
				<dt>Use External Emojis</dt>
				<dd>
					The standarized emojis are not enough to display everything clearly, so Wolfia packs a bunch of
					custom ones
				</dd>
				<dt>Embed Links</dt>
				<dd>Formatting of messages</dd>
				<dt>Add Reactions</dt>
				<dd>Display vote counts</dd>
				<dt>Manage Messages</dt>
				<dd>Clearing reactions off of votecounts</dd>
				<dt>Manage Roles</dt>
				<dd>
					Moderate the game channel with permission overrides (and just that; Wolfia does not create or
					delete any roles for the players)
				</dd>
			</dl>

			<h3 class="subtitle has-text-weight-light is-size-5">Optional</h3>
			<dl>
				<dt>Create Instant Invite</dt>
				<dd>
					Adds invites to the channel where the game is running to role pms and private chat servers which
					makes for a smooth navigation for players during the game.
				</dd>
			</dl>
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
