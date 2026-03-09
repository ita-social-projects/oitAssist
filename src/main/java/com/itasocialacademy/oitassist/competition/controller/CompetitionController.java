package com.itasocialacademy.oitassist.competition.controller;

import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateCompetitionDto;
import com.itasocialacademy.oitassist.competition.dao.dto.request.UpdateCompetitionDto;
import com.itasocialacademy.oitassist.competition.dao.dto.response.CompetitionResponseDto;
import com.itasocialacademy.oitassist.competition.service.interfaces.CompetitionService;
import com.itasocialacademy.oitassist.core.rest.controller.AbstractRestControllerImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Competitions v1", description = "Operations related to Olympiads")
@RestController
@RequestMapping("api/v1/competitions")
public class CompetitionController
    extends AbstractRestControllerImpl<
        Long,
        CreateCompetitionDto,
        UpdateCompetitionDto,
        CompetitionResponseDto,
        CompetitionService> {

    protected CompetitionController(CompetitionService service) {
        super(service);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORG')")
    @Override
    public ResponseEntity<CompetitionResponseDto> save(
        @Valid @RequestBody CreateCompetitionDto createDto) {
        return super.save(createDto);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORG')")
    @Override
    public ResponseEntity<CompetitionResponseDto> update(
        @Valid @RequestBody UpdateCompetitionDto updateDTO) {
        return super.update(updateDTO);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORG')")
    @Override
    public ResponseEntity<Void> delete(
        @PathVariable Long id) {
        return super.delete(id);
    }

    @GetMapping("/{id}")
    @Override
    public ResponseEntity<CompetitionResponseDto> getById(
        @PathVariable Long id) {
        return super.getById(id);
    }

    @GetMapping()
    public ResponseEntity<List<CompetitionResponseDto>> getAllCompetitions () {
        return null;
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<CompetitionResponseDto>> getCompetitionTasks (
        @PathVariable Long id) {
        return null;
    }
}
