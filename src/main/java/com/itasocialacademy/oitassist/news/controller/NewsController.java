package com.itasocialacademy.oitassist.news.controller;

import com.itasocialacademy.oitassist.core.dao.dto.response.PageResponse;
import com.itasocialacademy.oitassist.news.dao.dto.request.CreateNewsDTO;
import com.itasocialacademy.oitassist.news.dao.dto.request.UpdateNewsDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ArchivedNewsByYearDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ResponseNewsAdminListItemDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ResponseNewsDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ResponseNewsListItemDto;
import com.itasocialacademy.oitassist.news.service.interfaces.NewsArchivingService;
import com.itasocialacademy.oitassist.news.service.interfaces.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import static org.springframework.data.domain.Sort.Direction.DESC;

@Tag(name = "News v1", description = "Operations related to news")
@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class NewsController {
    private final NewsArchivingService newsArchivingService;
    private final NewsService service;

    @Operation(summary = "Create news", description = "Creates a new news item")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "News created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORG')")
    public ResponseEntity<ResponseNewsDto> save(
        @Valid @RequestBody CreateNewsDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(dto));
    }

    @Operation(summary = "Update news", description = "Updates existing news")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "News updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "News not found")
    })
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORG')")
    public ResponseEntity<ResponseNewsDto> update(
        @Valid @RequestBody UpdateNewsDto dto) {
        return ResponseEntity.ok().body(service.update(dto));
    }

    @Operation(summary = "Get news by id", description = "Returns news by its identifier")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "News found"),
        @ApiResponse(responseCode = "404", description = "News not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ResponseNewsDto> getById(
        @PathVariable Long id) {
        return ResponseEntity.ok().body(service.getById(id));
    }

    @Operation(summary = "Delete news", description = "Deletes news by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "News deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "News not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORG')")
    public ResponseEntity<Void> delete(
        @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Get published news",
        description = "Returns paginated list of published news sorted by publishedAt descending")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Published news retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<PageResponse<ResponseNewsListItemDto>> getPublishedNews(
        @ParameterObject @PageableDefault(size = 5, sort = "publishedAt", direction = DESC) Pageable pageable,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(PageResponse.from(service.getPublishedNews(pageable, search, date)));
    }

    @Operation(
        summary = "Get archived news",
        description = "Returns list of archived news grouped by year and month")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Archived news retrieved successfully")
    })
    @GetMapping("/archive")
    @PreAuthorize("hasAnyRole('ADMIN','ORG')")
    public ResponseEntity<List<ArchivedNewsByYearDto>> getArchivedNews() {
        return ResponseEntity.ok(newsArchivingService.getArchivedNewsGroupedByYearAndMonth());
    }

    @Operation(
        summary = "Get all news",
        description = "Returns paginated list of all news (with all statuses) for admin panel")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "News retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORG')")
    public ResponseEntity<PageResponse<ResponseNewsAdminListItemDto>> getAllNewsForAdmin(
        @ParameterObject @PageableDefault(size = 15, sort = "createdAt", direction = DESC) Pageable pageable,
        @RequestParam(required = false) String search) {
        return ResponseEntity.ok(PageResponse.from(service.getAllNewsForAdmin(pageable, search)));
    }
}
