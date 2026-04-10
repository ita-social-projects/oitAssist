package com.itasocialacademy.oitassist.auth.dto.event;

public record ActivationAccountEvent(
    String email,
    String firstName,
    String token) {
}