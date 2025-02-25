package net.berndreiss.zentodo.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.awt.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class RequestTimingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Store request received time
        response.setHeader("t2", TimeDrift.getTimeStamp());
        response.setHeader("t1", request.getHeader("t1"));
        System.out.println("PRE");
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // Retrieve request received time
        String requestReceivedAt = (String) request.getAttribute("requestReceivedAt");
        String responseSentAt = Instant.now().toString();

        System.out.println("POST");
        // Add timestamps to response headers
        response.setHeader("X-Request-Received-At", requestReceivedAt);
        response.setHeader("X-Response-Sent-At", responseSentAt);
        response.setHeader("Test", "AAAAAAAAAAAAA");
    }

}
