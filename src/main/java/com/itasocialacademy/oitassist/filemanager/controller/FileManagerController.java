package com.itasocialacademy.oitassist.filemanager.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/file")
@Tag(name = "File Manager V1", description = "Operations related to file management")
public class FileManagerController {
}
