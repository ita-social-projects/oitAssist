package com.itasocialacademy.oitassist.user.service;

import com.itasocialacademy.oitassist.user.dao.dto.event.UserRegisteredEvent;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    private final UserRepository userRepository;

    private final CreateUserRequestMapper createUserRequestMapper;

    private final PasswordEncoder passwordEncoder;

    private final ApplicationEventPublisher eventPublisher;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void createUser(CreateUserRequest request) {
        log.info("Registration attempt for email={}", request.getEmail());

        try {
            userRepository.findUserByEmail(request.getEmail()).ifPresent(this::checkUserStatus);

            User newUser = createUserRequestMapper.toEntity(request);
            newUser.setPassword(passwordEncoder.encode(request.getPassword()));

            String token = UUID.randomUUID().toString();

            RegistrationToken registrationToken = RegistrationToken.builder().createdAt(Instant.now())
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES)).token(token).build();

            newUser.setRegistrationToken(registrationToken);

            userRepository.save(newUser);

            log.info("User registration event published with email={}", request.getEmail());
            eventPublisher.publishEvent(
                new UserRegisteredEvent(
                    request.getEmail(),
                    request.getFirstName(),
                    token));

            log.info("User successfully created with email={}", request.getEmail());
        } catch (DataIntegrityViolationException e) {
            log.warn("Registration failed due to data integrity violation for email={}",
                request.getEmail(), e);

            throw new UserAlreadyExistsException();
        }
    }

    private void checkUserStatus(User user) {
        if (user.getUserStatus() == UserStatus.NOT_ACTIVATED) {
            throw new UserNotActivatedException();
        }
        throw new UserAlreadyExistsException();
    }
}
