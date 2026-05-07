<template>
  <el-container class="main-layout" :class="{ 'risk-portal': isRiskOfficer }">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <span>{{ layoutTitle }}</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        class="sidebar-menu"
        :background-color="menuBg"
        text-color="#bfcbd9"
        :active-text-color="menuActive"
      >
        <template v-for="item in menuItems" :key="item.index">
          <el-sub-menu v-if="item.children?.length" :index="item.index">
            <template #title>
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item v-for="child in item.children" :key="child.index" :index="child.index">
              <el-icon><component :is="child.icon" /></el-icon>
              <span>{{ child.title }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="item.index">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <span class="page-title">{{ route.meta.title || '工作台' }}</span>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :icon="UserFilled" />
              <span class="username">{{ userStore.userInfo?.realName || userStore.userInfo?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="settings">系统设置</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  HomeFilled, UserFilled, ArrowDown, Money, Document, Clock, DataAnalysis, ChatDotRound, Check,
  WarningFilled, Bell, User, Setting, Collection, TrendCharts, EditPen, Histogram
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const isRiskOfficer = computed(() => userStore.isRiskOfficer)

const menuBg = computed(() => (isRiskOfficer.value ? '#1a1f2e' : '#304156'))
const menuActive = computed(() => (isRiskOfficer.value ? '#E6A23C' : '#409EFF'))

const layoutTitle = computed(() => isRiskOfficer.value ? '智能风控 · 运营台' : '智能银行 · 客户中心')

const customerMenus = [
  { index: '/customer/dashboard', title: '总览', icon: HomeFilled },
  { index: '/customer/profile', title: '个人中心', icon: User },
  {
    index: '/customer/transaction',
    title: '进行交易',
    icon: Money,
    children: [
      { index: '/customer/transaction/create', title: '交易', icon: Money },
      { index: '/customer/transaction/records', title: '交易记录', icon: Clock }
    ]
  },
  { index: '/customer/application', title: '贷款/信用卡申请', icon: Document },
  { index: '/customer/progress', title: '申请进度', icon: Clock },
  { index: '/customer/credit-score', title: '个人信用评分', icon: DataAnalysis },
  { index: '/customer/service', title: '智能客服咨询', icon: ChatDotRound }
]

const riskMenus = [
  { index: '/risk/dashboard', title: '总览', icon: HomeFilled },
  { index: '/risk/approval', title: '审批贷款/信用卡', icon: Check },
  { index: '/risk/risk-warning', title: '风险预警', icon: WarningFilled },
  { index: '/risk/fraud-alert', title: '欺诈告警处理', icon: Bell },
  { index: '/risk/profile', title: '客户风险画像', icon: User },
  { index: '/risk/rules', title: '风控规则调整', icon: Setting },
  { index: '/risk/knowledge', title: '知识库管理', icon: Collection },
  { index: '/risk/chat-stat', title: '客服会话统计', icon: TrendCharts },
  { index: '/risk/customer-entry', title: '客户信息录入', icon: EditPen },
  { index: '/risk/transaction-record', title: '交易记录查看', icon: Money },
  { index: '/risk/credit-query', title: '信用评分查询', icon: DataAnalysis },
  { index: '/risk/limit-review', title: '授信上调复核', icon: Money },
  { index: '/risk/progress', title: '审批进度查看', icon: Histogram }
]

const menuItems = computed(() => isRiskOfficer.value ? riskMenus : customerMenus)

const handleCommand = async (command) => {
  switch (command) {
    case 'profile':
      router.push(isRiskOfficer.value ? '/risk/dashboard' : '/customer/profile')
      break
    case 'settings':
      router.push(isRiskOfficer.value ? '/risk/rules' : '/customer/service')
      break
    case 'logout':
      try {
        await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await userStore.logout()
        ElMessage.success('已退出登录')
        router.push('/login')
      } catch {
        // 取消退出
      }
      break
  }
}
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
}

.main-layout.risk-portal .header {
  border-bottom: 3px solid #e6a23c;
}

.sidebar {
  background-color: #304156;
}

.main-layout.risk-portal .sidebar {
  background-color: #1a1f2e;
}

.main-layout.risk-portal .main-content {
  background: #eaeef5;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  border-bottom: 1px solid #1f2d3d;
}

.sidebar-menu {
  border-right: none;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 0 8px;
}

.username {
  margin: 0 8px;
  font-size: 14px;
  color: #606266;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.main-content {
  background: #f0f2f5;
  padding: 20px;
}
</style>
