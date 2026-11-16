import request from '../utils/request'

export const getBookPage = params => request.get('/books/page', { params })
export const getBookDetail = id => request.get(`/books/${id}`)
export const addBook = data => request.post('/books', data)
export const updateBook = data => request.put('/books', data)
export const changeBookStatus = (id, status) =>
  request.put(`/books/${id}/status`, null, { params: { status } })
export const deleteBook = id => request.delete(`/books/${id}`)
