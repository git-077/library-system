import request from '../utils/request'

export const getOverview = () => request.get('/stat/overview')

/** 近 N 天借还趋势 */
export const getBorrowTrend = (days = 30) => request.get('/stat/borrow-trend', { params: { days } })

/** 分类馆藏分布 */
export const getCategoryDistribution = () => request.get('/stat/category-distribution')

/** 借阅次数排行 */
export const getTopBooks = (limit = 8) => request.get('/stat/top-books', { params: { limit } })
