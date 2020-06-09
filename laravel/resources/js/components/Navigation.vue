<template>
  <b-navbar type="dark" variant="dark" class="nav">
    <b-navbar-nav>
      <b-nav-item id="title" to="/">バリバリ</b-nav-item>
    </b-navbar-nav>
    <b-navbar-nav class="navbar-nav mx-auto" >
      <b-nav-item to="/" >관제</b-nav-item>
      <b-nav-item to="/statistics" >통계</b-nav-item>
      <b-nav-item to="/manage" >관리</b-nav-item>
    </b-navbar-nav>
  </b-navbar>
</template>
<script>
export default {
    name: 'Nav'
}
</script>

<style scoped>
.nav {
font-size: 1.4em;
margin-top: 0;
margin-bottom : 0;
}
.nav-item{
  margin-right : 100px;
}

#title{
  margin-left: 20px;
}


</style>