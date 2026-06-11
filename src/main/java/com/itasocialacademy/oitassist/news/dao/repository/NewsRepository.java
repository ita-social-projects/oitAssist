package com.itasocialacademy.oitassist.news.dao.repository;

import com.itasocialacademy.oitassist.core.rest.repository.EntityRepository;
import com.itasocialacademy.oitassist.news.dao.model.News;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsRepository
    extends JpaRepository<News, Long>, EntityRepository<News, Long>, JpaSpecificationExecutor<News> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
                        UPDATE news
                        SET status = :archivedStatus,
                            archived_at = :archivedAt
                        WHERE status = :publishedStatus
                        AND published_at IS NOT NULL
        AND CAST(published_at AT TIME ZONE 'Europe/Kyiv' AS DATE) <= :thresholdDate
        """, nativeQuery = true)
    int archivedPublishedNewsOlderThanOneMonth(
        @Param("publishedStatus") String publishedStatus,
        @Param("archivedStatus") String archivedStatus,
        @Param("thresholdDate") LocalDate thresholdDate,
        @Param("archivedAt") OffsetDateTime archivedAt);

    @Query("""
            SELECT n
            FROM News n
            WHERE n.status = com.itasocialacademy.oitassist.news.dao.enums.NewsStatus.ARCHIVED
              AND n.archivedAt IS NOT NULL
            ORDER BY n.archivedAt DESC
        """)
    List<News> findArchivedNewsOrderByArchivedAtDesc();
}
