package com.itasocialacademy.oitassist.user.mapper.request;

import com.itasocialacademy.oitassist.user.dao.dto.request.CreateUserRequest;
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
class CreateUserRequestMapperTest {
    private final CreateUserRequestMapper mapper = Mappers.getMapper(CreateUserRequestMapper.class);

    @Test
    void shouldMapRequestToEntity() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("test@mail.com");
        request.setPassword("1234");
        request.setFirstName("John");
        request.setLastName("Doe");

        User user = mapper.toEntity(request);

        assertEquals("test@mail.com", user.getEmail());
        assertEquals("1234", user.getPassword());
        assertEquals(request.getLastName(), user.getSurname());
        assertEquals(Role.USER, user.getRole());
        assertEquals(UserStatus.NOT_ACTIVATED, user.getUserStatus());
        assertNotNull(user.getCreatedAt());
    }
}