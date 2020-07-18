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
const webpack = require("webpack");

const devHost = "xyz.ngrok.io";
const prodHost = "bot.wolfia.party";
const deployHost = process.env.NODE_ENV === "production" ? prodHost : devHost;
const deployBaseUrl = `https://${deployHost}`;

process.env.VUE_APP_VERSION = require("./package.json").version;

module.exports = {
	outputDir: "build/dist",
	assetsDir: "asset",
	devServer: {
		public: deployBaseUrl,
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
	configureWebpack: {
		plugins: [
			new webpack.DefinePlugin({
				//Used as templates in index.html
				DEPLOY_URL: JSON.stringify(deployBaseUrl),
				TITLE: JSON.stringify("Wolfia Dashboard"),
				DESCRIPTION: JSON.stringify("Web Dashboard for the Wolfia Discord Bot"),
			}),
		],
	},
};
