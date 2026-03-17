package com.itasocialacademy.oitassist.news.controller;

import com.itasocialacademy.oitassist.core.dao.dto.response.PageResponse;
import com.itasocialacademy.oitassist.news.dao.dto.response.ResponseNewsListItemDto;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import com.itasocialacademy.oitassist.core.rest.controller.AbstractRestControllerImpl;
import com.itasocialacademy.oitassist.news.dao.dto.request.CreateNewsDTO;
import com.itasocialacademy.oitassist.news.dao.dto.request.UpdateNewsDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ResponseNewsDto;
import com.itasocialacademy.oitassist.news.service.interfaces.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import static org.springframework.data.domain.Sort.Direction.DESC;

@Tag(name = "News v1", description = "Operations related to news")
@RestController
@RequestMapping("/api/v1/news")
public class NewsController
    extends AbstractRestControllerImpl<Long, CreateNewsDTO, UpdateNewsDto, ResponseNewsDto, NewsService> {
    protected NewsController(NewsService service) {
        super(service);
    }

    @Operation(summary = "Create news", description = "Creates a new news item")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "News created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORG')")

    @Override
    public ResponseEntity<ResponseNewsDto> save(
        @Valid @RequestBody CreateNewsDTO dto) {
        return super.save(dto);
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
    @Override
    public ResponseEntity<ResponseNewsDto> update(
        @Valid @RequestBody UpdateNewsDto dto) {
        return super.update(dto);
    }

    @Operation(summary = "Get news by id", description = "Returns news by its identifier")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "News found"),
        @ApiResponse(responseCode = "404", description = "News not found")
    })
    @GetMapping("/{id}")
    @Override
    public ResponseEntity<ResponseNewsDto> getById(
        @PathVariable Long id) {
        return super.getById(id);
    }

    @Operation(summary = "Delete news", description = "Deletes news by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "News deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "News not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORG')")
    @Override
    public ResponseEntity<Void> delete(
        @PathVariable Long id) {
        return super.delete(id);
    }

    @Operation(
        summary = "Get published news",
        description = "Returns paginated list of published news sorted by publishedAt descending")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Published news retrieved successfully")
    })
    @Parameters({
        @Parameter(name = "page", description = "Zero-based page index", example = "0"),
        @Parameter(name = "size", description = "Page size", example = "5"),
        @Parameter(name = "sort", description = "Sorting criteria", example = "publishedAt,desc")
    })
    @GetMapping
    public ResponseEntity<PageResponse<ResponseNewsListItemDto>> getPublishedNews(
        @Parameter(hidden = true) @PageableDefault(
            size = 5,
            sort = "publishedAt",
            direction = DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(service.getPublishedNews(pageable)));
    }
}
