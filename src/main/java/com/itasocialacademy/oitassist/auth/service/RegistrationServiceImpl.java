package com.itasocialacademy.oitassist.auth.service;

import com.itasocialacademy.oitassist.auth.dto.request.RegisterRequest;
import com.itasocialacademy.oitassist.user.dao.model.UserActivationToken;
import com.itasocialacademy.oitassist.auth.exceptions.UserNotActivatedException;
import com.itasocialacademy.oitassist.auth.mapper.RegisterRequestMapper;
import com.itasocialacademy.oitassist.auth.service.interfaces.RegistrationService;
import com.itasocialacademy.oitassist.auth.service.interfaces.UserActivationService;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import com.itasocialacademy.oitassist.user.dao.model.User;
import com.itasocialacademy.oitassist.user.dao.repository.UserRepository;
import com.itasocialacademy.oitassist.user.exceptions.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    private final UserRepository userRepository;

    private final RegisterRequestMapper registerRequestMapper;

    private final PasswordEncoder passwordEncoder;

    private final UserActivationService userActivationService;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void createUser(RegisterRequest request) {
        log.info("Registration attempt for email={}", request.getEmail());

        try {
            userRepository.findUserByEmail(request.getEmail()).ifPresent(this::checkUserNotExists);

            User user = registerRequestMapper.toEntity(request);
            user.setPassword(passwordEncoder.encode(request.getPassword()));

            UserActivationToken userActivationToken = UserActivationToken.generateActivationToken();

            user.setUserActivationToken(userActivationToken);

            userRepository.save(user);

            userActivationService.publishActivationEvent(
                request.getEmail(),
                request.getFirstName(),
                userActivationToken.getToken());

            log.info("Activation account event published with email={}", request.getEmail());

            log.info("User successfully created with email={}", request.getEmail());
        } catch (DataIntegrityViolationException e) {
            log.warn("Registration failed due to data integrity violation for email={}",
                request.getEmail(), e);

            throw new UserAlreadyExistsException();
        }
    }

    private void checkUserNotExists(User user) {
        if (user.getUserStatus() == UserStatus.NOT_ACTIVATED) {
            log.warn("User already registered but not activated with email={}", user.getEmail());
            throw new UserNotActivatedException();
        }
        log.warn("User already exists with email={}", user.getEmail());
        throw new UserAlreadyExistsException();
    }
}
