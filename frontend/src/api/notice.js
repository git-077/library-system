import request from '../utils/request'

export const getNoticePage = params => request.get('/notices/page', { params })
export const addNotice = data => request.post('/notices', data)
export const updateNotice = (id, data) => request.put(`/notices/${id}`, data)
export const deleteNotice = id => request.delete(`/notices/${id}`)
