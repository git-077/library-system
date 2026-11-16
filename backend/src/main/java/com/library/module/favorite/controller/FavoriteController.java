package com.library.module.favorite.controller;

import com.library.common.result.Result;
import com.library.module.book.entity.Book;
import com.library.module.favorite.service.FavoriteService;
import com.library.common.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收藏接口（读者 / 管理员均可使用）
 */
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','USER')")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{bookId}")
    public Result<Void> add(@PathVariable Long bookId) {
        favoriteService.addFavorite(SecurityUtil.getCurrentUserId(), bookId);
        return Result.ok();
    }

    @DeleteMapping("/{bookId}")
    public Result<Void> remove(@PathVariable Long bookId) {
        favoriteService.removeFavorite(SecurityUtil.getCurrentUserId(), bookId);
        return Result.ok();
    }

    @GetMapping("/my")
    public Result<List<Book>> my() {
        return Result.ok(favoriteService.myFavorites(SecurityUtil.getCurrentUserId()));
    }
}
