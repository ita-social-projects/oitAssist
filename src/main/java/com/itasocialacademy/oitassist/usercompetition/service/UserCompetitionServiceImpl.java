package com.itasocialacademy.oitassist.usercompetition.service;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;

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
}
