package com.itasocialacademy.oitassist.envvar.service;

import static com.itasocialacademy.oitassist.envvar.dao.enums.AccessMode.ALL;
import static com.itasocialacademy.oitassist.envvar.dao.enums.AccessMode.BLACKLIST;
import static com.itasocialacademy.oitassist.envvar.dao.enums.AccessMode.WHITELIST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.envvar.dao.enums.AccessMode;
import com.itasocialacademy.oitassist.envvar.properties.EnvVariableProperties;
import com.itasocialacademy.oitassist.envvar.provider.interfaces.EnvVariableProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnvVariableServiceImplTest {

    private static final String PUBLIC_KEY = "APP_NAME";
    private static final String PUBLIC_VALUE = "oit-assist";
    private static final String RESTRICTED_KEY = "JWT_SIGN_KEY";
    private static final String RESTRICTED_VALUE = "sign-key-value";
    private static final String ABSENT_KEY = "NOT_SET_IN_ENVIRONMENT";

    @Mock
    private EnvVariableProvider envVariableProvider;

    @Test
    void getenv_ShouldReturnEveryVariable_WhenAccessModeIsAll() {
        when(envVariableProvider.getenv()).thenReturn(environment());

        Map<String, String> result = createService(ALL, null, null).getenv();

        assertThat(result).containsOnlyKeys(PUBLIC_KEY, RESTRICTED_KEY);
        assertThat(result).containsEntry(PUBLIC_KEY, PUBLIC_VALUE);
        assertThat(result).containsEntry(RESTRICTED_KEY, RESTRICTED_VALUE);
    }

    @Test
    void getenv_ShouldReturnOnlyListedKeys_WhenAccessModeIsWhitelist() {
        when(envVariableProvider.getenv()).thenReturn(environment());

        Map<String, String> result = createService(WHITELIST, Set.of(PUBLIC_KEY), null).getenv();

        assertThat(result).containsOnlyKeys(PUBLIC_KEY);
        assertThat(result).containsEntry(PUBLIC_KEY, PUBLIC_VALUE);
    }

    @Test
    void getenv_ShouldIgnoreListedKey_WhenItIsAbsentFromTheEnvironment() {
        when(envVariableProvider.getenv()).thenReturn(environment());

        Map<String, String> result =
            createService(WHITELIST, Set.of(PUBLIC_KEY, ABSENT_KEY), null).getenv();

        assertThat(result).containsOnlyKeys(PUBLIC_KEY);
        assertThat(result).doesNotContainKey(ABSENT_KEY);
    }

    @Test
    void getenv_ShouldReturnNothing_WhenAccessModeIsWhitelistAndNoKeyIsListed() {
        when(envVariableProvider.getenv()).thenReturn(environment());

        Map<String, String> result = createService(WHITELIST, null, null).getenv();

        assertThat(result).isEmpty();
    }

    @Test
    void getenv_ShouldExcludeListedKeys_WhenAccessModeIsBlacklist() {
        when(envVariableProvider.getenv()).thenReturn(environment());

        Map<String, String> result = createService(BLACKLIST, null, Set.of(RESTRICTED_KEY)).getenv();

        assertThat(result).containsOnlyKeys(PUBLIC_KEY);
        assertThat(result).doesNotContainKey(RESTRICTED_KEY);
    }

    @Test
    void getenv_ShouldReturnEveryVariable_WhenAccessModeIsBlacklistAndNoKeyIsListed() {
        when(envVariableProvider.getenv()).thenReturn(environment());

        Map<String, String> result = createService(BLACKLIST, null, null).getenv();

        assertThat(result).containsOnlyKeys(PUBLIC_KEY, RESTRICTED_KEY);
    }

    @Test
    void getenv_ShouldKeepTheKey_WhenItsValueIsNull() {
        Map<String, String> environment = new HashMap<>();
        environment.put(PUBLIC_KEY, null);
        when(envVariableProvider.getenv()).thenReturn(environment);

        Map<String, String> result = createService(BLACKLIST, null, null).getenv();

        assertThat(result).containsKey(PUBLIC_KEY);
        assertThat(result.get(PUBLIC_KEY)).isNull();
    }

    @Test
    void getenv_ShouldReturnUnmodifiableMap_WhenAccessModeIsAll() {
        when(envVariableProvider.getenv()).thenReturn(environment());

        Map<String, String> result = createService(ALL, null, null).getenv();

        assertThatThrownBy(() -> result.put(ABSENT_KEY, PUBLIC_VALUE))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getenv_ShouldReturnUnmodifiableMap_WhenFilteringIsApplied() {
        when(envVariableProvider.getenv()).thenReturn(environment());

        Map<String, String> result = createService(BLACKLIST, null, Set.of(RESTRICTED_KEY)).getenv();

        assertThatThrownBy(() -> result.put(RESTRICTED_KEY, RESTRICTED_VALUE))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    private EnvVariableServiceImpl createService(AccessMode accessMode, Set<String> whitelist,
        Set<String> blacklist) {
        return new EnvVariableServiceImpl(
            envVariableProvider,
            new EnvVariableProperties(accessMode, whitelist, blacklist));
    }

    private Map<String, String> environment() {
        Map<String, String> environment = new HashMap<>();
        environment.put(PUBLIC_KEY, PUBLIC_VALUE);
        environment.put(RESTRICTED_KEY, RESTRICTED_VALUE);
        return environment;
    }
}
