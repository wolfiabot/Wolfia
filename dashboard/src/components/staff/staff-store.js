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
import { User } from "@/components/user/user";
import { StaffMember } from "@/components/staff/staffmember";
import fetcher from "@/fetcher";

export const FETCH_STAFF = "FETCH_STAFF";

const FETCH_STAFF_INTERNAL = "FETCH_STAFF_INTERNAL";

const LOAD_STAFF = "LOAD_STAFF";
const FETCHING_STAFF = "FETCHING_STAFF";

export const staffStore = {
	namespaced: true,
	modules: {},
	state: () => ({
		staffLoading: false, //true each time there is a request for staff in flight
		staffLoaded: false, //true as soon as we received the data for the first time
		staff: [],
	}),
	getters: {},
	mutations: {
		[LOAD_STAFF](state, staff) {
			state.staffLoading = false;
			state.staff = staff;
			state.staffLoaded = true;
		},
		[FETCHING_STAFF](state) {
			state.staffLoading = true;
		},
	},
	actions: {
		async [FETCH_STAFF](context) {
			if (context.state.staffLoading) {
				return;
			}
			context.commit(FETCHING_STAFF);
			context.dispatch(FETCH_STAFF_INTERNAL);
		},
		async [FETCH_STAFF_INTERNAL](context) {
			const staff = await fetcher.get("/public/staff");
			if (!staff) {
				setTimeout(() => context.dispatch(FETCH_STAFF_INTERNAL), 5000);
				return;
			}

			let mappedStaff = staff.map((member) => {
				const user = new User(member.discordId, member.name, member.avatarId, member.discriminator);
				return new StaffMember(user, member.function, member.slogan, member.link);
			});
			context.commit(LOAD_STAFF, mappedStaff);
		},
	},
};
