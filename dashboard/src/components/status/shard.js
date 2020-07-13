/*
 * Copyright (C) 2016-2020 the original author or authors
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

const online = ["CONNECTED"];
const offline = ["FAILED_TO_LOGIN", "SHUTTING_DOWN", "SHUTDOWN"];
export class Shard {
	constructor(id, status) {
		this.id = id;
		this.status = status;
	}

	getShardStatus = () => {
		if (online.includes(this.status)) {
			return "Online";
		} else if (offline.includes(this.status)) {
			return "Offline";
		} else {
			return "Connecting";
		}
	};
}
