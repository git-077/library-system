package com.library.module.notice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.library.module.notice.dto.NoticeDTO;
import com.library.module.notice.entity.Notice;

public interface NoticeService extends IService<Notice> {

    /** 发布公告 */
    void saveNotice(NoticeDTO dto);

    /** 编辑公告 */
    void updateNotice(Long id, NoticeDTO dto);
}
