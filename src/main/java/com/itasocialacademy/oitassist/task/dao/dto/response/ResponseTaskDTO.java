package com.itasocialacademy.oitassist.task.dao.dto.response;

import com.itasocialacademy.oitassist.core.rest.dto.EntityDTO;
import lombok.*;
import org.springframework.modulith.NamedInterface;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@NamedInterface("ResponseTaskDTO")
public class ResponseTaskDTO implements EntityDTO<Long> {
    Long id;
    String title;
    String fileUrl;
    String description;
    Long competitionId;
}
