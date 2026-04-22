<template>
  <div class="portal-home">
    <!-- ===== Hero Header ===== -->
    <section class="hero-header">
      <div class="hero-overlay"></div>
      <div class="hero-particles">
        <span v-for="i in 6" :key="i" class="particle" :style="particleStyle(i)"></span>
      </div>
      <div class="hero-content">
        <div class="hero-badge">智慧医疗平台</div>
        <h1 class="hero-title">专业医疗团队 · 智慧健康服务</h1>
        <p class="hero-subtitle">
          以患者为中心，提供便捷的在线挂号、AI智能问诊、报告查询、费用管理等一站式智慧医疗服务
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" round class="hero-btn-primary" @click="go('/appointment')">
            <el-icon><Calendar /></el-icon>
            立即预约挂号
          </el-button>
          <el-button size="large" round class="hero-btn-ghost" @click="go('/department')">
            <el-icon><OfficeBuilding /></el-icon>
            查看科室医生
          </el-button>
        </div>
      </div>
      <div class="hero-scroll-hint">
        <span class="scroll-arrow"></span>
      </div>
    </section>

    <!-- ===== Main Glass Card Container ===== -->
    <div class="main-glass-card">

      <!-- ===== Stats Counter Section ===== -->
      <section class="stats-section">
        <div class="stats-grid">
          <div v-for="stat in stats" :key="stat.label" class="stat-item">
            <div class="stat-icon" :class="stat.color">
              <el-icon :size="28"><component :is="stat.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <h3 class="stat-title">{{ stat.label }}</h3>
              <p class="stat-desc">{{ stat.desc }}</p>
            </div>
            <div class="stat-number">
              <span class="counter-value" :data-target="stat.value">{{ stat.display }}</span>
              <span class="counter-suffix">{{ stat.suffix }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- ===== 3D Flip Card + Info Grid ===== -->
      <section class="flip-info-section">
        <div class="flip-info-grid">
          <!-- Left: 3D Flip Card -->
          <div class="flip-card-wrapper">
            <div class="flip-card" @mouseenter="flipped = true" @mouseleave="flipped = false" :class="{ flipped }">
              <div class="flip-card-front">
                <div class="flip-front-icon">
                  <el-icon :size="48"><FirstAidKit /></el-icon>
                </div>
                <h3>智慧医疗系统</h3>
                <p>悬停翻转查看详情</p>
                <div class="flip-hint">
                  <el-icon><RefreshRight /></el-icon>
                </div>
              </div>
              <div class="flip-card-back">
                <h3>全方位健康管理</h3>
                <ul class="flip-back-list">
                  <li><el-icon><Check /></el-icon> 在线预约挂号</li>
                  <li><el-icon><Check /></el-icon> AI智能推荐</li>
                  <li><el-icon><Check /></el-icon> 报告实时查询</li>
                  <li><el-icon><Check /></el-icon> 费用透明管理</li>
                  <li><el-icon><Check /></el-icon> 药品信息查询</li>
                </ul>
              </div>
            </div>
          </div>

          <!-- Right: 2x2 Info Grid -->
          <div class="info-grid">
            <div v-for="info in infoCards" :key="info.title" class="info-card" @click="go(info.path)">
              <div class="info-card-icon" :class="info.color">
                <el-icon :size="24"><component :is="info.icon" /></el-icon>
              </div>
              <h4 class="info-card-title">{{ info.title }}</h4>
              <p class="info-card-desc">{{ info.desc }}</p>
              <span class="info-card-link">
                了解更多 <el-icon><ArrowRight /></el-icon>
              </span>
            </div>
          </div>
        </div>
      </section>

      <!-- ===== Showcase Section ===== -->
      <section class="showcase-section">
        <div class="section-header">
          <h2 class="section-title">功能服务</h2>
          <p class="section-desc">覆盖就医全流程，让您的健康之旅更加顺畅</p>
        </div>
        <div class="showcase-layout">
          <!-- Left: Sticky Category Nav -->
          <aside class="showcase-nav">
            <nav>
              <a
                v-for="(cat, idx) in showcaseCategories"
                :key="cat.key"
                :class="{ active: activeCategory === cat.key }"
                @click="activeCategory = cat.key"
              >
                <el-icon><component :is="cat.icon" /></el-icon>
                {{ cat.label }}
              </a>
            </nav>
          </aside>
          <!-- Right: Card Grid -->
          <div class="showcase-grid">
            <div
              v-for="item in filteredShowcaseItems"
              :key="item.path"
              class="showcase-card"
              @click="go(item.path)"
            >
              <div class="showcase-card-img" :class="item.color">
                <el-icon :size="36"><component :is="item.icon" /></el-icon>
              </div>
              <div class="showcase-card-body">
                <h4>{{ item.title }}</h4>
                <p>{{ item.desc }}</p>
              </div>
              <div class="showcase-card-footer">
                <span class="showcase-card-tag" :class="item.tagColor">{{ item.tag }}</span>
                <el-icon class="showcase-card-arrow"><ArrowRight /></el-icon>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ===== Page Links + Developer + Feature Cards ===== -->
      <section class="features-section">
        <div class="features-grid">
          <!-- Page Links Card -->
          <div class="feature-card feature-card--links">
            <div class="feature-card-header">
              <el-icon :size="24"><Link /></el-icon>
              <h3>快捷导航</h3>
            </div>
            <div class="feature-links">
              <a v-for="link in quickLinks" :key="link.path" @click="go(link.path)">
                <el-icon><component :is="link.icon" /></el-icon>
                {{ link.title }}
                <el-icon class="link-arrow"><ArrowRight /></el-icon>
              </a>
            </div>
          </div>

          <!-- Developer Info Card -->
          <div class="feature-card feature-card--dev">
            <div class="feature-card-header">
              <el-icon :size="24"><User /></el-icon>
              <h3>开发团队</h3>
            </div>
            <div class="dev-info">
              <div class="dev-avatar">
                <el-icon :size="40"><Monitor /></el-icon>
              </div>
              <div class="dev-detail">
                <h4>HIS 开发组</h4>
                <p>基于 Spring Boot + Vue 3 构建的智慧医院信息管理系统</p>
              </div>
              <div class="dev-stats">
                <div><strong>Vue 3</strong><span>前端框架</span></div>
                <div><strong>Spring</strong><span>后端服务</span></div>
                <div><strong>AI</strong><span>智能服务</span></div>
              </div>
            </div>
          </div>

          <!-- Feature Highlights Card -->
          <div class="feature-card feature-card--highlights">
            <div class="feature-card-header">
              <el-icon :size="24"><Star /></el-icon>
              <h3>核心特性</h3>
            </div>
            <div class="highlights-list">
              <div v-for="h in highlights" :key="h.title" class="highlight-item" :class="h.color">
                <el-icon :size="20"><component :is="h.icon" /></el-icon>
                <div>
                  <strong>{{ h.title }}</strong>
                  <span>{{ h.desc }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ===== Three Column Gradient Feature Cards ===== -->
      <section class="gradient-features">
        <div class="gradient-features-grid">
          <div v-for="(gf, idx) in gradientFeatures" :key="idx" class="gradient-feature-card" :style="{ background: gf.gradient }">
            <div class="gradient-feature-icon">
              <el-icon :size="32"><component :is="gf.icon" /></el-icon>
            </div>
            <h3>{{ gf.title }}</h3>
            <p>{{ gf.desc }}</p>
            <a class="gradient-feature-link" @click="go(gf.path)">
              立即体验 <el-icon><ArrowRight /></el-icon>
            </a>
          </div>
        </div>
      </section>
    </div>

    <!-- ===== CTA Dark Gradient Section ===== -->
    <section class="cta-section">
      <div class="cta-wave">
        <svg viewBox="0 0 1440 120" preserveAspectRatio="none">
          <path d="M0,64L48,69.3C96,75,192,85,288,80C384,75,480,53,576,48C672,43,768,53,864,64C960,75,1056,85,1152,80C1248,75,1344,53,1392,42.7L1440,32L1440,120L1392,120C1344,120,1248,120,1152,120C1056,120,960,120,864,120C768,120,672,120,576,120C480,120,384,120,288,120C192,120,96,120,48,120L0,120Z" fill="rgba(255,255,255,0.06)"/>
        </svg>
      </div>
      <div class="cta-content">
        <h2>开启您的智慧医疗之旅</h2>
        <p>一站式管理您的健康档案、预约挂号、检查报告，让医疗服务触手可及</p>
        <el-button size="large" round class="cta-btn" @click="go('/appointment')">
          <el-icon><Calendar /></el-icon>
          立即开始使用
        </el-button>
      </div>
    </section>

    <!-- ===== System Introduction Section ===== -->
    <section class="intro-section">
      <div class="intro-wrapper">
        <!-- Left: Horizontal Card (overlaps ~1/5 onto image) -->
        <div class="intro-card">
          <div class="intro-card-top">
            <h2 class="intro-card-heading">系统介绍</h2>
            <p class="intro-card-body">
              本系统是一个基于 <strong>Spring Boot + Vue 3</strong> 前后端分离架构构建的智慧医院信息管理平台，
              涵盖预约挂号、病历管理、医疗报告、费用查询、药品信息、科室导航等核心功能模块，
              并集成 AI 智能推荐、AI 报告解读、AI 费用解释等前沿智能服务。
            </p>
          </div>
          <el-button round class="intro-card-btn" @click="go('/about')">
            <el-icon><ArrowRight /></el-icon>
            查看更多
          </el-button>
        </div>
        <!-- Right: Image -->
        <div class="intro-image">
          <img
            src="https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?w=800&q=80"
            alt="智慧医疗"
          />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  Calendar,
  Check,
  Document,
  FirstAidKit,
  Link,
  Monitor,
  Notebook,
  OfficeBuilding,
  RefreshRight,
  Star,
  TrendCharts,
  User,
  Wallet,
} from '@element-plus/icons-vue'

const router = useRouter()
function go(path: string) {
  router.push(path)
}

// ===== Hero Particles =====
function particleStyle(i: number) {
  const size = 4 + Math.random() * 6
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${10 + i * 15}%`,
    top: `${20 + (i % 3) * 20}%`,
    animationDelay: `${i * 0.8}s`,
    animationDuration: `${3 + i * 0.5}s`,
  }
}

// ===== Stats =====
const stats = ref([
  { label: '功能模块', desc: '覆盖就诊全流程服务', value: 6, display: 0, suffix: '+', icon: 'Grid', color: 'stat-blue' },
  { label: 'AI 智能服务', desc: 'AI推荐·AI问诊·AI解读', value: 3, display: 0, suffix: '', icon: 'MagicStick', color: 'stat-violet' },
  { label: '全天候在线', desc: '7×24小时智慧服务', value: 24, display: 0, suffix: 'h', icon: 'Timer', color: 'stat-cyan' },
])

let animFrame: number | null = null
function animateCounters() {
  const duration = 2000
  const start = performance.now()
  function step(now: number) {
    const elapsed = now - start
    const progress = Math.min(elapsed / duration, 1)
    const ease = 1 - Math.pow(1 - progress, 3)
    stats.value.forEach(s => {
      s.display = Math.round(s.value * ease)
    })
    if (progress < 1) {
      animFrame = requestAnimationFrame(step)
    }
  }
  animFrame = requestAnimationFrame(step)
}

onMounted(() => {
  setTimeout(animateCounters, 600)
})
onUnmounted(() => {
  if (animFrame) cancelAnimationFrame(animFrame)
})

// ===== 3D Flip =====
const flipped = ref(false)

// ===== Info Grid =====
const infoCards = [
  { title: '预约挂号', desc: '智能推荐科室医生，灵活选择排班时段', icon: Calendar, color: 'info-blue', path: '/appointment' },
  { title: '病历就诊', desc: '历史就诊记录与处方一键回溯查询', icon: Document, color: 'info-violet', path: '/records' },
  { title: '医疗报告', desc: '检查检验结果实时查看，健康数据了然于心', icon: Notebook, color: 'info-cyan', path: '/report' },
  { title: '费用查询', desc: '缴费记录与费用明细清晰透明', icon: Wallet, color: 'info-indigo', path: '/billing' },
]

// ===== Showcase =====
const activeCategory = ref('all')
const showcaseCategories = [
  { key: 'all', label: '全部功能', icon: 'Grid' },
  { key: 'core', label: '核心服务', icon: 'FirstAidKit' },
  { key: 'ai', label: 'AI 功能', icon: 'MagicStick' },
  { key: 'info', label: '信息查询', icon: 'Search' },
]

const showcaseItems = [
  { path: '/appointment', title: '预约挂号', desc: '智能推荐科室医生，灵活选择排班时段', icon: Calendar, color: 'bg-blue', tag: '核心', tagColor: 'tag-blue', category: 'core' },
  { path: '/records', title: '病历与就诊', desc: '历史就诊记录与处方一键回溯查询', icon: Document, color: 'bg-violet', tag: '核心', tagColor: 'tag-violet', category: 'core' },
  { path: '/report', title: '医疗报告', desc: 'AI解读检查检验结果，健康数据了然于心', icon: Notebook, color: 'bg-cyan', tag: 'AI', tagColor: 'tag-cyan', category: 'ai' },
  { path: '/billing', title: '费用查询', desc: 'AI费用解释，缴费记录与费用明细清晰透明', icon: Wallet, color: 'bg-indigo', tag: 'AI', tagColor: 'tag-indigo', category: 'ai' },
  { path: '/medicine', title: '药品查询', desc: 'AI药品推荐，药品信息与用法用量查询', icon: FirstAidKit, color: 'bg-green', tag: 'AI', tagColor: 'tag-green', category: 'ai' },
  { path: '/department', title: '科室信息', desc: '科室介绍与医生团队一览无余', icon: OfficeBuilding, color: 'bg-orange', tag: '信息', tagColor: 'tag-orange', category: 'info' },
]

const filteredShowcaseItems = computed(() => {
  if (activeCategory.value === 'all') return showcaseItems
  return showcaseItems.filter(i => i.category === activeCategory.value)
})

// ===== Quick Links =====
const quickLinks = [
  { path: '/appointment', title: '预约挂号', icon: Calendar },
  { path: '/records', title: '病历与就诊', icon: Document },
  { path: '/report', title: '医疗报告', icon: Notebook },
  { path: '/billing', title: '费用查询', icon: Wallet },
  { path: '/medicine', title: '药品查询', icon: FirstAidKit },
  { path: '/department', title: '科室信息', icon: OfficeBuilding },
]

// ===== Highlights =====
const highlights = [
  { title: 'AI 智能推荐', desc: '根据症状智能推荐科室与医生', icon: 'MagicStick', color: 'hl-blue' },
  { title: 'SSE 流式报告', desc: 'AI实时生成检查报告解读', icon: 'Document', color: 'hl-violet' },
  { title: '数据安全', desc: '全链路加密，隐私保护合规', icon: 'Lock', color: 'hl-cyan' },
  { title: '响应式设计', desc: '适配手机、平板、桌面多端', icon: 'Monitor', color: 'hl-green' },
]

// ===== Gradient Features =====
const gradientFeatures = [
  { title: '便捷预约', desc: '在线选择科室医生，智能排班推荐，告别排队等待', icon: Calendar, gradient: 'linear-gradient(195deg, #409eff 0%, #2f80ed 100%)', path: '/appointment' },
  { title: 'AI 智能问诊', desc: 'AI解读报告、推荐药品、解释费用，全方位智能服务', icon: 'MagicStick', gradient: 'linear-gradient(195deg, #8b5cf6 0%, #6d28d9 100%)', path: '/report' },
  { title: '透明管理', desc: '费用明细清晰可查，病历报告随时回溯，就医更安心', icon: TrendCharts, gradient: 'linear-gradient(195deg, #00b4d8 0%, #0077b6 100%)', path: '/billing' },
]


</script>

<style scoped>
/* ============================================================
   CSS Variables
   ============================================================ */
:root {
  --home-radius: 40px;
  --home-radius-sm: 20px;
  --home-radius-xs: 12px;
  --home-shadow: 0 20px 60px rgba(15, 23, 42, 0.08), 0 4px 20px rgba(15, 23, 42, 0.04);
  --home-shadow-hover: 0 25px 50px rgba(15, 23, 42, 0.15), 0 8px 24px rgba(15, 23, 42, 0.08);
  --home-glass-bg: rgba(255, 255, 255, 0.72);
  --home-glass-blur: saturate(200%) blur(30px);
  --home-blue: #409eff;
  --home-violet: #8b5cf6;
  --home-cyan: #00b4d8;
  --home-indigo: #667eea;
  --home-green: #43c6ac;
  --home-orange: #f6a623;
}

/* ============================================================
   Base
   ============================================================ */
.portal-home {
  background: #f0f4f8;
  min-height: 100%;
  overflow-x: hidden;
}

.section-header {
  text-align: center;
  margin-bottom: 40px;
}
.section-title {
  font-size: 28px;
  font-weight: 800;
  color: #1a2a4a;
  letter-spacing: 1px;
  margin-bottom: 8px;
}
.section-desc {
  font-size: 15px;
  color: #8c9ab5;
  line-height: 1.6;
}

/* ============================================================
   Hero Header
   ============================================================ */
.hero-header {
  position: relative;
  width: 100%;
  min-height: 75vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: url('https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?w=1920&q=80') center/cover no-repeat;
  overflow: hidden;
  margin-top: -60px;
  padding-top: 60px;
}
.hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    195deg,
    rgba(64, 158, 255, 0.92) 0%,
    rgba(47, 128, 237, 0.85) 40%,
    rgba(139, 92, 246, 0.75) 100%
  );
  z-index: 1;
}
.hero-particles {
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
}
.particle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  animation: particleFloat 4s ease-in-out infinite alternate;
}
@keyframes particleFloat {
  0% { transform: translateY(0) scale(1); opacity: 0.3; }
  100% { transform: translateY(-30px) scale(1.2); opacity: 0.6; }
}
.hero-content {
  position: relative;
  z-index: 3;
  text-align: center;
  padding: 80px 24px 60px;
  max-width: 800px;
}
.hero-badge {
  display: inline-block;
  padding: 6px 20px;
  border-radius: 50px;
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 1px;
  margin-bottom: 24px;
}
.hero-title {
  font-size: 52px;
  font-weight: 800;
  color: #fff;
  letter-spacing: 2px;
  margin-bottom: 20px;
  text-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  line-height: 1.2;
}
.hero-subtitle {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.8;
  margin-bottom: 36px;
  letter-spacing: 0.5px;
}
.hero-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
}
.hero-btn-primary {
  background: #fff !important;
  color: var(--home-blue) !important;
  border-color: #fff !important;
  font-weight: 700;
  padding: 14px 36px !important;
  height: auto !important;
  font-size: 16px !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease !important;
}
.hero-btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.2) !important;
}
.hero-btn-ghost {
  background: rgba(255, 255, 255, 0.15) !important;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  color: #fff !important;
  border-color: rgba(255, 255, 255, 0.4) !important;
  font-weight: 600;
  padding: 14px 36px !important;
  height: auto !important;
  font-size: 16px !important;
  transition: all 0.3s ease !important;
}
.hero-btn-ghost:hover {
  background: rgba(255, 255, 255, 0.28) !important;
  border-color: rgba(255, 255, 255, 0.6) !important;
  color: #fff !important;
  transform: translateY(-2px);
}
.hero-scroll-hint {
  position: absolute;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 3;
}
.scroll-arrow {
  display: block;
  width: 24px;
  height: 24px;
  border-right: 2px solid rgba(255, 255, 255, 0.6);
  border-bottom: 2px solid rgba(255, 255, 255, 0.6);
  transform: rotate(45deg);
  animation: scrollBounce 2s ease-in-out infinite;
}
@keyframes scrollBounce {
  0%, 100% { transform: rotate(45deg) translateY(0); opacity: 0.6; }
  50% { transform: rotate(45deg) translateY(8px); opacity: 1; }
}

/* ============================================================
   Main Glass Card
   ============================================================ */
.main-glass-card {
  position: relative;
  z-index: 10;
  max-width: 1200px;
  margin: 0 auto 0;
  padding: 48px 40px;
  background: var(--home-glass-bg);
  backdrop-filter: var(--home-glass-blur);
  -webkit-backdrop-filter: var(--home-glass-blur);
  border-radius: var(--home-radius);
  box-shadow:
    var(--home-shadow),
    0 0 80px rgba(64, 158, 255, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.5);
}

/* ============================================================
   Stats Counter Section
   ============================================================ */
.stats-section {
  margin-bottom: 56px;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 32px;
}
.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 28px 24px;
  border-radius: var(--home-radius-sm);
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.04);
  transition: all 0.3s ease;
}
.stat-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}
.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.stat-blue   { background: linear-gradient(135deg, #409eff, #2f80ed); }
.stat-violet { background: linear-gradient(135deg, #8b5cf6, #7c3aed); }
.stat-cyan   { background: linear-gradient(135deg, #00b4d8, #0077b6); }
.stat-info {
  flex: 1;
  min-width: 0;
}
.stat-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a2a4a;
  margin-bottom: 4px;
}
.stat-desc {
  font-size: 13px;
  color: #8c9ab5;
  line-height: 1.5;
}
.stat-number {
  font-size: 36px;
  font-weight: 800;
  background: linear-gradient(135deg, #409eff, #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  flex-shrink: 0;
}
.counter-suffix {
  font-size: 18px;
  font-weight: 600;
}

/* ============================================================
   3D Flip Card + Info Grid
   ============================================================ */
.flip-info-section {
  margin-bottom: 56px;
}
.flip-info-grid {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 32px;
  align-items: start;
}
.flip-card-wrapper {
  perspective: 1000px;
}
.flip-card {
  width: 100%;
  min-height: 320px;
  position: relative;
  transform-style: preserve-3d;
  transition: transform 0.7s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
}
.flip-card.flipped {
  transform: rotateY(180deg);
}
.flip-card-front,
.flip-card-back {
  position: absolute;
  inset: 0;
  backface-visibility: hidden;
  border-radius: var(--home-radius-sm);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px;
}
.flip-card-front {
  background: linear-gradient(195deg, #409eff 0%, #2f80ed 50%, #8b5cf6 100%);
  color: #fff;
  box-shadow: 0 10px 30px rgba(64, 158, 255, 0.3);
}
.flip-front-icon {
  margin-bottom: 16px;
  opacity: 0.9;
}
.flip-card-front h3 {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 8px;
}
.flip-card-front p {
  font-size: 14px;
  opacity: 0.8;
}
.flip-hint {
  margin-top: 16px;
  animation: flipHintPulse 2s ease-in-out infinite;
}
@keyframes flipHintPulse {
  0%, 100% { opacity: 0.5; transform: rotate(0deg); }
  50% { opacity: 1; transform: rotate(180deg); }
}
.flip-card-back {
  background: #fff;
  transform: rotateY(180deg);
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
  text-align: left;
  align-items: flex-start;
}
.flip-card-back h3 {
  font-size: 20px;
  font-weight: 700;
  color: #1a2a4a;
  margin-bottom: 16px;
}
.flip-back-list {
  list-style: none;
  padding: 0;
  margin: 0;
  width: 100%;
}
.flip-back-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  font-size: 15px;
  color: #4a5568;
  border-bottom: 1px solid #f0f0f0;
}
.flip-back-list li:last-child {
  border-bottom: none;
}
.flip-back-list .el-icon {
  color: var(--home-green);
}

/* Info Grid */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}
.info-card {
  padding: 24px;
  border-radius: var(--home-radius-sm);
  background: rgba(255, 255, 255, 0.65);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.04);
  cursor: pointer;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  text-align: left;
}
.info-card:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.1);
}
.info-card-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-bottom: 14px;
}
.info-blue   { background: linear-gradient(135deg, #409eff, #53a8ff); }
.info-violet { background: linear-gradient(135deg, #8b5cf6, #a78bfa); }
.info-cyan   { background: linear-gradient(135deg, #00b4d8, #0099ff); }
.info-indigo { background: linear-gradient(135deg, #667eea, #764ba2); }
.info-card-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a2a4a;
  margin-bottom: 6px;
}
.info-card-desc {
  font-size: 13px;
  color: #8c9ab5;
  line-height: 1.6;
  margin-bottom: 12px;
}
.info-card-link {
  font-size: 13px;
  color: var(--home-blue);
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: gap 0.3s ease;
}
.info-card:hover .info-card-link {
  gap: 8px;
}

/* ============================================================
   Showcase Section
   ============================================================ */
.showcase-section {
  margin-bottom: 56px;
}
.showcase-layout {
  display: grid;
  grid-template-columns: 200px 1fr;
  gap: 32px;
  align-items: start;
}
.showcase-nav {
  position: sticky;
  top: 80px;
}
.showcase-nav nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  border-radius: var(--home-radius-sm);
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.4);
}
.showcase-nav a {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
  cursor: pointer;
  transition: all 0.25s ease;
  text-decoration: none;
}
.showcase-nav a:hover {
  background: rgba(64, 158, 255, 0.08);
  color: var(--home-blue);
}
.showcase-nav a.active {
  background: linear-gradient(135deg, #409eff, #2f80ed);
  color: #fff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}
.showcase-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
.showcase-card {
  padding: 24px;
  border-radius: var(--home-radius-sm);
  background: rgba(255, 255, 255, 0.65);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.04);
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
}
.showcase-card:hover {
  transform: perspective(800px) rotateX(2deg) rotateY(-2deg) translateY(-8px);
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.12);
}
.showcase-card-img {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-bottom: 16px;
}
.bg-blue   { background: linear-gradient(135deg, #409eff, #53a8ff); }
.bg-violet { background: linear-gradient(135deg, #8b5cf6, #a78bfa); }
.bg-cyan   { background: linear-gradient(135deg, #00b4d8, #0099ff); }
.bg-indigo { background: linear-gradient(135deg, #667eea, #764ba2); }
.bg-green  { background: linear-gradient(135deg, #43c6ac, #68d391); }
.bg-orange { background: linear-gradient(135deg, #f6a623, #f7b955); }
.showcase-card-body h4 {
  font-size: 16px;
  font-weight: 700;
  color: #1a2a4a;
  margin-bottom: 6px;
}
.showcase-card-body p {
  font-size: 13px;
  color: #8c9ab5;
  line-height: 1.6;
  margin-bottom: 16px;
}
.showcase-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
}
.showcase-card-tag {
  font-size: 11px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 50px;
  letter-spacing: 0.5px;
}
.tag-blue   { background: rgba(64, 158, 255, 0.1); color: #409eff; }
.tag-violet { background: rgba(139, 92, 246, 0.1); color: #8b5cf6; }
.tag-cyan   { background: rgba(0, 180, 216, 0.1); color: #00b4d8; }
.tag-indigo { background: rgba(102, 126, 234, 0.1); color: #667eea; }
.tag-green  { background: rgba(67, 198, 172, 0.1); color: #43c6ac; }
.tag-orange { background: rgba(246, 166, 35, 0.1); color: #f6a623; }
.showcase-card-arrow {
  color: #c0c8d8;
  transition: all 0.3s ease;
}
.showcase-card:hover .showcase-card-arrow {
  color: var(--home-blue);
  transform: translateX(4px);
}

/* ============================================================
   Features Section
   ============================================================ */
.features-section {
  margin-bottom: 56px;
}
.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}
.feature-card {
  padding: 28px;
  border-radius: var(--home-radius-sm);
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.04);
  transition: all 0.3s ease;
}
.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);
}
.feature-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  color: var(--home-blue);
}
.feature-card-header h3 {
  font-size: 18px;
  font-weight: 700;
  color: #1a2a4a;
  margin: 0;
}
/* Links Card */
.feature-links a {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  font-size: 14px;
  color: #4a5568;
  cursor: pointer;
  transition: all 0.25s ease;
  border-bottom: 1px solid #f0f0f0;
}
.feature-links a:last-child {
  border-bottom: none;
}
.feature-links a:hover {
  color: var(--home-blue);
  padding-left: 4px;
}
.link-arrow {
  margin-left: auto;
  opacity: 0;
  transition: all 0.25s ease;
}
.feature-links a:hover .link-arrow {
  opacity: 1;
  transform: translateX(4px);
}
/* Dev Card */
.dev-info {
  text-align: center;
}
.dev-avatar {
  width: 72px;
  height: 72px;
  border-radius: 20px;
  background: linear-gradient(135deg, #409eff, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin: 0 auto 16px;
}
.dev-detail h4 {
  font-size: 16px;
  font-weight: 700;
  color: #1a2a4a;
  margin-bottom: 6px;
}
.dev-detail p {
  font-size: 13px;
  color: #8c9ab5;
  line-height: 1.6;
  margin-bottom: 20px;
}
.dev-stats {
  display: flex;
  gap: 16px;
  justify-content: center;
}
.dev-stats div {
  text-align: center;
}
.dev-stats strong {
  display: block;
  font-size: 15px;
  color: #1a2a4a;
}
.dev-stats span {
  font-size: 12px;
  color: #8c9ab5;
}
/* Highlights Card */
.highlights-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.highlight-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.5);
  transition: all 0.25s ease;
}
.highlight-item:hover {
  background: rgba(255, 255, 255, 0.8);
  transform: translateX(4px);
}
.highlight-item .el-icon {
  flex-shrink: 0;
}
.highlight-item strong {
  display: block;
  font-size: 14px;
  color: #1a2a4a;
}
.highlight-item span {
  font-size: 12px;
  color: #8c9ab5;
}
.hl-blue   .el-icon { color: var(--home-blue); }
.hl-violet .el-icon { color: var(--home-violet); }
.hl-cyan   .el-icon { color: var(--home-cyan); }
.hl-green  .el-icon { color: var(--home-green); }

/* ============================================================
   Gradient Feature Cards
   ============================================================ */
.gradient-features {
  margin-bottom: 16px;
}
.gradient-features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}
.gradient-feature-card {
  padding: 36px 28px;
  border-radius: var(--home-radius-sm);
  color: #fff;
  position: relative;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
}
.gradient-feature-card::before {
  content: '';
  position: absolute;
  top: -30%;
  right: -20%;
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  transition: all 0.4s ease;
}
.gradient-feature-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
}
.gradient-feature-card:hover::before {
  transform: scale(1.5);
}
.gradient-feature-icon {
  margin-bottom: 16px;
  opacity: 0.9;
}
.gradient-feature-card h3 {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 10px;
}
.gradient-feature-card p {
  font-size: 14px;
  opacity: 0.85;
  line-height: 1.7;
  margin-bottom: 20px;
}
.gradient-feature-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
  transition: gap 0.3s ease;
}
.gradient-feature-link:hover {
  gap: 10px;
}

/* ============================================================
   CTA Section
   ============================================================ */
.cta-section {
  position: relative;
  background: linear-gradient(195deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  padding: 80px 24px;
  text-align: center;
  overflow: hidden;
}
.cta-wave {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 1;
}
.cta-wave svg {
  display: block;
  width: 100%;
  height: 60px;
}
.cta-content {
  position: relative;
  z-index: 2;
  max-width: 600px;
  margin: 0 auto;
}
.cta-content h2 {
  font-size: 32px;
  font-weight: 800;
  color: #fff;
  margin-bottom: 16px;
  letter-spacing: 1px;
}
.cta-content p {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.75);
  line-height: 1.8;
  margin-bottom: 32px;
}
.cta-btn {
  background: linear-gradient(135deg, #409eff, #8b5cf6) !important;
  color: #fff !important;
  border: none !important;
  font-weight: 700;
  padding: 14px 40px !important;
  height: auto !important;
  font-size: 16px !important;
  box-shadow: 0 8px 24px rgba(64, 158, 255, 0.4);
  transition: all 0.3s ease !important;
}
.cta-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(64, 158, 255, 0.5) !important;
}

/* ============================================================
   System Introduction Section
   ============================================================ */
.intro-section {
  padding: 80px 24px;
  background: #f0f4f8;
}
.intro-wrapper {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr 1fr;
  position: relative;
  min-height: 400px;
}
/* Left: Horizontal card, right edge overlaps ~1/5 onto image */
.intro-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 48px 56px;
  background: #fff;
  position: relative;
  z-index: 2;
  border-radius: 20px 0 0 20px;
  box-shadow: 8px 20px 60px rgba(15, 23, 42, 0.1), 4px 4px 20px rgba(15, 23, 42, 0.05);
  margin-right: -20%;
}
.intro-card-top {
  margin-bottom: 28px;
}
.intro-card-heading {
  font-size: 28px;
  font-weight: 800;
  color: #1a2a4a;
  text-align: left;
  margin-bottom: 20px;
  letter-spacing: 1px;
  position: relative;
  padding-bottom: 16px;
}
.intro-card-heading::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  width: 48px;
  height: 4px;
  border-radius: 2px;
  background: linear-gradient(135deg, #409eff, #2f80ed);
}
.intro-card-body {
  font-size: 15px;
  color: #64748b;
  line-height: 1.9;
  text-align: left;
  margin: 0;
  font-family: -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif;
}
.intro-card-body strong {
  color: #409eff;
}
.intro-card-btn {
  align-self: flex-start;
  background: linear-gradient(135deg, #409eff, #2f80ed) !important;
  color: #fff !important;
  border: none !important;
  font-weight: 600;
  padding: 12px 28px !important;
  height: auto !important;
  font-size: 15px !important;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.3);
  transition: all 0.3s ease !important;
}
.intro-card-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(64, 158, 255, 0.4) !important;
}
/* Right Image */
.intro-image {
  overflow: hidden;
  position: relative;
  border-radius: 20px;
}
.intro-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.6s ease;
}
.intro-image:hover img {
  transform: scale(1.03);
}

/* ============================================================
   Responsive Design
   ============================================================ */
@media (max-width: 1200px) {
  .main-glass-card {
    margin-left: 16px;
    margin-right: 16px;
    padding: 36px 28px;
  }
}

@media (max-width: 992px) {
  .hero-header {
    min-height: 60vh;
  }
  .hero-title {
    font-size: 38px;
  }
  .hero-subtitle {
    font-size: 16px;
  }
  .main-glass-card {
    margin: -60px 12px 0;
    padding: 32px 20px;
    border-radius: 28px;
  }
  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
  }
  .stat-item {
    padding: 20px 16px;
    flex-direction: column;
    text-align: center;
  }
  .stat-number {
    font-size: 28px;
  }
  .flip-info-grid {
    grid-template-columns: 1fr;
  }
  .flip-card {
    min-height: 260px;
  }
  .showcase-layout {
    grid-template-columns: 1fr;
  }
  .showcase-nav {
    position: static;
  }
  .showcase-nav nav {
    flex-direction: row;
    overflow-x: auto;
    gap: 8px;
  }
  .showcase-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .features-grid {
    grid-template-columns: 1fr;
  }
  .gradient-features-grid {
    grid-template-columns: 1fr;
  }
  .intro-wrapper {
    grid-template-columns: 1fr;
  }
  .intro-card {
    margin-right: 0;
    border-radius: 20px 20px 0 0;
    padding: 40px 32px;
  }
  .intro-image {
    min-height: 300px;
    border-radius: 0 0 20px 20px;
  }
}

@media (max-width: 768px) {
  .hero-header {
    min-height: 50vh;
    margin-top: -50px;
    padding-top: 50px;
  }
  .hero-title {
    font-size: 30px;
  }
  .hero-content {
    padding: 50px 16px 40px;
  }
  .stats-grid {
    grid-template-columns: 1fr;
  }
  .info-grid {
    grid-template-columns: 1fr;
  }
  .showcase-grid {
    grid-template-columns: 1fr;
  }
  .main-glass-card {
    margin: -40px 8px 0;
    padding: 24px 16px;
    border-radius: 24px;
  }
  .section-title {
    font-size: 22px;
  }
  .gradient-features-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 576px) {
  .hero-header {
    min-height: 45vh;
  }
  .hero-title {
    font-size: 26px;
  }
  .hero-subtitle {
    font-size: 14px;
  }
  .hero-actions {
    flex-direction: column;
    align-items: center;
  }
  .hero-actions .el-button {
    width: 100%;
    max-width: 280px;
  }
  .main-glass-card {
    border-radius: 20px;
    padding: 20px 12px;
  }
  .stat-item {
    padding: 16px;
  }
  .stat-number {
    font-size: 24px;
  }
  .cta-content h2 {
    font-size: 24px;
  }
  .cta-content p {
    font-size: 14px;
  }
  .intro-wrapper {
    grid-template-columns: 1fr;
  }
  .intro-card {
    margin-right: 0;
    border-radius: 16px 16px 0 0;
    padding: 32px 20px;
  }
  .intro-card-heading {
    font-size: 22px;
  }
  .intro-image {
    min-height: 240px;
    border-radius: 0 0 16px 16px;
  }
}
</style>
