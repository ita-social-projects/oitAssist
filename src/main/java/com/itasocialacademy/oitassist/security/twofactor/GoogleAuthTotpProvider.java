package com.itasocialacademy.oitassist.security.twofactor;

import com.itasocialacademy.oitassist.security.properties.TwoFactorProperties;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * {@link TotpProvider} backed by {@code com.warrenstrange:googleauth}.
 *
 * <p>
 * Method usage here was verified directly against the library's real source
 * (github.com/wstrange/GoogleAuth), not assumed from memory. One thing worth
 * calling out from that check: {@link GoogleAuthenticatorQRGenerator} exposes
 * two URL-building methods — {@code getOtpAuthURL(...)}, which returns a URL to
 * a *third-party* QR-image-rendering service ({@code api.qrserver.com}) with
 * the secret embedded in the query string, and {@code getOtpAuthTotpURL(...)},
 * which returns the raw {@code otpauth://} URI with no external call at all.
 * Only the latter is used below — routing a user's TOTP secret through an
 * external HTTP call just to render a QR image would be a real secret-leak, not
 * an acceptable trade-off.
 * </p>
 *
 * <p>
 * The library's own {@code authorize(secret, code)} convenience method applies
 * its configured tolerance window internally but only returns a {@code boolean}
 * — it never reveals *which* time-bucket matched. Since replay protection (plan
 * section 2.4b) needs that bucket to record and reject reuse, this adapter sets
 * {@code windowSize(1)} (disabling the library's own internal tolerance,
 * confirmed from its source: window size 1 means "check only the current
 * bucket") and instead iterates the tolerance window itself via
 * {@code getTotpPassword(secret, explicitTimeMillis)}, which computes the code
 * for an arbitrary point in time. This keeps bucket-level granularity available
 * to the caller.
 * </p>
 */
@Component
public class GoogleAuthTotpProvider implements TotpProvider {
    private static final long TIME_STEP_MILLIS = TimeUnit.SECONDS.toMillis(30);

    private final GoogleAuthenticator googleAuthenticator;
    private final TwoFactorProperties properties;

    public GoogleAuthTotpProvider(TwoFactorProperties properties) {
        this.properties = properties;
        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
            .setWindowSize(1)
            .build();
        this.googleAuthenticator = new GoogleAuthenticator(config);
    }

    @Override
    public String generateSecret() {
        GoogleAuthenticatorKey credentials = googleAuthenticator.createCredentials();
        return credentials.getKey();
    }

    @Override
    public String buildProvisioningUri(String secret, String accountEmail) {
        GoogleAuthenticatorKey credentials = new GoogleAuthenticatorKey.Builder(secret).build();
        return GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(properties.getIssuer(), accountEmail, credentials);
    }

    @Override
    public Optional<Long> verify(String secret, String code) {
        int candidateCode;
        try {
            candidateCode = Integer.parseInt(code);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }

        long currentBucket = System.currentTimeMillis() / TIME_STEP_MILLIS;
        int tolerance = properties.getTotpToleranceSteps();

        for (long bucket = currentBucket - tolerance; bucket <= currentBucket + tolerance; bucket++) {
            int expectedCode = googleAuthenticator.getTotpPassword(secret, bucket * TIME_STEP_MILLIS);
            if (expectedCode == candidateCode) {
                return Optional.of(bucket);
            }
        }
        return Optional.empty();
    }
}