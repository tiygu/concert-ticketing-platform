import { onMounted, onUnmounted } from 'vue'

export function useParticles(count = 50) {
  let canvas: HTMLCanvasElement | null = null
  let ctx: CanvasRenderingContext2D | null = null
  let animationId = 0
  let particles: Particle[] = []

  interface Particle {
    x: number; y: number; r: number; speed: number; opacity: number; delay: number
  }

  function init(c: HTMLCanvasElement) {
    canvas = c
    ctx = c.getContext('2d')!
    resize()
    particles = Array.from({ length: count }, () => ({
      x: Math.random() * window.innerWidth,
      y: Math.random() * window.innerHeight,
      r: Math.random() * 3 + 1,
      speed: Math.random() * 0.5 + 0.2,
      opacity: Math.random() * 0.5 + 0.2,
      delay: Math.random() * 10
    }))
    animate()
  }

  function resize() {
    if (canvas) {
      canvas.width = window.innerWidth
      canvas.height = window.innerHeight
    }
  }

  function animate() {
    if (!ctx || !canvas) return
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    for (const p of particles) {
      p.y -= p.speed
      if (p.y < -10) {
        p.y = canvas.height + 10
        p.x = Math.random() * canvas.width
      }
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(0, 245, 255, ${p.opacity})`
      ctx.fill()
    }
    animationId = requestAnimationFrame(animate)
  }

  onMounted(() => window.addEventListener('resize', resize))
  onUnmounted(() => {
    window.removeEventListener('resize', resize)
    cancelAnimationFrame(animationId)
  })

  return { init }
}
