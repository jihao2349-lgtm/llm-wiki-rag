import type { Modality, SourceStatus, TaskStatus } from "../types"

export function sourceStatusType(status: SourceStatus) {
  if (status === "已摄入") return "success"
  if (status === "解析中") return "info"
  if (status === "失败") return "error"
  return "warning"
}

export function taskStatusType(status: TaskStatus) {
  if (status === "Done") return "success"
  if (status === "Processing") return "info"
  if (status === "Failed" || status === "Cancelled") return "error"
  return "warning"
}

export function taskStatusLabel(status: TaskStatus) {
  const labels: Record<TaskStatus, string> = {
    Pending: "等待中",
    Processing: "处理中",
    Done: "完成",
    Failed: "失败",
    Cancelled: "已取消",
  }

  return labels[status]
}

export function modalityIcon(modality: Modality) {
  switch (modality) {
    case "image":
      return "image"
    case "audio":
      return "audio"
    case "video":
      return "video"
    case "mixed":
      return "spark"
    default:
      return "file"
  }
}

export function modalityLabel(modality: Modality) {
  switch (modality) {
    case "image":
      return "图像"
    case "audio":
      return "音频"
    case "video":
      return "视频"
    case "mixed":
      return "多模态"
    default:
      return "文本"
  }
}
