package com.rkamradt.groceryai.controller;

import com.rkamradt.groceryai.dto.UserInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    @GetMapping("/api/me")
    public ResponseEntity<UserInfo> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.ok(new UserInfo(false, null, null, null));
        }
        if (auth.getPrincipal() instanceof OidcUser user) {
            return ResponseEntity.ok(new UserInfo(true, user.getFullName(),
                    user.getEmail(), user.getPicture()));
        }
        return ResponseEntity.ok(new UserInfo(true, auth.getName(), null, null));
    }
}
