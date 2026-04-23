<template>
  <div class="landed-page" :class="{ 'is-loaded': loaded }">
    <section id="banner" :ref="bindSectionRef('banner')" class="hero-panel">
      <img :src="heroImage" alt="项目名称" class="panel-image parallax-layer" :style="parallaxStyle('banner', 1.08)">
      <div class="panel-overlay" />
      <div class="content hero-content">
        <header>
          <div class="eyebrow" v-fade-in="{ distance: 18, delay: 80 }">Hospital Information System</div>
          <h2 v-fade-in="{ distance: 22, delay: 200 }">医院信息系统</h2>
          <p v-fade-in="{ distance: 18, delay: 340 }">面向患者就医全流程的综合信息平台</p>
        </header>
      </div>
      <button class="goto-next" type="button" @click="scrollTo('one')">Next</button>
    </section>

    <section id="one" :ref="bindSectionRef('one')" class="spotlight style1 bottom" style="background-color: #d4f4dd;">
      <img :src="backgroundImage" alt="项目背景" class="panel-image parallax-layer" :style="parallaxStyle('one', 1.1)">
      <div class="panel-overlay soft" />
      <div class="content reveal">
        <div class="container">
          <div class="row triple">
            <div class="column reveal stagger-1 slide-in-right" v-fade-in="{ distance: 12, delay: 80 }">
              <header>
                <h2>项目背景</h2>
                <p>聚焦医院业务数字化、患者服务线上化和信息查询便捷化。</p>
              </header>
            </div>
            <div class="column reveal stagger-2 slide-in-left" v-fade-in="{ distance: 12, delay: 160 }">
              <p>
                传统就医流程中，预约、问诊、报告、缴费和信息查询通常分散在多个系统或线下窗口，
                用户需要反复切换入口，体验割裂，医院的管理效率也难以提升。
              </p>
            </div>
            <div class="column reveal stagger-3 slide-in-right" v-fade-in="{ distance: 12, delay: 240 }">
              <p>
                本项目以患者视角重构就医链路，把常见功能收敛到统一平台中，
                既服务患者，也为医院的信息管理和后续扩展提供稳定基础。
              </p>
            </div>
          </div>
        </div>
      </div>
      <button class="goto-next" type="button" @click="scrollTo('two')">Next</button>
    </section>

    <section id="two" :ref="bindSectionRef('two')" class="spotlight style2 right" style="background-color: #d4f4dd;">
      <img :src="techIntroImage" alt="项目技术介绍" class="panel-image parallax-layer" :style="parallaxStyle('two', 1.1)">
      <div class="panel-overlay" />
      <div class="content single reveal">
        <header>
          <h2>项目技术与简介</h2>
          <p>基于前后端分离架构，覆盖患者端核心业务场景。</p>
        </header>
        <p>
          前端使用 Vue 3、TypeScript、Vite、Element Plus 构建交互界面，
          后端围绕医院信息业务设计接口与数据流程，实现预约挂号、病历就诊、报告查询、
          费用查询、药品查询和科室信息等模块。
        </p>
        <ul class="actions">
          <li>
            <el-button round @click="go('/project-tech')">查看项目技术页面</el-button>
          </li>
        </ul>
      </div>
      <button class="goto-next" type="button" @click="scrollTo('three')">Next</button>
    </section>

    <section id="three" :ref="bindSectionRef('three')" class="spotlight style3 left" style="background-color: #d4f4dd;">
      <img :src="businessImage" alt="未来商业推广" class="panel-image parallax-layer" :style="parallaxStyle('three', 1.1)">
      <div class="panel-overlay" />
      <div class="content single reveal">
        <header>
          <h2>未来商业推广</h2>
          <p>从系统原型逐步延伸到更完整的医疗数字化服务能力。</p>
        </header>
        <p>
          后续可以面向社区医院、体检中心和区域医疗服务机构推广，
          以模块化方式输出预约、报告、缴费和基础患者管理能力，
          结合数据分析、消息通知和运营服务，形成长期可持续的产品方案。
        </p>
        <ul class="actions">
          <li>
            <el-button round @click="go('/about')">查看系统概况</el-button>
          </li>
        </ul>
      </div>
      <button class="goto-next" type="button" @click="scrollTo('four')">Next</button>
    </section>

    <section id="four" class="wrapper style1 special fade-up">
      <div class="container narrow reveal">
        <header class="major">
          <h2>核心服务模块</h2>
          <p>保留首页原有的功能总览区，用于快速进入系统主要模块。</p>
        </header>
        <div class="box alt">
          <div class="row feature-grid">
            <section
              v-for="(feature, index) in features"
              :key="feature.title"
              class="feature-item reveal"
              :class="`stagger-${(index % 3) + 1}`"
              v-fade-in="{ distance: 14, delay: (index * 80) }"
              @click="go(feature.path)"
            >
              <span class="icon major">
                <el-icon><component :is="feature.icon" /></el-icon>
              </span>
              <h3>{{ feature.title }}</h3>
              <p>{{ feature.desc }}</p>
            </section>
          </div>
        </div>
      </div>
    </section>

    <section id="five" class="wrapper style2 special fade">
      <div class="container narrow reveal">
        <header>
          <h2>开始使用系统</h2>
          <p>快来尝试一下。</p>
        </header>
        <div class="cta-buttons">
          <el-button type="primary" size="large" class="cta-btn" @click="go('/login')">前往登录</el-button>
          <el-button size="large" class="cta-btn light" @click="go('/login?mode=register')">前往注册</el-button>
        </div>
      </div>
    </section>

    <footer id="footer" class="reveal">
      <ul class="icons">
        <li><span class="icon-circle"><el-icon><Calendar /></el-icon></span></li>
        <li><span class="icon-circle"><el-icon><Document /></el-icon></span></li>
        <li><span class="icon-circle"><el-icon><Notebook /></el-icon></span></li>
        <li><span class="icon-circle"><el-icon><Wallet /></el-icon></span></li>
        <li><span class="icon-circle"><el-icon><FirstAidKit /></el-icon></span></li>
        <li><span class="icon-circle"><el-icon><OfficeBuilding /></el-icon></span></li>
      </ul>
      <ul class="copyright">
        <li>© {{ currentYear }} 医院信息系统</li>
        <li>Project landing page for HIS</li>
      </ul>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import type { ComponentPublicInstance } from 'vue'
