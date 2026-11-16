import request from '../utils/request'

/** 操作日志分页查询（仅管理员） */
export const getLogPage = params => request.get('/logs/page', { params })
