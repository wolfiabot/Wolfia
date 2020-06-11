import Vue from "vue";
import App from "@/App.vue";
import router from "@/router";
import store from "@/store";
import * as Sentry from "@sentry/browser";
import * as Integrations from "@sentry/integrations";
import Buefy from "buefy";
import "bulmaswatch/darkly/bulmaswatch.scss";

Vue.use(Buefy);

if (process.env.NODE_ENV === "production") {
	Sentry.init({
		dsn: process.env.VUE_APP_SENTRY_DSN,
		integrations: [
			new Integrations.Vue({
				Vue,
				attachProps: true,
				logErrors: true,
			}),
		],
		release: process.env.VUE_APP_VERSION,
	});
}

Vue.config.productionTip = false;

new Vue({
	router,
	store,
	render: (h) => h(App),
}).$mount("#app");
