package com.library.module.favorite.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.library.module.book.entity.Book;
import com.library.module.favorite.entity.Favorite;

import java.util.List;

public interface FavoriteService extends IService<Favorite> {

    /** 收藏图书 */
    void addFavorite(Long userId, Long bookId);

    /** 取消收藏 */
    void removeFavorite(Long userId, Long bookId);

    /** 我的收藏图书列表 */
    List<Book> myFavorites(Long userId);
}
