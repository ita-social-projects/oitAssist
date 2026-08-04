package com.itasocialacademy.oitassist.user.api.facade;

import com.itasocialacademy.oitassist.user.api.dto.RegisterCommand;
import com.itasocialacademy.oitassist.user.api.dto.UserAuthDetails;
import com.itasocialacademy.oitassist.user.api.dto.UserProfileDetails;
import com.itasocialacademy.oitassist.user.api.interfaces.UserFacade;
import com.itasocialacademy.oitassist.user.service.interfaces.RegistrationService;
import com.itasocialacademy.oitassist.user.service.interfaces.UserActivationService;
import com.itasocialacademy.oitassist.user.service.interfaces.UserService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link UserFacade}.
 *
 * <p>
 * All four cross-module use cases are fully wired as of Phase 3. This class is
 * a pure routing layer — no business logic, no {@code @Transactional}. Each
 * underlying service manages its own transaction boundaries.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class UserFacadeImpl implements UserFacade {
    private final UserService userService;
    private final RegistrationService registrationService;
    private final UserActivationService userActivationService;

    /**
     * {@inheritDoc}
     *
     * <p>
     * Delegates to {@link RegistrationService#register(RegisterCommand)}.
     * </p>
     */
    @Override
    public void register(RegisterCommand command) {
        registrationService.register(command);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Delegates to {@link UserActivationService#verifyEmail(String)}.
     * </p>
     */
    @Override
    public void activate(String token) {
        userActivationService.verifyEmail(token);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Delegates to {@link UserActivationService#resendVerificationEmail(String)}.
     * </p>
     */
    @Override
    public void resendActivation(String email) {
        userActivationService.resendVerificationEmail(email);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Delegates to {@link UserService#findAuthDetailsByEmail(String)}.
     * </p>
     */
    @Override
    public Optional<UserAuthDetails> findByEmail(String email) {
        return userService.findAuthDetailsByEmail(email);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UserAuthDetails> findByIds(List<Long> userIds) {
        return userService.findAuthDetailsByIds(userIds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<UserProfileDetails> findProfileById(Long userId) {
        return userService.findProfileDetailsById(userId);
    }
}