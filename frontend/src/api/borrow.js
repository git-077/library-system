import request from '../utils/request'

export const borrowBook = bookId => request.post('/borrows', null, { params: { bookId } })
export const returnBook = id => request.put(`/borrows/${id}/return`)
export const renewBook = id => request.put(`/borrows/${id}/renew`)
export const getMyBorrows = params => request.get('/borrows/my', { params })
export const getBorrowPage = params => request.get('/borrows/page', { params })

/** 手动触发逾期扫描（管理员，幂等） */
export const checkOverdue = () => request.post('/borrows/overdue-check')

/** 手动触发到期前提醒（管理员，幂等：同一借阅同一应还日期只发一次） */
export const remindDueSoon = aheadDays =>
  request.post('/borrows/due-soon-remind', null, { params: { aheadDays } })

/** 导出借阅记录（管理员）：format=xlsx 输出 Excel（EasyExcel）、默认 CSV，返回 Blob */
export const exportBorrows = params =>
  request.get('/borrows/export', { params, responseType: 'blob' })
