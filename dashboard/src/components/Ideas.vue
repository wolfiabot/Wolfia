<!--
  - Copyright (C) 2016-2020 Dennis Neufeld
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU Affero General Public License as published
  - by the Free Software Foundation, either version 3 of the License, or
  - (at your option) any later version.
  -
  - This program is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU Affero General Public License for more details.
  -
  - You should have received a copy of the GNU Affero General Public License
  - along with this program.  If not, see <http://www.gnu.org/licenses/>.
  -->

<template>
	<div class="iframe-container">
		<iframe
			class="iframe-child"
			src="https://feedback.userreport.com/01987d31-0d58-48c6-a4d3-96f2ae42eb14#ideas?cb=0&logo=0&bug=0"
		></iframe>
	</div>
</template>

<script>
export default {
	name: "Ideas",
};
</script>

<style scoped lang="scss">
@import "node_modules/bulmaswatch/darkly/variables";
.iframe-container {
	position: fixed;
	height: 85%; //Good enough so that the footer is still visible
	width: 100%;
}
.iframe-child {
	height: 100%;
	width: 100%;
	background: $grey-lighter;
}
</style>