import { useRouter } from 'vue-router'
import {
  Calendar,
  Document,
  FirstAidKit,
  Notebook,
  OfficeBuilding,
  Wallet,
} from '@element-plus/icons-vue'
import heroImage from '../../../sucai/1.jpg'
import backgroundImage from '../../../sucai/2.jpg'
import techIntroImage from '../../../sucai/3.jpg'
import businessImage from '../../../sucai/4.jpg'

const router = useRouter()
const currentYear = new Date().getFullYear()

const features = [
  { title: '预约挂号', desc: '按科室、医生与时间段快速完成预约。', path: '/appointment', icon: Calendar },
  { title: '病历与就诊', desc: '查看个人就诊记录和历史病历。', path: '/records', icon: Document },
  { title: '医疗报告', desc: '检验、检查结果统一查看。', path: '/report', icon: Notebook },
  { title: '费用查询', desc: '费用明细更直观，减少重复确认。', path: '/billing', icon: Wallet },
  { title: '药品查询', desc: '快速了解药品信息和基础说明。', path: '/medicine', icon: FirstAidKit },
  { title: '科室信息', desc: '按需求定位合适的门诊与科室。', path: '/department', icon: OfficeBuilding },
]

const sectionElements = new Map<string, HTMLElement>()
const parallaxOffsets = reactive<Record<string, number>>({
  banner: 0,
  one: 0,
  two: 0,
  three: 0,
})

let revealObserver: IntersectionObserver | null = null
const loaded = ref(false)

function go(path: string) {
  router.push(path)
}

function scrollTo(id: string) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function bindSectionRef(id: keyof typeof parallaxOffsets) {
  return (target: Element | ComponentPublicInstance | null) => {
    if (target instanceof HTMLElement) {
      sectionElements.set(id, target)
    } else {
      sectionElements.delete(id)
    }
  }
}

function updateParallax() {
  const viewportCenter = window.innerHeight / 2

  sectionElements.forEach((element, key) => {
    const rect = element.getBoundingClientRect()
    const sectionCenter = rect.top + rect.height / 2
    const distance = viewportCenter - sectionCenter
    const offset = Math.max(-48, Math.min(48, distance * 0.08))
    parallaxOffsets[key] = offset
  })
}

