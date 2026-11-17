import request from '../utils/request'

export const login = data => request.post('/auth/login', data)
export const register = data => request.post('/auth/register', data)
export const getMe = () => request.get('/auth/me')
/** 自助修改密码：需已登录，oldPassword 校验通过才允许写入 */
export const changePassword = data => request.post('/auth/password', data)
