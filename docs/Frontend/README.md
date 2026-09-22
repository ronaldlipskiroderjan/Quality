# Quality Frontend

Frontend funcional do sistema de auditoria de qualidade, desenvolvido com React, TypeScript e Vite.

## Executar localmente

1. Copie `.env.example` para `.env.local`.
2. Confirme que `VITE_API_URL` aponta para a API Spring Boot.
3. Execute `npm install` e `npm run dev`.

A API deve liberar `http://localhost:5173` em `CORS_ALLOWED_ORIGINS` durante o desenvolvimento.

## Variáveis para a Vercel

Configure `VITE_API_URL` com a URL pública HTTPS da API, sem barra no final. Na API, inclua o domínio final da Vercel em `CORS_ALLOWED_ORIGINS`. O arquivo `vercel.json` direciona rotas do navegador para `index.html`, permitindo acesso direto às páginas internas.

## Scripts

- `npm run dev`: servidor local.
- `npm run build`: valida tipos e gera `dist/`.
- `npm run test`: testes unitários.
- `npm run typecheck`: validação TypeScript sem build.

## Funcionalidades conectadas

- cadastro, login, restauração da sessão na aba e logout;
- listagem, criação, edição, conclusão e exclusão de planos;
- participantes e papéis contextuais;
- upload, filtro, download e exclusão de documentos;
- criação de artefatos com documento auditado e auditor;
- criação, composição e publicação de checklists;
- listagem e leitura de notificações.

A execução de auditorias e o ciclo completo de não conformidades são a próxima fatia do frontend.
