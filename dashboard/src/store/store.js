import Vue from "vue";
import Vuex from "vuex";

Vue.use(Vuex);

export default new Vuex.Store({
	strict: process.env.NODE_ENV !== "production", //see https://vuex.vuejs.org/guide/strict.html
	state: {},
	mutations: {},
	actions: {},
	modules: {}
});
