package com.racephotos.auth.session;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import com.racephotos.auth.user.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

public class SessionAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                SessionUser user = (SessionUser) session.getAttribute(SessionAttributes.USER);
                if (user != null) {
                    var roles = user.roles() == null || user.roles().isEmpty()
                        ? Set.of(Role.BASIC)
                        : user.roles();
                    var authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority(role.asAuthority()))
                        .collect(Collectors.toList());
                    var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
