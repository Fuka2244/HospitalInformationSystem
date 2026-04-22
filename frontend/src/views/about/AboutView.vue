<template>
  <div class="about-page">
    <!-- Hero Banner -->
    <section class="about-hero">
      <div class="about-hero-overlay"></div>
      <div class="about-hero-content">
        <h1>系统概况</h1>
        <p>基于现代技术架构构建的智慧医院信息管理系统</p>
      </div>
    </section>

    <!-- Overview Section -->
    <section class="about-section">
      <div class="about-container">
        <div class="section-header">
          <h2 class="section-title">系统简介</h2>
          <div class="title-bar"></div>
        </div>
        <div class="overview-content">
          <div class="overview-text">
            <p>
              HIS 智慧医院信息管理系统是一个基于 <strong>Spring Boot + Vue 3</strong> 前后端分离架构构建的综合性医疗信息管理平台。
              系统涵盖了预约挂号、病历管理、医疗报告、费用查询、药品信息、科室导航等核心功能模块，
              并创新性地集成了 AI 智能推荐、AI 报告解读、AI 费用解释等前沿智能服务。
            </p>
            <p>
              我们致力于通过信息化手段，让医疗服务更高效、更透明、更智能，为患者提供便捷的就医体验，
              为医护人员提供高效的工作工具，为医院管理者提供科学的决策支持。
            </p>
          </div>
          <div class="overview-image">
            <img src="https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?w=600&q=80" alt="智慧医疗" />
          </div>
        </div>
      </div>
    </section>

    <!-- Architecture Section -->
    <section class="about-section about-section--alt">
      <div class="about-container">
        <div class="section-header">
          <h2 class="section-title">技术架构</h2>
          <div class="title-bar"></div>
        </div>
        <div class="arch-grid">
          <div class="arch-card" v-for="arch in architectures" :key="arch.title">
            <div class="arch-icon" :class="arch.color">
              <el-icon :size="28"><component :is="arch.icon" /></el-icon>
            </div>
            <h3>{{ arch.title }}</h3>
            <p>{{ arch.desc }}</p>
            <div class="arch-tags">
              <span v-for="tag in arch.tags" :key="tag">{{ tag }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section class="about-section">
      <div class="about-container">
        <div class="section-header">
          <h2 class="section-title">核心功能</h2>
          <div class="title-bar"></div>
        </div>
        <div class="feature-grid">
          <div class="feature-item" v-for="feat in features" :key="feat.title">
            <div class="feature-icon" :class="feat.color">
              <el-icon :size="24"><component :is="feat.icon" /></el-icon>
            </div>
            <div class="feature-info">
              <h4>{{ feat.title }}</h4>
              <p>{{ feat.desc }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- AI Services Section -->
    <section class="about-section about-section--dark">
      <div class="about-container">
        <div class="section-header">
          <h2 class="section-title" style="color: #fff;">AI 智能服务</h2>
          <div class="title-bar" style="background: rgba(255,255,255,0.3);"></div>
          <p class="section-subtitle">融合前沿人工智能技术，打造智能化医疗服务体验</p>
        </div>
        <div class="ai-grid">
          <div class="ai-card" v-for="ai in aiServices" :key="ai.title">
            <div class="ai-icon">
              <el-icon :size="32"><component :is="ai.icon" /></el-icon>
            </div>
            <h3>{{ ai.title }}</h3>
            <p>{{ ai.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Advantages Section -->
    <section class="about-section">
      <div class="about-container">
        <div class="section-header">
          <h2 class="section-title">系统优势</h2>
          <div class="title-bar"></div>
        </div>
        <div class="adv-grid">
          <div class="adv-item" v-for="adv in advantages" :key="adv.title">
            <div class="adv-number">{{ adv.num }}</div>
            <div class="adv-info">
              <h4>{{ adv.title }}</h4>
              <p>{{ adv.desc }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import {
  Calendar,
  Check,
  Cpu,
  Document,
  FirstAidKit,
  Lock,
  MagicStick,
  Monitor,
  Notebook,
  OfficeBuilding,
  Setting,
  TrendCharts,
  Wallet,
} from '@element-plus/icons-vue'

const architectures = [
  {
    title: '前端技术栈',
    desc: '基于 Vue 3 + TypeScript + Element Plus 构建现代化交互界面，采用 Composition API 和响应式设计，适配多端设备。',
    icon: Monitor,
    color: 'arch-blue',
    tags: ['Vue 3', 'TypeScript', 'Element Plus', 'Vite'],
  },
  {
    title: '后端技术栈',
    desc: '基于 Spring Boot 框架构建 RESTful API，保障数据安全与服务稳定，支持高并发访问和灵活扩展。',
    icon: Setting,
    color: 'arch-green',
    tags: ['Spring Boot', 'MySQL', 'MyBatis', 'JWT'],
  },
  {
    title: 'AI 服务层',
    desc: '集成大语言模型提供智能推荐、报告解读、费用解释等服务，采用 SSE 流式输出实现实时交互体验。',
    icon: Cpu,
    color: 'arch-violet',
    tags: ['LLM', 'SSE', 'RAG', 'Embedding'],
  },
]

const features = [
  { title: '预约挂号', desc: '智能推荐科室医生，灵活选择排班时段，支持 AI 症状推荐', icon: Calendar, color: 'feat-blue' },
  { title: '病历与就诊', desc: '历史就诊记录与处方一键回溯查询，完整病历档案管理', icon: Document, color: 'feat-violet' },
  { title: '医疗报告', desc: '检查检验结果实时查看，AI 智能解读报告内容', icon: Notebook, color: 'feat-cyan' },
  { title: '费用查询', desc: '缴费记录与费用明细清晰透明，AI 费用解释', icon: Wallet, color: 'feat-indigo' },
  { title: '药品查询', desc: '药品信息与用法用量查询，AI 智能药品推荐', icon: FirstAidKit, color: 'feat-green' },
  { title: '科室信息', desc: '科室介绍与医生团队一览无余，科室导航指引', icon: OfficeBuilding, color: 'feat-orange' },
]

const aiServices = [
  { title: 'AI 智能推荐', desc: '根据患者症状描述，智能推荐合适的科室与医生，减少盲目挂号，提升就医效率。', icon: MagicStick },
  { title: 'AI 报告解读', desc: '基于大语言模型的检查报告智能解读，将专业术语转化为通俗易懂的健康建议，SSE 流式实时输出。', icon: Document },
  { title: 'AI 费用解释', desc: '对复杂医疗费用明细进行智能解释，帮助患者理解每一项收费的合理性，实现费用透明化。', icon: TrendCharts },
  { title: 'AI 药品推荐', desc: '结合 RAG 检索增强生成技术，基于药品知识库提供精准的药品信息查询与推荐服务。', icon: FirstAidKit },
]

const advantages = [
  { num: '01', title: '前后端分离', desc: '前端 Vue 3 与后端 Spring Boot 独立开发部署，提升开发效率与系统可维护性' },
  { num: '02', title: '数据安全', desc: '全链路加密传输，JWT 身份认证，严格的权限控制，保障患者隐私数据安全' },
  { num: '03', title: '智能赋能', desc: '融合 AI 大模型能力，提供智能推荐、解读、解释等创新服务，提升医疗体验' },
  { num: '04', title: '响应式设计', desc: '适配手机、平板、桌面多种设备，随时随地享受便捷的智慧医疗服务' },
]
</script>

<style scoped>
/* ===== Base ===== */
.about-page {
  background: #f8fafc;
  min-height: 100vh;
}
.about-container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 24px;
}
.section-header {
  text-align: center;
  margin-bottom: 48px;
}
.section-title {
  font-size: 30px;
  font-weight: 800;
  color: #1a2a4a;
  letter-spacing: 1px;
  margin-bottom: 12px;
}
.title-bar {
  width: 48px;
  height: 4px;
  border-radius: 2px;
  background: linear-gradient(135deg, #409eff, #2f80ed);
  margin: 0 auto 16px;
}
.section-subtitle {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.6;
}

/* ===== Hero ===== */
.about-hero {
  position: relative;
  height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: url('https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?w=1920&q=80') center/cover no-repeat;
  overflow: hidden;
}
.about-hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.9), rgba(47, 128, 237, 0.85), rgba(139, 92, 246, 0.8));
}
.about-hero-content {
  position: relative;
  z-index: 2;
  text-align: center;
  color: #fff;
}
.about-hero-content h1 {
  font-size: 40px;
  font-weight: 800;
  margin-bottom: 12px;
  letter-spacing: 2px;
}
.about-hero-content p {
  font-size: 18px;
  opacity: 0.9;
}

/* ===== Section Common ===== */
.about-section {
  padding: 80px 0;
}
.about-section--alt {
  background: #fff;
}
.about-section--dark {
  background: linear-gradient(195deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}

/* ===== Overview ===== */
.overview-content {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 48px;
  align-items: center;
}
.overview-text p {
  font-size: 16px;
  color: #475569;
  line-height: 1.9;
  margin-bottom: 20px;
  text-align: justify;
}
.overview-text p strong {
  color: #409eff;
}
.overview-image {
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.1);
}
.overview-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* ===== Architecture ===== */
.arch-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 28px;
}
.arch-card {
  padding: 32px 28px;
  border-radius: 20px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  transition: all 0.3s ease;
}
.arch-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);
}
.arch-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-bottom: 20px;
}
.arch-blue { background: linear-gradient(135deg, #409eff, #2f80ed); }
.arch-green { background: linear-gradient(135deg, #43c6ac, #38a169); }
.arch-violet { background: linear-gradient(135deg, #8b5cf6, #7c3aed); }
.arch-card h3 {
  font-size: 20px;
  font-weight: 700;
  color: #1a2a4a;
  margin-bottom: 12px;
}
.arch-card p {
  font-size: 14px;
  color: #64748b;
  line-height: 1.8;
  margin-bottom: 16px;
}
.arch-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.arch-tags span {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 50px;
  background: rgba(64, 158, 255, 0.08);
  color: #409eff;
  font-weight: 500;
}

/* ===== Features ===== */
.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}
.feature-item {
  display: flex;
  gap: 16px;
  padding: 24px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #e2e8f0;
  transition: all 0.3s ease;
}
.feature-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}
.feature-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.feat-blue { background: linear-gradient(135deg, #409eff, #53a8ff); }
.feat-violet { background: linear-gradient(135deg, #8b5cf6, #a78bfa); }
.feat-cyan { background: linear-gradient(135deg, #00b4d8, #0099ff); }
.feat-indigo { background: linear-gradient(135deg, #667eea, #764ba2); }
.feat-green { background: linear-gradient(135deg, #43c6ac, #68d391); }
.feat-orange { background: linear-gradient(135deg, #f6a623, #f7b955); }
.feature-info h4 {
  font-size: 16px;
  font-weight: 700;
  color: #1a2a4a;
  margin-bottom: 6px;
}
.feature-info p {
  font-size: 13px;
  color: #64748b;
  line-height: 1.7;
}

/* ===== AI Services ===== */
.ai-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}
.ai-card {
  padding: 32px 24px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  text-align: center;
  color: #fff;
  transition: all 0.3s ease;
}
.ai-card:hover {
  background: rgba(255, 255, 255, 0.14);
  transform: translateY(-4px);
}
.ai-icon {
  margin-bottom: 16px;
  opacity: 0.9;
}
.ai-card h3 {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 10px;
}
.ai-card p {
  font-size: 13px;
  opacity: 0.8;
  line-height: 1.7;
}

/* ===== Advantages ===== */
.adv-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 28px;
}
.adv-item {
  display: flex;
  gap: 20px;
  padding: 28px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #e2e8f0;
  transition: all 0.3s ease;
}
.adv-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}
.adv-number {
  font-size: 36px;
  font-weight: 800;
  background: linear-gradient(135deg, #409eff, #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  flex-shrink: 0;
  line-height: 1;
}
.adv-info h4 {
  font-size: 18px;
  font-weight: 700;
  color: #1a2a4a;
  margin-bottom: 8px;
}
.adv-info p {
  font-size: 14px;
  color: #64748b;
  line-height: 1.7;
}

/* ===== Responsive ===== */
@media (max-width: 992px) {
  .overview-content {
    grid-template-columns: 1fr;
  }
  .overview-image {
    max-height: 300px;
  }
  .arch-grid {
    grid-template-columns: 1fr;
  }
  .feature-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .ai-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .adv-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .about-hero {
    height: 200px;
  }
  .about-hero-content h1 {
    font-size: 28px;
  }
  .about-hero-content p {
    font-size: 15px;
  }
  .about-section {
    padding: 56px 0;
  }
  .section-title {
    font-size: 24px;
  }
  .feature-grid {
    grid-template-columns: 1fr;
  }
  .ai-grid {
    grid-template-columns: 1fr;
  }
}
</style>
