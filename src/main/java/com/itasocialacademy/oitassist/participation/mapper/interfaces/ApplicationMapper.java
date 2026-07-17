package com.itasocialacademy.oitassist.participation.mapper.interfaces;

import com.itasocialacademy.oitassist.participation.dao.dto.request.CreateApplicationRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.CreateApplicationResponse;
import com.itasocialacademy.oitassist.participation.dao.model.Application;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface ApplicationMapper
    extends EnrollmentMapper<Application, CreateApplicationRequest, CreateApplicationResponse> {
}
