package com.itasocialacademy.oitassist.usercompetition.service;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.BusinessException;
import com.itasocialacademy.oitassist.core.exceptions.NotFoundException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.core.rest.service.AbstractServiceImpl;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.user.api.dto.CurrentUserDTO;
import com.itasocialacademy.oitassist.user.api.facade.UserFacadeImpl;
import com.itasocialacademy.oitassist.user.api.interfaces.UserFacade;
import com.itasocialacademy.oitassist.user.dao.dto.request.CreateUserDTO;
import com.itasocialacademy.oitassist.user.dao.dto.request.UpdateUserDTO;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseUserDTO;
import com.itasocialacademy.oitassist.user.dao.model.User;
import com.itasocialacademy.oitassist.user.dao.repository.UserRepository;
import com.itasocialacademy.oitassist.user.mapper.UserMapper;
import com.itasocialacademy.oitassist.usercompetition.dao.dto.request.CreateUserCompetitionDTO;
import com.itasocialacademy.oitassist.usercompetition.dao.dto.request.UpdateUserCompetitionDTO;
import com.itasocialacademy.oitassist.usercompetition.dao.dto.response.ResponseUserCompetitionDTO;
import com.itasocialacademy.oitassist.usercompetition.dao.enums.UserCompetitionStatus;
import com.itasocialacademy.oitassist.usercompetition.dao.model.UserCompetition;
import com.itasocialacademy.oitassist.usercompetition.dao.model.UserCompetitionId;
import com.itasocialacademy.oitassist.usercompetition.dao.repository.UserCompetitionRepository;
import com.itasocialacademy.oitassist.usercompetition.mapper.UserCompetitionMapper;
import com.itasocialacademy.oitassist.usercompetition.service.interfaces.UserCompetitionService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCompetitionServiceImpl extends AbstractServiceImpl<UserCompetitionId, UserCompetition, CreateUserCompetitionDTO, UpdateUserCompetitionDTO, ResponseUserCompetitionDTO, UserCompetitionRepository, UserCompetitionMapper> implements UserCompetitionService {

    private final SecurityFacade securityFacade;

    protected UserCompetitionServiceImpl(UserCompetitionRepository repository, UserCompetitionMapper mapper, SecurityFacade securityFacade) {
        super(repository, mapper);
        this.securityFacade = securityFacade;
    }

    @Override
    public boolean hasActiveCompetitions(Long userId, List<CompetitionStatus> statuses) {
        List<String> statusStrings = statuses.stream()
            .map(Enum::name)
            .toList();

        return repository.existsByUserIdAndStatusIn(userId, statusStrings);
    }

    @Override
    public Page<ResponseUserCompetitionDTO> getAllCompetitionsByStatus(UserCompetitionStatus status, Pageable pageable) {
        Long authorId = securityFacade.getCurrentUserId()
                .orElseThrow(() -> new AuthorizationException("User is not authenticated", ErrorCode.ACCESS_DENIED));

        Page<UserCompetition> userCompetitions = repository.findAllByAuthorIdAndStatus(authorId, status, pageable);

        return userCompetitions.map(mapper::toDto);
    }

    @Override
    public void markAsRead(Long competitionId){
        Long authorId = securityFacade.getCurrentUserId()
                .orElseThrow(() -> new AuthorizationException("User is not authenticated", ErrorCode.ACCESS_DENIED));

        UserCompetitionId userCompetitionId = new UserCompetitionId(authorId, competitionId);

        UserCompetition userCompetition = repository.findById(userCompetitionId)
                .orElseThrow(() -> new NotFoundException("Invitation not found", ErrorCode.ENTITY_NOT_FOUND));

        userCompetition.setRead(true);
        repository.save(userCompetition);
    }

    @Override
    public Long countOfUnreadInvites() {
        Long authorId = securityFacade.getCurrentUserId()
                .orElseThrow(() -> new AuthorizationException("User is not authenticated", ErrorCode.ACCESS_DENIED));

        return repository.countByAuthorIdAndStatusAndIsReadFalse(authorId, UserCompetitionStatus.INVITED);
    }

    @Override
    @Transactional
    public ResponseUserCompetitionDTO updateUserCompetitionStatus(Long competitionId, UserCompetitionStatus status) {
        validateResponseStatus(status);

        Long authorId = securityFacade.getCurrentUserId()
                .orElseThrow(() -> new AuthorizationException("User is not authenticated", ErrorCode.ACCESS_DENIED));

        UserCompetitionId userCompetitionId = new UserCompetitionId(authorId, competitionId);

        UserCompetition userCompetition = repository.findById(userCompetitionId)
                .orElseThrow(() -> new NotFoundException("Invitation not found", ErrorCode.ENTITY_NOT_FOUND));

        validateInvitationIsPending(userCompetition);

        userCompetition.setStatus(status);
        userCompetition.setRead(true);
        userCompetition.setUserRespondedAt(Instant.now());

        return mapper.toDto(userCompetition);
    }

    private void validateResponseStatus(UserCompetitionStatus status) {
        if (status != UserCompetitionStatus.ACCEPTED && status != UserCompetitionStatus.REJECTED) {
            throw new ValidationException("Status must be ACCEPTED or REJECTED", ErrorCode.INVITATION_INVALID_RESPONSE_STATUS);
        }
    }

    private void validateInvitationIsPending(UserCompetition userCompetition) {
        if (userCompetition.getStatus() != UserCompetitionStatus.INVITED) {
            throw new ValidationException("Invitation has already been responded to or expired", ErrorCode.INVITATION_ALREADY_RESPONDED);
        }
    }
}