function parallaxStyle(id: keyof typeof parallaxOffsets, scale = 1.08) {
  return computed(() => ({
    transform: `translate3d(0, ${parallaxOffsets[id]}px, 0) scale(${scale})`,
  })).value
}

onMounted(() => {
  // initial loading entrance
  setTimeout(() => { loaded.value = true }, 60)

  const revealTargets = document.querySelectorAll<HTMLElement>('.reveal')
  revealObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-visible')
          revealObserver?.unobserve(entry.target)
        }
      })
    },
    { threshold: 0.18, rootMargin: '0px 0px -10% 0px' },
  )

  revealTargets.forEach((element) => revealObserver?.observe(element))

  updateParallax()
  window.addEventListener('scroll', updateParallax, { passive: true })
  window.addEventListener('resize', updateParallax)
})

onBeforeUnmount(() => {
  revealObserver?.disconnect()
  window.removeEventListener('scroll', updateParallax)
  window.removeEventListener('resize', updateParallax)
})
</script>

<style scoped>
.landed-page {
  min-height: 100%;
  background: #1c1d26;
  color: rgba(255, 255, 255, 0.78);
  font-family: "Roboto", "Helvetica Neue", "PingFang SC", sans-serif;
  line-height: 1.75;
  --accent-green-dark: #0f6b3f;
  --accent-green-light: #a8e6c3;
}

.landed-page *,
.landed-page *::before,
.landed-page *::after {
  box-sizing: border-box;
}

#banner,
.spotlight {
  position: relative;
  min-height: calc(100vh - 60px);
  overflow: hidden;
}

.hero-panel {
  display: flex;
  align-items: center;
}

.panel-image {
  position: absolute;
  inset: -3.5rem 0;
  width: 100%;
  height: calc(100% + 7rem);
  object-fit: cover;
  transform-origin: center center;
  transition: transform 0.15s linear;
  will-change: transform;
}

.panel-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(rgba(18, 22, 34, 0.26), rgba(18, 22, 34, 0.72)),
    radial-gradient(circle at 18% 24%, rgba(228, 76, 101, 0.14), transparent 22%),
    radial-gradient(circle at 82% 32%, rgba(39, 174, 174, 0.16), transparent 24%);
}

.panel-overlay.soft {
  background:
    linear-gradient(rgba(18, 22, 34, 0.34), rgba(18, 22, 34, 0.6)),
    radial-gradient(circle at 50% 20%, rgba(255, 255, 255, 0.08), transparent 18%);
}

#banner .content,
.spotlight .content,
.wrapper .container,
#footer {
  position: relative;
  z-index: 1;
  width: min(72rem, calc(100% - 3rem));
  margin: 0 auto;
}

#banner .content {
  display: flex;
  align-items: center;
  min-height: calc(100vh - 60px);
}

/* (no custom fill for spotlight content — keep original dark page aesthetics) */

.hero-content header {
  max-width: 42rem;
}

.eyebrow {
  display: inline-block;
  margin-bottom: 1rem;
  padding: 0.42rem 0.9rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.18);
  color: rgba(255, 255, 255, 0.84);
  font-size: 0.8rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

#banner h2 {
  margin: 0 0 1rem;
  color: #fff;
  font-size: clamp(3rem, 6vw, 4.8rem);
  font-weight: 300;
  line-height: 1.05;
  text-shadow: 0 10px 34px rgba(0, 0, 0, 0.24);
}

#banner p {
  margin: 0;
  color: rgba(255, 255, 255, 0.9);
  font-size: 1.2rem;
}

.goto-next {
  position: absolute;
  left: 50%;
  bottom: 2rem;
  width: 5rem;
  height: 5rem;
  margin-left: -2.5rem;
  border: 0;
  background: transparent;
  color: transparent;
  cursor: pointer;
  z-index: 2;
}

.goto-next::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  width: 1.25rem;
  height: 1.25rem;
  margin: -1rem 0 0 -0.625rem;
  border-right: 2px solid rgba(255, 255, 255, 0.74);
  border-bottom: 2px solid rgba(255, 255, 255, 0.74);
  transform: rotate(45deg);
}

.spotlight::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(rgba(10, 14, 24, 0.1), rgba(10, 14, 24, 0.28));
}

.spotlight .content {
  min-height: calc(100vh - 60px);
}

.spotlight.style1 .content {
  display: flex;
  align-items: flex-end;
  width: 100%;
  min-height: calc(100vh - 60px);
  padding: 0;
}

