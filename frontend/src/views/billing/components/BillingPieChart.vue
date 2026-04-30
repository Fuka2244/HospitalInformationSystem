<template>
  <div class="pie-root">
    <div v-if="totalValue <= 0" class="pie-empty">
      <el-empty description="暂无可视化数据" :image-size="80" />
    </div>
    <div v-else class="pie-wrap" @mouseleave="hoverKey = null">
      <div class="pie-chart">
        <div class="pie-glow"></div>
        <svg viewBox="0 0 100 100" class="pie-svg">
          <template v-for="slice in slices" :key="slice.key">
            <path
              :d="slice.path"
              :fill="slice.fillColor"
              class="pie-slice"
              :class="{ active: hoverKey === slice.key }"
              @mouseenter="hoverKey = slice.key"
              @mouseleave="hoverKey = null"
              @click="emitSelect(slice)"
            >
              <title>{{ slice.label }}：¥{{ slice.value.toFixed(2) }}（{{ (slice.percent * 100).toFixed(1) }}%）</title>
            </path>
          </template>
        </svg>
        <div class="pie-hole"></div>
        <div class="pie-center">
          <div class="pie-total-label">总计</div>
          <div class="pie-total-value">¥{{ totalValue.toFixed(2) }}</div>
        </div>
      </div>

      <div class="pie-legend">
        <div
          v-for="slice in slices"
          :key="slice.key"
          class="legend-item"
          :class="{ active: hoverKey === slice.key }"
          @mouseenter="hoverKey = slice.key"
          @mouseleave="hoverKey = null"
          @click="emitSelect(slice)"
        >
          <span class="legend-color" :style="{ background: slice.fillColor }"></span>
          <span class="legend-label">{{ slice.label }}</span>
          <span class="legend-value">¥{{ slice.value.toFixed(2) }}</span>
          <span class="legend-percent">{{ (slice.percent * 100).toFixed(1) }}%</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

export type PieItem = {
  key: string
  label: string
  value: number
  color?: string
  count?: number
}

const fallbackColors = ['#47c88a', '#2fb8a0', '#4b97e8', '#f2b557', '#e97878', '#8a84dd']

const props = defineProps<{ items?: PieItem[] }>()
const displayItems = computed(() => props.items ?? [])

const emit = defineEmits<{ (e: 'select', item: PieItem & { percent: number }): void }>()

const hoverKey = ref<string | null>(null)

type Slice = PieItem & { start: number; end: number; percent: number; path: string; fillColor: string }

const totalValue = computed(() => displayItems.value.reduce((s, i) => s + (Number(i.value) || 0), 0))

function arcPath(cx: number, cy: number, r: number, startAngle: number, endAngle: number) {
  const largeArc = endAngle - startAngle > Math.PI ? 1 : 0
  const pad = 0.02
  const s = startAngle + pad
  const e = endAngle - pad
  return `M${cx},${cy} L${cx + r * Math.cos(s)},${cy + r * Math.sin(s)} A${r},${r} 0 ${largeArc} 1 ${cx + r * Math.cos(e)},${cy + r * Math.sin(e)} Z`
}

const slices = computed<Slice[]>(() => {
  const data = displayItems.value
    .map(item => ({ ...item, value: Number(item.value) || 0 }))
    .filter(item => item.value > 0)
  const total = data.reduce((s, i) => s + i.value, 0)
  if (!total) return []
  
  let cur = -Math.PI / 2
  return data.map((item, idx) => {
    const pct = item.value / total
    const start = cur, end = cur + pct * Math.PI * 2
    cur = end
    const fullCircle = pct > 0.999999
    const path = fullCircle
      ? 'M50 50 m 0 -40 a 40 40 0 1 1 0 80 a 40 40 0 1 1 0 -80'
      : arcPath(50, 50, 40, start, end)
    return {
      ...item,
      start,
      end,
      percent: pct,
      path,
      fillColor: fallbackColors[idx % fallbackColors.length],
    }
  })
})

function emitSelect(slice: Slice) {
  emit('select', { ...slice, percent: slice.percent })
}
</script>

<style scoped>
.pie-root {
  width: 100%;
}

.pie-empty {
  padding: 20px 0;
}

.pie-wrap {
  display: flex;
  align-items: center;
  gap: 20px;
  position: relative;
}

.pie-chart {
  position: relative;
  width: 168px;
  height: 168px;
  flex-shrink: 0;
}

.pie-chart::before {
  content: '';
  position: absolute;
  inset: -6px;
  border-radius: 50%;
  border: 1px solid rgba(var(--his-primary-rgb), 0.24);
  background: radial-gradient(circle at 65% 30%, rgba(255, 255, 255, 0.7), rgba(255, 255, 255, 0));
  pointer-events: none;
}

.pie-glow {
  position: absolute;
  inset: 4px;
  border-radius: 50%;
  background: radial-gradient(circle at 32% 28%, rgba(var(--his-primary-rgb), 0.36), rgba(var(--his-primary-rgb), 0)),
    radial-gradient(circle at 70% 72%, rgba(75, 151, 232, 0.22), rgba(75, 151, 232, 0));
  filter: blur(14px);
  pointer-events: none;
}

.pie-svg {
  width: 100%;
  height: 100%;
  filter: drop-shadow(0 12px 20px rgba(var(--his-primary-rgb), 0.2));
  animation: pie-fade-in 0.45s ease;
}

.pie-slice {
  cursor: pointer;
  transform-origin: 50px 50px;
  stroke: rgba(255, 255, 255, 0.95);
  stroke-width: 1.6;
  transition: transform 0.2s ease, opacity 0.2s ease, filter 0.2s ease;
}

.pie-slice:hover,
.pie-slice.active {
  opacity: 0.96;
  transform: scale(1.05);
  filter: brightness(1.04);
}

.pie-hole {
  position: absolute;
  inset: 41px;
  border-radius: 50%;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(245, 250, 247, 0.96));
  border: 1px solid rgba(var(--his-primary-rgb), 0.22);
  box-shadow: inset 0 2px 10px rgba(var(--his-primary-rgb), 0.1), 0 2px 8px rgba(var(--his-primary-rgb), 0.14);
  pointer-events: none;
}

.pie-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  width: 84px;
}

.pie-total-label {
  font-size: 12px;
  color: var(--his-text-2);
}

.pie-total-value {
  font-size: 16px;
  font-weight: 800;
  color: var(--his-text);
  margin-top: 3px;
  letter-spacing: 0.2px;
}

.pie-legend {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 10px;
  border: 1px solid rgba(var(--his-primary-rgb), 0.08);
  background: rgba(var(--his-primary-rgb), 0.03);
  cursor: pointer;
  transition: background 0.18s ease, transform 0.18s ease, border-color 0.18s ease;
}

.legend-item:hover,
.legend-item.active {
  background: rgba(var(--his-primary-rgb), 0.12);
  border-color: rgba(var(--his-primary-rgb), 0.22);
  transform: translateX(2px);
}

.legend-color {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.8);
  flex-shrink: 0;
}

.legend-label {
  font-size: 12px;
  color: var(--his-text-2);
  flex: 1;
}

.legend-value {
  font-size: 12px;
  color: var(--his-text);
  font-weight: 700;
}

.legend-percent {
  font-size: 11px;
  color: var(--his-primary);
  font-weight: 700;
}

@keyframes pie-fade-in {
  from {
    opacity: 0.3;
    transform: scale(0.96);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@media (max-width: 480px) {
  .pie-wrap {
    flex-direction: column;
  }
}
</style>
