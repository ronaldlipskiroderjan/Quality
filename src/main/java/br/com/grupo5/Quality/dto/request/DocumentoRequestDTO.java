package br.com.grupo5.Quality.dto.request;

import br.com.grupo5.Quality.database.enums.Classificacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record DocumentoRequestDTO(
        @NotBlank @Size(max = 150) String nome,
        @NotBlank @Size(max = 30) String versao,
        @NotNull MultipartFile arquivo,
        @NotNull Classificacao classificacao
) {
}
