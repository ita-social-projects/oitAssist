package com.itasocialacademy.oitassist.user.dao.dto.event;

public record UserRegisteredEvent(
    String email,
    String firstName,
    String token) {
}