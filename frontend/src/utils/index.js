/**
 * 通用展示工具
 */

/** 借阅记录状态文案（逾期按 due_date 判断） */
export function borrowStatus(row) {
  if (row.status === 1) return '已归还'
  const due = new Date(String(row.dueDate).replace(/-/g, '/'))
  return due.getTime() < Date.now() ? '已逾期' : '借出中'
}

/** 借阅状态对应的标签类型 */
export function borrowStatusType(row) {
  const status = borrowStatus(row)
  if (status === '已归还') return 'success'
  if (status === '已逾期') return 'danger'
  return 'warning'
}
