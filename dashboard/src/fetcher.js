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

import { ToastProgrammatic as Toast } from "buefy";
import { LOG_OUT } from "@/components/user/user-store";

let theStore;

export default {
	setStore(store) {
		theStore = store;
	},

	headers() {
		let csrfToken = document.cookie.replace(/(?:(?:^|.*;\s*)XSRF-TOKEN\s*=\s*([^;]*).*$)|^.*$/, "$1");
		return {
			"X-XSRF-TOKEN": csrfToken,
			"Content-Type": "application/json",
		};
	},

	defaultFailureCallback() {
		return async (response) => {
			if (!response) return;
			if (response.status === 401 || response.status === 403) {
				const userLoaded = await theStore.getters["user/userLoaded"];
				if (userLoaded) {
					theStore.dispatch("user/" + LOG_OUT);
					Toast.open({
						message: "Looks like you need to log in again!",
						type: "is-warning",
						duration: 3000,
					});
				}
			} else {
				Toast.open({
					message: "Oh no, is your internet down? Try again in a moment.",
					type: "is-danger",
					duration: 3000,
				});
			}
		};
	},

	/**
	 * @return {Promise<any>} returns the json response of the request or in case an error happened null
	 */
	get(url) {
		return this.fetch(url, "GET", this.headers(), this.defaultFailureCallback());
	},

	/**
	 * @return {Promise<any>} returns the json response of the request or in case an error happened null
	 */
	delete(url) {
		return this.fetch(url, "DELETE", this.headers(), this.defaultFailureCallback());
	},

	/**
	 * @return {Promise<any>} returns the json response of the request or in case an error happened null
	 */
	post(url, body) {
		return this.fetch(url, "POST", this.headers(), this.defaultFailureCallback(), body);
	},

	/**
	 * @return {Promise<any> | Promise<string>} returns the json response of the request or in case an error happened null
	 */
	async fetch(url, method, headers, failureCallback, body) {
		try {
			const response = await fetch(url, {
				method: method,
				headers: headers,
				body: JSON.stringify(body),
			});

			if (response.status !== 200 && response.status !== 204) {
				if (failureCallback) {
					failureCallback(response);
				}
				throw new Error("Response is not successful: " + response.status);
			}

			if (response.status === 204) {
				return await response.text();
			}
			return await response.json();
		} catch (err) {
			console.error(err);
			if (failureCallback) {
				failureCallback();
			}
			return null;
		}
	},
};
