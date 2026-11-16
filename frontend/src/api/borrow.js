import request from '../utils/request'

export const borrowBook = bookId => request.post('/borrows', null, { params: { bookId } })
export const returnBook = id => request.put(`/borrows/${id}/return`)
export const renewBook = id => request.put(`/borrows/${id}/renew`)
export const getMyBorrows = params => request.get('/borrows/my', { params })
export const getBorrowPage = params => request.get('/borrows/page', { params })

/** 手动触发逾期扫描（管理员，幂等） */
export const checkOverdue = () => request.post('/borrows/overdue-check')

/** 导出借阅记录 CSV（管理员），返回 Blob */
export const exportBorrows = params => request.get('/borrows/export', { params, responseType: 'blob' })
