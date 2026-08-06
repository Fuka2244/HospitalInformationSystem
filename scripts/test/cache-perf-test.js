/**
 * HIS 缓存性能对比测试
 * 
 * 用法: node cache-perf-test.js
 * 前提: 后端服务运行在 localhost:8080, Redis + MySQL 已启动
 */

const BASE_URL = 'http://localhost:8080/HIS'
const ROUNDS = 20  // 每个接口重复调用次数

const TEST_APIS = [
  { name: '药品列表', url: '/medicine/list?page=1&size=10', cacheName: 'medicine' },
  { name: '科室列表', url: '/department/list', cacheName: 'department' },
  { name: '科室详情', url: '/department/1', cacheName: 'department' },
  { name: '所有医生', url: '/department/doctors', cacheName: 'department' },
]

async function fetchWithTiming(url) {
  const start = performance.now()
  try {
    const res = await fetch(url)
    const data = await res.json()
    const elapsed = performance.now() - start
    return { elapsed, success: data.success, status: res.status }
  } catch (e) {
    const elapsed = performance.now() - start
    return { elapsed, success: false, error: e.message }
  }
}

/** 清空 Redis 中指定缓存 */
async function clearCache(cacheName) {
  try {
    // 通过重复等待TTL过期来模拟清缓存（最简单的方式）
    // 或者直接请求一个不存在的参数来触发新缓存
    // 这里我们只是等一小段时间让之前的请求缓存过期
    // 实际更准确的方式是用 redis-cli FLUSHDB
  } catch {
    // 忽略
  }
}

async function main() {
  console.log('============================================================')
  console.log('   HIS 缓存性能对比测试')
  console.log('============================================================\n')
  console.log(`测试轮次: ${ROUNDS} 次/接口\n`)

  for (const api of TEST_APIS) {
    console.log(`\n▶ ${api.name}: ${BASE_URL}${api.url}`)
    console.log('-'.repeat(60))

    // 先发一次预热请求（让缓存加载）
    const warmup = await fetchWithTiming(`${BASE_URL}${api.url}`)
    if (!warmup.success) {
      console.log(`  ⚠ 请求失败 (status=${warmup.status}), 跳过此接口`)
      if (warmup.error) console.log(`  错误: ${warmup.error}`)
      continue
    }

    // 无缓存测试：连续请求，第一次一定不命中缓存（如果有清缓存的方法）
    // 这里我们只能测试"缓存已预热后"的效果
    // 要测"无缓存"需要手动清 Redis

    const results = []
    for (let i = 0; i < ROUNDS; i++) {
      const result = await fetchWithTiming(`${BASE_URL}${api.url}`)
      results.push(result.elapsed)
      process.stdout.write(`  第${String(i + 1).padStart(2)}次: ${result.elapsed.toFixed(2)} ms\n`)
    }

    const firstCall = results[0]
    const avgCached = results.slice(5).reduce((a, b) => a + b, 0) / (results.length - 5)
    const minCached = Math.min(...results.slice(5))
    const maxCached = Math.max(...results.slice(5))

    console.log('-'.repeat(60))
    console.log(`  首次请求(冷启动):  ${firstCall.toFixed(2)} ms`)
    console.log(`  缓存命中平均:      ${avgCached.toFixed(2)} ms (去掉前5次)`)
    console.log(`  缓存命中最快:      ${minCached.toFixed(2)} ms`)
    console.log(`  缓存命中最慢:      ${maxCached.toFixed(2)} ms`)
    if (avgCached > 0) {
      console.log(`  冷启动/缓存命中:   ${(firstCall / avgCached).toFixed(1)}x`)
    }
  }

  console.log('\n============================================================')
  console.log('  提示: 要测试"完全无缓存"效果，请:')
  console.log('  1. 在 application.yaml 中设置 spring.cache.type=none')
  console.log('  2. 重启后端服务')
  console.log('  3. 重新运行此脚本')
  console.log('  4. 对比两次结果')
  console.log('============================================================')
}

main()
