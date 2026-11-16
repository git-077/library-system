package com.library.module.favorite.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.library.common.exception.BizException;
import com.library.module.book.entity.Book;
import com.library.module.book.service.BookService;
import com.library.module.favorite.entity.Favorite;
import com.library.module.favorite.mapper.FavoriteMapper;
import com.library.module.favorite.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    private final BookService bookService;

    @Override
    public void addFavorite(Long userId, Long bookId) {
        Book book = bookService.getById(bookId);
        if (book == null || book.getStatus() == 0) {
            throw new BizException("图书不存在或已下架");
        }
        Long count = lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getBookId, bookId)
                .count();
        if (count > 0) {
            throw new BizException("已收藏过该图书");
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setBookId(bookId);
        save(favorite);
    }

    @Override
    public void removeFavorite(Long userId, Long bookId) {
        remove(lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getBookId, bookId)
                .getWrapper());
    }

    @Override
    public List<Book> myFavorites(Long userId) {
        List<Long> bookIds = lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .orderByDesc(Favorite::getId)
                .list()
                .stream()
                .map(Favorite::getBookId)
                .toList();
        if (bookIds.isEmpty()) {
            return List.of();
        }
        return bookService.listByIds(bookIds);
    }
}
