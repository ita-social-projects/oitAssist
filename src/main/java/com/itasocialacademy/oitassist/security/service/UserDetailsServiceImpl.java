package com.itasocialacademy.oitassist.security.service;

import com.itasocialacademy.oitassist.user.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.user.api.interfaces.UserFacade;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserFacade userFacade;

    public UserDetailsServiceImpl(UserFacade userFacade) {
        this.userFacade = userFacade;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetailsImpl userDetailsImpl = userFacade.getUserByEmail(username);
        if (userDetailsImpl == null) {
            throw new UsernameNotFoundException("User not Found!");
        }
        return userDetailsImpl;
    }
}
