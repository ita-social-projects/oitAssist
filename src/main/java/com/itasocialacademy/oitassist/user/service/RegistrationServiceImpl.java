package com.itasocialacademy.oitassist.user.service;

import com.itasocialacademy.oitassist.user.api.dto.OAuthProvisionCommand;
import com.itasocialacademy.oitassist.user.api.dto.RegisterCommand;
import com.itasocialacademy.oitassist.user.api.dto.UserAuthDetails;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import com.itasocialacademy.oitassist.user.dao.model.User;
import com.itasocialacademy.oitassist.user.dao.repository.UserRepository;
import com.itasocialacademy.oitassist.user.exceptions.UserAlreadyExistsException;
import com.itasocialacademy.oitassist.user.exceptions.UserNotActivatedException;
import com.itasocialacademy.oitassist.user.mapper.OAuthProvisionCommandMapper;
import com.itasocialacademy.oitassist.user.mapper.RegisterCommandMapper;
import com.itasocialacademy.oitassist.user.mapper.UserMapper;
import com.itasocialacademy.oitassist.user.service.interfaces.RegistrationService;
import com.itasocialacademy.oitassist.user.service.interfaces.UserActivationService;
import com.itasocialacademy.oitassist.user.service.interfaces.UserService;
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
    private final UserService userService;
    private final RegisterCommandMapper registerCommandMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserActivationService userActivationService;
    private final OAuthProvisionCommandMapper oauthProvisionCommandMapper;
    private final UserMapper userMapper;
    private final RandomPasswordGenerator randomPasswordGenerator;

    /**
     * {@inheritDoc}
     *
     * <p>
     * Checks whether the email is already taken, persists the new user with an
     * encoded password and {@code PENDING} status, then delegates token generation
     * and email dispatch to {@link UserActivationService#initializeActivation}.
     * </p>
     *
     * <p>
     * A {@link DataIntegrityViolationException} guard handles the race condition
     * where two concurrent registrations with the same email both pass the initial
     * uniqueness check — the database unique constraint on {@code users.email} is
     * the authoritative guard.
     * </p>
     */
    @Override
    @Transactional
    public void register(RegisterCommand command) {
        log.info("Registration attempt for email={}", command.email());

        try {
            userRepository.findUserByEmail(command.email())
                .ifPresent(this::rejectIfUserExists);

            User user = registerCommandMapper.toEntity(command);
            user.setPassword(passwordEncoder.encode(command.password()));

            userRepository.save(user);

            userActivationService.initializeActivation(command.email(), command.firstName());
            log.info("User successfully created with email={}", command.email());
        } catch (DataIntegrityViolationException e) {
            log.warn("Registration failed due to data integrity violation for email={}",
                command.email(), e);
            throw new UserAlreadyExistsException();
        }
    }

    /**
     * Provisions a new OAuth user or retrieves the existing user based on the
     * provided OAuth command. If a user with the given email already exists, their
     * authentication details are returned. Otherwise, a new user is created and
     * their authentication details are returned.
     *
     * @param command the command containing the required OAuth user details, such
     *                as email and profile information
     * @return the authentication details of the newly created or existing user
     */
    @Override
    @Transactional
    public UserAuthDetails provisionOAuthUser(OAuthProvisionCommand command) {
        log.info("OAuth2 provisioning attempt for email={}", command.email());

        return userService.findAuthDetailsByEmail(command.email())
            .map(existing -> {
                log.debug("OAuth2 login matched existing user email={}", command.email());
                return existing;
            })
            .orElseGet(() -> createNewOAuthUser(command));
    }

    private UserAuthDetails createNewOAuthUser(OAuthProvisionCommand command) {
        try {
            User user = oauthProvisionCommandMapper.toEntity(command);
            user.setPassword(passwordEncoder.encode(randomPasswordGenerator.generate()));

            User saved = userRepository.save(user);
            log.info("OAuth2 user provisioned email={} id={}", saved.getEmail(), saved.getId());
            return userMapper.toUserAuthDetails(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent OAuth2 provisioning for email={}; resolving by re-read",
                command.email(), e);
            return userService.findAuthDetailsByEmail(command.email())
                .orElseThrow(() -> new IllegalStateException(
                    "Unique constraint violated but no row found for email " + command.email(), e));
        }
    }

    /**
     * Rejects a registration attempt when a user with the given email already
     * exists. Always throws — the name reflects intent, not a boolean check.
     *
     * @param user the existing user found by email
     * @throws UserNotActivatedException  if the existing account is {@code PENDING}
     * @throws UserAlreadyExistsException if the existing account is in any other
     *                                    status
     */
    private void rejectIfUserExists(User user) {
        if (user.getUserStatus() == UserStatus.PENDING) {
            log.warn("Registration rejected — account exists but not activated for email={}",
                user.getEmail());
            throw new UserNotActivatedException();
        }
        log.warn("Registration rejected — account already exists for email={}", user.getEmail());
        throw new UserAlreadyExistsException();
    }
}