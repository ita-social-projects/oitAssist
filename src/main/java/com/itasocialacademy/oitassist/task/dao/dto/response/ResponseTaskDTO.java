package com.itasocialacademy.oitassist.task.dao.dto.response;

import com.itasocialacademy.oitassist.core.rest.dto.EntityDTO;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ResponseTaskDTO implements EntityDTO<Long> {
    Long id;
    String title;
    String description;
    Long competitionId;
}
