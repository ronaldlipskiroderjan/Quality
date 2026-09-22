package br.com.grupo5.Quality.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SecurityProblemHandler {

    private final ObjectMapper objectMapper;

    public void responderNaoAutenticado(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        escrever(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                "Não autenticado",
                "NAO_AUTENTICADO",
                "É necessário autenticar-se para acessar este recurso."
        );
    }

    public void responderAcessoNegado(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        escrever(
                request,
                response,
                HttpStatus.FORBIDDEN,
                "Acesso negado",
                "ACESSO_NEGADO",
                "Você não possui permissão para esta operação."
        );
    }

    private void escrever(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String titulo,
            String codigo,
            String detalhe
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiProblem.criar(status, titulo, codigo, detalhe, request)
        );
    }
}
