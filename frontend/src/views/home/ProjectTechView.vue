<template>
  <div class="project-tech">
    <section class="hero">
      <div class="hero-overlay" />
      <img :src="techImage" alt="项目技术" class="hero-image">
      <div class="hero-content">
        <div class="eyebrow">Project Technology</div>
        <h1>医院信息系统技术页面</h1>
        <p>对项目采用的前端、后端和业务模块做一个简洁说明。</p>
      </div>
    </section>

    <section class="content">
      <div class="card-grid">
        <article class="card">
          <h2>前端技术</h2>
          <p>使用 Vue 3 + TypeScript + Vite 构建页面，结合 Element Plus 完成交互组件与页面布局。</p>
        </article>
        <article class="card">
          <h2>后端能力</h2>
          <p>围绕预约挂号、病历就诊、报告查询、费用查询等业务设计接口与数据处理流程。</p>
        </article>
        <article class="card">
          <h2>模块设计</h2>
          <p>系统划分为患者信息、挂号预约、检查报告、药品信息、费用信息和科室信息等核心模块。</p>
        </article>
        <article class="card">
          <h2>项目特点</h2>
          <p>采用前后端分离方式，界面清晰、模块独立，便于后续继续扩展运营、消息和数据分析能力。</p>
        </article>
      </div>

      <div class="actions">
        <el-button round @click="router.push('/home')">返回首页</el-button>
        <el-button type="primary" round @click="router.push('/login')">前往登录</el-button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import techImage from '../../../sucai/5.jpg'

const router = useRouter()
</script>

<style scoped>
.project-tech {
  min-height: 100%;
  background: #f7f7fb;
  color: #1f2937;
}

.hero {
  position: relative;
  min-height: 48vh;
  display: flex;
  align-items: center;
  overflow: hidden;
}

.hero-image,
.hero-overlay {
  position: absolute;
  inset: 0;
}

.hero-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-overlay {
  background: linear-gradient(rgba(17, 24, 39, 0.45), rgba(17, 24, 39, 0.72));
}

.hero-content {
  position: relative;
  z-index: 1;
  width: min(72rem, calc(100% - 3rem));
  margin: 0 auto;
  color: #fff;
}

.eyebrow {
  display: inline-block;
  margin-bottom: 1rem;
  padding: 0.35rem 0.85rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.22);
  font-size: 0.8rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.hero-content h1 {
  margin: 0 0 1rem;
  font-size: clamp(2.2rem, 4vw, 3.8rem);
  font-weight: 700;
}

.hero-content p {
  margin: 0;
  max-width: 42rem;
  font-size: 1.05rem;
  color: rgba(255, 255, 255, 0.86);
}

.content {
  width: min(72rem, calc(100% - 3rem));
  margin: 0 auto;
  padding: 3rem 0 4rem;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1.25rem;
}

.card {
  padding: 1.6rem;
  border-radius: 1.1rem;
  background: #fff;
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.08);
}

.card h2 {
  margin: 0 0 0.75rem;
  font-size: 1.25rem;
  color: #111827;
}

.card p {
  margin: 0;
  color: #4b5563;
  line-height: 1.8;
}

.actions {
  display: flex;
  justify-content: center;
  gap: 1rem;
  flex-wrap: wrap;
  margin-top: 2rem;
}

@media (max-width: 768px) {
  .card-grid {
    grid-template-columns: 1fr;
  }
}
</style>
