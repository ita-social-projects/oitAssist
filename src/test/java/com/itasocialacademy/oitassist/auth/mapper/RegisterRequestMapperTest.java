package com.itasocialacademy.oitassist.auth.mapper;

import com.itasocialacademy.oitassist.auth.dao.dto.request.RegisterRequest;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import com.itasocialacademy.oitassist.user.dao.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class RegisterRequestMapperTest {
    private final RegisterRequestMapper mapper = Mappers.getMapper(RegisterRequestMapper.class);

    @Test
    void toEntity_shouldMapRequestToEntity() {
        // given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@mail.com");
        request.setPassword("1234");
        request.setFirstName("John");
        request.setLastName("Doe");

        // when + then
        User user = mapper.toEntity(request);

        assertEquals("test@mail.com", user.getEmail());
        assertEquals("1234", user.getPassword());
        assertEquals(request.getLastName(), user.getSurname());
        assertEquals(Role.USER, user.getRole());
        assertEquals(UserStatus.NOT_ACTIVATED, user.getUserStatus());
        assertNotNull(user.getCreatedAt());
    }
}