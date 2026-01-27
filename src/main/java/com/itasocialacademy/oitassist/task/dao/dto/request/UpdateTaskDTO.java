package com.itasocialacademy.oitassist.task.dao.dto.request;

import com.itasocialacademy.oitassist.core.rest.dto.UpdateEntityDTO;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UpdateTaskDTO implements UpdateEntityDTO<Long> {
    Long id;
    String title;
    String description;
    Long competitionId;
}
