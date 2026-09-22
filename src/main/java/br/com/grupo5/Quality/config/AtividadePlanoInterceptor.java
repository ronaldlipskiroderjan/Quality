package br.com.grupo5.Quality.config;

import br.com.grupo5.Quality.service.AtividadePlanoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.Principal;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class AtividadePlanoInterceptor implements HandlerInterceptor {

    private static final Pattern CAMINHO_PLANO = Pattern.compile(
            "^/v1/planos/([0-9a-fA-F-]{36})(?:/.*)?$"
    );
    private static final Set<String> METODOS_AUDITAVEIS = Set.of(
            "POST",
            "PUT",
            "PATCH",
            "DELETE"
    );

    private final AtividadePlanoService atividadePlanoService;

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception
    ) {
        Principal principal = request.getUserPrincipal();
        if (principal == null
                || exception != null
                || response.getStatus() >= 400
                || !METODOS_AUDITAVEIS.contains(request.getMethod())) {
            return;
        }

        Matcher matcher = CAMINHO_PLANO.matcher(request.getRequestURI());
        if (!matcher.matches()) {
            return;
        }

        try {
            atividadePlanoService.registrar(
                    UUID.fromString(matcher.group(1)),
                    principal.getName(),
                    request.getMethod(),
                    request.getRequestURI()
            );
        } catch (RuntimeException erro) {
            log.warn(
                    "Não foi possível registrar a atividade do plano {}.",
                    matcher.group(1),
                    erro
            );
        }
    }
}
