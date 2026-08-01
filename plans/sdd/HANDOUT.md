# HANDOUT — Registro de progresso

> Memória de trabalho entre sessões. **Leia este arquivo antes de retomar o projeto.**
> Atualize-o ao fim de cada sessão significativa.

- **Projeto:** Claude Code Dock — tool window dedicada para o Claude Code em IDEs JetBrains
- **Última atualização:** 2026-08-01

---

## Estado atual

**Fase: DEF-01 corrigido, RF-24/26/27 implementados (SPEC v1.3). Falta a validação manual.**

| Artefato                                                         | Estado                                                   |
| ---------------------------------------------------------------- | -------------------------------------------------------- |
| [../20260801-initial-project.md](../20260801-initial-project.md) | Documento de origem (contexto + roteiro SDD)             |
| [SPEC.md](SPEC.md)                                               | ✅ v1.3 — DEF-01, RF-24/25/26 e Q-14 incorporados         |
| `HANDOUT.md`                                                     | ✅ Este arquivo                                           |
| Código do plugin                                                 | ✅ Implementado — compila, **51 testes passando**         |
| **T-4 (bloqueante)**                                             | ✅ **APROVADO** — premissa central validada empiricamente |
| RF-17 (`Esc`), RF-18 (estado vazio), RF-19 (`CLAUDE_CONFIG_DIR`) | ✅ Implementados **e validados no IDE** (T-3.7 a T-3.9)   |
| T-3.1 e T-3.2 (diff ponta a ponta)                               | ✅ **APROVADOS** — a integração com o oficial funciona    |
| RF-22 (`/export`) — T-3.11                                       | ✅ **APROVADO** — saída limpa, verificada no arquivo gerado |
| RF-21 (copiar buffer) — DEF-01                                   | ⚰️ **Substituído** por RF-24; buffer não é mais lido       |
| RF-24 (cópia via `/export`), RF-26 (botão), RF-27 (saída plana) | ✅ Implementados e testados unitariamente                  |
| R-13 (bloqueante de RF-24)                                       | ✅ **FECHADO** por leitura do binário, sem teste manual    |
| Validação manual T-3.14 a T-3.17                                 | ⏳ Pendente — reinstalar o ZIP de 16:54                    |
| Roteiros T-3.3 a T-3.6                                           | ⏳ Pendente                                               |

**Próximo passo imediato:** reinstalar `build/distributions/claude-code-dock-0.1.0.zip` e rodar
T-3.14 (cópia sem duplicação), T-3.15/T-3.16 (botão flutuante) e T-3.17 (saída plana — é o dado
que decide se Q-14 continua de pé).

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

### Copiar e exportar: o que a plataforma dá e o que não dá (2026-08-01, tarde/3)

> Mesma família de armadilha do `Esc`: **o comportamento depende do engine, não do widget.**

| Fato                                                                                                                                                          | Como foi descoberto                                                       |
| --------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------- |
| **`TerminalWidget.getText()` já devolve scrollback + tela** no CLASSIC — a seleção vai de `(0, -historyLinesCount)` a `(width, screenLinesCount-1)`, sob `buffer.lock()` | `javap -c` de `JBTerminalWidget.getText(TerminalPanel)` e do `TerminalWidgetBridge` |
| Fora do CLASSIC o `default` da interface devolve **string vazia** — degrada, não lança                                                                        | `javap -c` da interface: `ldc ""` / `areturn`                             |
| **Em Kotlin é `widget.getText()`, não `widget.text`** — ao contrário de `ttyConnector`, não é property                                                        | erro de compilação `Unresolved reference 'text'`                          |
| **Não existe "copiar tudo" no CLASSIC.** `Terminal.SelectAll` só está no `Terminal.ReworkedTerminalContextMenu`, e seu `update()` exige `isReworkedTerminalEditor` | `plugin.xml` do terminal + `javap -c` de `TerminalSelectAllAction`         |
| `sendCommandToExecute` **não serve** para falar com um TUI vivo: `ShellTerminalWidget.executeCommand` lança `IOException` se já houver texto digitado no prompt | `javap -c` de `executeCommand`                                            |
| O CLI tem `/export`: `{type:"local-jsx", name:"export", description:"Export the current conversation to a file or clipboard", argumentHint:"[filename]"}`      | string extraída do binário `claude` 2.1.220                               |
| **O destino "clipboard" do `/export` depende de `wl-copy`/`xclip`/`xsel` — nenhum instalado aqui** (Wayland). Delegar tudo ao CLI teria deixado o caso principal sem solução | `grep` no binário + `command -v`                                          |
| Limite do scrollback vem do advanced setting `terminal.buffer.max.lines.count`                                                                                 | `javap -c` de `JBTerminalSystemSettingsProviderBase.getBufferMaxLinesCount` |
| `CopyPasteManager.copyTextToClipboard` é estático; `Content` é `UserDataHolder` (dá para pendurar o widget na aba, sem mapa próprio)                          | `javap` de `intellij.platform.editor.ui.jar` e de `Content`               |

