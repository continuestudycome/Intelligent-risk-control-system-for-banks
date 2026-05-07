import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { public: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register/index.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/customer/dashboard',
    children: [
      {
        path: 'customer/dashboard',
        name: 'CustomerDashboard',
        component: () => import('@/views/dashboard/customer.vue'),
        meta: { title: '客户首页', role: 'customer' }
      },
      {
        path: 'customer/transaction',
        redirect: '/customer/transaction/create'
      },
      {
        path: 'customer/transaction/create',
        name: 'CustomerTransactionCreate',
        component: () => import('@/views/customer/transaction-create.vue'),
        meta: { title: '交易', role: 'customer' }
      },
      {
        path: 'customer/transaction/records',
        name: 'CustomerTransactionRecords',
        component: () => import('@/views/customer/transaction-records.vue'),
        meta: { title: '交易记录', role: 'customer' }
      },
      {
        path: 'customer/application',
        name: 'CustomerApplication',
        component: () => import('@/views/customer/application.vue'),
        meta: { title: '贷款/信用卡申请', role: 'customer' }
      },
      {
        path: 'customer/progress',
        name: 'CustomerProgress',
        component: () => import('@/views/customer/progress.vue'),
        meta: { title: '申请进度', role: 'customer' }
      },
      {
        path: 'customer/credit-score',
        name: 'CustomerCreditScore',
        component: () => import('@/views/customer/credit-score.vue'),
        meta: { title: '个人信用评分', role: 'customer' }
      },
      {
        path: 'customer/service',
        name: 'CustomerService',
        component: () => import('@/views/customer/service.vue'),
        meta: { title: '智能客服咨询', role: 'customer' }
      },
      {
        path: 'customer/profile',
        name: 'CustomerProfile',
        component: () => import('@/views/customer/profile.vue'),
        meta: { title: '个人中心', role: 'customer' }
      },
      {
        path: 'risk/dashboard',
        name: 'RiskDashboard',
        component: () => import('@/views/dashboard/risk.vue'),
        meta: { title: '风控首页', role: 'risk' }
      },
      {
        path: 'risk/approval',
        name: 'RiskApproval',
        component: () => import('@/views/risk/approval.vue'),
        meta: { title: '审批贷款/信用卡', role: 'risk' }
      },
      {
        path: 'risk/risk-warning',
        name: 'RiskWarning',
        component: () => import('@/views/risk/risk-warning.vue'),
        meta: { title: '风险预警', role: 'risk' }
      },
      {
        path: 'risk/fraud-alert',
        name: 'RiskFraudAlert',
        component: () => import('@/views/risk/fraud-alert.vue'),
        meta: { title: '欺诈告警处理', role: 'risk' }
      },
      {
        path: 'risk/profile',
        name: 'RiskProfile',
        component: () => import('@/views/risk/profile.vue'),
        meta: { title: '客户风险画像', role: 'risk' }
      },
      {
        path: 'risk/rules',
        name: 'RiskRules',
        component: () => import('@/views/risk/rules.vue'),
        meta: { title: '风控规则调整', role: 'risk' }
      },
      {
        path: 'risk/knowledge',
        name: 'RiskKnowledge',
        component: () => import('@/views/risk/knowledge.vue'),
        meta: { title: '知识库管理', role: 'risk' }
      },
      {
        path: 'risk/chat-stat',
        name: 'RiskChatStat',
        component: () => import('@/views/risk/chat-stat.vue'),
        meta: { title: '客服会话统计', role: 'risk' }
      },
      {
        path: 'risk/customer-entry',
        name: 'RiskCustomerEntry',
        component: () => import('@/views/risk/customer-entry.vue'),
        meta: { title: '客户信息录入', role: 'risk' }
      },
      {
        path: 'risk/transaction-record',
        name: 'RiskTransactionRecord',
        component: () => import('@/views/risk/transaction-record.vue'),
        meta: { title: '交易记录查看', role: 'risk' }
      },
      {
        path: 'risk/credit-query',
        name: 'RiskCreditQuery',
        component: () => import('@/views/risk/credit-query.vue'),
        meta: { title: '信用评分查询', role: 'risk' }
      },
      {
        path: 'risk/limit-review',
        name: 'RiskLimitReview',
        component: () => import('@/views/risk/limit-review.vue'),
        meta: { title: '授信上调复核', role: 'risk' }
      },
      {
        path: 'risk/progress',
        name: 'RiskProgress',
        component: () => import('@/views/risk/progress.vue'),
        meta: { title: '审批进度查看', role: 'risk' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：返回目标路由或 true（Vue Router 4 推荐写法，勿再用 next()）
router.beforeEach((to) => {
  const userStore = useUserStore()

  if (to.meta.public) {
    if (userStore.isLoggedIn && (to.path === '/login' || to.path === '/register')) {
      return userStore.isRiskOfficer ? '/risk/dashboard' : '/customer/dashboard'
    }
    return true
  }

  if (!userStore.isLoggedIn) {
    return '/login'
  }

  const role = userStore.isRiskOfficer ? 'risk' : 'customer'
  if (to.meta.role && to.meta.role !== role) {
    return role === 'risk' ? '/risk/dashboard' : '/customer/dashboard'
  }

  // 客户资料完整性拦截：未完善时仅允许访问首页和个人中心
  if (
    role === 'customer' &&
    userStore.userInfo &&
    !userStore.userInfo.profileCompleted &&
    to.path.startsWith('/customer/') &&
    to.path !== '/customer/dashboard' &&
    to.path !== '/customer/profile'
  ) {
    return '/customer/profile'
  }

  return true
})

export default router
