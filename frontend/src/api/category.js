import request from '../utils/request'

export const listCategories = () => request.get('/categories')
export const addCategory = data => request.post('/categories', data)
export const updateCategory = (id, data) => request.put(`/categories/${id}`, data)
export const deleteCategory = id => request.delete(`/categories/${id}`)
