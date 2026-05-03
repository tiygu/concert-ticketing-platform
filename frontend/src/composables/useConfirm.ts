import { ref } from 'vue'

export function useConfirm() {
  const visible = ref(false)
  const title = ref('确认操作')
  const message = ref('')
  let resolvePromise: ((v: boolean) => void) | null = null

  function open(msg: string, t?: string): Promise<boolean> {
    message.value = msg
    title.value = t || '确认操作'
    visible.value = true
    return new Promise(resolve => { resolvePromise = resolve })
  }

  function confirm() {
    visible.value = false
    resolvePromise?.(true)
  }

  function cancel() {
    visible.value = false
    resolvePromise?.(false)
  }

  return { visible, title, message, open, confirm, cancel }
}

// Singleton instance shared across the app
let singletonConfirm: ReturnType<typeof useConfirm> | null = null

export function useGlobalConfirm() {
  if (!singletonConfirm) {
    singletonConfirm = useConfirm()
  }
  return singletonConfirm
}
