process.env.VUE_APP_VERSION = require("./package.json").version;

module.exports = {
	outputDir: "build/dist",
	assetsDir: "static",
	devServer: {
		proxy: {
			"/api": {
				target: "http://localhost:4567",
				ws: true,
				changeOrigin: true
			}
		},
		disableHostCheck: true
	}
};
