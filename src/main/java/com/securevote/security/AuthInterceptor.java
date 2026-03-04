package com.securevote.security;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String uri = request.getRequestURI();

        // Allow static resources
        if (uri.startsWith("/css") || uri.startsWith("/js") || uri.startsWith("/images")
                || uri.startsWith("/h2-console")) {
            return true;
        }

        // Allow public pages
        if (uri.equals("/login") || uri.equals("/do-login") || uri.equals("/register") || uri.equals("/do-register")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("VOTER_ID") == null) {
            response.sendRedirect("/login");
            return false;
        }

        String role = (String) session.getAttribute("USER_ROLE");
        if (uri.startsWith("/admin") && !"ROLE_ADMIN".equals(role)) {
            response.sendError(403, "Access Denied");
            return false;
        }

        return true;
    }
}
