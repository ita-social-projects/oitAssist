package com.itasocialacademy.oitassist.auth.dao.dto.event;

public record ActivationAccountEvent(
    String email,
    String firstName,
    String token) {
}