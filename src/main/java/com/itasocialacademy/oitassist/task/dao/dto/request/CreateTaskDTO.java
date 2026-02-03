package com.itasocialacademy.oitassist.task.dao.dto.request;

import com.itasocialacademy.oitassist.core.rest.dto.CreateEntityDTO;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CreateTaskDTO implements CreateEntityDTO<Long> {
    String title;
    String description;
    Long competitionId;
}
