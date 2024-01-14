/*
 * Copyright (C) 2016-2024 the original author or authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package space.npstr.wolfia.domain.setup.lastactive

import io.lettuce.core.pubsub.RedisPubSubAdapter
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.stereotype.Component
import space.npstr.wolfia.domain.setup.GameSetupService
import space.npstr.wolfia.system.redis.Redis

/**
 * This component listens the keys expiring from [ActivityService] and takes action
 *
 *
 * Since we could lose the redis connection, or restart, or <insert any other reason to miss expiry events>,
 * a "manual" check when starting the game should still be performed.
</insert> */
@Component
class AutoOuter(
	redis: Redis,
	private val gameSetupService: GameSetupService,
	private val shardManager: ShardManager,
) : RedisPubSubAdapter<String, String>() {

	private val expireChannel = "__keyevent@*__:expired"
	private val redisKeyParser = RedisKeyParser()

	init {
		redis.pubSub.let {
			it.addListener(this)
			it.sync().psubscribe(expireChannel)
		}
	}

	override fun message(channel: String, message: String) {
		if (expireChannel == channel) {
			this.expired(message)
		}
	}

	override fun message(pattern: String, channel: String, message: String) {
		if (expireChannel == pattern || expireChannel == channel) {
			this.expired(message)
		}
	}

	private fun expired(key: String) {
		redisKeyParser.fromKey(key)?.let { outUser(it) }
	}

	private fun outUser(userId: Long) {
		gameSetupService.outUserDueToInactivity(userId, this.shardManager)
	}
}
