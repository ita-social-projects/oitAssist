package com.itasocialacademy.oitassist.security.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized 2FA policy configuration.
 *
 * <p>
 * Lives in {@code security/properties/}, matching the convention already used
 * across the codebase for {@code @ConfigurationProperties} classes —
 * {@code core/properties/SwaggerProperties},
 * {@code core/properties/WebClientProperties},
 * {@code filemanager/properties/GraphProperties} — rather than co-located with
 * its owning feature cluster. {@code security/jwt/JwtProperties} is the one
 * outlier to this pattern in the codebase, not a second precedent for it; two
 * modules doing it the {@code properties/}-folder way outweighs one doing it
 * differently.
 * </p>
 *
 * <p>
 * Expected {@code application.yaml} block (add under the app's config root):
 * </p>
 *
 * <pre>{@code
 * two-factor:
 *   issuer: OITAssist
 *   recovery-code-count: 10
 *   recovery-code-length: 8
 *   totp-tolerance-steps: 1
 *   pending-token-validity-millis: 300000       # 5 minutes
 *   email-otp-validity-millis: 600000           # 10 minutes
 * }</pre>
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "two-factor")
public class TwoFactorProperties {
    private String issuer;
    private int recoveryCodeCount;
    private int recoveryCodeLength;
    private int totpToleranceSteps;
    private long pendingTokenValidityMillis;
    private long emailOtpValidityMillis;
}