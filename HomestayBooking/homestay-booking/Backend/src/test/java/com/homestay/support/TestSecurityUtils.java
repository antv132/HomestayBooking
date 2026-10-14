package com.homestay.support;

import com.homestay.entity.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

public final class TestSecurityUtils {

    private TestSecurityUtils() {}

    public static RequestPostProcessor userPrincipal(User user) {
        String roleName = "ROLE_" + user.getRole().name();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user,
                "N/A",
                List.of(new SimpleGrantedAuthority(roleName)));
        return SecurityMockMvcRequestPostProcessors.authentication(auth);
    }
}
