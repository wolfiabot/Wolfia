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

import { createApp } from "vue";
import App from "@/App.vue";
import router from "@/router";
import * as Sentry from "@sentry/vue";
import store from "@/store";
import fetcher from "@/fetcher";

const app = createApp(App);

if (import.meta.env.PROD) {
	Sentry.init({
		app,
		attachProps: true,
		logErrors: true,
		dsn: import.meta.env.VITE_APP_SENTRY_DSN,
		release: import.meta.env.VITE_APP_VERSION,
	});
}

fetcher.setStore(store);

app.use(router);
app.use(store);
router.isReady().then(() => app.mount("#app"));

console.log(`
                              __
                            .d$$b
                           .' TO$;\\
        Wolfia            /  : TP._;
    Werewolf & Mafia     / _.;  :Tb|
      Discord bot       /   /   ;j$j
                    _.-"       d$$$$
                  .' ..       d$$$$;
                 /  /P'      d$$$$P. |\\
                /   "      .d$$$P' |\\^"l
              .'           \`T$P^"""""  :
          ._.'      _.'                ;
       \`-.-".-'-' ._.       _.-"    .-"
     \`.-" _____  ._              .-"
    -(.g$$$$$$$b.              .'
      ""^^T$$$P^)            .(:
        _/  -"  /.'         /:/;
     ._.'-'\`-'  ")/         /;/;
  \`-.-"..--""   " /         /  ;
 .-" ..--""        -'          :
 ..--""--.-"         (\\      .-(\\
   ..--""              \`-\\(\\/;\`
     _.                      :
                             ;\`-
                            :\\
                            ;
`);
