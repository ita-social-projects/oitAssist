package com.itasocialacademy.oitassist.news.controller;

import com.itasocialacademy.oitassist.core.rest.controller.AbstractRestControllerImpl;
import com.itasocialacademy.oitassist.news.dao.dto.request.CreateNewsDTO;
import com.itasocialacademy.oitassist.news.dao.dto.request.UpdateNewsDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ResponseNewsDto;
import com.itasocialacademy.oitassist.news.service.interfaces.NewsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/news")

public class NewsController
    extends AbstractRestControllerImpl<Long, CreateNewsDTO, UpdateNewsDto, ResponseNewsDto, NewsService> {
    protected NewsController(NewsService service) {
        super(service);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORG')")
    @Override
    public ResponseEntity<ResponseNewsDto> save(
        @Valid @RequestBody CreateNewsDTO dto) {
        return super.save(dto);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORG')")
    @Override
    public ResponseEntity<ResponseNewsDto> update(
        @Valid @RequestBody UpdateNewsDto dto) {
        return super.update(dto);
    }

    @GetMapping("/{id}")
    @Override
    public ResponseEntity<ResponseNewsDto> getById(
        @PathVariable Long id) {
        return super.getById(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORG')")
    @Override
    public ResponseEntity<Void> delete(
        @PathVariable Long id) {
        return super.delete(id);
    }
}
