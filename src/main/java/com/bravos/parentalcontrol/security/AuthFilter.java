package com.bravos.parentalcontrol.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthFilter extends OncePerRequestFilter {
  private final PasswordEncoder passwordEncoder;

  public AuthFilter(PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
    if (request.getRequestURI().startsWith("/ws")) {
      filterChain.doFilter(request, response);
      return;
    }
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }
    String token = authHeader.trim();
    if (!isValidPassword(token)) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }
    Authentication authentication = new TestingAuthenticationToken("admin", "xxx", "ROLE_ADMIN");
    authentication.setAuthenticated(true);
    SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
    filterChain.doFilter(request, response);
  }

  private boolean isValidPassword(String token) {
    if (token == null || token.isEmpty()) {
      return false;
    }
    String hashedPassword = System.getenv("PARENTAL_CONTROL_PASSWORD_HASH");
    if (hashedPassword == null || hashedPassword.isEmpty()) {
      return false;
    }
    return passwordEncoder.matches(token, hashedPassword);
  }
}