**Lição:** uma ação registrada no IDE **não é** uma capacidade disponível. Ler o `update()` dela
faz parte da verificação — senão o resultado é um botão morto.

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
| **D-16** | _(v1.2)_ O `/export` é enviado por **escrita direta no `TtyConnector`**, não por `sendCommandToExecute`                                      | `sendCommandToExecute` cai em `ShellTerminalWidget.executeCommand`, que lança `IOException` quando já há texto digitado no prompt — a regra, e não a exceção, com um TUI vivo na frente. A escrita direta é o mesmo canal que o `ClaudeEscapeForwarder` já usa em produção                                                       |
| **D-17** | _(v1.2)_ **Dois botões**, não um: copiar o buffer e disparar o `/export`                                                                     | São naturezas opostas e nenhuma substitui a outra. A cópia é o render literal (rápido, fiel à tela, com bordas de TUI); o `/export` é a transcrição limpa, produzida pelo CLI. Delegar tudo ao CLI não era opção: o destino "clipboard" dele exige `wl-copy`/`xclip`/`xsel`, ausentes nesta máquina                              |
| **D-18** | _(v1.2)_ O widget fica pendurado no `Content` da aba por um `Key`, em vez de um mapa no serviço                                              | A referência morre junto com a aba, sem código de limpeza e sem risco de vazar widget de aba fechada                                                                                                                                                                                                                              |
| **D-19** | _(v1.2)_ `readText`/`sendInput` moram em `ClaudeTerminalSessionFactory`, não nas ações                                                       | RNF-15 exige o contato com a API de terminal num arquivo só. As ações falam com `ClaudeDockSessions`, que sabe qual aba está selecionada                                                                                                                                                                                          |

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
| **Q-12** | _(v1.2)_ Vale passar `[filename]` ao `/export` e abrir o arquivo no editor? Economiza cliques, mas exige adivinhar a semântica do argumento     | 🟢 Baixo     |
| **Q-13** | _(v1.2)_ A cópia deveria respeitar a seleção do mouse quando houver? `Ctrl+C` já cobre; `JBTerminalWidget.getSelectedText()` existe se mudarmos | 🟢 Baixo     |

---

## Próximos passos

Concluído: ~~aprovação do SPEC~~ · ~~T-4~~ · ~~esqueleto Gradle~~ · ~~componentes + testes T-1.\*~~ ·
~~`buildPlugin`~~ · ~~primeiro teste no IDE real~~ · ~~RF-17/RF-18/RF-19~~ ·
~~T-3.1/T-3.2 (diff ponta a ponta)~~ · ~~T-3.7/T-3.8/T-3.9~~ · ~~RF-21/RF-22~~

**Pendente:**

1. **Validar no IDE real** — o ZIP já está construído (16:54):

   ```sh
   # Settings → Plugins → ⚙ → Install Plugin from Disk…
   #   build/distributions/claude-code-dock-0.1.0.zip
   ```

   Roteiro mínimo, com o plugin oficial ainda instalado:
   - **T-3.14** — conversa longa, janela redimensionada no meio, "Copiar Conversa": tem de vir
     **uma vez só**. E conferir que não sobra `claude-dock-export-*.md` em `/tmp`;
   - **T-3.15 / T-3.16** — selecionar com o mouse: o botão aparece, copia só o trecho, some ao
     desfazer — e digitar em seguida ainda vai para a sessão (foco não roubado);
   - **T-3.17** — ligar "Saída plana" em Settings e comparar. **É o dado que decide Q-14**;
   - **T-3.12** — com a última aba fechada, acionar os botões: notificam, não quebram;
   - **regressão T-3.7** — `/usage` + `Esc`: agora três caminhos escrevem no mesmo
     `TtyConnector` (Esc, `/export` e o comando inicial).

