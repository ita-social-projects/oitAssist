package com.itasocialacademy.oitassist.news.dao.repository;

import com.itasocialacademy.oitassist.news.dao.model.News;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsRepository extends JpaRepository<News, Long> {
}
