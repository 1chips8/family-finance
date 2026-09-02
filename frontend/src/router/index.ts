import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AuthLayout from '../layouts/AuthLayout.vue'
import AppLayout from '../layouts/AppLayout.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import OnboardingView from '../views/OnboardingView.vue'
import DashboardView from '../views/DashboardView.vue'
import EntriesView from '../views/EntriesView.vue'
import MembersView from '../views/MembersView.vue'
import CategoriesView from '../views/CategoriesView.vue'
import ProfileView from '../views/ProfileView.vue'
import NotFoundView from '../views/NotFoundView.vue'
import ForbiddenView from '../views/ForbiddenView.vue'

export const appRoutes: RouteRecordRaw[] = [
  { path: '/login', component: LoginView, meta: { public: true } },
  { path: '/register', component: RegisterView, meta: { public: true } },
  { path: '/onboarding', component: OnboardingView, meta: { auth: true } },
  { path: '/dashboard', component: DashboardView, meta: { auth: true, household: true } },
  { path: '/entries', component: EntriesView, meta: { auth: true, household: true } },
  { path: '/members', component: MembersView, meta: { auth: true, household: true } },
  { path: '/categories', component: CategoriesView, meta: { auth: true, household: true } },
  { path: '/profile', component: ProfileView, meta: { auth: true, household: true } },
  { path: '/forbidden', component: ForbiddenView, meta: { auth: true, household: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/dashboard' },
    { path: '/', component: AuthLayout, children: appRoutes.filter((route) => route.meta?.public) },
    { path: '/', component: AppLayout, children: appRoutes.filter((route) => route.meta?.auth) },
    { path: '/:pathMatch(.*)*', component: NotFoundView },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  await auth.restore()
  if (to.meta.public && auth.authenticated) return auth.inHousehold ? '/dashboard' : '/onboarding'
  if (to.meta.auth && !auth.authenticated) return { path: '/login', query: { redirect: to.fullPath } }
  if (to.meta.household && !auth.inHousehold) return '/onboarding'
  if (to.path === '/onboarding' && auth.inHousehold) return '/dashboard'
})

export default router
