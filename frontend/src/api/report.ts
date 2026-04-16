import { get, post, put, download } from './request'
import type {
  MedicalReport,
  ReportGenerateDto,
  ReportListParams,
} from '@/types'

/** AI生成医疗报告 */
export function generateReport(data: ReportGenerateDto) {
  return post<MedicalReport>('/report/generate', data)
}

/** AI生成医疗报告（流式，实时展示思维链） */
export function generateReportStream(
  data: ReportGenerateDto,
  onStep: (step: number, title: string, message: string, progress: number) => void,
  onThoughtChain: (thoughtChain: string) => void,
  onComplete: (report: MedicalReport) => void,
  onError: (error: string) => void
): () => void {
  const controller = new AbortController()
  const signal = controller.signal

  const fetchStream = async () => {
    try {
      const response = await fetch('/HIS/report/generate-stream', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream',
        },
        body: JSON.stringify(data),
        signal,
      })

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const reader = response.body?.getReader()
      if (!reader) {
        throw new Error('Response body is null')
      }

      const decoder = new TextDecoder()
      let buffer = ''
      let currentEvent = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (let i = 0; i < lines.length; i++) {
          const line = lines[i].trim()

          if (line.startsWith('event:')) {
            currentEvent = line.substring(6).trim()
          } else if (line.startsWith('data:')) {
            const data = line.substring(5).trim()
            if (data) {
              try {
                const parsed = JSON.parse(data)

                if (currentEvent === 'step') {
                  onStep(
                    parsed.step,
                    parsed.title,
                    parsed.message,
                    parsed.progress
                  )
                } else if (currentEvent === 'thoughtchain') {
                  onThoughtChain(parsed)
                } else if (currentEvent === 'complete') {
                  onComplete(parsed)
                } else if (currentEvent === 'error') {
                  onError(parsed)
                } else if (currentEvent === 'finish') {
                  // 生成完成
                }
              } catch (e) {
                console.error('Failed to parse SSE data:', data, e)
              }
            }
          } else if (line === '') {
            // 空行表示事件结束，重置当前事件
            currentEvent = ''
          }
        }
      }
    } catch (error: any) {
      if (error.name !== 'AbortError') {
        onError(error.message || '网络错误')
      }
    }
  }

  fetchStream()

  // 返回取消函数
  return () => {
    controller.abort()
  }
}

/** 报告列表 */
export function getReportList(params: ReportListParams) {
  return get<MedicalReport[]>('/report/list', params)
}

/** 报告详情 */
export function getReportDetail(id: number) {
  return get<MedicalReport>(`/report/${id}`)
}

/** 导出PDF */
export function exportReportPdf(id: number, title?: string) {
  const filename = title ? `${title}.pdf` : `医疗报告_${id}.pdf`
  return download(`/report/${id}/export-pdf`, filename)
}

/** 确认报告 */
export function confirmReport(id: number) {
  return put(`/report/${id}/confirm`)
}
