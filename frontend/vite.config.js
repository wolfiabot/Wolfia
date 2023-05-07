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
import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";

const path = require("path");

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
	const htmlPlugin = () => {
		const env = loadEnv(mode, ".");
		return {
			name: "html-transform",
			transformIndexHtml(html) {
				return html.replace(/%(.*?)%/g, function (match, p1) {
					return env[p1];
				});
			},
		};
	};

	return {
		plugins: [vue(), htmlPlugin()],
		define: {
			VITE_APP_VERSION: JSON.stringify(process.env.npm_package_version),
		},
		resolve: {
			alias: {
				"@": path.resolve(__dirname, "./src"),
			},
		},
		build: {
			outDir: "build/dist",
		},
		server: {
			port: 8080,
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
				"/login": {
					target: "http://localhost:4567",
					ws: true,
					changeOrigin: true,
				},
			},
		},
	};
});