2. **Testes de integração T-2.\*** — tool window registrada, isolamento de abas, liberação de
   PTY ao fechar aba, e o comportamento com `TerminalEngine.REWORKED` vs `CLASSIC` (Q-04, Q-10,
   CB-26 — no REWORKED a cópia devolve vazio).
3. **Q-02** — investigar a ambiguidade de sessão dupla durante o uso real.
4. Repetir a instalação nos demais IDEs (T-3.5); T-3.3, T-3.4 e T-3.6 seguem em aberto.
5. Registrar achados neste arquivo.

---

## Log

### 2026-08-01 (noite) — RF-24, RF-26 e RF-27 implementados

- **R-13 fechado sem teste manual.** `/export` é `local-jsx` e exige TUI, então não dá para
  exercitá-lo por `-p`. Em vez de chutar, extraí a implementação do binário `claude` 2.1.220
  (funções `azb`, `Y5b`, `u0n`). Quatro respostas que definiram o código:
  - **com argumento não há UI nenhuma** (`return null` logo após gravar);
  - `writeFile` comum → **sobrescreve** sem perguntar;
  - `mkdir(dirname, {recursive:true})` → cria a árvore;
  - **sem extensão o CLI acrescenta `.txt`** → o destino tem de terminar em `.md`, senão o
    arquivo aparece em outro caminho e nós esperaríamos para sempre pelo caminho errado.
- **A pegadinha que quase virou bug:** o argumento é `r.trim()` **cru**. Se eu tivesse reusado
  `ClaudeCommand.quote` — como o T-1.13 que eu mesmo escrevi no SPEC v1.3 mandava —, o CLI teria
  criado um arquivo chamado `'/tmp/....md'`, **com aspas no nome**. É o inverso de RNF-08: lá o
  perigo é o shell, aqui quem lê a string é o CLI e proteger contra shell quebra a chamada. O
  T-1.13 foi corrigido antes de virar código.
- **RF-24:** `ClaudeSessionExport` cria o temporário (`createTempFile` já nasce `rw-------` em
  POSIX, e o CLI grava por cima sem mudar o modo — é o que satisfaz RF-25), envia
  `/export <caminho>\r`, sonda o arquivo até deixar de estar vazio e apaga em `finally`.
  Sondagem simples em vez de `WatchService`, marcada com `ponytail:`.
- **RF-26:** botão flutuante em `mouseReleased`, não em `selectionChanged` — durante o arraste a
  seleção muda a cada pixel e o popup piscaria. O `selectionChanged` ficou só para **esconder**.
  `setRequestFocus(false)` para não roubar o foco da sessão.
- **RF-27:** caixa "Saída plana (`--ax-screen-reader`)" nas configurações, desligada por padrão.
  É a alternativa barata a Q-14: mede quanto do incômodo era só a moldura do TUI antes de
  cogitar reescrever a UI. **Efeito colateral a observar:** menos animação pode significar menos
  repintura — e portanto menos DEF-01 no buffer.
- Resultado: **51 testes, 0 falhas**, sem warnings. ZIP de 50 KB às 16:54.
- **Ainda não validado:** T-3.14 a T-3.17. Em especial, se o popup se comporta sob arraste
  rápido e troca de aba.

### 2026-08-01 (tarde/4) — DEF-01 e SPEC v1.3

Primeiro uso real dos dois botões. **O `/export` passou** — o arquivo gerado às 16:23 tem a
conversa uma única vez, sem rodapé. **A cópia do buffer falhou**: traz a conversa duas vezes,
cada uma com banner e barra de status.

- **Causa (DEF-01):** o TUI é Ink e repinta o frame **inteiro** a cada resize da tool window.
  Cada repintura empurra mais uma cópia da conversa para o scrollback. `getText()` está certo —
  o buffer é que tem a conversa repetida. A prova está na própria amostra: o rodapé do primeiro
  bloco marca `⧉ In README.md` e o do segundo, `⧉ In a.txt`. **Dois frames, instantes
  diferentes.**
- **Por que não dá para consertar por heurística:** cortar do último banner em diante só
  funciona enquanto a conversa cabe na tela — passando disso, trunca em silêncio, que é pior.
