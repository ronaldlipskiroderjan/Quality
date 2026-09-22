package br.com.grupo5.Quality.handler;

import br.com.grupo5.Quality.exception.AlreadyExistsException;
import br.com.grupo5.Quality.exception.InvalidRequestException;
import br.com.grupo5.Quality.exception.NotFoundException;
import br.com.grupo5.Quality.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            NotFoundException ex,
            HttpServletRequest request
    ) {
        return criar(
                HttpStatus.NOT_FOUND,
                "Recurso não encontrado",
                "RECURSO_NAO_ENCONTRADO",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleEndpointNotFound(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        return criar(
                HttpStatus.NOT_FOUND,
                "Endpoint não encontrado",
                "ENDPOINT_NAO_ENCONTRADO",
                "A operação solicitada não está disponível nesta versão da API.",
                request
        );
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleConflict(
            AlreadyExistsException ex,
            HttpServletRequest request
    ) {
        return criar(
                HttpStatus.CONFLICT,
                "Recurso já existente",
                "RECURSO_JA_EXISTENTE",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ProblemDetail> handleInvalidRequest(
            InvalidRequestException ex,
            HttpServletRequest request
    ) {
        return criar(
                HttpStatus.BAD_REQUEST,
                "Requisição inválida",
                "REQUISICAO_INVALIDA",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ProblemDetail> handleUnauthorized(
            UnauthorizedException ex,
            HttpServletRequest request
    ) {
        return criar(
                HttpStatus.UNAUTHORIZED,
                "Não autenticado",
                "NAO_AUTENTICADO",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        return criar(
                HttpStatus.FORBIDDEN,
                "Acesso negado",
                "ACESSO_NEGADO",
                "Você não possui permissão para esta operação.",
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }

        ResponseEntity<ProblemDetail> response = criar(
                HttpStatus.BAD_REQUEST,
                "Dados inválidos",
                "DADOS_INVALIDOS",
                "Revise os campos informados.",
                request
        );
        response.getBody().setProperty("campos", campos);
        return response;
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestPartException.class
    })
    public ResponseEntity<ProblemDetail> handleMalformedRequest(
            Exception ex,
            HttpServletRequest request
    ) {
        return criar(
                HttpStatus.BAD_REQUEST,
                "Requisição inválida",
                "REQUISICAO_INVALIDA",
                "A requisição possui parâmetros ou conteúdo inválidos.",
                request
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleLargeFile(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request
    ) {
        return criar(
                HttpStatus.CONTENT_TOO_LARGE,
                "Arquivo muito grande",
                "ARQUIVO_MUITO_GRANDE",
                "O arquivo excede o limite permitido.",
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataConflict(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        log.warn(
                "Conflito de integridade em {} {}: {}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMostSpecificCause().getMessage()
        );
        return criar(
                HttpStatus.CONFLICT,
                "Conflito de dados",
                "CONFLITO_DADOS",
                "A operação viola uma regra de integridade.",
                request
        );
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleConcurrentUpdate(
            ObjectOptimisticLockingFailureException ex,
            HttpServletRequest request
    ) {
        return criar(
                HttpStatus.CONFLICT,
                "Conflito de atualização",
                "CONFLITO_ATUALIZACAO",
                "O recurso foi alterado por outra operação. Atualize os dados e tente novamente.",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {
        return criar(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno",
                "ERRO_INTERNO",
                "Não foi possível concluir a operação.",
                request
        );
    }

    private ResponseEntity<ProblemDetail> criar(
            HttpStatus status,
            String titulo,
            String codigo,
            String detalhe,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status)
                .body(ApiProblem.criar(status, titulo, codigo, detalhe, request));
    }
}
