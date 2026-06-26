/*
 * Copyright (C) 2016-2026 the original author or authors
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
package space.npstr.wolfia.commands

import org.springframework.stereotype.Service
import space.npstr.wolfia.config.properties.WolfiaConfig

/**
 * Assembles the user facing list of public commands, grouped by [CommandCategory], for consumption by the
 * public web API. Reads from the same [CommandCategory] definitions as the in-Discord `w.commands` output so the
 * two stay in sync.
 */
@Service
class CommandsUiService(
	private val commRegistry: CommRegistry,
) {

	data class CommandCategoryUiInfo(
		val name: String,
		val commands: List<CommandUiInfo>,
	)

	data class CommandUiInfo(
		val trigger: String,
		val aliases: List<String>,
		val usage: String,
		val description: String,
		val examples: List<String>,
		val xmasOnly: Boolean,
	)

	fun getCommandCategories(): List<CommandCategoryUiInfo> {
		return CommandCategory.entries.map { category ->
			CommandCategoryUiInfo(
				name = category.displayName(),
				commands = category.triggers().mapNotNull { trigger -> toCommandInfo(trigger) },
			)
		}
	}

	private fun toCommandInfo(trigger: String): CommandUiInfo? {
		val command = commRegistry.getCommand(trigger) ?: return null
		val prefix = WolfiaConfig.DEFAULT_PREFIX
		val helpLines = command.help().split("\n")
		// the first line of a command's help is its usage, e.g. "w.shoot @player"
		val usage = helpLines.firstOrNull()?.trim().orEmpty()
		val body = helpLines.drop(1).map { it.trim() }.filter { it.isNotEmpty() }
		// the description lines are prefixed with '#', see BaseCommand implementations
		val description = body
			.filter { it.startsWith("#") }
			.joinToString(" ") { it.removePrefix("#").trim() }
		// the remaining lines are example invocations
		val examples = body.filter { !it.startsWith("#") }

		return CommandUiInfo(
			trigger = prefix + command.trigger,
			aliases = command.aliases.map { prefix + it },
			usage = usage,
			description = description,
			examples = examples,
			xmasOnly = CommandCategory.isXmasOnly(trigger),
		)
	}
}
