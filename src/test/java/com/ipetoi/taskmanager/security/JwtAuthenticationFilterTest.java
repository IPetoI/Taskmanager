package com.ipetoi.taskmanager.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ipetoi.taskmanager.model.User;
import com.ipetoi.taskmanager.model.UserRole;
import com.ipetoi.taskmanager.service.UserService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    JwtTokenProvider tokenProvider;
    @Mock
    UserService userService;
    @Mock
    HttpServletRequest request;
    @Mock
    HttpServletResponse response;
    @Mock
    FilterChain filterChain;
    @InjectMocks
    JwtAuthenticationFilter filter;

    @BeforeEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerTokenShouldSetAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.token.here");
        when(tokenProvider.validateToken("valid.token.here")).thenReturn(true);
        when(tokenProvider.getUsername("valid.token.here")).thenReturn("peto");

        User mockUser = new User();
        mockUser.setUsername("peto");
        mockUser.setRole(UserRole.USER);
        when(userService.findByUsername("peto")).thenReturn(mockUser);

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("peto", auth.getName());

        // The filter chain continues without stopping.
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void missingAuthorizationHeaderShouldNotSetAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(tokenProvider);
    }

    @Test
    void emptyAuthorizationHeaderShouldNotSetAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void headerWithoutBearerPrefixShouldNotSetAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(tokenProvider);
    }

    @Test
    void invalidTokenShouldNotSetAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid.token");
        when(tokenProvider.validateToken("invalid.token")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        // getUsername is never called when the token is invalid.
        verify(tokenProvider, never()).getUsername(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void filterShouldAlwaysContinueChainEvenWithInvalidToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer bad");
        when(tokenProvider.validateToken("bad")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        // The filter never stops the chain, even on errors - Spring Security handles this
        // through the filter chain when no Authentication is present in the SecurityContext.
        verify(filterChain).doFilter(request, response);
    }
}