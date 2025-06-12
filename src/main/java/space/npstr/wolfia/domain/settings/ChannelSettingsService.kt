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
package space.npstr.wolfia.domain.settings

import java.time.Clock
import org.springframework.stereotype.Service

@Service
class ChannelSettingsService(private val repository: ChannelSettingsRepository, private val clock: Clock) {
	fun channels(channelIds: Collection<Long>): MultiAction {
		return MultiAction(channelIds)
	}

	inner class MultiAction internal constructor(private val channelIds: Collection<Long>) {
		fun getOrDefault(): List<ChannelSettings> {
			return repository.findOrDefault(channelIds)
		}
	}

	/**
	 * This service has many calls that require passing in multiple long ids. This fluent action api should help avoid
	 * mistakes where arguments are passed in the wrong order.
	 *
	 * @return an action that can be executed on the passed in channel
	 */
	fun channel(channelId: Long): Action {
		return Action(channelId)
	}

	inner class Action internal constructor(private val channelId: Long) {
		fun getOrDefault(): ChannelSettings {
			return repository.findOneOrDefault(channelId)
		}

		fun setAccessRoleId(accessRoleId: Long): ChannelSettings {
			return repository.setAccessRoleId(channelId, accessRoleId)
		}

		fun enableAutoOut(): ChannelSettings {
			return repository.setAutoOut(channelId, true)
		}

		fun disableAutoOut(): ChannelSettings {
			return repository.setAutoOut(channelId, false)
		}

		fun enableGameChannel(): ChannelSettings {
			return repository.setGameChannel(channelId, true)
		}

		fun disableGameChannel(): ChannelSettings {
			return repository.setGameChannel(channelId, false)
		}

		fun setTagCooldown(tagCooldown: Long): ChannelSettings {
			return repository.setTagCooldown(channelId, tagCooldown)
		}

		fun tagUsed(): ChannelSettings {
			return repository.setTagLastUsed(channelId, clock.millis())
		}

		fun addTag(tag: Long): ChannelSettings {
			return addTags(setOf(tag))
		}

		fun addTags(tags: Collection<Long>): ChannelSettings {
			return if (tags.isEmpty()) {
				getOrDefault()
			} else {
				repository.addTags(channelId, tags)
			}
		}

		fun removeTag(tag: Long): ChannelSettings {
			return removeTags(setOf(tag))
		}

		fun removeTags(tags: Collection<Long>): ChannelSettings {
			return if (tags.isEmpty()) {
				getOrDefault()
			} else {
				repository.removeTags(channelId, tags)
			}
		}

		fun reset() {
			repository.delete(channelId)
		}
	}
}
