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

process.env.VUE_APP_VERSION = require("./package.json").version;

module.exports = {
	outputDir: "build/dist",
	assetsDir: "static",
	devServer: {
		//public: "xxx.ngrok.io",
		proxy: {
			"/api": {
				target: "http://localhost:4567",
				ws: true,
				changeOrigin: true,
			},
			"/public": {
				target: "http://localhost:4567",
				ws: true,
				changeOrigin: true,
			},
			"/oauth2": {
				target: "http://localhost:4567",
				ws: true,
				changeOrigin: true,
			},
			"/invite": {
				target: "http://localhost:4567",
				ws: true,
				changeOrigin: true,
			},
		},
		disableHostCheck: true,
	},
};
