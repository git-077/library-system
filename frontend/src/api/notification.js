import request from '../utils/request'

/** 我的通知分页：unreadOnly=true 只看未读 */
export const getNotificationPage = params => request.get('/notifications/page', { params })

/** 未读数：铃铛角标 */
export const getUnreadCount = () => request.get('/notifications/unread-count')

export const markNotificationRead = id => request.put(`/notifications/${id}/read`)

export const markAllNotificationsRead = () => request.put('/notifications/read-all')
