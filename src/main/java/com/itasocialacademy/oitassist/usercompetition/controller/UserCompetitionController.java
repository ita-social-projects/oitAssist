package com.itasocialacademy.oitassist.usercompetition.controller;

import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import com.itasocialacademy.oitassist.core.rest.controller.AbstractRestControllerImpl;
import com.itasocialacademy.oitassist.usercompetition.dao.dto.request.CreateUserCompetitionDTO;
import com.itasocialacademy.oitassist.usercompetition.dao.dto.request.UpdateUserCompetitionDTO;
import com.itasocialacademy.oitassist.usercompetition.dao.dto.response.ResponseUserCompetitionDTO;
import com.itasocialacademy.oitassist.usercompetition.dao.enums.UserCompetitionStatus;
import com.itasocialacademy.oitassist.usercompetition.service.interfaces.UserCompetitionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.itasocialacademy.oitassist.usercompetition.dao.model.UserCompetitionId;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "UsersCompetition", description = "Operations related to users competitions")
@RequestMapping("/api/v1/userCompetition")
public class UserCompetitionController extends AbstractRestControllerImpl<UserCompetitionId, CreateUserCompetitionDTO, UpdateUserCompetitionDTO, ResponseUserCompetitionDTO, UserCompetitionService> {
    protected UserCompetitionController(UserCompetitionService service) {
        super(service);
    }

    @GetMapping("/getAllCompetitionsByStatus")
    public ResponseEntity<Page<ResponseUserCompetitionDTO>> getAllCompetitionByStatus(
            @RequestParam UserCompetitionStatus status,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(service.getAllCompetitionsByStatus(status, pageable));
    }

    @GetMapping("/unreadCount")
    public ResponseEntity<Long> countOfUnreadInvites() {

        return ResponseEntity.ok(service.countOfUnreadInvites());
    }

    @PatchMapping("/{competitionId}/userRespond")
    public ResponseEntity<ResponseUserCompetitionDTO> userRespondToInvitation(
            @PathVariable Long competitionId,
            @RequestParam UserCompetitionStatus status
    ) {
        return ResponseEntity.ok(service.updateUserCompetitionStatus(competitionId, status));
    }


}