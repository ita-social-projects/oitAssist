package com.itasocialacademy.oitassist.security.api.facade;

import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityService;
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
}