- **Correção especificada (RF-24):** a cópia passa a usar o `/export` para arquivo temporário,
  lê, joga no clipboard e apaga. Isso responde Q-12, que a v1.2 tinha deixado em aberto.
  **Bloqueado por R-13/T-3.13:** não sei se o argumento `[filename]` aceita caminho absoluto.
  Testar à mão antes de escrever qualquer linha.
- **Botão flutuante na seleção (RF-26):** viável e barato. `TerminalPanel.addSelectionListener`
  é público, `getSelectedText()` já existe. Não há célula→pixel público (`myCharSize` é
  `protected`), então o botão se posiciona pela posição do mouse.
- **Markdown viewer (Q-14): analisado e recusado.** É possível — o CLI tem
  `--print --output-format stream-json --input-format stream-json`. Mas descarta o terminal e
  tudo que ele dá de graça (aprovação de ferramentas, plan mode, slash commands, `--resume`), e
  acopla a um formato JSON sem contrato de estabilidade. Seria outro produto, para ganhar
  apresentação e não capacidade.
- **Correção de fato registrada:** a v1.2 afirmou que não havia como selecionar o buffer inteiro
  no CLASSIC. `TerminalPanel.selectAll()` **é público**. O que não existe é ação registrada que
  o alcance. A redação foi corrigida no SPEC.
- **Lição (Achado 16):** o trade-off nº 1 da v1.2 já dizia "a cópia é o render, não a conversa".
  Nomear o trade-off não é aceitá-lo pelo usuário — quando a entrega é uma aproximação do que
  foi pedido, isso é item de validação, não ressalva.
- Nada foi implementado nesta rodada: só diagnóstico e especificação.

### 2026-08-01 (tarde/3) — Cópia e exportação da sessão (SPEC v1.2)

T-3.1 e T-3.2 aprovados pelo usuário: **a janela funciona e o diff abre no IDE de ponta a
ponta.** Com a premissa central provada na prática, a sessão virou para ergonomia: a extensão
de VS Code tem um ícone que copia o conteúdo da janela, e aqui não havia equivalente.

- **A investigação começou certa desta vez.** Em vez de varrer a plataforma atrás de "como
  copiar", parti do que a nossa factory já devolve: `TerminalWidget`. A interface **já tem**
  `getText()`, e no CLASSIC ela lê scrollback + tela inteira. A funcionalidade custou uma
  chamada. (Contraste com a metodologia do `Esc` na tarde/2 — a lição pegou.)
- **Mas o reuso óbvio era um botão morto.** `Terminal.SelectAll` e `Terminal.CopySelectedText`
  existem no IDE e **não funcionam para nós**: o `update()` das duas exige um `Editor` de
  terminal reformulado, que o JediTerm clássico não tem. Ler o `update()` antes de referenciar
  a ação foi o que evitou entregar um ícone que não faz nada. Virou o Achado 14 do SPEC.
- **RF-21 — "Copiar Sessão".** `readText` → `ClaudeSessionText.normalize` → `CopyPasteManager`.
  A normalização apara espaços à direita e o bloco de linhas vazias abaixo do prompt; sem ela,
  colar traz dezenas de linhas em branco.
- **RF-22 — "Exportar Conversa".** O CLI já tem `/export` ("to a file or clipboard"). O botão
  escreve `"/export\r"` **direto no `TtyConnector`** — `sendCommandToExecute` não serve, porque
  `ShellTerminalWidget.executeCommand` lança `IOException` se já houver texto digitado no
  prompt, que é a regra com um TUI vivo (D-16).
- **Por que dois botões e não um** (D-17): a cópia é o render literal, o `/export` é a
  transcrição limpa, e nenhum substitui o outro. Delegar tudo ao CLI **não era opção**: o
  destino "clipboard" do `/export` depende de `wl-copy`/`xclip`/`xsel`, e **nenhum está
  instalado** nesta máquina (Wayland). Ou seja, o caso principal ficaria sem solução.
- Um tropeço de compilação que vale registrar: **`widget.text` não resolve em Kotlin** —
  `getText()` não é property nesta interface, ao contrário de `ttyConnector`. Chamar como
  função resolve.
- Resultado: **39 testes, 0 falhas** (eram 34). ZIP de 40 KB gerado às 16:12.
- **Ainda não validado:** nada disso foi exercitado no IDE. T-3.10, T-3.11 e T-3.12 são a
  próxima ação, e R-12 (o autocomplete de slash command reagindo ao `\r`) é o ponto de dúvida
  real.

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
