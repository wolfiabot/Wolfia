---
layout: default
---

{: .invite-image.center}
[![Invite the Wolfia bot to your server.](http://i.imgur.com/qEWSU6D.png)](https://discordapp.com/oauth2/authorize?client_id=306583221565521921&response_type=code&redirect_uri=https%3A%2F%2Fdiscordapp.com%2Finvite%2FnvcfX3q&permissions=268787777&scope=bot)

{:.center}
[Click the banner above to invite Wolfia](https://discordapp.com/oauth2/authorize?client_id=306583221565521921&response_type=code&redirect_uri=https%3A%2F%2Fdiscordapp.com%2Finvite%2FnvcfX3q&permissions=268787777&scope=bot)

* * *

<div class="flex-parent">
    <div class="flex-child">
        <h1 id="join-the-wolfia-lounge">Join the Wolfia Lounge</h1>
        <ul>
            <li>Play games</li>
            <li>Get support</li>
            <li>Leave feedback</li>
            <li>Get notified of updates</li>
            <li>Vote on the roadmap of Wolfia</li>
        </ul>
        <p>
            <a href="https://discord.gg/nvcfX3q">
                <img src="https://discordapp.com/api/guilds/315944983754571796/embed.png?style=banner2" alt="Join Wolfia Lounge">
            </a>
        </p>
    </div>
    <div class="flex-child">
        <p class="logo-image right">
            <a href="https://discord.gg/nvcfX3q">
                <img src="https://i.imgur.com/7Ie8tB3.png" alt="Wolfia logo">
            </a>
        </p>
    </div>
</div>

* * *

# β version

Wolfia is currently in **beta** status, which means the following:
- A small selection of games and modes is supported. More stuff is being worked on.
- Bugs during games might happen. Please drop by the Wolfia Lounge to help sort these out.
- Data and commands might change without notice.
- Output and formatting of messages might look a bit rough.

Uptime over last 7 days:  
[![Uptime Robot ratio (7 days)](https://img.shields.io/uptimerobot/ratio/7/m778927695-04e353308ad0d207bd0489b8.svg?style=flat-square)]()

* * *

# Commands

### Starting a game

Command    | What it does                                                 | Example usage / aliases
---------- | ------------------------------------------------------------ | -------------
`w.in`     | sign up for a game                                           | `w.in`
`w.out`    | drop from the sign up list; moderators can out other players | `w.out` `w.out @player`
`w.setup`  | set up the game in the current channel                       | `w.setup daylength 5` `w.setup game mafia` `w.setup mode classic`
`w.start`  | start a game                                                 | `w.start`
`w.rolepm` | ask the bot to resend you your rolepm for the ongoing game   | `w.rpm`
`w.status` | show the current status of an ongoing game or sign ups       | `w.st`


### Game actions

Command             | What it does                                        | Example usage / aliases
------------------- | --------------------------------------------------- | -------------
`w.check`           | check a players alignment                           | `w.check 6`
`w.shoot`           | shoot another player                                | `w.s @player`
`w.unvote`          | unvote                                              | `w.u`
`w.vote`            | vote a player for lynch                             | `w.v @player`
`w.votecount`       | post the current votecount                          | `w.vc`
`w.nightkill`       | vote a player for nightkill (in mafia chat only)    | `w.nk @player`
`w.hohoho`          | give a player a present (xmas mode only)            | `w.ho 5`
`w.items`           | list the items that you own (xmas mode only)        | `w.items`
`w.openpresent`     | open a present that you received (xmas mode only)   | `w.op`


### Bot Settings

Command             | What it does                                        | Example usage / aliases
------------------- | --------------------------------------------------- | -------------
`w.channelsettings` | set up settings for this channel                    | `w.cs accessrole Mafiaplayer` `w.channelsettings tagcooldown 10`

### Statistics

Command             | What it does                                        | Example usage / aliases
------------------- | --------------------------------------------------- | -------------
`w.userstats`       | show stats of a user                                | `w.userstats @user`
`w.guildstats`      | show stats of the current guild                     | `w.guildstats` `w.guildstats <guildId>`
`w.botstats`        | show bot wide stats                                 | `w.botstats`



### Other Commands

Command             | What it does                                        | Example usage / aliases
------------------- | --------------------------------------------------- | -------------
`w.commands`        | show a list of all available commands               | `w.comms`
`w.help`            | send some help your way                             | `w.help`
`w.info`            | show some general information about Wolfia          | `w.info`
`w.invite`          | post invite links for Wolfia and the Wolfia Lounge  | `w.inv`
`w.replay`          | show the replay of a game                           | `w.replay #gameid`
`w.tag`             | post or sign up for the tag list of the channel     | `w.tag +` `w.tag -` `w.tag + @role`

* * *

# Game Modes

## Mafia

Town ![][t]{:height="15" width="15"} against Mafia ![][m]{:height="15" width="15"}

- The Mafia knows their team, they receive an invite to mafia chat with their role pms
- During the day everyone votes to lynch one of the players
- During the night the Mafia kills players
- Town wins when all Mafia are dead, Mafia wins when they reach parity.

### Mafia Lite
9+ players

Power roles:  
The Cop ![][cop]{:height="15" width="15"} investigates the alignment of a player at night.

### Xmas Mafia
6+ players

Power roles:  
The Cop ![][cop]{:height="15" width="15"} investigates the alignment of a player at night.  
(Many) Santas ![][santa]{:height="15" width="15"} who give out presents to other players, which contain useful, but also
dangerous items.


## Popcorn

Village ![][v]{:height="15" width="15"} against Wolves ![][w]{:height="15" width="15"}
- The ![][w]{:height="15" width="15"}s know their team.
- A ![][v]{:height="15" width="15"} holds the ![][gun]{:height="15" width="15"}.
- If the ![][v]{:height="15" width="15"} shoots a ![][w]{:height="15" width="15"}, the ![][w]{:height="15" width="15"} dies and the ![][v]{:height="15" width="15"} can shoot again.
- If the ![][v]{:height="15" width="15"} shoots another ![][v]{:height="15" width="15"}, the shooter dies, and the ![][gun]{:height="15" width="15"} goes to the ![][v]{:height="15" width="15"} that was shot at.
- ![][v]{:height="15" width="15"}s win when all ![][w]{:height="15" width="15"}s are dead, ![][w]{:height="15" width="15"}s win when they reach parity.

### Popcorn Wild

3+ players

The Wild mode randomizes who gets the ![][gun]{:height="15" width="15"}.
The channel is never be closed, non-players and dead players can post all the time.

### Popcorn Classic

3-26 players

The Classic mode allows the ![][w]{:height="15" width="15"}s to have a separate hidden chat, where they may decide which ![][v]{:height="15" width="15"} gets the ![][gun]{:height="15" width="15"}.
The game channel is moderated, which means during a game only the living players are allowed to talk in the channel.


* * *

# Permissions:

Wolfia requires some permissions to run games flawlessly. It will try to automatically set itself up with what it needs, or ask you to give it the required permissions to do so.

Using the official invite link provided at the top of this page, or by running the `w.help` command to invite Wolfia to your server, will have it request the required permissions. If permissions on your server are broken for Wolfia or the required ones have been updated, kicking and reinviting should restore permissions to a working state.


Nevertheless, and also for the control freaks among us, here is a comprehensive list of what is required and why:


### Required
<dl>
<dt>- Read Message History</dt>
<dd>Edit it's own messages after they have been sent</dd>
<dt>- Use External Emojis</dt>
<dd>The standarized emojis are not enough to display everything clearly, so Wolfia packs a bunch of custom ones</dd>
<dt>- Embed Links</dt>
<dd>Formatting of messages</dd>
<dt>- Add Reactions</dt>
<dd>Display vote counts</dd>
<dt>- Manage Messages</dt>
<dd>Clearing reactions off of votecounts</dd>
<dt>- Manage Roles</dt>
<dd>Moderate the game channel with permission overrides (and just that; Wolfia does not create or delete any roles for the players)</dd>
</dl>

### Optional
<dl>
<dt>- Create Instant Invite</dt>
<dd>Adds invites to the channel where the game is running to role pms and private chat servers which makes for a smooth navigation for players during the game.</dd>
</dl>

* * *

# Special mentions:
- Written in Java using the excellent [JDA (Java Discord API)](https://github.com/DV8FromTheWorld/JDA). They maintain a super helpful crowd in their Discord guild.
- Several functions and architectural decisions inspired by and/or plain copy pasta'd from [Frederikam's](https://frederikam.com/) music bot [FredBoat](https://fredboat.com/).
- Thanks to the folks at [Mafia Universe](http://www.mafiauniverse.com/forums/) and their Discord guild for helping testing and refining the initial version.

* * *

{: .center}
Coded with lots of ![](https://canary.discordapp.com/assets/25c09e6fde32411da2b0da00f5cb9c84.svg){:height="15" width="15"} by [Napster](https://npstr.space/)


[w]:https://canary.discordapp.com/assets/04ff67f3321f9158ad57242a5412782b.svg
[v]:https://canary.discordapp.com/assets/984390b3eefc024ea770ccbfcfbdc4e2.svg
[m]:https://canary.discordapp.com/assets/a39460d0f6baa307386a4bb2984de363.svg
[t]:https://canary.discordapp.com/assets/984390b3eefc024ea770ccbfcfbdc4e2.svg
[gun]:https://canary.discordapp.com/assets/3071dbc60204c84ca0cf423b8b08a204.svg
[cop]:https://canary.discordapp.com/assets/3896096ba07324c04ed0fe7e1acc3643.svg
[santa]:https://canary.discordapp.com/assets/37017a416c2a6ab030f0de3db9f6e73e.svg
