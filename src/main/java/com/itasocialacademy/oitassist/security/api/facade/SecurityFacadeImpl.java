package com.itasocialacademy.oitassist.security.api.facade;

import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.security.service.interfaces.SecurityService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecurityFacadeImpl implements SecurityFacade {
    private final SecurityService securityService;

    @Override
    public Optional<String> getCurrentUserEmail() {
        return securityService.getCurrentUserEmail();
    }

    @Override
    public Optional<Long> getCurrentUserId() {
        return securityService.getCurrentUserId();
    }

    @Override
    public boolean isOwner(Long ownerId) {
        return securityService.isOwner(ownerId);
    }

    @Override
    public boolean hasRole(String role) {
        return securityService.hasRole(role);
    }
}
