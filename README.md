<img align="right" src="https://i.imgur.com/7Ie8tB3.png" height="200" width="200">

[![Build Status](https://img.shields.io/travis/napstr/wolfia/master.svg?style=flat-square)](https://travis-ci.org/napstr/wolfia)
[![Download](https://api.bintray.com/packages/napster/wolfia/beta/images/download.svg) ](https://bintray.com/napster/wolfia/beta/_latestVersion)
[![Dependency Status](https://img.shields.io/versioneye/d/ruby/rails.svg?style=flat-square)](https://www.versioneye.com/user/projects/592b1be8c0295d002dd17a34)
[![GitHub tag](https://img.shields.io/github/tag/napstr/wolfia.svg?style=flat-square)]()
[![License](https://img.shields.io/github/license/napstr/wolfia.svg?style=flat-square)]()
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/b6bd0bab45034ee9b154d9fa02a0ca68?style=flat-square)](https://www.codacy.com/app/napstr/wolfia?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=napstr/wolfia&amp;utm_campaign=Badge_Grade)
[![Uptime Robot ratio (7 days)](https://img.shields.io/uptimerobot/ratio/7/m778927695-04e353308ad0d207bd0489b8.svg?style=flat-square)]()
[![Discord](https://img.shields.io/discord/315944983754571796.svg?style=flat-square)](https://discord.gg/nvcfX3q)

# Wolfia

## A Discord bot to play Mafia, Werewolf and similar games

[![Invite the Wolfia bot to your server.](http://i.imgur.com/qEWSU6D.png)](https://discordapp.com/oauth2/authorize?&client_id=306583221565521921&scope=bot)

[Click the banner above or this link](https://discordapp.com/oauth2/authorize?&client_id=306583221565521921&scope=bot)

## Commands and supported modes

Wolfia is considered to be beta status. New games, modes and features are coming out regularly.

Please check out https://docs.wolfia.party for a full list of commands and games!


## Support, feedback, roadmap and games in the Wolfia Lounge

[![Join Wolfia Lounge](https://discordapp.com/api/guilds/315944983754571796/embed.png?style=banner2)](https://discord.gg/nvcfX3q)


## Selfhosting

Wolfia requires a [PostgreSQL database](https://www.postgresql.org/) with the [hstore extension enabled](http://postgresguide.com/cool/hstore.html).

[Download the current Wolfia jar from bintray](https://bintray.com/napster/wolfia/beta/_latestVersion), or build it yourself with `./gradlew build`.

There are also [docker images available](https://hub.docker.com/r/napstr/wolfia/). See the `docker-compose.yaml` file for an example on running it.

To successfully run Wolfia, make sure to fill out the `wolfia.example.yaml` file and rename it to `wolfia.yaml`.

Using docker has the following advantages and benefits:
- No building, no installing (besides docker and docker-compose) necessary
- Automatic updates via the watchtower container
- Fully set up and managed PostgreSQL database, including regular backups to Backblaze Cloud Storage (b2)
- Images with ARM support for both the Wolfia bot and the PostgreSQL database are available (if you need docker-compose on arm64v8, check out this: https://hub.docker.com/r/napstr/compose/)
- It is the current way that the public version of Wolfia is hosted and has therefore the highest chances of being supported in the future

**Please note: No selfhosting support will be provided.** If you are not familiar with the technologies mentioned above,
or are unable to teach them to yourself from the links provided as well as using your preferred web search engine to answer
the questions left, then please accept that selfhosting is not for you. [Instead, use the available free, public version of Wolfia.](https://wolfia.party/)


## Special mentions
- Written in Java using the excellent [JDA (Java Discord API)](https://github.com/DV8FromTheWorld/JDA). They maintain a super helpful crowd in their Discord guild.
- Several functions and architectural decisions inspired by and/or plain copy pasta'd from [Frederikam's](https://github.com/Frederikam) music bot [FredBoat](https://github.com/Frederikam/FredBoat).
- Thanks to the folks at [Mafia Universe](http://www.mafiauniverse.com) and their Discord guild for helping testing and refining the initial version.
