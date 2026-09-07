<template>
  <div class="auth-layout" :class="{ 'auth-layout--illustrated': $route.path === '/login' }">
    <div class="auth-orbit orbit-one" />
    <div class="auth-orbit orbit-two" />
    <header v-if="$route.path !== '/login'" class="auth-brand"><span class="brand-mark">家</span><span>家账</span></header>
    <RouterView />
    <footer class="auth-footer">家庭的每一笔，都值得被看见。</footer>
  </div>
</template>
