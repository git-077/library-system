import request from '../utils/request'

export const getUserPage = params => request.get('/users/page', { params })
export const addUser = data => request.post('/users', data)
export const updateUser = (id, data) => request.put(`/users/${id}`, data)
export const changeUserStatus = (id, status) =>
  request.put(`/users/${id}/status`, null, { params: { status } })
export const resetUserPassword = id => request.put(`/users/${id}/password`)
export const deleteUser = id => request.delete(`/users/${id}`)
