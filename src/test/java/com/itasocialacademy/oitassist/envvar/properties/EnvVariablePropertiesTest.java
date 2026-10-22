package com.itasocialacademy.oitassist.envvar.properties;

import static com.itasocialacademy.oitassist.envvar.dao.enums.AccessMode.ALL;
import static com.itasocialacademy.oitassist.envvar.dao.enums.AccessMode.BLACKLIST;
import static com.itasocialacademy.oitassist.envvar.dao.enums.AccessMode.WHITELIST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EnvVariablePropertiesTest {

    private static final String RESTRICTED_KEY = "JWT_SIGN_KEY";
    private static final String PUBLIC_KEY = "APP_NAME";

    @Test
    void constructor_ShouldUseEmptySets_WhenListsAreNull() {
        EnvVariableProperties properties = new EnvVariableProperties(BLACKLIST, null, null);

        assertThat(properties.whitelist()).isEmpty();
        assertThat(properties.blacklist()).isEmpty();
    }

    @Test
    void constructor_ShouldKeepKeys_WhenListsAreConfigured() {
        EnvVariableProperties properties =
            new EnvVariableProperties(WHITELIST, Set.of(PUBLIC_KEY), Set.of(RESTRICTED_KEY));

        assertThat(properties.accessMode()).isEqualTo(WHITELIST);
        assertThat(properties.whitelist()).containsExactly(PUBLIC_KEY);
        assertThat(properties.blacklist()).containsExactly(RESTRICTED_KEY);
    }

    @Test
    void constructor_ShouldCopyList_WhenTheSourceIsMutatedAfterwards() {
        Set<String> whitelist = new HashSet<>(Set.of(PUBLIC_KEY));

        EnvVariableProperties properties = new EnvVariableProperties(WHITELIST, whitelist, null);
        whitelist.add(RESTRICTED_KEY);

        assertThat(properties.whitelist()).containsExactly(PUBLIC_KEY);
    }

    @Test
    void whitelist_ShouldBeUnmodifiable_WhenItIsConfigured() {
        Set<String> whitelist = new EnvVariableProperties(WHITELIST, Set.of(PUBLIC_KEY), null).whitelist();

        assertThatThrownBy(() -> whitelist.add(RESTRICTED_KEY))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void blacklist_ShouldBeUnmodifiable_WhenItIsConfigured() {
        Set<String> blacklist = new EnvVariableProperties(BLACKLIST, null, Set.of(RESTRICTED_KEY)).blacklist();

        assertThatThrownBy(() -> blacklist.add(PUBLIC_KEY))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void constructor_ShouldThrow_WhenAccessModeIsAllAndWhitelistIsConfigured() {
        Set<String> whitelist = Set.of(PUBLIC_KEY);

        assertThatThrownBy(() -> new EnvVariableProperties(ALL, whitelist, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("accessMode=ALL");
    }

    @Test
    void constructor_ShouldThrow_WhenAccessModeIsAllAndBlacklistIsConfigured() {
        Set<String> blacklist = Set.of(RESTRICTED_KEY);

        assertThatThrownBy(() -> new EnvVariableProperties(ALL, null, blacklist))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("accessMode=ALL");
    }

    @Test
    void constructor_ShouldNotThrow_WhenAccessModeIsAllAndNoListIsConfigured() {
        assertThatCode(() -> new EnvVariableProperties(ALL, null, null))
            .doesNotThrowAnyException();
    }
}