.spotlight.style1 .panel-image {
  inset: 0 0 auto;
  height: 45%;
}

.spotlight.style1 .panel-overlay {
  inset: 0 0 auto;
  height: 45%;
  background: linear-gradient(rgba(20, 25, 38, 0.1), rgba(20, 25, 38, 0.38));
}

.spotlight.style1 .container {
  width: 100%;
  max-width: none;
  padding: 4.5rem 0 5.75rem;
  background: rgba(255, 255, 255, 0.96);
  color: rgba(39, 40, 51, 0.82);
  box-shadow: 0 18px 44px rgba(17, 24, 39, 0.12);
  border-top: 0.45rem solid var(--accent-green-light);
}

.spotlight.style1 .row.triple {
  width: min(72rem, calc(100% - 3rem));
  margin: 0 auto;
}

.spotlight.style2 .content,
.spotlight.style3 .content {
  display: flex;
  flex-direction: column;
  justify-content: center;
  width: min(40rem, 34%);
  min-height: calc(100vh - 60px);
  padding: 5rem 4rem;
  background: rgba(255, 255, 255, 0.96);
  color: rgba(39, 40, 51, 0.82);
  box-shadow: 0 18px 44px rgba(17, 24, 39, 0.12);
  backdrop-filter: blur(2px);
}

.spotlight.style2 .content {
  margin-left: auto;
  margin-right: 0;
  border-left: 0.45rem solid var(--accent-green-light);
}

.spotlight.style3 .content {
  margin-left: 0;
  margin-right: auto;
  border-right: 0.45rem solid var(--accent-green-light);
}

.spotlight.style2 .panel-image {
  inset: 0 auto 0 0;
  width: 66%;
  height: 100%;
}

.spotlight.style3 .panel-image {
  inset: 0 0 0 auto;
  width: 66%;
  height: 100%;
}

.spotlight.style2 .panel-overlay {
  background:
    linear-gradient(90deg, rgba(18, 22, 34, 0.24) 0%, rgba(18, 22, 34, 0.2) 55%, rgba(18, 22, 34, 0.78) 100%);
}

.spotlight.style3 .panel-overlay {
  background:
    linear-gradient(90deg, rgba(18, 22, 34, 0.82) 0%, rgba(18, 22, 34, 0.22) 42%, rgba(18, 22, 34, 0.18) 100%);
}

.content header h2,
.major h2,
.wrapper header h2 {
  margin: 0 0 1rem;
  color: #fff;
  font-size: 2rem;
  line-height: 1.35;
  font-weight: 300;
}

.content header p,
.major p,
.wrapper header p {
  margin: 0 0 1rem;
}

.container {
  padding: 4rem 0;
}

.narrow {
  max-width: 70rem;
}

.row.triple {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 3rem;
}

.column p,
.single p {
  margin: 0;
}

.column {
  padding: 0;
  border-radius: 0;
  background: transparent;
  border: 0;
  backdrop-filter: none;
}

.spotlight.style1 .column:nth-child(1) {
  padding-right: 1.5rem;
}

.spotlight.style1 .column:nth-child(2),
.spotlight.style1 .column:nth-child(3) {
  color: rgba(39, 40, 51, 0.72);
  font-size: 0.98rem;
}

.spotlight.style1 .column header p {
  color: rgba(39, 40, 51, 0.72);
}

.spotlight.style1 .column h2 {
  font-size: clamp(2.5rem, 4vw, 4rem);
  line-height: 1.32;
}

.spotlight.style2 .single,
.spotlight.style3 .single {
  gap: 1.75rem;
}

.spotlight.style2 .single h2,
.spotlight.style3 .single h2 {
  font-size: clamp(2.1rem, 3vw, 3.3rem);
  line-height: 1.32;
}

.spotlight.style2 .single > p,
.spotlight.style3 .single > p {
  color: rgba(39, 40, 51, 0.72);
  font-size: 0.98rem;
}

/* Make spotlight headings readable on light cards */
.spotlight.style1 h2,
.spotlight.style2 h2,
.spotlight.style3 h2 {
  color: rgba(17, 24, 39, 0.92);
}

.wrapper.style1 {
  background:
    linear-gradient(rgba(255, 255, 255, 0.94), rgba(255, 255, 255, 0.96)),
    #ffffff;
  color: rgba(39, 40, 51, 0.75);
}

