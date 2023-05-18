/*
 * Copyright (C) 2016-2023 the original author or authors
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
package space.npstr.wolfia.domain.staff

import java.net.URI
import java.util.Optional.*
import java.util.concurrent.TimeUnit
import kotlin.jvm.optionals.getOrNull
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.stereotype.Service
import space.npstr.wolfia.App
import space.npstr.wolfia.config.properties.WolfiaConfig
import space.npstr.wolfia.db.gen.enums.StaffFunction
import space.npstr.wolfia.db.gen.tables.records.StaffMemberRecord
import space.npstr.wolfia.domain.UserCache
import space.npstr.wolfia.game.tools.ExceptionLoggingExecutor
import space.npstr.wolfia.system.logger

/**
 * Provides and updates information about the staff behind Wolfia
 */
@Service
class StaffService internal constructor(
	private val staffRepository: StaffRepository,
	private val userCache: UserCache,
	private val shardManager: ShardManager,
	@Suppress("unused") private val wolfiaConfig: WolfiaConfig, // for debugging
	scheduler: ExceptionLoggingExecutor,
) {
	init {
		scheduler.scheduleAtFixedRate({ updateIfPossible() }, 10, 60, TimeUnit.SECONDS)
	}

	/**
	 * @return a list of all enabled and active staff members
	 */
	fun enabledActiveStaffMembers(): List<StaffMember> {
		return staffRepository.fetchAllStaffMembers()
			.filter { it.active && isEnabled(it) }
			.mapNotNull { toStaffMember(it) }
	}

	private fun isEnabled(record: StaffMemberRecord): Boolean {
		return record.enabled // || wolfiaConfig.isDebug() //for debugging/developing, it is handy to see a large list of staffers
	}

	/**
	 * Entry method to further fluent operations on specific users.
	 */
	fun user(userId: Long): Action {
		return Action(userId, staffRepository, this)
	}

	class Action internal constructor(private val userId: Long, private val staffRepository: StaffRepository, private val staffService: StaffService) {

		fun get(): StaffMember? {
			return staffRepository.getStaffMember(userId)?.let { staffService.toStaffMember(it) }
		}

		fun enable(): StaffMember {
			return staffRepository.updateEnabled(userId, true)!!
				.let { staffService.toStaffMember(it)!! }
		}

		fun disable(): StaffMember {
			return staffRepository.updateEnabled(userId, false)!!
				.let { staffService.toStaffMember(it)!! }
		}

		fun setSlogan(slogan: String): StaffMember {
			return staffRepository.updateSlogan(userId, slogan)!!
				.let { staffService.toStaffMember(it)!! }
		}

		fun removeSlogan(): StaffMember {
			return staffRepository.updateSlogan(userId, null)!!
				.let { staffService.toStaffMember(it)!! }
		}

		fun setLink(uri: URI): StaffMember {
			return staffRepository.updateLink(userId, uri)!!
				.let { staffService.toStaffMember(it)!! }
		}

		fun removeLink(): StaffMember {
			return staffRepository.updateLink(userId, null)!!
				.let { staffService.toStaffMember(it)!! }
		}
	}

	/**
	 * Attempts to update the staff member records if the wolfia lounge is available.
	 * This task is also run periodically.
	 */
	fun updateIfPossible() {
		val wolfiaLounge = shardManager.getGuildById(App.WOLFIA_LOUNGE_ID)
		wolfiaLounge?.let { updateStaff(it) }
	}

	private fun updateStaff(wolfiaLounge: Guild) {
		val moderators = findHumanMembersOfRole(wolfiaLounge, App.MODERATOR_ROLE_ID)
		val setupManagers = findHumanMembersOfRole(wolfiaLounge, App.SETUP_MANAGER_ROLE_ID)
		val developers = findHumanMembersOfRole(wolfiaLounge, App.DEVELOPER_ROLE_ID)
		for (moderator in moderators) {
			staffRepository.updateOrCreateStaffMemberFunction(moderator.idLong, StaffFunction.MODERATOR)
		}
		for (setupManager in setupManagers) {
			staffRepository.updateOrCreateStaffMemberFunction(setupManager.idLong, StaffFunction.SETUP_MANAGER)
		}
		for (developer in developers) {
			staffRepository.updateOrCreateStaffMemberFunction(developer.idLong, StaffFunction.DEVELOPER)
		}

		val staffIds = listOf(moderators, setupManagers, developers)
			.flatten()
			.map { it.idLong }
			.toSet()

		staffRepository.updateAllActive(staffIds)
	}

	private fun findHumanMembersOfRole(wolfiaLounge: Guild, roleId: Long): List<Member> {
		val role = wolfiaLounge.getRoleById(roleId)
		if (role == null) {
			logger().warn("Could not find role {} in the wolfia lounge, where did it go?", roleId)
			return listOf()
		}
		return wolfiaLounge.getMembersWithRoles(role)
			.filter { !it.user.isBot }
	}

	private fun toStaffMember(staffMemberRecord: StaffMemberRecord): StaffMember? {
		val user = userCache.user(staffMemberRecord.userId).fetch().getOrNull() ?: return null

		return StaffMember(
			user.idLong,
			user.name,
			user.discriminator,
			ofNullable(user.avatarId),
			staffMemberRecord.function,
			ofNullable(staffMemberRecord.slogan),
			ofNullable(staffMemberRecord.link),
			staffMemberRecord.enabled,
			staffMemberRecord.active
		)
	}
}
