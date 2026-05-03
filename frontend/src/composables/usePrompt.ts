import { ref } from 'vue'

export function usePrompt() {
  const visible = ref(false)
  const title = ref('')
  const label = ref('')
  let resolvePromise: ((v: string | null) => void) | null = null

  function open(msg: string, t = '请输入'): Promise<string | null> {
    label.value = msg
    title.value = t
    visible.value = true
    return new Promise(resolve => { resolvePromise = resolve })
  }

  function submit(inputValue: string) {
    visible.value = false
    resolvePromise?.(inputValue)
  }

  function cancel() {
    visible.value = false
    resolvePromise?.(null)
  }

  return { visible, title, label, open, submit, cancel }
}

let singletonPrompt: ReturnType<typeof usePrompt> | null = null

export function useGlobalPrompt() {
  if (!singletonPrompt) {
    singletonPrompt = usePrompt()
  }
  return singletonPrompt
}
