import Vue from "vue";
import App from "@/App.vue";
import router from "@/router/router";
import store from "@/store/store";
import * as Sentry from "@sentry/browser";
import * as Integrations from "@sentry/integrations";

if (process.env.NODE_ENV === "production") {
	Sentry.init({
		dsn: process.env.VUE_APP_SENTRY_DSN,
		integrations: [
			new Integrations.Vue({
				Vue,
				attachProps: true,
				logErrors: true
			})
		],
		release: process.env.VUE_APP_VERSION
	});
}

Vue.config.productionTip = false;

new Vue({
	router,
	store,
	render: h => h(App)
}).$mount("#app");
