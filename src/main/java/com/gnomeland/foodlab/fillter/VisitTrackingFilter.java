package com.gnomeland.foodlab.fillter;

import com.gnomeland.foodlab.service.VisitService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class VisitTrackingFilter implements Filter {

    private final VisitService visitService;

    public VisitTrackingFilter(VisitService visitService) {
        this.visitService = visitService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI();
        visitService.recordVisit(uri);
        chain.doFilter(request, response);
    }

}