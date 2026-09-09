package br.com.grupo5.Quality.database.enums;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static br.com.grupo5.Quality.database.enums.PermissaoPlano.CONCLUIR;
import static br.com.grupo5.Quality.database.enums.PermissaoPlano.EDITAR;
import static br.com.grupo5.Quality.database.enums.PermissaoPlano.EXCLUIR;
import static br.com.grupo5.Quality.database.enums.PermissaoPlano.GERENCIAR_DOCUMENTOS;
import static br.com.grupo5.Quality.database.enums.PermissaoPlano.GERENCIAR_PARTICIPANTES;
import static br.com.grupo5.Quality.database.enums.PermissaoPlano.VISUALIZAR;

public enum PapelPlano {
    PROPRIETARIO(EnumSet.allOf(PermissaoPlano.class)),
    RESPONSAVEL_QUALIDADE(EnumSet.of(VISUALIZAR, EDITAR, GERENCIAR_DOCUMENTOS)),
    AUDITOR(EnumSet.of(VISUALIZAR)),
    PARTICIPANTE(EnumSet.of(VISUALIZAR)),
    RESPONSAVEL_RESOLUCAO(EnumSet.of(VISUALIZAR)),
    RESPONSAVEL_N1(EnumSet.of(VISUALIZAR)),
    RESPONSAVEL_N2(EnumSet.of(VISUALIZAR));

    private final Set<PermissaoPlano> permissoes;

    PapelPlano(Set<PermissaoPlano> permissoes) {
        this.permissoes = Collections.unmodifiableSet(permissoes);
    }

    public Set<PermissaoPlano> getPermissoes() {
        return permissoes;
    }

    public boolean permite(PermissaoPlano permissao) {
        return permissoes.contains(permissao);
    }
}
