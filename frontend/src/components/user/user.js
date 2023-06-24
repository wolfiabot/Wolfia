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

/**
 * Represents a single Discord User.
 */
export class User {
	constructor(discordId, name, avatarId, roles) {
		this.discordId = discordId;
		this.name = name;
		this.avatarId = avatarId;
		this.roles = roles;
	}

	avatarUrl() {
		if (this.avatarId === null || this.avatarId === "") {
			let number = (this.discordId >> 22) % 6;
			return `https://cdn.discordapp.com/embed/avatars/${number}.png`;
		}
		const ext = this.avatarId.startsWith("a_") ? "gif" : "png";
		return `https://cdn.discordapp.com/avatars/${this.discordId}/${this.avatarId}.${ext}`;
	}
}
