package br.com.grupo5.Quality.dto.request;

import br.com.grupo5.Quality.database.enums.PapelPlano;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record NovoParticipanteRequestDTO(
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um e-mail válido.")
        String email,

        @NotNull(message = "O papel é obrigatório.")
        PapelPlano papel
) {
    public NovoParticipanteRequestDTO(String email, Set<PapelPlano> papeis) {
        this(email, papelUnico(papeis));
    }

    private static PapelPlano papelUnico(Set<PapelPlano> papeis) {
        return papeis != null && papeis.size() == 1
                ? papeis.iterator().next()
                : null;
    }
}
