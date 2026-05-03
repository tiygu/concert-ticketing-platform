import { reactive, ref, type Ref } from 'vue'

export interface FormRules {
  [field: string]: {
    required?: boolean
    minLen?: number
    maxLen?: number
    message?: string
    validator?: (v: unknown) => string | true
  }[]
}

export function useForm<T extends Record<string, unknown>>(initial: T, rules: FormRules = {}) {
  const form = reactive({ ...initial }) as T
  const errors: Ref<Record<string, string>> = ref({})
  const submitting = ref(false)

  function validateField(field: string): string | true {
    const val = (form as Record<string, unknown>)[field]
    const fieldRules = rules[field]
    if (!fieldRules) return true

    for (const rule of fieldRules) {
      if (rule.required && (!val || (typeof val === 'string' && !val.trim()))) {
        return rule.message || '此字段不能为空'
      }
      if (rule.minLen && typeof val === 'string' && val.length < rule.minLen) {
        return rule.message || `最少 ${rule.minLen} 个字符`
      }
      if (rule.maxLen && typeof val === 'string' && val.length > rule.maxLen) {
        return rule.message || `最多 ${rule.maxLen} 个字符`
      }
      if (rule.validator) {
        const result = rule.validator(val)
        if (result !== true) return result
      }
    }
    return true
  }

  function validate(): boolean {
    const newErrors: Record<string, string> = {}
    let valid = true
    for (const field of Object.keys(rules)) {
      const result = validateField(field)
      if (result !== true) {
        newErrors[field] = result
        valid = false
      }
    }
    errors.value = newErrors
    return valid
  }

  function clearErrors() {
    errors.value = {}
  }

  function reset() {
    Object.keys(initial).forEach(k => {
      (form as Record<string, unknown>)[k] = initial[k]
    })
    errors.value = {}
    submitting.value = false
  }

  return { form, errors, submitting, validate, validateField, clearErrors, reset }
}
