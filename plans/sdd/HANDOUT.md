# HANDOUT — Registro de progresso

> Memória de trabalho entre sessões. **Leia este arquivo antes de retomar o projeto.**
> Atualize-o ao fim de cada sessão significativa.

- **Projeto:** Claude Code Dock — tool window dedicada para o Claude Code em IDEs JetBrains
- **Última atualização:** 2026-08-01

---

## Estado atual

**Fase: primeiro teste no IDE real feito. Três correções implementadas (SPEC v1.1). Falta revalidar.**

| Artefato                                                         | Estado                                                   |
| ---------------------------------------------------------------- | -------------------------------------------------------- |
| [../20260801-initial-project.md](../20260801-initial-project.md) | Documento de origem (contexto + roteiro SDD)             |
| [SPEC.md](SPEC.md)                                               | ✅ v1.1 — RF-17, RF-18 e RF-19 incorporados               |
| `HANDOUT.md`                                                     | ✅ Este arquivo                                           |
| Código do plugin                                                 | ✅ Implementado — compila, **34 testes passando**         |
| **T-4 (bloqueante)**                                             | ✅ **APROVADO** — premissa central validada empiricamente |
| Primeiro teste manual no IDE                                     | ✅ Feito — janela abre e funciona; 2 defeitos achados     |
| RF-17 (`Esc`), RF-18 (estado vazio), RF-19 (`CLAUDE_CONFIG_DIR`) | ✅ Implementados e testados unitariamente                 |
| Revalidação manual (T-3.7, T-3.8, T-3.9)                         | ⏳ Pendente — exige rebuild, reinstalação e uso           |
| Roteiros T-3.1 a T-3.6 completos                                 | ⏳ Parcial — falta a prova ponta a ponta do diff (T-3.2)  |

