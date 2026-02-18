package com.itasocialacademy.oitassist.news.controller;

import com.itasocialacademy.oitassist.news.api.interfaces.NewsFacade;
import com.itasocialacademy.oitassist.news.dao.dto.request.CreateNewsDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {
    private final NewsFacade newsFacade;

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_ORG')")
    public ResponseEntity<Void> createNews(@Valid @RequestBody CreateNewsDTO dto,
                                           @AuthenticationPrincipal(expression = "id") Long userId) {
        newsFacade.createNews(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
