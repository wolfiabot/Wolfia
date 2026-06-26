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

// the role icons that the backend may reference via :token: in game descriptions, see public/img/roles
const ROLE_ICONS = ["wolf", "villager", "townie", "mafia", "cop", "santa", "gun"];

/**
 * Replaces :role: tokens in backend supplied game descriptions with the corresponding role icon.
 * The input originates from our own backend and is therefore trusted; the output is meant for v-html.
 * @param {string} text
 * @return {string} html with inline role icons
 */
export function renderGameText(text) {
	if (!text) return "";
	return text.replace(/:(\w+):/g, (match, name) => {
		if (ROLE_ICONS.includes(name)) {
			return `<img class="role-icon" src="/img/roles/${name}.svg" alt="${name}" />`;
		}
		return match;
	});
}