.wrapper.style1 h2,
.wrapper.style1 h3,
.wrapper.style1 p {
  color: inherit;
}

.wrapper.style1 .major {
  text-align: center;
}

.box.alt {
  margin-top: 3rem;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 2rem 1.5rem;
}

.feature-item {
  text-align: center;
  padding: 1.4rem 1rem 0;
  cursor: pointer;
  border-radius: 1.2rem;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.feature-item:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 34px rgba(31, 41, 55, 0.08);
}

.icon.major {
  display: inline-grid;
  place-items: center;
  width: 5rem;
  height: 5rem;
  margin-bottom: 1.25rem;
  border-radius: 50%;
  background: linear-gradient(
    135deg,
    rgba(168, 230, 195, 0.55),
    rgba(15, 107, 63, 0.12)
  );
  color: var(--accent-green-dark);
  font-size: 1.8rem;
}

.feature-item h3 {
  margin: 0 0 0.75rem;
  font-size: 1.25rem;
  font-weight: 400;
}

.feature-item p {
  margin: 0;
}

.wrapper.style2 {
  background:
    linear-gradient(135deg, rgba(15, 107, 63, 0.96), rgba(11, 72, 42, 0.96)),
    var(--accent-green-dark);
  color: rgba(255, 255, 255, 0.88);
  text-align: center;
}

.cta-buttons {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 1rem;
  margin-top: 2rem;
}

.cta-btn {
  min-width: 12rem;
}

.cta-btn.light {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.32);
  color: #fff;
}

#footer {
  padding: 4rem 0 5rem;
  text-align: center;
}

.icons,
.copyright,
.actions {
  list-style: none;
  padding: 0;
  margin: 0;
}

.icons {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 0.875rem;
}

.icon-circle {
  display: grid;
  place-items: center;
  width: 3rem;
  height: 3rem;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.16);
  color: rgba(255, 255, 255, 0.72);
  background: rgba(255, 255, 255, 0.04);
}

.copyright {
  display: flex;
  justify-content: center;
  gap: 1rem;
  flex-wrap: wrap;
  margin-top: 1.5rem;
  font-size: 0.92rem;
}

.actions {
  margin-top: 1.75rem;
}

.actions.special {
  display: flex;
  justify-content: center;
}

.reveal {
  opacity: 0;
  transform: translate3d(0, 42px, 0);
  transition:
    opacity 0.8s ease,
    transform 0.8s cubic-bezier(0.22, 1, 0.36, 1);
}

.reveal.is-visible {
  opacity: 1;
  transform: translate3d(0, 0, 0);
}

.stagger-1 {
  transition-delay: 0.06s;
}

.stagger-2 {
  transition-delay: 0.14s;
}

.stagger-3 {
  transition-delay: 0.22s;
}

:deep(.el-button) {
  min-height: 2.75rem;
  padding-inline: 1.5rem;
  border-radius: 999px;
}

@media (max-width: 980px) {
  .row.triple,
  .feature-grid {
    grid-template-columns: 1fr;
  }

  #banner .content {
    text-align: center;
    justify-content: center;
  }

  .spotlight.style2 .content,
  .spotlight.style3 .content {
    width: min(100%, calc(100% - 2rem));
    min-height: auto;
    margin: 0 auto;
    padding: 3rem 2rem;
    border-left: 0;
    border-right: 0;
  }

  .spotlight {
    min-height: auto;
    padding-top: 16rem;
  }

  .panel-image {
    inset: 0;
    height: 18rem;
    transform: none !important;
  }

  .spotlight.style1 .content {
    min-height: auto;
  }

  .spotlight.style1 .container {
    padding: 2.75rem 0 4rem;
  }

  .spotlight.style2 .panel-image,
  .spotlight.style3 .panel-image {
    width: 100%;
  }
}

@media (max-width: 640px) {
  #banner,
  .spotlight {
    min-height: auto;
  }

  #banner .content,
  .spotlight .content,
  .wrapper .container,
  #footer {
    width: min(72rem, calc(100% - 1.5rem));
  }

  #banner h2,
  .content header h2,
  .major h2,
  .wrapper header h2 {
    font-size: 1.8rem;
  }

  #banner p {
    font-size: 1rem;
  }

  .spotlight.style2 .content,
  .spotlight.style3 .content {
    padding: 2rem 1.25rem;
  }

  .column {
    padding: 0;
  }
}
</style>
