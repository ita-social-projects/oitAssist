package com.itasocialacademy.oitassist.chat.dao.repository;

import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionThreadRepository extends JpaRepository<QuestionThread, Long> {
}