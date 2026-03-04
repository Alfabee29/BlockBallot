package com.securevote.security;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Security headers filter — injects hardened HTTP headers on every response.
 *
 * Headers set:
 * • Content-Security-Policy — restricts script/style/font origins
 * • X-Content-Type-Options — prevents MIME-sniffing
 * • X-Frame-Options — prevents click-jacking
 * • X-XSS-Protection — legacy XSS filter (for older browsers)
 * • Referrer-Policy — limits referrer leakage
 * • Permissions-Policy — disables dangerous browser features
 * • Strict-Transport-Security — forces HTTPS (when behind TLS)
 * • Cache-Control — prevents caching of sensitive pages
 */
@Component
@Order(1)
public class SecurityHeadersFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        /* no-op */ }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        HttpServletResponse httpResp = (HttpServletResponse) response;
        HttpServletRequest httpReq = (HttpServletRequest) request;

        // ── Content Security Policy ────────────────────────
        httpResp.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com; " +
                        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                        "font-src 'self' https://fonts.gstatic.com; " +
                        "img-src 'self' data:; " +
                        "connect-src 'self'; " +
                        "frame-ancestors 'none'; " +
                        "form-action 'self'; " +
                        "base-uri 'self';");

        // ── Anti-click-jacking ─────────────────────────────
        httpResp.setHeader("X-Frame-Options", "DENY");

        // ── Prevent MIME sniffing ──────────────────────────
        httpResp.setHeader("X-Content-Type-Options", "nosniff");

        // ── Legacy XSS filter ──────────────────────────────
        httpResp.setHeader("X-XSS-Protection", "1; mode=block");

        // ── Referrer policy ────────────────────────────────
        httpResp.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // ── Permissions policy ─────────────────────────────
        httpResp.setHeader("Permissions-Policy",
                "camera=(), microphone=(), geolocation=(), payment=()");

        // ── HSTS (effective when behind TLS termination) ───
        httpResp.setHeader("Strict-Transport-Security",
                "max-age=31536000; includeSubDomains");

        // ── Prevent caching of HTML pages (not static assets) ──
        String path = httpReq.getRequestURI();
        if (!path.startsWith("/css/") && !path.startsWith("/js/") && !path.startsWith("/images/")) {
            httpResp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            httpResp.setHeader("Pragma", "no-cache");
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        /* no-op */ }
}
