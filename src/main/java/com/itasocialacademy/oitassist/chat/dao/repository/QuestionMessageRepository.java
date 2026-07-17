package com.itasocialacademy.oitassist.chat.dao.repository;

import com.itasocialacademy.oitassist.chat.dao.model.QuestionMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionMessageRepository extends JpaRepository<QuestionMessage, Long> {
}