package com.itasocialacademy.oitassist.auth.service.interfaces;

import com.itasocialacademy.oitassist.auth.dto.request.RegisterRequest;

/**
 * Service responsible for handling user registration workflow. Defines
 * operations required to create a new user account, including validation,
 * persistence and any additional registration-related business logic.
 */
public interface RegistrationService {
    /**
     * Creates a new user in the system based on the provided request data.
     * Implementations may include:
     * <ul>
     * <li>validation of input data</li>
     * <li>password encoding</li>
     * <li>setting default role and status</li>
     * <li>saving the user to the database</li>
     * <li>initializing account activation and sending a verification email</li>
     * </ul>
     *
     * @param request the registration request containing user personal information
     *                and credentials
     * @throws RuntimeException if user creation fails
     */
    void createUser(RegisterRequest request);
}
