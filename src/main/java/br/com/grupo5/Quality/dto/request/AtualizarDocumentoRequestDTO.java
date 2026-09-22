package br.com.grupo5.Quality.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record AtualizarDocumentoRequestDTO(
        @NotBlank @Size(max = 150) String nome,
        @NotBlank @Size(max = 30) String versao,
        MultipartFile arquivo
) {
}
