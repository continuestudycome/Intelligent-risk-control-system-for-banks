/**
 * 筛选条件变化时延迟触发请求，避免每输入一个字符就打接口。
 */
const DEFAULT_MS = 350

export function useDebouncedLoader(run: () => void | Promise<void>, debounceMs = DEFAULT_MS) {
  let timer: ReturnType<typeof setTimeout> | null = null

  const scheduleReload = () => {
    if (timer != null) clearTimeout(timer)
    timer = setTimeout(() => {
      timer = null
      void run()
    }, debounceMs)
  }

  const cancelScheduled = () => {
    if (timer != null) {
      clearTimeout(timer)
      timer = null
    }
  }

  /** 跳过防抖立即执行（首次进入页面、重置条件、提交成功后刷新等） */
  const reloadNow = () => {
    cancelScheduled()
    void run()
  }

  return { scheduleReload, reloadNow, cancelScheduled }
}
