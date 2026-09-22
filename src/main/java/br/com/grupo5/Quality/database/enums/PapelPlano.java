package br.com.grupo5.Quality.database.enums;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static br.com.grupo5.Quality.database.enums.PermissaoPlano.TRATAR_NAO_CONFORMIDADE;

public enum PapelPlano {
    AUDITOR_RESPONSAVEL_QUALIDADE(EnumSet.complementOf(
            EnumSet.of(
                    TRATAR_NAO_CONFORMIDADE,
                    PermissaoPlano.GERENCIAR_ESCALONAMENTOS
            )
    )),
    MEMBRO_EQUIPE_RESOLUCAO(EnumSet.of(TRATAR_NAO_CONFORMIDADE)),
    SUPERIOR_N1(EnumSet.of(
            PermissaoPlano.VISUALIZAR,
            PermissaoPlano.GERENCIAR_ESCALONAMENTOS
    )),
    SUPERIOR_N2(EnumSet.of(
            PermissaoPlano.VISUALIZAR,
            PermissaoPlano.GERENCIAR_ESCALONAMENTOS
    ));

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
