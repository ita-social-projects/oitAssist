package com.itasocialacademy.oitassist.user.api.interfaces;

import com.itasocialacademy.oitassist.user.api.dto.RegisterCommand;
import com.itasocialacademy.oitassist.user.api.dto.UserAuthDetails;
import com.itasocialacademy.oitassist.user.api.dto.UserProfileDetails;
import org.springframework.modulith.NamedInterface;
import java.util.List;
import java.util.Optional;

/**
 * Cross-module entry point into the {@code user} module.
 *
 * <p>
 * This facade is the only sanctioned way for other Spring application modules
 * (e.g. {@code auth}, {@code security}) to invoke {@code user}-owned use cases.
 * It deliberately exposes a narrow, use-case-shaped surface — not generic CRUD
 * — so that internal {@code user} concerns (entities, repositories, mappers,
 * domain services) can evolve without breaking external callers.
 * </p>
 *
 * <p>
 * Every method on this interface has a real, current cross-module caller.
 * Methods that have no external consumer (profile self-view, internal lookups
 * by id, etc.) are intentionally absent: they live on {@code UserService} and
 * are reachable only from inside {@code user}.
 * </p>
 *
 * <h2>Boundary discipline</h2>
 * <ul>
 * <li>Inputs are {@code @NamedInterface} DTOs from {@code user.api.dto}. Other
 * modules construct these directly from their own request DTOs.</li>
 * <li>Outputs are either {@code void} or {@code @NamedInterface} DTOs from
 * {@code user.api.dto}. Internal types ({@code User} entity,
 * {@code UserActivationToken}, etc.) never leak across this boundary.</li>
 * <li>Exceptions thrown are caught generically by
 * {@code core::GlobalExceptionHandler} and mapped to HTTP responses; callers
 * should not catch specific exception types.</li>
 * </ul>
 */
@NamedInterface("UserFacade")
public interface UserFacade {
    /**
     * Registers a new user account in {@code PENDING} status, generates an
     * activation token, and triggers an activation email (asynchronously, after the
     * registration transaction commits).
     *
     * <p>
     * Called by {@code auth.RegistrationController} when a user submits the
     * registration form.
     * </p>
     *
     * @param command the registration data
     * @throws com.itasocialacademy.oitassist.user.exceptions.UserAlreadyExistsException if
     *                                                                                   an
     *                                                                                   active
     *                                                                                   account
     *                                                                                   with
     *                                                                                   the
     *                                                                                   given
     *                                                                                   email
     *                                                                                   already
     *                                                                                   exists
     */
    void register(RegisterCommand command);

    /**
     * Activates a user account by consuming an activation token. Transitions the
     * user from {@code PENDING} to {@code ACTIVE} and removes the token so it
     * cannot be reused.
     *
     * <p>
     * Called by {@code auth.UserActivationController} when a user clicks the
     * verification link in their activation email.
     * </p>
     *
     * @param token the activation token from the email link
     */
    void activate(String token);

    /**
     * Resends an activation email to a user whose account is still {@code PENDING}.
     * If the existing token has expired, a fresh one is generated; otherwise the
     * resend cooldown is enforced.
     *
     * <p>
     * Called by {@code auth.UserActivationController} when a user requests a new
     * activation email.
     * </p>
     *
     * @param email the email address of the pending user
     */
    void resendActivation(String email);

    /**
     * Looks up a user by email and returns the authentication-side projection
     * needed by {@code security}.
     *
     * <p>
     * Called by {@code security.UserDetailsServiceImpl} during the Spring Security
     * authentication flow. The returned DTO carries the encoded password —
     * {@code security} maps it into its own {@code UserDetails} implementation and
     * never propagates the raw fields further.
     * </p>
     *
     * @param email the user's email
     * @return the auth-side projection, or empty if no user exists with the given
     *         email
     */
    Optional<UserAuthDetails> findByEmail(String email);

    /**
     * Searches for the list of users by their IDs and returns the list of auth-side
     * projections required by {@code UserFacade.findByIds}. Returns empty if no
     * users were found.
     *
     * @param userIds the users' IDs
     * @return the users' auth details DTOs
     */
    List<UserAuthDetails> findByIds(List<Long> userIds);

    /**
     * Looks up a user by their ID and returns the display-side projection needed by
     * {@code participation}. Called by {@code participation.ApplicationServiceImpl}
     * during the email-sending process.
     *
     * @param userId the user's ID
     * @return the display-side projection, or empty if no user exists with the
     *         given ID
     */
    Optional<UserProfileDetails> findProfileById(Long userId);

    /**
     * Bulk variant of {@link #findProfileById} — returns the display-side
     * projections for all matching users. IDs with no matching user are simply
     * omitted from the result, consistent with {@link #findByIds}.
     */
    List<UserProfileDetails> findProfilesByIds(List<Long> userIds);

    /**
     * Filters a provided list of candidate user IDs based on a text search against
     * the user's first name, surname or email, required by {@code participation}
     * module.
     * <p>
     * This method is designed to perform a constrained search. Instead of searching
     * the entire database, it only searches within the provided
     * {@code candidateIds}.
     * </p>
     *
     * @param search       the text search query (e.g., "Ivan", "ivan@email.com").
     * @param candidateIds the pool of user IDs to restrict the search to.
     * @return an {@code Optional} containing the filtered list of matching user
     *         IDs, or {@code Optional.empty()} if the search string is null/blank.
     */
    Optional<List<Long>> findUserIdsBySearchWithinIds(String search, List<Long> candidateIds);
}