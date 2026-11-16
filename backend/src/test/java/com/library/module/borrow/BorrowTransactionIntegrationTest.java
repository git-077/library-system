package com.library.module.borrow;

import com.library.common.exception.BizException;
import com.library.module.book.entity.Book;
import com.library.module.book.mapper.BookMapper;
import com.library.module.borrow.entity.BorrowRecord;
import com.library.module.borrow.service.BorrowService;
import com.library.module.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 借阅事务集成测试：跑在真实 MySQL 容器上。
 *
 * <p>为什么必须用真实库：单元测试用 Mock 验证不了
 * 「条件 UPDATE 扣库存 + 事务回滚 + 并发超借」这类只有数据库才有的语义。
 *
 * <p>本机没装 Docker 时 {@code disabledWithoutDocker = true} 会自动跳过，
 * 所以本地 {@code mvn test} 不受影响，CI（ubuntu runner 自带 Docker）会真正执行。
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class BorrowTransactionIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.33")
            .withDatabaseName("library_system_db")
            // 直接用仓库里的建表脚本，测试与生产同源
            .withInitScript("init.sql");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private UserService userService;

    @Test
    @DisplayName("借书成功：库存 -1、已借出 +1、生成在借记录")
    void borrowShouldDeductStockAndCreateRecord() {
        Long bookId = createBook("IT-BOOK-1", 2);
        Long userId = createUser("it_borrower_a");

        borrowService.borrowBook(userId, bookId);

        Book after = bookMapper.selectById(bookId);
        assertEquals(1, after.getStock(), "在馆库存应减 1");
        assertEquals(1, after.getBorrowed(), "已借出应加 1");

        long active = borrowService.lambdaQuery()
                .eq(BorrowRecord::getBookId, bookId)
                .eq(BorrowRecord::getStatus, 0)
                .count();
        assertEquals(1, active, "应生成 1 条在借记录");
    }

    @Test
    @DisplayName("库存为 0 时借书失败，且不产生脏记录")
    void borrowShouldFailWhenOutOfStock() {
        Long bookId = createBook("IT-BOOK-2", 0);
        Long userId = createUser("it_borrower_b");

        assertThrows(BizException.class, () -> borrowService.borrowBook(userId, bookId));

        Book after = bookMapper.selectById(bookId);
        assertEquals(0, after.getStock());
        assertEquals(0, after.getBorrowed(), "失败不应改变已借出数量");
        assertEquals(0, borrowService.lambdaQuery()
                .eq(BorrowRecord::getBookId, bookId).count(), "失败不应留下借阅记录");
    }

    @Test
    @DisplayName("并发借同一本书：库存 1 时只能有 1 人成功，不会超借")
    void concurrentBorrowShouldNotOversell() throws Exception {
        Long bookId = createBook("IT-BOOK-3", 1);
        Long userA = createUser("it_borrower_c");
        Long userB = createUser("it_borrower_d");

        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (Long userId : new Long[]{userA, userB}) {
                pool.submit(() -> {
                    try {
                        start.await();
                        borrowService.borrowBook(userId, bookId);
                        success.incrementAndGet();
                    } catch (BizException e) {
                        rejected.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(15, TimeUnit.SECONDS), "并发任务应在 15 秒内结束");
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, success.get(), "只应有 1 人借成功");
        assertEquals(1, rejected.get(), "另一人应被库存不足拒绝");
        Book after = bookMapper.selectById(bookId);
        assertEquals(0, after.getStock(), "库存应为 0，不能被扣成负数");
        assertEquals(1, after.getBorrowed());
        assertEquals(1, borrowService.lambdaQuery()
                .eq(BorrowRecord::getBookId, bookId).eq(BorrowRecord::getStatus, 0).count());
    }

    // ---------- 造数辅助 ----------

    private Long createBook(String isbn, int stock) {
        Book book = new Book();
        book.setIsbn(isbn);
        book.setName("集成测试图书-" + isbn);
        book.setAuthor("IT");
        book.setPublisher("测试出版社");
        book.setCategoryId(1L);
        book.setPrice(new BigDecimal("10.00"));
        book.setStock(stock);
        book.setBorrowed(0);
        book.setStatus(1);
        book.setDeleted(0);
        bookMapper.insert(book);
        return book.getId();
    }

    private Long createUser(String username) {
        userService.createUser(username, "123456", "集成测试用户", null);
        return userService.lambdaQuery().eq(com.library.module.user.entity.User::getUsername, username)
                .one().getId();
    }
}
