package com.library.module.portal.service;

import com.library.module.portal.dto.PortalOverviewVO;

/**
 * 读者门户服务：只做公开数据的聚合读，不做写操作。
 */
public interface PortalService {

    /** 门户首页概览：上架品种 / 在馆册数 / 已借出 / 分类数 / 读者数 */
    PortalOverviewVO overview();
}
