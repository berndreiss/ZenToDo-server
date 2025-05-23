package net.berndreiss.zentodo.util;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.time.Instant;
/**
 * TODO
 */
@Component
public class RequestTimingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws InterruptedException {

        // Store request received time
        response.setHeader("t2", TimeDrift.getTimeStamp());
        response.setHeader("t1", request.getHeader("t1"));
        return true;
    }

    // The timestamp for sending the reply is implemented in the TimeResponseBodyAdvice, since responses are already
    // commited in postHandle and headers therefore can not be modified.
}
