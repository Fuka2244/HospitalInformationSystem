<template>
  <div class="pie-root">
    <div v-if="totalValue <= 0" class="pie-empty">
      <el-empty description="暂无可视化数据" :image-size="90" />
    </div>
    <div v-else class="pie-wrap" @mouseleave="hoverKey = null">
      <svg class="pie-svg" viewBox="0 0 220 220" role="img" aria-label="费用构成饼图">
        <g>
          <template v-for="slice in slices" :key="slice.key">
            <path
              :d="slice.path"
              :fill="slice.color"
              :transform="sliceTransform(slice)"
              class="pie-slice"
              @mouseenter="hoverKey = slice.key"
              @click="emitSelect(slice)"
            >
              <title>{{ slice.label }}：¥{{ slice.value.toFixed(2) }}（{{ (slice.percent * 100).toFixed(1) }}%）</title>
            </path>
          </template>
        </g>
        <circle cx="110" cy="110" r="56" fill="rgba(255,255,255,0.92)" />
        <text x="110" y="104" text-anchor="middle" class="pie-center-title">总计</text>
        <text x="110" y="128" text-anchor="middle" class="pie-center-value">¥{{ totalValue.toFixed(2) }}</text>
      </svg>

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
          <span class="dot" :style="{ background: slice.color }"></span>
          <span class="label">{{ slice.label }}</span>
          <span class="percent">{{ (slice.percent * 100).toFixed(1) }}%</span>
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
  color: string
  count?: number
}

const props = defineProps<{
  items: PieItem[]
}>()

const emit = defineEmits<{
  (e: 'select', item: PieItem & { percent: number }): void
}>()

const hoverKey = ref<string | null>(null)

type Slice = PieItem & {
  start: number
  end: number
  mid: number
  percent: number
  path: string
}

const totalValue = computed(() =>
  (props.items || []).reduce((sum, item) => sum + (Number(item.value) || 0), 0)
)

function polarToCartesian(cx: number, cy: number, r: number, angleRad: number) {
  return {
    x: cx + r * Math.cos(angleRad),
    y: cy + r * Math.sin(angleRad),
  }
}

function arcPath(cx: number, cy: number, r: number, start: number, end: number) {
  const startPoint = polarToCartesian(cx, cy, r, start)
  const endPoint = polarToCartesian(cx, cy, r, end)
  const largeArcFlag = end - start > Math.PI ? 1 : 0
  return `M ${cx} ${cy} L ${startPoint.x} ${startPoint.y} A ${r} ${r} 0 ${largeArcFlag} 1 ${endPoint.x} ${endPoint.y} Z`
}

const slices = computed<Slice[]>(() => {
  const normalized = (props.items || [])
    .map(i => ({ ...i, value: Number(i.value) || 0 }))
    .filter(i => i.value > 0)

  const total = normalized.reduce((sum, i) => sum + i.value, 0)
  if (total <= 0) return []

  const cx = 110
  const cy = 110
  const r = 88
  let cursor = -Math.PI / 2

  return normalized.map((item) => {
    const percent = item.value / total
    const start = cursor
    const end = cursor + percent * Math.PI * 2
    cursor = end

    const fullCircle = percent > 0.999999
    const path = fullCircle
      ? `M ${cx} ${cy} m 0 -${r} a ${r} ${r} 0 1 1 0 ${2 * r} a ${r} ${r} 0 1 1 0 -${2 * r}`
      : arcPath(cx, cy, r, start, end)

    return {
      ...item,
      start,
      end,
      mid: (start + end) / 2,
      percent,
      path,
    }
  })
})

function sliceTransform(slice: Slice) {
  if (hoverKey.value !== slice.key) return undefined
  const offset = 7
  const dx = Math.cos(slice.mid) * offset
  const dy = Math.sin(slice.mid) * offset
  return `translate(${dx}, ${dy})`
}

function emitSelect(slice: Slice) {
  emit('select', { ...slice, percent: slice.percent })
}
</script>

<style scoped>
.pie-root{ width: 100%; }
.pie-empty{ padding: 10px 0; }
.pie-wrap{
  display:flex;
  gap: 14px;
  align-items: center;
  justify-content: space-between;
}
.pie-svg{
  width: 220px;
  height: 220px;
  flex: 0 0 auto;
}
.pie-slice{
  cursor: pointer;
  stroke: rgba(255,255,255,0.98);
  stroke-width: 2;
  transition: transform 160ms ease, filter 160ms ease, opacity 160ms ease;
  filter: drop-shadow(0 8px 18px rgba(15, 23, 42, 0.12));
}
.pie-slice:hover{
  filter: drop-shadow(0 12px 26px rgba(15, 23, 42, 0.18));
}
.pie-center-title{
  fill: rgba(15, 23, 42, 0.6);
  font-size: 12px;
  font-weight: 700;
}
.pie-center-value{
  fill: rgba(15, 23, 42, 0.9);
  font-size: 16px;
  font-weight: 900;
}
.pie-legend{
  flex: 1;
  min-width: 0;
  display:flex;
  flex-direction: column;
  gap: 8px;
}
.legend-item{
  display:flex;
  align-items:center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 12px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(15, 23, 42, 0.02);
  cursor: pointer;
  transition: background 160ms ease, border-color 160ms ease, transform 160ms ease;
}
.legend-item:hover{
  background: rgba(15, 23, 42, 0.04);
  border-color: rgba(15, 23, 42, 0.10);
  transform: translateY(-1px);
}
.legend-item.active{
  background: rgba(99, 102, 241, 0.06);
  border-color: rgba(99, 102, 241, 0.22);
}
.dot{ width: 10px; height: 10px; border-radius: 999px; flex: 0 0 auto; }
.label{ flex: 1; min-width: 0; font-weight: 700; color: rgba(15, 23, 42, 0.86); }
.percent{ font-variant-numeric: tabular-nums; color: rgba(15, 23, 42, 0.6); font-size: 12px; }

@media (max-width: 1200px){
  .pie-wrap{ flex-direction: column; align-items: stretch; }
  .pie-svg{ margin: 0 auto; }
}
</style>

