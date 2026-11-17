import request from '../utils/request'

/**
 * 上传图书封面。后端会把文件落到 uploads/covers/yyyyMMdd/ 下，
 * 返回站内相对地址（/uploads/covers/...），直接写进 book.cover_url 即可。
 */
export const uploadCover = file => {
  const form = new FormData()
  form.append('file', file)
  return request.post('/files/cover', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 30000
  })
}
