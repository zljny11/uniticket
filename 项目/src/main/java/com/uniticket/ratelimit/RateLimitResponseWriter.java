package com.uniticket.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniticket.dto.Result;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class RateLimitResponseWriter {

    @Resource
    private ObjectMapper objectMapper;

    public void writeRejected(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(message)));
    }
}
