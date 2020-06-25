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
