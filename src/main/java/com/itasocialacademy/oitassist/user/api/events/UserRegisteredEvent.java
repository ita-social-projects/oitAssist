package com.itasocialacademy.oitassist.user.api.events;

import org.springframework.modulith.NamedInterface;

/**
 * Domain event published by the {@code user} module after a new user account
 * has been persisted in {@code PENDING} status and an activation token has been
 * generated.
 *
 * <p>
 * This event crosses the module boundary: it is published inside the
 * {@code user} module and consumed by
 * {@code auth.listener.ActivationAccountListener}, which builds the activation
 * link and sends the verification email asynchronously after the publishing
 * transaction commits.
 * </p>
 *
 * <p>
 * The event carries only the data the listener needs — email, first name, and
 * the raw activation token. No {@code user}-internal types (entities,
 * repositories) are exposed.
 * </p>
 *
 * @param email     the email address of the newly registered user
 * @param firstName the user's first name, used to personalise the email
 * @param token     the raw activation token to embed in the verification link
 */
@NamedInterface("UserRegisteredEvent")
public record UserRegisteredEvent(
    String email,
    String firstName,
    String token) {
}