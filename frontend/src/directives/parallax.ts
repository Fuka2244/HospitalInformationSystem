import { Directive } from 'vue'

function safeNumber(v: any, fallback = 0.5){
  const n = parseFloat(String(v))
  return Number.isFinite(n) ? n : fallback
}

const Parallax: Directive = {
  mounted(el, binding) {
    const speed = safeNumber(binding?.value?.speed ?? binding?.value ?? 0.5)
    el.dataset.parallaxSpeed = String(speed)

    const handler = () => {
      const rect = el.getBoundingClientRect()
      const winH = window.innerHeight
      // only update when visible
      if (rect.bottom > 0 && rect.top < winH) {
        const base = window.pageYOffset || document.documentElement.scrollTop
        const y = (base - (el.offsetTop || (base + rect.top))) * speed
        el.style.backgroundPosition = `50% ${y}px`
      }
    }

    const onScroll = () => requestAnimationFrame(handler)
    el.__parallax_onScroll = onScroll
    window.addEventListener('scroll', onScroll, { passive: true })
    window.addEventListener('resize', onScroll)
    // initial
    onScroll()
  },
  unmounted(el) {
    const fn = (el as any).__parallax_onScroll
    if (fn) {
      window.removeEventListener('scroll', fn as EventListener)
      window.removeEventListener('resize', fn as EventListener)
    }
  }
}

export default Parallax
