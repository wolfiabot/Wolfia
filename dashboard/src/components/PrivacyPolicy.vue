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
	<div class="columns">
		<div class="column is-half is-full-touch is-offset-one-quarter-desktop has-text-left">
			<h1 class="title">Wolfia Privacy Policy</h1>
			<p>Wolfia is an Open Source project.</p>
			<p>
				The source code can be found <a :href="sourceCodeLink">here</a>. Anyone can host a copy of Wolfia
				provided they adhere to the license and make the source code available to end users.
			</p>
			<p>
				This specific instance on <a :href="host">{{ host }}</a> is hosted by
				<a :href="ownerLink">{{ owner }}</a> and will further be referred to as "this Wolfia instance".
			</p>
			<p>
				This privacy policy will explain how this Wolfia instance uses the personal data it collects from you
				when you use this website or play games with it on Discord.
			</p>

			<h2 class="subtitle">What data do we collect?</h2>
			<ul>
				<li>- Your Discord id, name, nicknames and avatar</li>
				<li>- Statistics about games and commands used</li>
				<li>- The last time you were active on Discord</li>
			</ul>

			<h2 class="subtitle">How do we collect your data?</h2>
			<p>
				We receive this data from the Discord API when an admin of a Discord server adds Wolfia to a server you
				are a member of as well, or you log into the website using Discord OAuth2. We collect statistics when
				you interact with this Wolfia instance via commands on Discord and play games with it.
			</p>

			<h2 class="subtitle">How will we use your data?</h2>
			<p>
				The data is used to
			</p>
			<br />
			<ul>
				<li>- display your statistics</li>
				<li>- display replays of games</li>
				<li>- display your logged-in status on the website</li>
				<li>- remove users from signed up games when they go inactive</li>
			</ul>

			<h2 class="subtitle">How do we store your data?</h2>
			<p>
				The data is stored in databases such as PostgreSQL and Redis. The current datacenter location is
				{{ serverLocation }}. Servers running the software as well as the databases are secured with industry
				standard security measures.
			</p>

			<h2 class="subtitle">We keep the data as long as it is required</h2>
			<ul>
				<li>- Game statistics are kept forever</li>
				<li>
					- Last activity is kept until timing out and being removed from signed up games (20 minutes by
					default)
				</li>
				<li>- Sessions on the website are kept until you log yourself out or a timeout of one year</li>
			</ul>

			<h2 class="subtitle">What are your data protection rights?</h2>
			<p>
				This Wolfia instance would like to make sure you are fully aware of all of your data protection rights.
				Every user is entitled to the following:
			</p>
			<br />
			<ul>
				<li>
					<strong>The right to access</strong> – You have the right to request this Wolfia instance for copies
					of your personal data. We may charge you a small fee for this service.
				</li>
				<li>
					<strong>The right to rectification</strong> – You have the right to request that this Wolfia
					instance correct any information you believe is inaccurate. You also have the right to request
					Wolfia to complete the information you believe is incomplete.
				</li>
				<li>
					<strong>The right to erasure</strong> – You have the right to request that this Wolfia instance
					erase your personal data, under certain conditions.
				</li>
				<li>
					<strong>The right to restrict processing</strong> – You have the right to request that this Wolfia
					instance restrict the processing of your personal data, under certain conditions.
				</li>
				<li>
					<strong>The right to object to processing</strong> – You have the right to object to this Wolfia’s
					instance processing of your personal data, under certain conditions.
				</li>
				<li>
					<strong>The right to data portability</strong> – You have the right to request that this Wolfia
					instance transfer the data that we have collected to another organization, or directly to you, under
					certain conditions. If you make a request, we have one month to respond to you.
				</li>
			</ul>
			<br />
			<p>
				If you would like to exercise any of these rights, please contact us at our email
				<a :href="`mailto:${mail}`">{{ mail }}</a> or join <a :href="invite">our support server</a>.
			</p>

			<h2 class="subtitle">Cookies</h2>
			<p>This Wolfia instance uses cookies to keep you signed in.</p>

			<h2 class="subtitle">How to manage cookies</h2>
			<p>
				You can set your browser not to accept cookies, and remove cookies from your browser. However, some of
				our website features may not function as a result.
			</p>

			<h2 class="subtitle">Privacy policies of other websites</h2>
			<p>
				This Wolfia's instance website contains links to other websites. Our privacy policy applies only to our
				website, so if you click on a link to another website, you should read their privacy policy.
			</p>

			<h2 class="subtitle">Changes to our privacy policy</h2>
			<p>
				This Wolfia instance keeps its privacy policy under regular review and places any updates on this web
				page. This privacy policy was last updated on 2nd August 2020.
			</p>

			<h2 class="subtitle">How to contact us</h2>
			<p>
				If you have any questions about this Wolfia’s instance privacy policy, the data we hold on you, or you
				would like to exercise one of your data protection rights, please do not hesitate to contact us: Email
				us at <a :href="`mailto:${mail}`">{{ mail }}</a> or join <a :href="invite">our support server</a>.
			</p>
			<br />
			<p>This Privacy Policy has been adapted from <a href="https://gdpr.eu/privacy-notice">GDPR.EU</a></p>
		</div>
	</div>
</template>

<script>
export default {
	name: "PrivacyPolicy",
	data: function () {
		return {
			sourceCodeLink: process.env.VUE_APP_PP_SOURCE_CODE_LINK,
			host: `${window.location.protocol}//${window.location.hostname}`,
			owner: process.env.VUE_APP_PP_OWNER,
			ownerLink: process.env.VUE_APP_PP_OWNER_LINK,
			mail: `hello@${window.location.hostname}`,
			serverLocation: process.env.VUE_APP_PP_SERVER_LOCATION,
			invite: process.env.VUE_APP_SUPPORT_INVITE,
		};
	},
};
</script>

<style scoped>
.title,
.subtitle {
	margin-top: 1.5em;
}
</style>
