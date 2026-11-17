import request from '../utils/request'

/** 预约一本已借完的书 */
export const reserveBook = bookId => request.post('/reservations', null, { params: { bookId } })

/** 取消预约（本人或管理员） */
export const cancelReservation = id => request.delete(`/reservations/${id}`)

/** 我的预约（含排队位置） */
export const getMyReservations = () => request.get('/reservations/my')

/** 单本书的预约态：队列长度 + 我的状态，供详情页按钮使用 */
export const getBookReservation = bookId => request.get(`/reservations/book/${bookId}`)

/** 管理员：预约队列分页 */
export const getReservationPage = params => request.get('/reservations/page', { params })
