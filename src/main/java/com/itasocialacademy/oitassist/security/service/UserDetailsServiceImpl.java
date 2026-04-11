package com.itasocialacademy.oitassist.security.service;

import com.itasocialacademy.oitassist.security.api.interfaces.SecurityUserProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final SecurityUserProvider securityUserProvider;

    public UserDetailsServiceImpl(SecurityUserProvider securityUserProvider) {
        this.securityUserProvider = securityUserProvider;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return securityUserProvider.findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
    }
}