**Próximo passo imediato:** `./gradlew buildPlugin`, reinstalar e rodar T-3.7/T-3.8/T-3.9
(ver [Próximos passos](#próximos-passos)).

---

## Fatos verificados

> Todos obtidos por inspeção direta do ambiente em 2026-08-01. **Não reinvestigar** —
> apenas revalidar após upgrade de IDE.

### Ambiente

| Item                            | Valor                                                                      |
| ------------------------------- | -------------------------------------------------------------------------- |
| IDE de referência               | IntelliJ IDEA Ultimate 2026.2, build `IU-262.8665.258`                     |
| Outros IDEs instalados          | GoLand, PyCharm, RubyMine, WebStorm, Air (todos 2026.2)                    |
| CLI Claude Code                 | v`2.1.220`, em `~/.local/bin/claude`                                       |
| JDK                             | Zulu 21.0.8 (LTS)                                                          |
| Gradle                          | 9.2.0                                                                      |
| IntelliJ Platform Gradle Plugin | 2.18.1 (publicado em 10/07/2026)                                           |
| Rede de build                   | `plugins.gradle.org` e `cache-redirector.jetbrains.com` respondem HTTP 200 |

### Como o plugin oficial funciona

Plugin oficial: `com.anthropic.code.plugin` v`0.1.14-beta`, `since-build 242`.
Instalado em `~/.local/share/JetBrains/<IDE>/claude-code-jetbrains-plugin/`.

| Fato                                                                                                    | Evidência                                                                                                                                                                         |
| ------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Abre a sessão com `TerminalToolWindowManager.createShellWidget(...)` + `sendCommandToExecute("claude")` | `javap -c` de `com.anthropic.code.plugin.TerminalUtil`                                                                                                                            |
| **Prende a sessão à tool window nativa "Terminal"** — origem do problema                                | literal `"Terminal"` em `ToolWindowManager.getToolWindow(...)`, no mesmo `TerminalUtil`                                                                                           |
| Não é só um lançador: **hospeda um servidor MCP WebSocket** dentro do IDE                               | jars `ktor-server-{cio,websockets,sse}`, `kotlin-sdk-jvm-0.4.0`; pacote `mcp/tools` com `DiffTools`, `DiagnosticTools`, `EditorTools`, `FileTools`, `WebSocketMcpServerTransport` |
| Descoberta pelo CLI via env var `CLAUDE_CODE_SSE_PORT`                                                  | literal em `TerminalCustomizer.class`, registrado no EP `org.jetbrains.plugins.terminal.localTerminalCustomizer` (`plugin-terminal.xml`)                                          |
| Descoberta alternativa via lockfile `~/.claude/ide/<port>.lock`                                         | arquivos reais no ambiente: `{"pid":…,"workspaceFolders":[…],"ideName":"GoLand","transport":"ws","authToken":"…"}`                                                                |
| Ações registradas                                                                                       | `SendToClaudeAction` (`Ctrl+Alt+K`), `OpenClaudeInTerminalAction` (`Ctrl+Esc`)                                                                                                    |
| Outras env vars encontradas no jar                                                                      | `CLAUDE_CONFIG_DIR`, `CLAUDE_ELAPSED_TIME_MS`, `CLAUDE_CODE_JETBRAINS_PLUGIN_HIDE_BUTTON` (semântica **não** verificada)                                                          |

### A descoberta que define a arquitetura

**`TerminalToolWindowManager.createShellWidget(...)` delega a
`AbstractTerminalRunner.startShellTerminalWidget(Disposable, ShellStartupOptions, boolean)`**
(confirmado por `javap -c` de `TerminalToolWindowManager`).

Esse método é público e retorna `com.intellij.terminal.ui.TerminalWidget`, que estende
`com.intellij.openapi.ui.ComponentContainer` — portanto embutível em qualquer container Swing,
inclusive no `Content` de uma tool window customizada.

A aplicação dos `LocalTerminalCustomizer` ocorre **abaixo** desse ponto, na cadeia
`LocalTerminalDirectRunner.createProcess` → `LocalOptionsConfigurer.configureStartupOptions`.

➡️ **Consequência:** um terminal criado em tool window própria deve receber o mesmo tratamento
de ambiente que o terminal nativo — inclusive a injeção de `CLAUDE_CODE_SSE_PORT` feita pelo
plugin oficial. **É isto que torna a arquitetura de casca fina viável.**

✅ **CONFIRMADO EMPIRICAMENTE em 2026-08-01** pelo teste automatizado
`TerminalCustomizerReachTest` (T-4). O teste registra um `LocalTerminalCustomizer` próprio e
verifica que a variável sentinela aparece em
`runner.configureStartupOptions(options).envVariables` — o mesmo caminho que o plugin oficial
usa. Um segundo teste de controle confirma que a variável **não** aparece sem o customizer
registrado, provando que a medição é do mecanismo e não de resíduo do ambiente.

**Consequência:** a arquitetura de casca fina se sustenta. Q-01 resolvido, R-02 fechado.
Este teste fica como regressão permanente para cada upgrade de IDE (T-5.3).

### Descobertas da implementação (2026-08-01)

| Fato                                                                                                                                                                     | Como foi descoberto                                                     |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------- |
| **A Community Edition (IC) deixou de ser publicada a partir de 2025.3 (253).** A distribuição é unificada; usa-se `intellijIdea("2026.2")`                               | erro do IntelliJ Platform Gradle Plugin ao resolver `create("IC", ...)` |
| **A plataforma 2026.2 embute metadata Kotlin 2.4.0**, exigindo compilador Kotlin ≥ 2.4 (2.1.20 lê só até 2.2.0)                                                          | erro de compilação; corrigido com `kotlin("jvm") version "2.4.10"`      |
| `ShellStartupOptions` cru **não** pode ir direto para `createProcess`: exige `runner.configureStartupOptions(...)` antes (finaliza o `EelPath` do working dir)           | `IllegalStateException` no primeiro T-4                                 |
| É **`configureStartupOptions`** que aplica os `LocalTerminalCustomizer` — não `createProcess`                                                                            | T-4 verde após a correção acima                                         |
| O único consumidor do EP no bytecode é `TerminalExecOptionsCustomizationRemoteApiImplKt` (módulo backend), alcançado também no modo monolítico                           | `javap` + teste empírico                                                |
| _(v1.1)_ `ToolWindowFactory.createToolWindowContent` roda **uma vez por projeto**: fechar a última aba deixa a janela viva, vazia e sem caminho de volta                 | primeiro teste manual no IDE                                            |
| _(v1.1)_ `ToolWindowEx.emptyText` é `StatusText`, e `appendLine(texto, atributos, listener)` aceita link clicável — resolve RF-18 sem componente próprio                 | compilação + API da plataforma                                          |
| _(v1.1)_ `FileUtil.expandUserHome` está **deprecated** em 262; o substituto é `OSAgnosticPathUtil.expandUserHome`, para onde ele delega                                  | warning de compilação + `javap -c` de `FileUtil`                        |
| _(v1.1)_ `envVariables` de `ShellStartupOptions` é **aditivo**: convive com o que os customizers injetam, então `CLAUDE_CONFIG_DIR` não atrapalha `CLAUDE_CODE_SSE_PORT` | mesma cadeia validada em T-4                                            |

### A armadilha do `Esc` fora da tool window "Terminal" (2026-08-01, tarde/2)

> **Este é o achado mais importante depois de T-4.** Vale reler antes de mexer em teclado.

`com.intellij.terminal.TerminalEscapeKeyListener.shouldSwitchFocusToEditor` (`javap -c`):

```java
ToolWindow tw = panel.getContextToolWindow();          // PlatformDataKeys.TOOL_WINDOW
AnAction action = ActionManager.getAction("Terminal.SwitchFocusToEditor");
if (tw == null) return false;
if (action != null) {
    Collection<KeyStroke> strokes = KeymapUtil.getKeyStrokes(action.getShortcutSet());
    if (JBTerminalWidget.isTerminalToolWindow(tw))     // "Terminal".equals(tw.getId())
        return isMatched(e, strokes);
    return strokes.isEmpty() ? isEscape(e) : isMatched(e, strokes);   // <-- nossa janela
}
return isEscape(e);
```

**O default significa coisas opostas dentro e fora da tool window `"Terminal"`.** Sem atalho
configurado (o padrão do 2026.2), a janela nativa entrega o `Esc` ao shell e a nossa consome
todo `Esc`. Por isso `/usage` prendia a sessão.

**Onde entramos:** `JBTerminalPanel.handleKeyEvent` roda os pre-handlers **antes** do listener,
e o listener ignora evento já consumido:

```java
for (Consumer<KeyEvent> c : myPreKeyEventConsumers) c.accept(e);
myEscapeKeyListener.handleKeyEvent(e);   // no-op se e.isConsumed()
if (!e.isConsumed()) super.handleKeyEvent(e);
```

`ClaudeEscapeForwarder` escreve `\u001b` no `TtyConnector` e consome. Só `Esc` puro em
`KEY_PRESSED`: com modificador o evento segue intacto, preservando `Ctrl+Esc` do oficial e
`Shift+Esc` do IDE.

**Descartado — e por quê:**

| Alternativa                                                               | Motivo da recusa                                                                                       |
| ------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| Dar atalho não-`Esc` a `Terminal.SwitchFocusToEditor`                     | Conserta por acidente, mexe no keymap global do usuário e viola RF-13                                  |
| Anular `PlatformDataKeys.TOOL_WINDOW` num `UiDataProvider` sobre o widget | Faz `tw == null` e resolve o `Esc`, mas quebra `Shift+Esc` e ações de tool window com foco no terminal |
| Nomear nossa tool window de `"Terminal"`                                  | Colide com a nativa                                                                                    |
| Migrar para o engine `REWORKED`                                           | Caminho diferente (`Terminal.Escape` + EP `escapeHandler`); mudança grande, não investigada (Q-10)     |

### API de terminal disponível na build 262

`AbstractTerminalRunner.startShellTerminalWidget` · `LocalTerminalDirectRunner.createTerminalRunner`
· `ShellStartupOptions` · `TerminalWidget` (`sendCommandToExecute`, `addTerminationCallback`)
· `TerminalEngine` (`CLASSIC`, `REWORKED`, `NEW_TERMINAL`) · `LocalTerminalCustomizer.EP_NAME`

---

## Decisões tomadas

| #        | Decisão                                                                                                                        | Porquê                                                                                                                                                                                                                                         |
| -------- | ------------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **D-01** | **Casca fina**: o plugin entrega apenas a tool window; o plugin oficial permanece instalado e fornece diff/seleção/diagnostics | O protocolo MCP/lockfile é privado, não documentado e instável. Reimplementá-lo custaria 10× mais código e quebraria a cada mudança da Anthropic. Como o customizer oficial alcança qualquer terminal da plataforma, a integração vem de graça |
| **D-02** | **Histórico via `claude --resume`**, sem persistência própria                                                                  | O CLI já resolve. Persistir por conta própria duplicaria responsabilidade, arriscaria divergir do formato de sessão e criaria um segundo local com código-fonte do usuário em disco — passivo de segurança sem contrapartida                   |
| **D-03** | **Alvo: todos os IDEs JetBrains**, `since-build 252`, sem `until-build`                                                        | O usuário roda 6 IDEs diferentes. Sem `until-build`, o plugin não é desabilitado a cada upgrade; em troca, aceita-se o risco de exceção em runtime (R-08)                                                                                      |
| **D-04** | Sem UI de chat própria; a UI **é** o terminal                                                                                  | O widget JediTerm já dá seleção, cópia, colagem, busca e links clicáveis. Construir chat próprio exigiria o protocolo privado (ver D-01)                                                                                                       |
| **D-05** | Todo acoplamento à API de terminal em **uma única classe** (`ClaudeTerminalSessionFactory`)                                    | A API não tem garantia de estabilidade entre builds. Concentrar o risco reduz a auditoria de upgrade a um arquivo                                                                                                                              |
| **D-06** | **Sem telemetria**                                                                                                             | Privacidade. Monitoramento é o `idea.log` local                                                                                                                                                                                                |
| **D-07** | Validar a premissa central (**T-4**) **antes** de implementar o resto                                                          | Toda a arquitetura depende dela; descobrir tarde custaria o retrabalho completo                                                                                                                                                                |
| **D-08** | Este `HANDOUT.md` vive em `plans/sdd/`, não na raiz                                                                            | O hook `enforce-plans-dir.py` do projeto exige artefatos `.md` sob `plans/`. Decisão do usuário: manter o nome pedido pelo plano de origem, mas dentro de `plans/sdd/`, ao lado do SPEC                                                        |

| **D-09** | O comando é enviado com `sendCommandToExecute` (string digitada no shell), com **escapamento POSIX** do caminho do executável                | Mesma abordagem do plugin oficial, e mantém o shell vivo após o `claude` sair — essencial para o diagnóstico previsto no Fluxo D. Como a string é interpretada pelo shell, o caminho é protegido por aspas simples. **Isso ajusta o RNF-08**, que pedia montagem por argv: argv exigiria substituir o shell e perder o fallback |
| **D-10** | Kotlin 2.4.10 e distribuição unificada `intellijIdea("2026.2")`                                                                              | Imposições da plataforma, descobertas na compilação — não são escolhas                                                                                                                                                                                                                                                          |
| **D-11** | T-4 virou **teste automatizado permanente**, não roteiro manual                                                                              | Determinístico, roda em cada `./gradlew test` e serve de regressão a cada upgrade de IDE (T-5.3), que era exatamente o risco R-01                                                                                                                                                                                               |
| **D-12** | _(v1.1)_ O `Esc` é corrigido por **pre-handler no painel** (`ClaudeEscapeForwarder`), não mexendo em keymap nem em `TerminalOptionsProvider` | É a única opção que corrige só o `Esc`, só na nossa janela. Alterar keymap ou settings do terminal seria alterar o IDE do usuário — violação direta de RF-13. Anular `TOOL_WINDOW` no data context resolveria o `Esc` mas quebraria `Shift+Esc` e ações de tool window com foco no terminal                                     |
| **D-13** | _(v1.1)_ O estado vazio usa `ToolWindowEx.emptyText` com links, e **não** recria sessão automaticamente ao reabrir a janela                  | Nativo, três linhas, e mantém a escolha com o usuário. Recriar sozinho exigiria listener de tool window e reabriria o debate de "quando é demais" — sem demanda comprovada                                                                                                                                                      |
| **D-14** | _(v1.1)_ `CLAUDE_CONFIG_DIR` é **por projeto** e vive no arquivo de workspace; o executável continua por aplicação                           | Em IDEs JetBrains uma janela é um projeto, então projeto já entrega a granularidade pedida. Workspace e não `.idea/` versionado porque é caminho local de máquina e aponta para diretório com credenciais do CLI                                                                                                                |
| **D-15** | _(v1.1)_ Uma única tela `projectConfigurable` mostra os dois campos, rotulando o escopo de cada um                                           | Duas telas para duas configurações seria burocracia. O rótulo ("Vale para todos os projetos" / "Somente este projeto") resolve a ambiguidade — mesmo padrão da tela de Terminal do IDE                                                                                                                                          |

### Correção registrada

A premissa do documento de origem — _"a Anthropic inviabiliza plugins de terceiros rodarem o
Claude Code"_ — **não se confirma**. Não há bloqueio técnico a executar `claude` em um PTY.
O que não é público é o **protocolo de integração**. A arquitetura escolhida contorna isso ao
não tocar no protocolo, e não porque executar o CLI fosse proibido.

---

## Desafios em aberto

| #        | Desafio                                                                                                                                          | Criticidade |
| -------- | ------------------------------------------------------------------------------------------------------------------------------------------------ | ----------- |
| **Q-01** | ✅ **RESOLVIDO em 2026-08-01.** O customizer alcança sim — validado por `TerminalCustomizerReachTest` (T-4), com teste de controle. R-02 fechado  | ✅ Resolvido |
| **Q-02** | Duas sessões simultâneas (aba nativa + janela dedicada) no mesmo servidor MCP: qual "possui" um diff aberto?                                     | 🟡 Médio     |
| **Q-03** | Semântica exata de `CLAUDE_CODE_JETBRAINS_PLUGIN_HIDE_BUTTON` — string encontrada, comportamento não verificado                                  | 🟢 Baixo     |
| **Q-04** | `TerminalEngine.REWORKED` se comporta como `CLASSIC` fora da tool window nativa?                                                                 | 🟡 Médio     |
| **Q-05** | Vale ocultar o ponto de entrada do oficial para evitar confusão? Depende de Q-03                                                                 | 🟢 Baixo     |
| **Q-06** | Remote Dev / split mode / WSL — declarados fora de escopo, reavaliar depois                                                                      | 🟢 Baixo     |
| **Q-07** | Restaurar sessões ao reabrir o projeto? Sem demanda comprovada                                                                                   | 🟢 Baixo     |
| **Q-08** | `since-build` definido como `252` por conservadorismo, mas **só `262` foi testado**                                                              | 🟢 Baixo     |
| **Q-09** | _(v1.1)_ `CLAUDE_CONFIG_DIR` **por aba**, e não só por projeto? Exigiria diálogo a cada "Nova sessão"                                            | 🟢 Baixo     |
| **Q-10** | _(v1.1)_ O `Esc` se comporta igual no engine `REWORKED`? Lá o caminho é `Terminal.Escape` + EP `escapeHandler`, não o pre-handler. Ligado a Q-04 | 🟡 Médio     |
| **Q-11** | _(v1.1)_ `CLAUDE_CONFIG_DIR` deveria ser versionável em `.idea/` em vez de ficar no workspace?                                                   | 🟢 Baixo     |

---

## Próximos passos

Concluído: ~~aprovação do SPEC~~ · ~~T-4~~ · ~~esqueleto Gradle~~ · ~~componentes + testes T-1.\*~~ ·
~~`buildPlugin`~~ · ~~primeiro teste no IDE real~~ · ~~RF-17/RF-18/RF-19~~

**Pendente:**

1. **Revalidar no IDE real** — rebuild e reinstalação, agora com os três ajustes:

   ```sh
   ./gradlew buildPlugin
   # Settings → Plugins → ⚙ → Install Plugin from Disk…
   #   build/distributions/claude-code-dock-0.1.0.zip
   ```

   Roteiro mínimo, com o plugin oficial ainda instalado:
   - **T-3.7** — rodar `/usage` e sair com `Esc`; o foco tem de **ficar** no terminal;
   - **T-3.8** — fechar a última aba e usar os links do estado vazio;
   - **T-3.9** — definir `CLAUDE_CONFIG_DIR` em Settings → Tools → Claude Code Dock, abrir nova
     sessão e conferir `echo $CLAUDE_CONFIG_DIR` **e** `echo $CLAUDE_CODE_SSE_PORT` — os dois
     precisam estar presentes;
   - **T-3.2** (ainda em aberto) — pedir uma edição de arquivo e confirmar que o **diff abre no
     visualizador do IDE**. É a prova de ponta a ponta que falta;
   - **T-5.2 / T-5.4** — `Ctrl+Esc` do oficial e `Shift+Esc` do IDE seguem funcionando.

2. **Testes de integração T-2.\*** — tool window registrada, isolamento de abas, liberação de
   PTY ao fechar aba, e o comportamento com `TerminalEngine.REWORKED` vs `CLASSIC` (Q-04, Q-10).
3. **Q-02** — investigar a ambiguidade de sessão dupla durante o uso real.
4. Repetir a instalação nos demais IDEs (T-3.5).
5. Registrar achados neste arquivo.

---

## Log

### 2026-08-01 (tarde/2) — Primeiro teste no IDE real e SPEC v1.1

O plugin foi instalado e aberto no IntelliJ. **A janela funciona:** ícone "Claude Code Dock" na
barra lateral, sessão iniciando sozinha. Dois defeitos apareceram no uso, e uma melhoria foi
pedida. Os três viraram RF-17, RF-18 e RF-19.

- **RF-17 — `Esc` não chegava ao Claude Code.** Comandos como `/usage` prendiam a sessão. A
  causa não era nossa: `TerminalEscapeKeyListener` só entrega o `Esc` ao shell quando a tool
  window tem id literal `"Terminal"`; em qualquer outra, o default do 2026.2 (atalho vazio para
  `Terminal.SwitchFocusToEditor`) significa o **oposto** — consumir todo `Esc`. Ver a seção
  dedicada em [Fatos verificados](#a-armadilha-do-esc-fora-da-tool-window-terminal-2026-08-01-tarde2),
  com o bytecode e as alternativas descartadas.
  - O popup que o usuário viu e fechou era o `Terminal: Escape behavior changed` do próprio IDE,
    que aponta para Settings → Tools → Terminal → _Move focus to the Editor with:_. Mudar lá
    também contornaria, mas por acidente: qualquer atalho não-`Esc` desarma o listener. A
    correção adotada não depende da configuração do usuário.
- **RF-18 — janela morta depois de fechar a última aba.** `createToolWindowContent` roda uma vez
  por projeto; sem aba sobrava o "Nothing to show" padrão. Resolvido com `ToolWindowEx.emptyText`
  e dois links. Os botões `+` / retomar do cabeçalho já existiam, mas não se liam como saída.
- **RF-19 — `CLAUDE_CONFIG_DIR` por projeto.** Novo `ClaudeDockProjectSettings` (workspace),
  injetado via `ShellStartupOptions.envVariables`. A tela de configurações virou
  `projectConfigurable` com os dois campos e o escopo rotulado. Ver D-14 e D-15.
- De quebra: a montagem do comando estava duplicada em três lugares e foi centralizada em
  `ClaudeDockSessions.openNewSession()` / `openResumeSession()`.
- Resultado: **34 testes, 0 falhas**, sem warnings de compilação.
- **Metodologia — o que custou caro aqui.** A investigação do `Esc` começou larga demais
  (varredura de jars da plataforma atrás de "quem consome `Esc`") antes de olhar o caminho que
  o **nosso** código realmente percorre. O usuário interrompeu, com razão. O achado veio em
  minutos depois de partir de `ClaudeTerminalSessionFactory` → `startShellTerminalWidget` →
  `JBTerminalPanel`. **Comece pelo caminho do próprio código, não pelo espaço de busca da
  plataforma.**
- **Ainda não validado:** nada disso foi reexercitado no IDE. T-3.7, T-3.8 e T-3.9 são a
  próxima ação.

### 2026-08-01 (tarde) — Implementação da Fase 1 e validação de T-4

- SPEC aprovado pelo usuário; implementação iniciada pelo teste bloqueante, como previsto.
- **T-4 APROVADO.** `TerminalCustomizerReachTest` prova que um `LocalTerminalCustomizer`
  registrado alcança as opções entregues ao processo pelo mesmo runner que usamos em produção.
  Um segundo teste de controle (sem customizer) garante que a medição não é resíduo do ambiente.
  **Q-01 resolvido, R-02 fechado: a arquitetura de casca fina se sustenta.**
- O caminho exato importa: é **`configureStartupOptions`** que aplica os customizers, não
  `createProcess`. Descoberto porque a primeira versão do T-4 falhou com
  `IllegalStateException: Working directory has not been finalized`. A produção não é afetada —
  `startShellTerminalWidget` já faz esse pré-processamento internamente.
- Ironia útil: a primeira falha do T-4 **já continha a resposta** — o dump da exceção mostrava
  `CLAUDE_DOCK_REACH_PROBE=reached` dentro de `envVariables`.
- Duas premissas do SPEC caíram na compilação: IC não é mais publicada desde 253 (usa-se a
  distribuição unificada) e a plataforma 2026.2 exige Kotlin ≥ 2.4. Ver D-10.
- RNF-08 ajustado na prática (D-09): `sendCommandToExecute` é interpretado pelo shell, então
  o caminho do executável recebe escapamento POSIX em vez de montagem por argv — que exigiria
  substituir o shell e perder o fallback de diagnóstico do Fluxo D.
- Extraídas `ClaudeTabTitle` e `ClaudeWorkingDirectory` como objetos puros só para viabilizar
  teste sem subir IDE.
- Resultado: 21 testes, 0 falhas. ZIP de 26 KB gerado, `since-build=252` sem `until-build`.
- **Ainda não validado:** o comportamento real no IDE (T-3.\*). Nada aqui prova que a janela
  abre bonita ou que o diff funciona de ponta a ponta — só que o mecanismo de env existe.

### 2026-08-01 (manhã) — Descoberta e especificação

- Repositório continha apenas `CLAUDE.md`, `README.md` e o plano de origem. Nenhum código.
- Investigado o plugin oficial por inspeção direta do jar instalado (`unzip` + `javap`), em vez
  de presumir seu funcionamento a partir do comportamento observável.
- **Achado que mudou o projeto:** o plugin oficial não é um lançador de terminal simples — ele
  hospeda um servidor MCP WebSocket e injeta `CLAUDE_CODE_SSE_PORT` via `LocalTerminalCustomizer`,
  extension point que alcança **todo** terminal local da plataforma.
- **Achado que definiu a arquitetura:** `createShellWidget` delega a `startShellTerminalWidget`,
  público e embutível em tool window própria. Logo, a integração do oficial deve seguir
  funcionando numa janela customizada — reduzindo o escopo do plugin a "só a janela".
- Confirmado o protocolo de lockfile em `~/.claude/ide/`, com um arquivo escrito pelo próprio
  GoLand, provando que o plugin JetBrains oficial usa o mesmo mecanismo do VS Code.
- Corrigida a premissa do plano de origem sobre bloqueio da Anthropic (ver acima).
- Decisões D-01 a D-03 confirmadas com o usuário; D-04 a D-07 derivadas na especificação;
  D-08 decidida após bloqueio do hook de projeto.
- `SPEC.md` escrito com as 16 seções exigidas, incluindo a revisão crítica da Fase 3 com
  10 achados incorporados.
- Nota metodológica: uma verificação inicial com `grep` sobre arquivos `.class` produziu
  **falsos negativos** por tratamento de binário; corrigida com `grep -a` após um teste de
  controle. Vale lembrar em futuras investigações de bytecode.
