package com.itasocialacademy.oitassist.chat.service.interfaces;

import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateQuestionRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadSummaryResponseDTO;
import org.springframework.data.domain.Page;

public interface ParticipantForumService {
    Page<QuestionThreadSummaryResponseDTO> getForumQuestions(
            Long taskAssignmentId,
            int page,
            int size
    );

    QuestionThreadResponseDTO createQuestion(
            Long taskAssignmentId,
            CreateQuestionRequestDTO request
    );
}