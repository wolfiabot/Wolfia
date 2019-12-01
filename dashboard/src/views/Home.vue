<template>
  <div class="home">
    <img alt="Vue logo" src="../assets/logo.png">
    <HelloWorld msg="Welcome to Your Vue.js App" v-bind:user-name="userName"/>
  </div>
</template>

<script>
  // @ is an alias to /src
  import HelloWorld from '@/components/HelloWorld.vue'

  export default {
    name: 'home',
    components: {
      HelloWorld
    },
    data() {
      return {
        userName: ''
      }
    },

    mounted() {
      this.getName();
    },

    methods: {
      async getName() {
        const response = await fetch('/api/test/name');
        this.userName = await response.text();
      },
    },
  }
</script>
