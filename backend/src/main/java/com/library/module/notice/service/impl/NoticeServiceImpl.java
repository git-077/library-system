package com.library.module.notice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.library.common.exception.BizException;
import com.library.module.notice.dto.NoticeDTO;
import com.library.module.notice.entity.Notice;
import com.library.module.notice.mapper.NoticeMapper;
import com.library.module.notice.service.NoticeService;
import org.springframework.stereotype.Service;

@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

    @Override
    public void saveNotice(NoticeDTO dto) {
        Notice notice = new Notice();
        notice.setTitle(dto.getTitle().trim());
        notice.setContent(dto.getContent());
        save(notice);
    }

    @Override
    public void updateNotice(Long id, NoticeDTO dto) {
        if (getById(id) == null) {
            throw new BizException("公告不存在");
        }
        Notice notice = new Notice();
        notice.setId(id);
        notice.setTitle(dto.getTitle().trim());
        notice.setContent(dto.getContent());
        updateById(notice);
    }
}
