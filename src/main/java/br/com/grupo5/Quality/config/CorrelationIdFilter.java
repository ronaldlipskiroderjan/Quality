package br.com.grupo5.Quality.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-ID";
    public static final String ATRIBUTO = CorrelationIdFilter.class.getName() + ".id";

    private static final String CHAVE_MDC = "correlationId";
    private static final Pattern VALOR_VALIDO = Pattern.compile("[A-Za-z0-9._-]{1,100}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = resolver(request.getHeader(HEADER));
        request.setAttribute(ATRIBUTO, correlationId);
        response.setHeader(HEADER, correlationId);
        MDC.put(CHAVE_MDC, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CHAVE_MDC);
        }
    }

    public static String obter(HttpServletRequest request) {
        Object correlationId = request.getAttribute(ATRIBUTO);
        if (correlationId instanceof String valor && StringUtils.hasText(valor)) {
            return valor;
        }

        String novoId = resolver(request.getHeader(HEADER));
        request.setAttribute(ATRIBUTO, novoId);
        return novoId;
    }

    private static String resolver(String valorRecebido) {
        if (StringUtils.hasText(valorRecebido)
                && VALOR_VALIDO.matcher(valorRecebido).matches()) {
            return valorRecebido;
        }
        return UUID.randomUUID().toString();
    }
}
