import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { Icon, type IconName } from "../components/Icon";

const etapas = [
  {
    icone: "folder" as IconName,
    titulo: "Plano",
    texto: "Defina objetivo, equipe, documentos e critérios antes de iniciar o trabalho.",
  },
  {
    icone: "clipboard" as IconName,
    titulo: "Auditoria",
    texto: "Construa checklists, responda os itens e preserve versões ao longo da execução.",
  },
  {
    icone: "resolution" as IconName,
    titulo: "Resolução",
    texto: "Encaminhe NCs, acompanhe responsáveis, prazos e escalonamentos N1 e N2.",
  },
];

const linhasChecklist = [
  ["01", "O documento está na versão aprovada?", "Conforme", "is-conforme"],
  ["02", "Os critérios de aceite estão evidenciados?", "Não conforme", "is-nao-conforme"],
  ["03", "O requisito se aplica ao artefato?", "N/A", "is-na"],
  ["04", "A rastreabilidade foi registrada?", "Conforme", "is-conforme"],
];

function Brand() {
  return (
    <span className="brand home-brand">
      <span className="brand-mark" aria-hidden="true"><i /><i /><i /></span>
      <span>Quality</span>
    </span>
  );
}

function ProductPreview() {
  return (
    <div className="home-product-preview" aria-hidden="true">
      <div className="home-preview-windowbar"><i /><i /><i /></div>
      <aside className="home-preview-sidebar">
        <strong>Quality</strong>
        {["Visão geral", "Documentos", "Auditorias", "Resoluções"].map((item, indice) => (
          <span className={indice === 2 ? "is-active" : ""} key={item}>
            <Icon name={(["overview", "document", "clipboard", "resolution"] as IconName[])[indice]} size={14} />
            {item}
          </span>
        ))}
      </aside>
      <section className="home-preview-workspace">
        <header className="home-preview-heading">
          <span><small>Plano de qualidade</small><strong>Auditoria do produto</strong></span>
          <i>Em andamento</i>
        </header>
        <div className="home-preview-context">
          <div><small>Documento auditado</small><strong>Especificação funcional.pdf</strong><span>Versão 2.1</span></div>
          <div><small>Referências</small><strong>3 documentos</strong><span>Critérios e templates</span></div>
          <div><small>Auditoria</small><strong>Checklist v4</strong><span>4 itens nesta visão</span></div>
        </div>
        <div className="home-preview-sheet">
          <header><span>Nº</span><span>Item de verificação</span><span>Resultado</span></header>
          {linhasChecklist.map(([numero, pergunta, resultado, classe]) => (
            <div className={classe} key={numero}>
              <span>{numero}</span><strong>{pergunta}</strong><small>{resultado}</small>
            </div>
          ))}
        </div>
      </section>
      <div className="home-preview-resolution">
        <div className="is-origin">
          <span><Icon name="warning" size={17} /></span>
          <small>Não conformidade</small>
          <strong>Critério sem evidência</strong>
        </div>
        <i><Icon name="arrowRight" size={18} /></i>
        <div>
          <span><Icon name="users" size={17} /></span>
          <small>Equipe de resolução</small>
          <strong>Em correção</strong>
        </div>
        <i><Icon name="arrowRight" size={18} /></i>
        <div>
          <span><Icon name="clock" size={17} /></span>
          <small>Prazo acompanhado</small>
          <strong>Escalonamento N1 / N2</strong>
        </div>
      </div>
    </div>
  );
}

