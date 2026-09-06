import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AuthLayout from '../layouts/AuthLayout.vue'
import AppLayout from '../layouts/AppLayout.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import OnboardingView from '../views/OnboardingView.vue'
import NotFoundView from '../views/NotFoundView.vue'

export const appRoutes: RouteRecordRaw[] = [
  { path: '/login', component: LoginView, meta: { public: true } },
  { path: '/register', component: RegisterView, meta: { public: true } },
  { path: '/onboarding', component: OnboardingView, meta: { auth: true } },
  { path: '/dashboard', component: () => import('../views/DashboardView.vue'), meta: { auth: true, household: true } },
  { path: '/entries', component: () => import('../views/EntriesView.vue'), meta: { auth: true, household: true } },
  { path: '/budgets', component: () => import('../views/BudgetsView.vue'), meta: { auth: true, household: true } },
  { path: '/members', component: () => import('../views/MembersView.vue'), meta: { auth: true, household: true } },
  { path: '/categories', component: () => import('../views/CategoriesView.vue'), meta: { auth: true, household: true } },
  { path: '/recurring', component: () => import('../views/RecurringView.vue'), meta: { auth: true, household: true } },
  { path: '/audit-logs', component: () => import('../views/AuditLogsView.vue'), meta: { auth: true, household: true, parentOnly: true } },
  { path: '/profile', component: () => import('../views/ProfileView.vue'), meta: { auth: true, household: true } },
  { path: '/forbidden', component: () => import('../views/ForbiddenView.vue'), meta: { auth: true, household: true } },
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
  if (to.meta.parentOnly && !auth.isParent) return '/forbidden'
  if (to.path === '/onboarding' && auth.inHousehold) return '/dashboard'
})

export default router
