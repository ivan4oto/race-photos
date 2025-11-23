package com.racephotos.auth.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.racephotos.auth.api.dto.AuthenticatedUserResponse;
import com.racephotos.auth.api.dto.CreateSessionRequest;
import com.racephotos.auth.service.CognitoTokenVerifier;
import com.racephotos.auth.session.SessionAttributes;
import com.racephotos.auth.session.SessionUser;
import com.racephotos.auth.user.UserService;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final CognitoTokenVerifier tokenVerifier;
    private final UserService userService;

    public AuthController(CognitoTokenVerifier tokenVerifier, UserService userService) {
        this.tokenVerifier = tokenVerifier;
        this.userService = userService;
    }

    @PostMapping("/session")
    public ResponseEntity<AuthenticatedUserResponse> createSession(
        @Valid @RequestBody CreateSessionRequest request,
        HttpServletRequest servletRequest
    ) {
        var cognitoUser = tokenVerifier.verifyIdToken(request.idToken());
        var persistedUser = userService.syncFromCognito(cognitoUser);
        SessionUser sessionUser = SessionUser.from(persistedUser);
        renewSession(servletRequest);
        HttpSession session = servletRequest.getSession(true);
        session.setAttribute(SessionAttributes.USER, sessionUser);
        log.info("Created backend session for Cognito user {}", sessionUser.email());
        return ResponseEntity.ok(AuthenticatedUserResponse.from(sessionUser));
    }

    @GetMapping("/session")
    public ResponseEntity<AuthenticatedUserResponse> currentSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SessionUser user = (SessionUser) session.getAttribute(SessionAttributes.USER);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(AuthenticatedUserResponse.from(user));
    }

    @DeleteMapping("/session")
    public ResponseEntity<Void> deleteSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
            log.info("Cleared backend session");
        }
        return ResponseEntity.noContent().build();
    }

    private void renewSession(HttpServletRequest request) {
        HttpSession existing = request.getSession(false);
        if (existing != null) {
            existing.invalidate();
        }
    }
}
