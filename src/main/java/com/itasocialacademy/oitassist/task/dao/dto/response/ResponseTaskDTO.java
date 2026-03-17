package com.itasocialacademy.oitassist.task.dao.dto.response;

import com.itasocialacademy.oitassist.core.rest.dto.EntityDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.modulith.NamedInterface;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@NamedInterface("ResponseTaskDTO")
@Schema(description = "Dto to represent Task response")
public class ResponseTaskDTO implements EntityDTO<Long> {
    @Schema(description = "unique identifier of the task", example = "1")
    Long id;
    @Schema(description = "title of the task", example = "best title ever")
    String title;
    @Schema(description = "url to the file in SharePoint", example = "https://sharepoint/folder1/folder2/file")
    String fileUrl;
    @Schema(description = "description of the task", example = "best description ever")
    String description;
    @Schema(description = "id of the competition", example = "1")
    Long competitionId;
}
