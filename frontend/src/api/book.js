import request from '../utils/request'

export const getBookPage = params => request.get('/books/page', { params })
export const getBookDetail = id => request.get(`/books/${id}`)
export const addBook = data => request.post('/books', data)
export const updateBook = data => request.put('/books', data)
export const changeBookStatus = (id, status) =>
  request.put(`/books/${id}/status`, null, { params: { status } })
export const deleteBook = id => request.delete(`/books/${id}`)

/** 批量导入 CSV：后端逐行校验，返回成功/跳过/失败统计与逐行原因 */
export const importBooks = file => {
  const form = new FormData()
  form.append('file', file)
  return request.post('/books/import', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}

/** 下载导入模板（blob，避免浏览器直接打开 CSV） */
export const downloadImportTemplate = () =>
  request.get('/books/import-template', { responseType: 'blob' })
