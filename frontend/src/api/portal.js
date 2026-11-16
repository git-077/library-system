import request from '../utils/request'

/** 门户概览：上架品种数 / 在馆册数 / 已借册数 / 分类数 / 读者数 */
export const getPortalOverview = () => request.get('/portal/overview')
