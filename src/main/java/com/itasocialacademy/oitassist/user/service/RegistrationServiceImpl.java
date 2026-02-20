package com.itasocialacademy.oitassist.user.service;

import com.itasocialacademy.oitassist.core.service.interfaces.EmailService;
import com.itasocialacademy.oitassist.user.dao.dto.request.CreateUserRequest;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import com.itasocialacademy.oitassist.user.dao.model.RegistrationToken;
import com.itasocialacademy.oitassist.user.dao.model.User;
import com.itasocialacademy.oitassist.user.dao.repository.UserRepository;
import com.itasocialacademy.oitassist.user.exceptions.UserAlreadyExistsException;
import com.itasocialacademy.oitassist.user.exceptions.UserNotActivatedException;
import com.itasocialacademy.oitassist.user.mapper.request.CreateUserRequestMapper;
import com.itasocialacademy.oitassist.user.service.interfaces.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    @Value("${web_client.origin}")
    private String webClientOrigin;

    private final UserRepository userRepository;

    private final EmailService emailService;

    private final CreateUserRequestMapper createUserRequestMapper;

    private final PasswordEncoder passwordEncoder;

    /**
     * {@inheritDoc}
     */
    @Override
    public void createUser(CreateUserRequest request) {
        User user = userRepository.findUserByEmail(request.getEmail()).orElse(null);

        if (user != null) {
            checkUserStatus(user);
            return;
        }

        User newUser = createUserRequestMapper.toEntity(request);
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));

        String token = UUID.randomUUID().toString();

        RegistrationToken registrationToken = RegistrationToken.builder().createdAt(Instant.now())
            .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES)).token(token).build();

        newUser.setRegistrationToken(registrationToken);

        userRepository.save(newUser);

        sendActivationEmail(request, token);
    }

    private void checkUserStatus(User user) {
        if (user.getUserStatus() == UserStatus.NOT_ACTIVATED) {
            throw new UserNotActivatedException();
        }
        throw new UserAlreadyExistsException();
    }

    private void sendActivationEmail(CreateUserRequest request, String token) {
        String activationLink = webClientOrigin + "/confirm_registration?token=" + token;
        Map<String, Object> root = new HashMap<>();
        root.put("name", request.getFirstName());
        root.put("link", activationLink);
        emailService.sendHtmlEmail(request.getEmail(), "registration-confirmation.html", root);
    }
}
