import { Directive } from 'vue'

const FadeIn: Directive = {
  mounted(el, binding) {
    const distance = binding?.value?.distance ?? 12
    const delay = binding?.value?.delay ?? 0
    el.classList.add('fade-in')
    el.style.setProperty('--fade-distance', `${distance}px`)
    el.style.setProperty('--fade-delay', `${delay}ms`)

    const options = { threshold: 0.08 }
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          el.classList.add('fade-in--visible')
          if (binding?.value?.once ?? true) observer.unobserve(el)
        } else if (!(binding?.value?.once ?? true)) {
          el.classList.remove('fade-in--visible')
        }
      })
    }, options)

    observer.observe(el)
    ;(el as any).__fadeInObserver = observer
  },
  unmounted(el) {
    const o = (el as any).__fadeInObserver
    if (o) o.disconnect()
  }
}

export default FadeIn
