export function toErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : "请求失败"
}
