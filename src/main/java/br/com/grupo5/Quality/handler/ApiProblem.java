package br.com.grupo5.Quality.handler;

import br.com.grupo5.Quality.config.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Locale;

public final class ApiProblem {

    private static final String TIPO_BASE = "urn:quality:problema:";

    private ApiProblem() {
    }

    public static ProblemDetail criar(
            HttpStatus status,
            String titulo,
            String codigo,
            String detalhe,
            HttpServletRequest request
    ) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        problema.setType(URI.create(TIPO_BASE + normalizar(codigo)));
        problema.setInstance(URI.create(request.getRequestURI()));
        problema.setProperty("codigo", codigo);
        problema.setProperty("correlationId", CorrelationIdFilter.obter(request));
        problema.setProperty("timestamp", OffsetDateTime.now());
        return problema;
    }

    private static String normalizar(String codigo) {
        return codigo.toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
