/*
 * Copyright (C) 2016-2020 Dennis Neufeld
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
/**
 * Represents a single Discord Guild.
 */
export class Guild {
	constructor(discordId, name, icon, botPresent, canEdit) {
		this.discordId = discordId;
		this.name = name;
		this.icon = icon;
		this.botPresent = botPresent;
		this.canEdit = canEdit;
	}

	iconUrl() {
		if (this.icon === null || this.icon === "") {
			return `https://discord.com/assets/2c21aeda16de354ba5334551a883b481.png`;
		}

		const ext = this.icon.startsWith("a_") ? "gif" : "png";
		return `https://cdn.discordapp.com/icons/${this.discordId}/${this.icon}.${ext}`;
	}
}
