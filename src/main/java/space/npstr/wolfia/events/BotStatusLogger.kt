/*
 * Copyright (C) 2016-2025 the original author or authors
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
package space.npstr.wolfia.events

import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.receive.ReadonlyMessage
import club.minnced.discord.webhook.send.WebhookMessageBuilder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component
import space.npstr.wolfia.utils.discord.TextchatUtils

@Component
class BotStatusLogger(
	private val botStatusWebhook: WebhookClient?,
) {

	@OptIn(ExperimentalCoroutinesApi::class)
	private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default.limitedParallelism(1))

	fun fireAndForget(emoji: String, message: String) {
		scope.launch {
			fire(emoji, message).await()
		}
	}

	fun fire(emoji: String, message: String): Deferred<ReadonlyMessage?> = scope.async {
		if (botStatusWebhook == null) {
			return@async null
		}

		val webhookMessage = deepFriedBuilder()
			.setContent("${TextchatUtils.toUtcTime(System.currentTimeMillis())} $emoji ${Zalgo.convert(message)}")
			.build()

		return@async botStatusWebhook.send(webhookMessage).await()
	}

	private fun deepFriedBuilder(): WebhookMessageBuilder {
		return WebhookMessageBuilder()
			.setAvatarUrl("https://i.imgur.com/GhyO7Y9.png")
			.setUsername("Deep Fried Wolfia")
	}

	//copy pasta'd from https://github.com/JaneJeon/Zalgo4J/blob/master/Zalgo.java with some adaptations
	private object Zalgo {
		private val zalgo_up = listOf(
			'\u030d', '\u030e', '\u0304', '\u0305', '\u033f', '\u0311', '\u0306', '\u0310', '\u0352', '\u0357',
			'\u0351', '\u0307', '\u0308', '\u030a', '\u0342', '\u0343', '\u0344', '\u034a', '\u034b', '\u034c',
			'\u0303', '\u0302', '\u030c', '\u0350', '\u0300', '\u0301', '\u030b', '\u030f', '\u0312', '\u0313',
			'\u0314', '\u033d', '\u0309', '\u0363', '\u0364', '\u0365', '\u0366', '\u0367', '\u0368', '\u0369',
			'\u036a', '\u036b', '\u036c', '\u036d', '\u036e', '\u036f', '\u033e', '\u035b', '\u0346', '\u031a',
		)
		private val zalgo_down = listOf(
			'\u0316', '\u0317', '\u0318', '\u0319', '\u031c', '\u031d', '\u031e', '\u031f', '\u0320', '\u0324',
			'\u0325', '\u0326', '\u0329', '\u032a', '\u032b', '\u032c', '\u032d', '\u032e', '\u032f', '\u0330',
			'\u0331', '\u0332', '\u0333', '\u0339', '\u033a', '\u033b', '\u033c', '\u0345', '\u0347', '\u0348',
			'\u0349', '\u034d', '\u034e', '\u0353', '\u0354', '\u0355', '\u0356', '\u0359', '\u035a', '\u0323',
		)

		private fun randInt(max: Int): Int {
			return (Math.random() * max).toInt()
		}

		private fun randZalgo(list: List<Char>): Char {
			return list[randInt(list.size)]
		}

		private fun isZalgo(c: Char): Boolean {
			return zalgo_up.contains(c) || zalgo_down.contains(c)
		}

		private fun isAscii(c: Char): Boolean {
			return StandardCharsets.US_ASCII.newEncoder().canEncode(c)
		}

		fun convert(s: String): String {
			val result = StringBuilder()
			for (c: Char in s) {
				if (isZalgo(c)) continue
				// add the normal character
				result.append(c)
				if (!isAscii(c)) continue
				val numUp = randInt(8)
				val numDown = randInt(8)
				// add the zalgo decorations
				for (j in 0 until numUp) result.append(randZalgo(zalgo_up))
				for (j in 0 until numDown) result.append(randZalgo(zalgo_down))
			}
			return result.toString()
		}
	}
}