export function HomePage() {
  const { autenticado } = useAuth();

  return (
    <div className="public-home">
      <a className="skip-link" href="#conteudo-home">Ir para o conteúdo</a>
      <header className="topbar home-header">
        <div className="topbar-inner home-header-inner">
          <Link className="home-brand-link" to="/" aria-label="Quality, página inicial"><Brand /></Link>
          <nav className="home-navigation" aria-label="Navegação principal">
            <a href="#como-funciona">Como funciona</a>
            <a href="#recursos">Recursos</a>
            <a href="#responsabilidades">Para cada papel</a>
          </nav>
          <div className="home-header-actions">
            {autenticado ? (
              <Link className="home-button home-button-primary" to="/dashboard">Ir para dashboard</Link>
            ) : (
              <>
                <Link className="home-sign-in" to="/entrar">Entrar</Link>
                <Link className="home-button home-button-primary" to="/cadastro">Criar conta</Link>
              </>
            )}
          </div>
        </div>
      </header>

      <main id="conteudo-home">
        <section className="home-hero">
          <div className="home-hero-inner">
            <div className="home-hero-copy">
              <h1>Auditorias de qualidade,<br />do plano à resolução.</h1>
              <p>Planeje auditorias, centralize documentos, execute checklists e acompanhe cada não conformidade em um único fluxo rastreável.</p>
              <div className="home-hero-actions">
                <Link className="home-button home-button-primary" to={autenticado ? "/dashboard" : "/cadastro"}>
                  {autenticado ? "Acessar plataforma" : "Criar conta"}
                  <Icon name="arrowRight" size={18} />
                </Link>
                {!autenticado && <Link className="home-button home-button-secondary" to="/entrar">Entrar</Link>}
              </div>
              <ul className="home-hero-truths" aria-label="Principais capacidades">
                <li><Icon name="document" size={19} /><span>Documentos e referências no mesmo contexto</span></li>
                <li><Icon name="users" size={19} /><span>Responsáveis e permissões definidos por plano</span></li>
                <li><Icon name="activity" size={19} /><span>Histórico preservado em todas as etapas</span></li>
              </ul>
            </div>
            <ProductPreview />
          </div>
        </section>

        <section className="home-workflow" id="como-funciona" aria-labelledby="titulo-fluxo">
          <div className="home-section-intro">
            <h2 id="titulo-fluxo">Um fluxo conectado, da preparação ao acompanhamento.</h2>
            <p>Cada etapa recebe o contexto da anterior. O trabalho não se perde entre planilhas, mensagens e documentos espalhados.</p>
          </div>
          <ol className="home-workflow-track">
            {etapas.map((etapa, indice) => (
              <li key={etapa.titulo}>
                <span className="home-workflow-icon"><Icon name={etapa.icone} size={25} /></span>
                <div><strong>{etapa.titulo}</strong><p>{etapa.texto}</p></div>
                {indice < etapas.length - 1 && <Icon name="arrowRight" size={22} />}
              </li>
            ))}
          </ol>
        </section>

        <section className="home-capabilities" id="recursos" aria-labelledby="titulo-recursos">
          <header className="home-capabilities-heading">
            <h2 id="titulo-recursos">O plano organiza.<br />A auditoria comprova.</h2>
            <p>O Quality reúne as fontes, as decisões e as ações necessárias para que cada resultado possa ser entendido depois.</p>
          </header>

          <article className="home-feature home-feature-documents">
            <div className="home-feature-copy">
              <span className="home-feature-symbol"><Icon name="document" size={26} /></span>
              <h3>Referências e documentos auditados, cada um no seu lugar.</h3>
              <p>Separe o que orienta a auditoria do que será avaliado. Nome real do arquivo, versão e classificação continuam visíveis durante o trabalho.</p>
            </div>
            <div className="home-document-table" aria-label="Exemplo de biblioteca de documentos">
              <header><span>Nome</span><span>Arquivo</span><span>Versão</span></header>
              <div><strong>Critérios de aceite</strong><span>criterios.pdf</span><small>3.0</small></div>
              <div><strong>Especificação funcional</strong><span>especificacao.docx</span><small>2.1</small></div>
              <div><strong>Registro do produto</strong><span>registro-final.pdf</span><small>1.4</small></div>
            </div>
          </article>

          <article className="home-feature home-feature-checklist">
            <div className="home-checklist-detail" aria-label="Exemplo de item do checklist">
              <header><span>Item de verificação</span><span>Resultado</span><span>Status da NC</span></header>
              <div>
                <strong>Os critérios de aceite estão evidenciados?</strong>
                <span className="is-nao-conforme">Não conforme</span>
                <span>Em correção</span>
              </div>
              <footer>
                <span><small>Responsável</small><strong>Equipe de resolução</strong></span>
                <span><small>Classificação</small><strong>Severa</strong></span>
                <span><small>Prazo</small><strong>Data e hora definidas</strong></span>
              </footer>
            </div>
            <div className="home-feature-copy">
              <span className="home-feature-symbol"><Icon name="clipboard" size={26} /></span>
              <h3>Um checklist que acompanha a evolução da auditoria.</h3>
              <p>Edite as perguntas, responda em linhas compactas e salve versões para consultar exatamente como a execução estava em cada momento.</p>
            </div>
          </article>

          <article className="home-feature home-feature-resolution">
            <div className="home-feature-copy">
              <span className="home-feature-symbol"><Icon name="resolution" size={26} /></span>
              <h3>A não conformidade segue até uma decisão clara.</h3>
              <p>Encaminhe o item à equipe, atribua um responsável e acompanhe a correção. Quando o prazo vence, os superiores definidos entram no fluxo.</p>
            </div>
            <ol className="home-resolution-timeline">
              <li className="is-complete"><span /><div><strong>NC enviada</strong><small>Equipe de resolução notificada</small></div></li>
              <li className="is-current"><span /><div><strong>Em correção</strong><small>Responsável e prazo visíveis</small></div></li>
              <li><span /><div><strong>Escalonamento N1</strong><small>Superior acompanha o novo prazo</small></div></li>
              <li><span /><div><strong>Escalonamento N2</strong><small>Segundo nível recebe o contexto completo</small></div></li>
            </ol>
          </article>
        </section>

        <section className="home-roles" id="responsabilidades" aria-labelledby="titulo-papeis">
          <header>
            <h2 id="titulo-papeis">Cada pessoa vê o que precisa para agir.</h2>
            <p>As responsabilidades do plano permanecem explícitas sem expor áreas que não fazem parte do trabalho de cada papel.</p>
          </header>
          <div className="home-role-lanes">
            <article><span><Icon name="clipboard" size={22} /></span><div><h3>Auditoria e qualidade</h3><p>Estrutura o plano, conduz auditorias e valida as resoluções informadas.</p></div><strong>Plano completo</strong></article>
            <article><span><Icon name="users" size={22} /></span><div><h3>Equipe de resolução</h3><p>Recebe as NCs encaminhadas ao time e trabalha nas correções atribuídas.</p></div><strong>Fila de resolução</strong></article>
            <article><span><Icon name="warning" size={22} /></span><div><h3>Superiores N1 e N2</h3><p>Acompanham escalonamentos e podem revisar o prazo quando a resolução atrasa.</p></div><strong>Visão de escalonamentos</strong></article>
          </div>
        </section>

        <section className="home-traceability" aria-labelledby="titulo-rastreabilidade">
          <div>
            <h2 id="titulo-rastreabilidade">Qualidade, conduzida com clareza.</h2>
            <p>Do documento de referência à última decisão sobre uma NC, a informação permanece vinculada ao plano e disponível para consulta.</p>
          </div>
          <ul>
            <li><Icon name="check" size={20} /><span><strong>Versões preservadas</strong><small>Consulte o checklist no estado em que foi salvo.</small></span></li>
            <li><Icon name="check" size={20} /><span><strong>Ações registradas</strong><small>Alterações importantes mantêm autor e horário.</small></span></li>
            <li><Icon name="check" size={20} /><span><strong>Acesso contextual</strong><small>Cada participação possui um papel dentro do plano.</small></span></li>
          </ul>
        </section>

        <section className="home-final-cta" aria-labelledby="titulo-comecar">
          <div>
            <h2 id="titulo-comecar">Comece pelo plano.<br />Mantenha o controle até a resolução.</h2>
            <p>Crie seu acesso e organize o próximo ciclo de auditoria em um fluxo único.</p>
          </div>
          <Link className="home-button home-button-primary" to={autenticado ? "/dashboard" : "/cadastro"}>
            {autenticado ? "Ir para dashboard" : "Criar conta"}<Icon name="arrowRight" size={18} />
          </Link>
        </section>
      </main>

      <footer className="home-footer">
        <Link className="home-brand-link" to="/"><Brand /></Link>
        <p>Planejamento, auditoria e resolução em uma única trilha.</p>
        <Link to={autenticado ? "/dashboard" : "/entrar"}>{autenticado ? "Acessar plataforma" : "Entrar"}</Link>
      </footer>
    </div>
  );
}
