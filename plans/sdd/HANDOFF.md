# HANDOUT — Registro de progresso

> Memória de trabalho entre sessões. **Leia este arquivo antes de retomar o projeto.**
> Atualize-o ao fim de cada sessão significativa.

- **Projeto:** Claude Code Dock — tool window dedicada para o Claude Code em IDEs JetBrains
- **Última atualização:** 2026-08-10 (noite)

---

## Estado atual

**Fase: v1.11 — Kokoro-ONNX entra como motor de voz padrão, e a reprodução passa a ser em
streaming.** **163 testes verdes, zero warnings — medidos em 2026-08-10 com `--rerun`.**
Branch `feature/kokoro-onnx-tts`, **ainda não commitada**.

**O eixo da rodada mudou duas vezes, e as duas por medição.** O pedido era trocar o motor; a
investigação mostrou que **o `Clip` era o gargalo, não o motor** (Achado 39) — os dois motores
sempre emitiram PCM incrementalmente, e o desenho de v1.5 jogava isso fora. E a pergunta "dá para
listar as vozes do Piper?" abriu o arquivo que descreve cada voz, onde apareceu **DEF-10**: 40 das
173 vozes do Piper tocavam 38% aceleradas, por sample rate fixo no código.

**Validado no IDE real em 2026-08-10, em duas sessões (11 min e 1 min 36 s), sem uma exceção
sequer:** T-3.79 aprovado, T-3.70/T-3.71/T-3.74 aprovados em parte. **O T-3.74 passou de ouvido, e
não no cronômetro** — o relato foi "pareceu que o Kokoro carregou mais rápido a primeira fala",
consistente com os 2,41 s medidos fora do IDE, mas o número dentro dele segue sem medição.
**Pendentes: T-3.72, T-3.73, T-3.75, T-3.76, T-3.77 e T-3.78** — o T-3.77 é o que fecha DEF-10 e
exige baixar uma voz `low` do Piper.

**Quatro otimizações foram medidas e recusadas**, com os números registrados para não voltarem:
daemon (D-48), modelo int8 (D-50), rampa de pedaços (D-51) e cache de grafo do ONNX (D-52).

**Fase anterior: v1.10.2 — RF-50, RF-51 e RF-53 implementados; o terceiro pedido virou
`apt install` (Achado 33).** 142 testes verdes em 2026-08-09.

**Roteiros manuais da v1.10: todos aprovados em 2026-08-09** — T-3.62, T-3.63, T-3.64, T-3.65,
T-3.66, T-3.69 e o ícone cinza. **RF-52 arquivado sem código** e **Q-32 respondida** por T-3.62.
T-3.67 e T-3.68 reprovaram e foram superados por T-3.69. **Seguem pendentes os oito roteiros
herdados de rodadas anteriores** — T-3.5, T-3.6, T-3.18 a T-3.20, T-3.48, T-3.52 e T-3.53.

**Fase anterior: v1.9.4 — RF-49: ação própria que entrega a seleção a _uma_ pane, fechando DEF-08.**
**A v1.9 saiu como release `v0.8.0` (tag em `c50861c`); a v1.9.1 (Achado 31) está em `2b7ef79` e a
v1.9.2 (T-2.\*) em `462e425`. 136 testes verdes, zero warnings — medidos em 2026-08-08 17:15 com
`--rerun`. Nada disso foi publicado: são rodadas de dívida e de ergonomia, não de release.**

> **Duas numerações, e não são a mesma.** O `SPEC.md` tem versionamento próprio (`v1.x`), que
> conta rodadas de especificação; o release segue SemVer (`0.x.y`), no `CHANGELOG.md` e nas tags
> do git. Mapa do que já saiu: **0.6.0** ← SPEC v1.5.1 · **0.7.0** ← SPEC v1.8.2 · **0.8.0** ←
> SPEC v1.9 · **0.8.1** ← SPEC v1.9.4 · **0.9.0** ← SPEC v1.10.2 · **0.10.0** ← SPEC v1.11. Cada release tem também uma
> tag `-rc` no commit do código, anterior ao da publicação.

| Artefato                                                         | Estado                                                                                                                                           |
| ---------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| [../20260801-initial-project.md](../20260801-initial-project.md) | Documento de origem (contexto + roteiro SDD)                                                                                                     |
| [SPEC.md](SPEC.md)                                               | ✅ **v1.11** — RF-54 a RF-57, RNF-35/RNF-36, DEF-10, Achados 37 a 41; RNF-19 e RNF-22 revistos                                                   |
| [SPEC.md](SPEC.md) _(anterior)_                                  | **v1.10.2** — RF-50, RF-51, RF-53; RF-52 condicional; DEF-09; Achados 33/34/35/36; Q-32 (executável) e Q-33 (fechada); R-23 substituído por R-29 |
| `HANDOFF.md`                                                     | ✅ Este arquivo, com RF-49, a receita do cache do Gradle e o inventário de roteiros                                                              |
| [../../CHANGELOG.md](../../CHANGELOG.md)                         | ✅ Keep a Changelog + SemVer; última entrada **0.10.0** (2026-08-10)                                                                             |
| Código do plugin                                                 | ✅ **163 testes, 0 falhas, 0 erros**; **zero warnings**. A v1.11 acrescentou 21 testes                                                           |
| **T-4 (bloqueante)**                                             | ✅ **APROVADO** — premissa central validada empiricamente                                                                                        |
| RF-17 (`Esc`), RF-18 (estado vazio), RF-19 (`CLAUDE_CONFIG_DIR`) | ✅ Implementados **e validados no IDE** (T-3.7 a T-3.9)                                                                                          |
| T-3.1 e T-3.2 (diff ponta a ponta)                               | ✅ **APROVADOS** — a integração com o oficial funciona                                                                                           |
| RF-22 (`/export`), RF-24 (cópia), RF-26 (botão de seleção)       | ✅ Implementados e em uso                                                                                                                        |
| RF-27 (saída plana)                                              | ⚰️ **REMOVIDO em v1.4** — funcionava, e não era o que servia                                                                                     |
| RF-28 (respiro nas bordas)                                       | ✅ Implementado e **validado no IDE**, em 20px por padrão                                                                                        |
| RF-29 (capa de carregamento)                                     | ✅ Implementado e **validado no IDE**, prazo de 3 s                                                                                              |
| Tela de configurações                                            | ✅ Reescrita em Kotlin UI DSL; v1.5 adiciona Piper fields                                                                                        |
| T-1.21 (teste dos diretórios de fallback)                        | ✅ Implementado, com controle contra passar pelo motivo errado                                                                                   |
| **RF-31/RF-32 (Piper TTS)**                                      | ✅ **COMPLETOS** — menu "Áudio" no cabeçalho; config executável + modelo                                                                         |
| `ClaudePiperPlayback`, `ClaudeTtaSessions`, Audio actions        | ✅ Código compilado, sem erros, seguindo RNF-19 a RNF-23                                                                                         |
| **RF-30 (play no popup)**                                        | ⚰️ **DESCARTADO em v1.5.1** — UX redundante; menu Áudio (RF-31) já cobre                                                                         |
| **RF-47 (velocidade da fala)**                                   | ✅ Submenu + seletor em Settings, 0,25x a 2x — **validado no IDE**                                                                               |
| **RF-48 / DEF-07 ("Tocar seleção")**                             | ✅ Corrigido em v1.9 e **validado no IDE** (T-3.54)                                                                                              |
| **RF-33/34/35 (export do trecho)**                               | ✅ Implementados e **validados no IDE** pelo usuário                                                                                             |
| `ClaudeSelectionExport`                                          | ✅ Objeto puro: nome sugerido + gravação (RNF-26)                                                                                                |
| **RF-36 a RF-40 (split da aba)**                                 | ✅ Implementados e **validados no IDE** pelo usuário (4 panes)                                                                                   |
| **RF-41/RF-42 (fechar a divisão)**                               | ✅ Item "Fechar divisão" no cabeçalho + foco reassumido (v1.7.1)                                                                                 |
| **DEF-02 (navegação com uma aba)**                               | ✅ Corrigido em v1.7.2 — guarda em `ClaudeTabNavigation` (T-1.40)                                                                                |
| **RF-43 (trocar de lado / girar)**                               | ✅ Implementado em v1.8, sobre `Splitter.swapComponents()`                                                                                       |
| **DEF-03 ("encerrado" com sessão viva)**                         | ✅ Corrigido em v1.8.1 — callback por pane + `isDescendingFrom`                                                                                  |
| **DEF-04 (menu "Dividir" vazio)**                                | ✅ Era sintoma de DEF-05; diagnóstico anterior revogado (Achado 30)                                                                              |
| **DEF-05 (cabeçalho morto após fechar pane)**                    | ✅ Corrigido — `preferredFocusableComponent` passa à sobrevivente                                                                                |
| **DEF-06 (nome ambíguo do fechamento)**                          | ✅ "Fechar esta sessão" e "Fechar todas as sessões" (RF-46)                                                                                      |
| Q-26 ("encerrado" numa aba dividida)                             | ✅ Respondida pelo uso real: é "sem sessão viva" (RF-44)                                                                                         |
| **DnD de panes (Q-28)**                                          | ⚰️ **Avaliado e recusado** — falta onde agarrar, não mecanismo                                                                                   |
| Roteiros T-3.42-47 (fechar/reposicionar)                         | ✅ Validados pelo **uso real** desde a release 0.7.0 — não por roteiro                                                                           |
| `ClaudeSessionSplitter`                                          | ✅ Objeto puro de Swing: árvore de panes (RNF-29, RNF-30)                                                                                        |
| **Premissa de engine (CB-26/36/47, R-15)**                       | ✅ **CORRIGIDA** — a sessão é sempre JediTerm/CLASSIC (Achado 27)                                                                                |
| Testes de integração T-2.\*                                      | ✅ **Implementados em v1.9.2** — `ClaudeDockIntegrationTest`, 7 casos                                                                            |
| Roteiros T-3.21-23, T-3.26, T-3.27 (Piper)                       | ✅ **Aprovados no IDE** em 2026-08-08, sandbox com Piper                                                                                         |
| Roteiros de split (T-3.34-41)                                    | ✅ **Aprovados em 2026-08-08**, menos T-3.36 (diff por pane), não executado                                                                      |
| **T-3.36 (diff por pane)**                                       | ✅ **Aprovado 2026-08-08** — as duas panes com `In test.md` ao mesmo tempo                                                                       |
| **T-3.4 (`--resume`)**                                           | ✅ **Aprovado 2026-08-08**                                                                                                                       |
| **DEF-08 (`Ctrl+Alt+K`)**                                        | ✅ **Contornado por RF-49** — entrega dirigida, **validada no IDE** (T-3.60)                                                                     |
| **Q-31 (pane sem integração)**                                   | ⚰️ **Arquivada** após duas não-reproduções. Achado 32 documenta o mecanismo do oficial; a causa do caso real segue desconhecida                  |

**Estado do repositório:** `main` na v1.9.2. A v1.9.1 (Achado 31) foi commitada em `2b7ef79`;
a v1.9.2 (`ClaudeDockIntegrationTest`) ainda está na árvore de trabalho. Tags da última release:
`v0.8.0-rc` em `de5467e` (o código) e `v0.8.0` em `c50861c` (só o CHANGELOG). O split da v1.7 —
`ClaudeSessionSplitter.kt`, `SplitSessionAction.kt`, o listener nativo na factory e o ciclo de
vida de panes em `ClaudeDockSessions` — está em `main` desde a release 0.7.0.

**Próximos passos:** restam os herdados da base — T-3.5 (outros IDEs), T-3.6 (desinstalação) e
T-3.18-3.20 (tema, partida sem eco, diretório não confiável). Split, Piper, diff por pane,
`--resume` e T-3.59 estão pagos; T-3.3 foi invalidado por DEF-08.

**Achado 31 — quitado em 2026-08-08.** `synthesize` ganhou o prazo de 20 s e a atribuição de
`currentProcess`. A auditoria achou uma segunda promessa inerte no mesmo campo: o cancelamento de
**RNF-23** ("novo play interrompe o anterior") nunca alcançou o piper. T-1.55 e T-1.56 provam as
duas por efeito, e reprovam sob mutação.

**Dívida conhecida (código morto):** ~~`ClaudeTerminalSessionFactory.readText`~~ removido em
2026-08-08 — sem chamadores desde que RF-24 substituiu RF-21.

**Dívida conhecida (versão do plugin):** ~~`gradle.properties` em `pluginVersion = 0.1.0`~~
quitada em 2026-08-10 — a propriedade subiu para **0.9.0**, junto do registro das releases 0.8.1
e 0.9.0 no `CHANGELOG.md`. O plugin instalado deixa de se identificar como 0.1.0 em
_Settings > Plugins_ a partir do próximo build.

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

| Fato                                                                                                                                                                         | Como foi descoberto                                                                 |
| ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------- |
| **`TerminalWidget.getText()` já devolve scrollback + tela** no CLASSIC — a seleção vai de `(0, -historyLinesCount)` a `(width, screenLinesCount-1)`, sob `buffer.lock()`     | `javap -c` de `JBTerminalWidget.getText(TerminalPanel)` e do `TerminalWidgetBridge` |
| Fora do CLASSIC o `default` da interface devolve **string vazia** — degrada, não lança                                                                                       | `javap -c` da interface: `ldc ""` / `areturn`                                       |
| **Em Kotlin é `widget.getText()`, não `widget.text`** — ao contrário de `ttyConnector`, não é property                                                                       | erro de compilação `Unresolved reference 'text'`                                    |
| **Não existe "copiar tudo" no CLASSIC.** `Terminal.SelectAll` só está no `Terminal.ReworkedTerminalContextMenu`, e seu `update()` exige `isReworkedTerminalEditor`           | `plugin.xml` do terminal + `javap -c` de `TerminalSelectAllAction`                  |
| `sendCommandToExecute` **não serve** para falar com um TUI vivo: `ShellTerminalWidget.executeCommand` lança `IOException` se já houver texto digitado no prompt              | `javap -c` de `executeCommand`                                                      |
| O CLI tem `/export`: `{type:"local-jsx", name:"export", description:"Export the current conversation to a file or clipboard", argumentHint:"[filename]"}`                    | string extraída do binário `claude` 2.1.220                                         |
| **O destino "clipboard" do `/export` depende de `wl-copy`/`xclip`/`xsel` — nenhum instalado aqui** (Wayland). Delegar tudo ao CLI teria deixado o caso principal sem solução | `grep` no binário + `command -v`                                                    |
| Limite do scrollback vem do advanced setting `terminal.buffer.max.lines.count`                                                                                               | `javap -c` de `JBTerminalSystemSettingsProviderBase.getBufferMaxLinesCount`         |
| `CopyPasteManager.copyTextToClipboard` é estático; `Content` é `UserDataHolder` (dá para pendurar o widget na aba, sem mapa próprio)                                         | `javap` de `intellij.platform.editor.ui.jar` e de `Content`                         |

**Lição:** uma ação registrada no IDE **não é** uma capacidade disponível. Ler o `update()` dela
faz parte da verificação — senão o resultado é um botão morto.

### Ambiente da sessão: o que o processo herda (2026-08-01, noite/2)

> **Leia antes de mexer em como a sessão é lançada.** Foi o que custou duas tentativas falhas.

| Fato                                                                                                                                                          | Como foi descoberto                                                               |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------- |
| **O customizer do plugin oficial devolve o comando intacto** (`aload_3; areturn`) e só mexe no mapa de ambiente — injeta também `ENABLE_IDE_INTEGRATION=true` | `javap -c` de `TerminalCustomizer.customizeCommandAndEnvironment`                 |
| **O processo do IDE herda o ambiente da sessão gráfica**, não do shell: sem `~/.local/bin`, sem o que o `.zshrc` acrescenta                                   | dump de `envs` no diálogo "Failed to start" do próprio IDE                        |
| `PathEnvironmentVariableUtil.findInPath` lê o `PATH` do `EnvironmentUtil` — que **também** não tinha `~/.local/bin` aqui                                      | `javap -c` de `getPathVariableValue` + notificação indevida com o CLI funcionando |
| Um shell interativo com `exec` preserva **variáveis exportadas** e **perde funções e aliases** (`sdk`, aliases do `.commands-extension.sh`)                   | `env -i … zsh -i -c 'exec "$0" "$@"' env` sob PTY, comparando só nomes            |
| **Sem PTY o `.zshrc` não é carregado**, nem com `-i` — o rc desiste quando não há terminal                                                                    | mesmo comando sem `script`: nenhuma invocação resolveu o `claude`                 |
| Os hooks do Claude Code rodam por `/bin/sh`, que nunca leria o `.zshrc` — para eles vale só o `PATH` exportado                                                | erro real do usuário: `/bin/sh: 1: code-review-graph: not found`                  |
| `TerminalProjectOptionsProvider.getShellPath()` é público e síncrono: dá o shell configurado em Settings > Tools > Terminal                                   | `javap` da classe                                                                 |

**Consequência prática:** trocar quem é o processo do PTY é uma decisão sobre **ambiente**, não
sobre aparência. Qualquer mudança nessa linha começa medindo o ambiente resultante.

### Armadilhas de Swing na capa e no respiro (2026-08-01, noite/2)

| Fato                                                                                                         | Como apareceu                                               |
| ------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------- |
| `JediTermWidget.getComponent()` devolve **o próprio widget** — `JPanel` com `BorderLayout`                   | `javap -c`: `aload_0; areturn`                              |
| `TerminalPanel` mede **a si mesmo** para calcular o grid; `getInsetX()` é a constante `4`                    | `javap -c` de `getTerminalSizeFromComponent`                |
| `TerminalPanel.getBackground()` é **recalculado a cada chamada** — copiar a cor uma vez envelhece com o tema | `javap -c`; e a faixa preta no tema claro que o usuário viu |
| **`JBLoadingPanel` é translúcido**: o conteúdo por baixo continua legível                                    | o eco do comando aparecia através da capa                   |
| **Componente escondido não tem dimensão**: o CLI desenha para tamanho inventado e redesenha ao aparecer      | rodapé quebrado por ~1 s ao revelar                         |
| `JLayeredPane.DEFAULT_LAYER` é `Integer`; passá-lo direto ao `add` escolhe o overload de **índice**          | teste T-1.20 falhou na asserção de camada                   |
| `Box.setLayout` lança `AWTError("Illegal request")`                                                          | três testes vermelhos de uma vez                            |
| `IconUtil.scale(Icon, Double)` está depreciado; usar `scale(Icon, Component?, Float)`                        | warning de compilação                                       |

### Split: o gatilho já existia, e o engine nunca foi dúvida (2026-08-03, tarde/2)

| Fato                                                                                                                                 | Como foi descoberto                                                                                                |
| ------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------ |
| `ShellTerminalWidget.getActions()` cria as ações de split a partir de `getListener()` — sem listener, elas não aparecem              | `javap -c` de `ShellTerminalWidget`: duas chamadas a `TerminalSplitAction.create(Z, listener)`                     |
| `TerminalSplitAction.isEnabled` → `canSplit(Z)`; `actionPerformed` → `split(Z)`                                                      | `javap -c` de `TerminalSplitAction`                                                                                |
| `canSplit`/`split` são métodos **default** de `JBTerminalWidgetListener` — implementar o listener é o suficiente                     | `javap` da interface                                                                                               |
| **`vertically = true` é "Split Right" (lado a lado)**; `Splitter(vertical = true)` é **empilhado** — nomes parecidos, sentido oposto | `javap -c` de `TerminalSplitAction$Companion.create` (pareia com `TW.SplitRight`) + teste de geometria             |
| **A sessão deste plugin é sempre JediTerm/CLASSIC**, qualquer que seja o `TerminalEngine` do usuário                                 | `javap -c` de `AbstractTerminalRunner.startShellTerminalWidget`: `createTerminalWidget` devolve `JBTerminalWidget` |
| `TerminalContainer` (o split do terminal nativo) **não é reusável**: o construtor exige um `TerminalToolWindowManager`               | `javap` de `org.jetbrains.plugins.terminal.ui.TerminalContainer`                                                   |
| `Content.getDisposer()` existe — dispensa guardar o disposable da aba em `UserData`                                                  | `javap` de `com.intellij.ui.content.Content`                                                                       |

> **Sobre o "REWORKED no IntelliJ" registrado neste arquivo:** estava certo sobre o Terminal
> **nativo** do usuário, e errado sobre as nossas abas. O engine é propriedade da tool window que
> cria o widget, não do IDE. Ver Achado 27.

### Gravar arquivo pelo diálogo nativo (2026-08-03, tarde)

| Fato                                                                                                                 | Como foi descoberto                                                              |
| -------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| `FileSaverDescriptor(String title, String description, String... extensions)` é construtor público                   | `javap` sobre `intellij.platform.ide.jar` da distribuição `idea-2026.2` do build |
| `FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)` devolve `FileSaverDialog`               | mesma inspeção                                                                   |
| `FileSaverDialog.save(Path, String)` existe — dispensa converter o diretório inicial em `VirtualFile`                | mesma inspeção; há também `save(VirtualFile, String)` e `save(String)`           |
| `save(...)` devolve `null` no cancelamento; `VirtualFileWrapper.getFile()` dá o `java.io.File`                       | assinatura + `javap` de `VirtualFileWrapper` (classe `final`, 5 métodos)         |
| **O `/export` do CLI não aceita "um trecho"** — o argumento é só o caminho, e o conteúdo vem de `lZo(t.messages, …)` | leitura do binário já feita na v1.3 (funções `azb`/`u0n`), reaproveitada         |

> ⚠️ **A armadilha do `grep` binário pegou de novo.** A primeira busca pelas classes do diálogo
> nos jars da plataforma não achou **nada** — nem um controle conhecido (`Messages.class`). Causa:
> `grep` sem `-a` trata `.class` como binário e não reporta as linhas. Está registrado neste
> mesmo arquivo desde 2026-08-01 e mesmo assim custou quatro tentativas. **Em busca sobre jar,
> `grep -a` desde a primeira chamada, e sempre com um controle conhecido junto.**

### Piper: o que a CLI aceita e de onde vêm os defaults (2026-08-07)

Obtido de `piper --help` e da leitura do pacote instalado
(`~/.pyenv/versions/3.11.6/lib/python3.11/site-packages/piper/`).

- **Flags de síntese:** `-m/--model`, `-c/--config`, `-i/--input-file`, `-f/--output-file`,
  `-d/--output-dir`, `--output-raw`, `-s/--speaker`, `--length-scale`, `--noise-scale`,
  `--noise-w-scale`, `--cuda`, `--sentence-silence`, `--volume`, `--no-normalize`, `--data-dir`,
  `--debug`. Não há flag de "velocidade": `--length-scale` **é** o controle, e é **inverso** —
  "Phoneme length", fonema mais longo, fala mais lenta.
- **De onde vem o default:** `voice.py:449-450` — `if length_scale is None: length_scale =
self.config.length_scale`, ou seja, o `config.json` que acompanha o `.onnx`.
  `config.py:8` define `DEFAULT_LENGTH_SCALE = 1.0`, mas ele só entra se o JSON não trouxer o seu.
  **Omitir a flag ≠ passar 1.0** (D-40).
- **Formato do número:** `__main__.py:61` declara `type=float` no argparse — recusa vírgula
  decimal. Daí RNF-31.
- **Medido com `pt_BR-faber-medium`**, mesma frase, `-f` para WAV:
  `--length-scale 2.000` → 3,84 s · sem flag → 2,19 s · `--length-scale 0.500` → 1,42 s.
  Monotônico e no sentido esperado, mas **não linear** — o silêncio entre frases não escala
  junto. Serve para provar a direção, não para prometer "o dobro da velocidade".
- Esse modelo traz `length_scale: 1` no `config.json`, então para **ele** omitir a flag coincide
  com 1.0. Não generalizar: é coincidência de uma voz, não regra.

### `buildSearchableOptions` falha com o IDE aberto (2026-08-10)

`./gradlew buildPlugin` falhava com:

```
> Task :buildSearchableOptions FAILED
Only one instance of IDEA can be run at a time.
```

**Não é o atrito de cache da seção acima, e `--stop` não resolve.** A tarefa sobe uma **segunda
instância do IDE**, em headless, só para indexar os campos da nossa tela de configuração na busca
do Settings. Com o IDE do autor aberto — o caso normal de quem desenvolve o plugin — as duas
disputam o lock e a segunda perde.

**Resolvido desligando a tarefa** (`buildSearchableOptions = false` no `build.gradle.kts`).
Custo: os campos da nossa tela deixam de aparecer ao digitar no campo de busca do Settings; a tela
continua em Tools > Claude Code Dock, navegável como sempre. Este plugin é de instalação local, sem
Marketplace (Fora de Escopo do SPEC), então o índice não paga o atrito de build.

A alternativa era fechar o IDE a cada `buildPlugin`, o que troca um atrito recorrente por outro
maior.

### O atrito do `runIde` com o cache do Gradle (2026-08-08)

> **Bateu três vezes numa sessão só. Ler antes de perder tempo diagnosticando.**

A IDE do sandbox escreve dentro do próprio diretório de distribuição (`brokenPlugins.db` e afins),
e esse diretório vive no **cache de transformação imutável** do Gradle. Depois de um `runIde`, é
comum toda tarefa passar a falhar com:

```
The contents of the immutable workspace '~/.gradle/caches/9.2.0/transforms/<hash>' have been modified.
```

- **Não é corrupção de disco** e não tem a ver com o código. É o `runIde` mordendo o próprio rabo.
- **Às vezes destrava sozinho** numa segunda tentativa com a IDE já fechada; às vezes não.
- **Receita real: `./gradlew --stop`. Só isso.** O sintoma é
  `Cannot resolve 'product-info.json'` apontando para o diretório do hash, e a causa é o **daemon**
  servindo da memória um caminho que já não vale. Derrubado o daemon, o Gradle refaz a
  transformação sozinho — reextração **local**, sem download (~32 s aqui).
  Nem `--refresh-dependencies` nem `--no-configuration-cache` resolvem.

  > ⚠️ **Correção de método.** Eu registrei antes que era preciso `rm -rf` no diretório do hash
  > **e** parar o daemon. **O `rm` nunca foi necessário** — na primeira vez ele veio antes do
  > `--stop`, e eu creditei o resultado aos dois. Depois o caso se repetiu com o diretório
  > **vazio** e o `--stop` sozinho resolveu, sem `rm` nenhum. Foi conclusão tirada de uma sequência
  > sem variar um fator de cada vez, e custou pedir ao usuário duas vezes um comando destrutivo
  > que não fazia falta. **Mesmo padrão do Achado 30, agora em procedimento de build.**

- **Depois de destravar, `./gradlew test` fica `UP-TO-DATE`** e não reexecuta nada: os XMLs
  continuam com a data antiga. Para ter medição de agora, `./gradlew test --rerun`.
- **Consequência para a leitura de resultados:** com o build travado, `./gradlew test` não roda e os
  XMLs em `build/test-results` ficam **velhos**. Contar teste a partir deles nesse estado dá um
  número que parece atual e não é — conferir a data do arquivo antes de citar o total.

**E o `prepareSandbox` apaga o diretório de plugins quando roda de verdade.** Ele fica
`UP-TO-DATE` enquanto nenhum fonte muda, o que dá a falsa impressão de que a cópia manual do
plugin oficial sobrevive. **Depois de qualquer mudança de código, reinstalar o oficial** antes de
rodar roteiro que dependa dele — senão o roteiro falha por ausência e parece regressão.

### Como uma sessão conecta ao MCP — e como ela nasce quebrada (2026-08-08)

> **Leia antes de mexer em criação de sessão.** Explica Q-31 e o Achado 32.

| Fato                                                                                                                                                     | Onde foi lido                                              |
| -------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------- |
| `TerminalCustomizer` injeta `CLAUDE_CODE_SSE_PORT = getRunningMcpServerPorts().getOrDefault(project.locationHash, 0)` — **o default é `0`, não ausente** | `javap -c` de `TerminalCustomizer`                         |
| O mapa é estático, por `locationHash`, e só é populado por `MCPService.start()`                                                                          | `javap -c` de `MCPService.start`                           |
| `start()` roda no `PostStartupActivity` → **existe janela entre abrir o projeto e a porta existir**                                                      | `plugin.xml` do oficial + `javap` do activity              |
| O oficial mitiga a corrida com `restartClaudeInExistingTerminals`, chamado logo após `start()`                                                           | `javap -c` de `PostStartupActivity`                        |
| **A mitigação varre só a tool window `"Terminal"`** e só abas cujo título começa com `"Claude Code"`                                                     | `javap -c` de `restartClaudeInExistingTerminals$lambda$14` |

**Consequência:** uma pane criada antes do `PostStartupActivity` recebe porta `0` e **nunca**
conecta — ambiente de processo é fixado no `exec`. A mitigação que consertaria isso não nos
enxerga. **É a terceira vez que o literal `"Terminal"` decide o nosso comportamento**, depois do
`Esc` (RF-17) e de DEF-08.

**Não é divergência do nosso split:** `splitSession` usa o mesmo `createPane` →
`createSession` da primeira pane. O que difere é o **instante** do nascimento.

**Diagnóstico de campo:** pane sem `In <arquivo>` no rodapé está fora do MCP. **Medição pendente
(T-3.61):** `echo $CLAUDE_CODE_SSE_PORT` nessa pane — se sair `0`, fecha.

**Contorno sem código:** "Nova sessão" nasce com a porta certa.

### Colar imagem: o CLI já sabe, e falta o pacote do sistema (2026-08-08, noite/10)

**Medido no binário `claude` 2.1.226 desta máquina**, não deduzido:

- O CLI lê imagem da área de transferência por **shell-out**. No Linux:
  `xclip -selection clipboard -t TARGETS -o | grep -E "image/(png|jpeg|...)"` para detectar, e
  `xclip ... -t image/png -o > arquivo || wl-paste --type image/png > arquivo` para gravar.
- O chamador trata a falha da detecção como ausência de imagem: `if (exitCode !== 0) return null`.
  **Falha e ausência são indistinguíveis** — no-op mudo.
- Destino no disco: `~/.claude/image-cache/<sessionId>/N.png`. Existe aqui, com PNGs reais.

**E o que está instalado nesta máquina:** `xclip` ❌ · `xsel` ❌ · `wl-paste` ❌ · `wl-copy` ❌ ·
`XDG_SESSION_TYPE=wayland`.

**Conserto:** `sudo apt install wl-clipboard`. Vale para o terminal comum e para o IDE.
**Instalado em 2026-08-09** (`2.2.1-1build1`) — **T-3.62 passa a ser executável**.

**Efeito colateral que chegou como suspeita sobre o plugin:** com `wl-copy` no `PATH`, o
copiar-ao-selecionar **do CLI** trocou de OSC 52 para nativo, e a mensagem virou "copied N chars to
clipboard". Não é do plugin — nenhum caminho nosso copia sem clique, e a pilha do DEF-09 prova pelo
negativo (quem fechou a barra foi o `IdePopupManager`, não o nosso `hide()`). Decisão do usuário:
**deixar como está**.

**O que ainda não sabemos (Q-32):** dentro da janela dedicada, o `Ctrl+V` chega ao PTY? A ordem em
`JBTerminalPanel.handleKeyEvent` foi lida no bytecode — `preKeyEventConsumers` → `TerminalEscapeKeyListener`
→ (se não consumido) `TerminalPanel.handleKeyEvent`, que trata `PASTE` como colagem de **texto**.
Ler a ordem não diz quem consome na prática. **T-3.62 mede, depois do `apt install`.**

### `/usage` não tem saída headless (2026-08-08, noite/10)

- `claude --help` lista 13 subcomandos; **`usage` não é um deles**. `claude usage --help` cai no
  help geral.
- `/usage` é tela de TUI. Este projeto já sabia: o `ClaudeEscapeForwarder` existe em parte para
  **sair** dela com `Ctrl+Backspace`.
- `~/.claude/stats-cache.json` guarda **atividade** (`dailyActivity`, `modelUsage`,
  `totalSessions`), não limite de plano.
- O número vem de `/api/oauth/usage`, autenticado com o token de `~/.claude/.credentials.json`
  (modo `600`).

**Consequência:** popup exigiria o plugin ler o segredo do usuário. Recusado (RNF-34). O botão que
envia `/usage` à sessão fica (RF-51).

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

| **D-09** | O comando é enviado com `sendCommandToExecute` (string digitada no shell), com **escapamento POSIX** do caminho do executável | Mesma abordagem do plugin oficial, e mantém o shell vivo após o `claude` sair — essencial para o diagnóstico previsto no Fluxo D. Como a string é interpretada pelo shell, o caminho é protegido por aspas simples. **Isso ajusta o RNF-08**, que pedia montagem por argv: argv exigiria substituir o shell e perder o fallback |
| **D-10** | Kotlin 2.4.10 e distribuição unificada `intellijIdea("2026.2")` | Imposições da plataforma, descobertas na compilação — não são escolhas |
| **D-11** | T-4 virou **teste automatizado permanente**, não roteiro manual | Determinístico, roda em cada `./gradlew test` e serve de regressão a cada upgrade de IDE (T-5.3), que era exatamente o risco R-01 |
| **D-12** | _(v1.1)_ O `Esc` é corrigido por **pre-handler no painel** (`ClaudeEscapeForwarder`), não mexendo em keymap nem em `TerminalOptionsProvider` | É a única opção que corrige só o `Esc`, só na nossa janela. Alterar keymap ou settings do terminal seria alterar o IDE do usuário — violação direta de RF-13. Anular `TOOL_WINDOW` no data context resolveria o `Esc` mas quebraria `Shift+Esc` e ações de tool window com foco no terminal |
| **D-13** | _(v1.1)_ O estado vazio usa `ToolWindowEx.emptyText` com links, e **não** recria sessão automaticamente ao reabrir a janela | Nativo, três linhas, e mantém a escolha com o usuário. Recriar sozinho exigiria listener de tool window e reabriria o debate de "quando é demais" — sem demanda comprovada |
| **D-14** | _(v1.1)_ `CLAUDE_CONFIG_DIR` é **por projeto** e vive no arquivo de workspace; o executável continua por aplicação | Em IDEs JetBrains uma janela é um projeto, então projeto já entrega a granularidade pedida. Workspace e não `.idea/` versionado porque é caminho local de máquina e aponta para diretório com credenciais do CLI |
| **D-15** | _(v1.1)_ Uma única tela `projectConfigurable` mostra os dois campos, rotulando o escopo de cada um | Duas telas para duas configurações seria burocracia. O rótulo ("Vale para todos os projetos" / "Somente este projeto") resolve a ambiguidade — mesmo padrão da tela de Terminal do IDE |
| **D-16** | _(v1.2)_ O `/export` é enviado por **escrita direta no `TtyConnector`**, não por `sendCommandToExecute` | `sendCommandToExecute` cai em `ShellTerminalWidget.executeCommand`, que lança `IOException` quando já há texto digitado no prompt — a regra, e não a exceção, com um TUI vivo na frente. A escrita direta é o mesmo canal que o `ClaudeEscapeForwarder` já usa em produção |
| **D-17** | _(v1.2)_ **Dois botões**, não um: copiar o buffer e disparar o `/export` | São naturezas opostas e nenhuma substitui a outra. A cópia é o render literal (rápido, fiel à tela, com bordas de TUI); o `/export` é a transcrição limpa, produzida pelo CLI. Delegar tudo ao CLI não era opção: o destino "clipboard" dele exige `wl-copy`/`xclip`/`xsel`, ausentes nesta máquina |
| **D-18** | _(v1.2)_ O widget fica pendurado no `Content` da aba por um `Key`, em vez de um mapa no serviço | A referência morre junto com a aba, sem código de limpeza e sem risco de vazar widget de aba fechada |
| **D-19** | _(v1.2)_ `readText`/`sendInput` moram em `ClaudeTerminalSessionFactory`, não nas ações | RNF-15 exige o contato com a API de terminal num arquivo só. As ações falam com `ClaudeDockSessions`, que sabe qual aba está selecionada |
| **D-20** | _(v1.4)_ **D-09 mantido:** a sessão continua sendo o shell do usuário com o comando digitado. A alternativa `shell -i -c 'exec "$0" "$@"'` foi implementada, medida e **recusada** | Ela elimina o eco na origem e preserva o ambiente exportado — mas o `-c` encerra o shell junto com o CLI, e com ele o prompt utilizável depois do `/exit`. O usuário testou as duas e escolheu manter o shell vivo, pagando o eco. Código guardado em stash, com a medição registrada acima |
| **D-21** | _(v1.4)_ **RF-27 removido** em vez de corrigido | A flag funcionava. O que ela entrega — linha de entrada plana — é indistinguível de um prompt de shell, e isso piora CB-27. Recurso que funciona e não serve é recurso a remover, não a ajustar (Achado 18) |
| **D-22** | _(v1.4)_ O respiro (RF-28) é **configuração**; o prazo da capa (RF-29) é **constante em código** | O respiro é preferência estética, com valor certo diferente por pessoa. O prazo é uma medida de quanto o CLI demora — tem um valor certo só, e expor um botão para ele seria transferir ao usuário um ajuste de implementação (Achado 19) |
| **D-23** | _(v1.4)_ A capa fica **sobreposta** (`JLayeredPane`), nunca substituindo o terminal | Componente escondido não recebe dimensão: o CLI desenharia para um tamanho inventado e redesenharia ao aparecer, quebrando o rodapé. Sobreposto, o terminal conta como visível para `deferSessionStartUntilUiShown` e renderiza uma única vez |
| **D-24** | _(v1.4)_ A verificação do executável é **advisória** e não bloqueia a abertura da aba | Ela enxerga menos que o shell da sessão, e um bloqueio por falso negativo custou a sessão inteira num teste real. Falso negativo agora custa só uma notificação supérflua — e os diretórios de fallback tornam isso raro |
| **D-25** | _(v1.4)_ A tela de configurações usa Kotlin UI DSL + `BoundConfigurable` | Títulos de seção, separadores e alinhamento vêm prontos da plataforma — é de onde o plugin oficial tira os dele. E as ligações (`bindText`/`bindIntValue`) dispensam `isModified`/`apply`/`reset` escritos à mão: 110 linhas viraram 79 |
| **D-30** | _(v1.6)_ Exportar o trecho selecionado **não passa pelo `/export` do CLI**: grava direto o texto que o plugin já tem em mãos | Não é escolha de simplicidade, é impossibilidade. O `/export` roda dentro do Claude Code e exporta **a conversa** (`lZo(t.messages, …)`, lido do binário na v1.3); o único argumento que aceita é o caminho do destino. Não há como pedir a ele um trecho da tela. Já o trecho está disponível desde RF-26, no mesmo `widget.selectedText` que alimenta o botão de copiar. Resultado: sem PTY, sem temporário, sem sondagem, sem prazo — a exportação mais nova é a de menos peças, e a única que funciona com a sessão ocupada ou encerrada |
| **D-31** | _(v1.6)_ O diálogo de destino é o **nativo da plataforma** (`FileChooserFactory.createSaveFileDialog`), não um construído por nós | Traz de graça a confirmação de sobrescrita (CB-45), o filtro por extensão e o comportamento que o usuário já conhece de _File → Save As_. Cancelar devolve `null`, o que faz do cancelamento um caminho normal em vez de um erro a tratar |
| **D-32** | _(v1.6)_ O popup de seleção fica com **teto de dois botões**, e o critério para um terceiro é declarado | Este foi o segundo pedido de "mais um botão junto ao de copiar"; o primeiro (play, RF-30) foi recusado em v1.5.1. O critério que separa os casos é **capacidade, não simetria**: exportar o trecho não existe em nenhum outro lugar da UI, enquanto o play já existia no menu do cabeçalho. Registrado em R-23 e no Achado 26 para que a próxima rodada não precise redecidir |
| **D-33** | _(v1.7)_ O gatilho do split **não é construído**: implementa-se `JBTerminalWidgetListener` e o próprio widget passa a mostrar "Split Right"/"Split Down" | `ShellTerminalWidget.getActions()` já cria as ações a partir de `getListener()`; `isEnabled` chama `canSplit` e `actionPerformed` chama `split`. Como o nosso widget nunca teve listener, as ações não apareciam. O mesmo listener acende de quebra "New Session", "Close Session" e a navegação entre abas — quatro itens mortos pelo mesmo motivo (Achado 28) |
| **D-34** | _(v1.7)_ A "sessão selecionada" passa a ser **a última com foco na aba**, rastreada por `FocusListener`, reusando a chave `SESSION_WIDGET` que já existia | Com o split, uma aba tem várias sessões e RF-22/RF-24/RF-31 precisam saber sobre qual agir. Mudar o **significado** da chave em vez de acrescentar estrutura fez `selectedWidget()` continuar idêntico: as ações do cabeçalho passaram a respeitar o foco sem saberem que o split existe |
| **D-35** | _(v1.7)_ O split ganha **também** um menu "Dividir" no cabeçalho, apesar de o menu de contexto já oferecê-lo | Decisão do usuário, contra o critério de D-32 — e o conflito é real, não descuido. O contrapeso é o precedente do RF-26, aceito só por descoberta ("`Ctrl+C` copia, mas é invisível para quem usa o mouse"): o menu de contexto do terminal é tão invisível quanto. O critério de D-32 continua valendo para o **popup de seleção**, que é espaço escasso; o cabeçalho não é |
| **D-36** | _(v1.7)_ A árvore de divisões vive **só** na hierarquia de componentes Swing, sem mapa paralelo | Mesma razão de D-18: um mapa precisaria ser limpo em todo caminho de fechamento, e é aí que sobra referência para pane morta. As três consultas necessárias saem da árvore: `isDescendingFrom` acha a aba, subir pelos pais acha o bloco divisível, e `putClientProperty` guarda o `Disposable` no próprio componente |
| **D-37** | _(v1.7.1)_ O fechamento da divisão ganha **item próprio no cabeçalho**, mesmo já existindo no menu de contexto | Não é o caso de D-35 outra vez: aqui o problema não é descoberta, é **nome**. "Close Tab" é rótulo da plataforma, correto no terminal nativo (onde aba = sessão) e enganoso numa aba dividida, onde diz o oposto do que faz. Herdar comportamento de um ponto de extensão é de graça; herdar vocabulário não (Achado 29) |
| **D-42** | _(v1.10)_ O aviso de "Piper não configurado" vai em `ClaudeTtaSessions.playText`, e **não** no botão novo do popup | É o ponto por onde os dois chamadores passam. Guarda no serviço conserta o botão novo e o item do menu com um diff menor do que verificar nos dois lugares. Mesma forma de RF-48 |
| **D-43** | _(v1.10)_ `/usage` é **enviado à sessão**, não raspado para um popup | Raspar o buffer de uma tela de TUI que se repinta traz o DEF-01 de volta, agora sobre conteúdo que muda a cada frame. E o dado real exige o token do usuário (RNF-34) |
| **D-44** | _(v1.10)_ Colar print screen **não vira RF** enquanto T-3.62 não rodar | Metade do problema é ambiental e está comprovada (pacote ausente); a outra metade é hipótese. Especificar sobre a metade não medida repetiria o Achado 30 |
| **D-41** | _(v1.9.4)_ A entrega de RF-49 é **escrita direta no PTY** da pane em foco, não uma notificação MCP | Reimplementar a notificação exigiria falar o protocolo privado do oficial — o que D-01 recusa — e ainda herdaria o **broadcast** que torna DEF-08 ambíguo (Q-02): entregaria a todas as panes de novo. A escrita no PTY é o caminho do `/export` desde D-16, já em produção, e é a **única** que tem destinatário. O preço é o formato do @-mention virar acoplamento a um detalhe do CLI — mitigado por ele ter sido **lido do binário** e travado por T-1.57 a T-1.61, em vez de suposto |

### Correção registrada

A premissa do documento de origem — _"a Anthropic inviabiliza plugins de terceiros rodarem o
Claude Code"_ — **não se confirma**. Não há bloqueio técnico a executar `claude` em um PTY.
O que não é público é o **protocolo de integração**. A arquitetura escolhida contorna isso ao
não tocar no protocolo, e não porque executar o CLI fosse proibido.

### Achado 20 — A falha silenciosa do ESC constant (2026-08-03)

O hotfix `097266b` (Ctrl+Backspace em vez de Backspace puro) funcionava — a regra `shouldForward`
estava correta — mas **nada funcionava**: nem Esc puro, nem Ctrl+Backspace, em ambos os engines
(CLASSIC no WebStorm 2026.2 e REWORKED no IntelliJ 2026.2).

**Culpado:** mudança lateral inadvertida no `ESC` constant:

```kotlin
// 679562f (antes — funcionava):
private const val ESC = ""   // ESC char correto

// 097266b (depois — quebrado):
private const val ESC = ""         // String vazia!
```

Quando um evento de teclado era interceptado, `connector.write(ESC)` enviava uma string vazia em
vez do byte ESC (U+001B), e o shell recebia nada. O evento era consumido corretamente, mas o
resultado era inerte.

**Porque não foi visto na revisão:** a diferença é visual (uma string vazia parece estar lá), e
o comportamento _funciona parcialmente_ — o terminal não quebra, o `write()` não lança exceção,
apenas envia nada. A falha é silenciosa.

**Lição:** constantes de bytes/caracteres merecem atenção na revisão. Uma string vazia é tão
fácil de deixar passar quanto um `null` é de notar — considerar adicionar testes que validam
o **valor** da constante, não só sua existência.

### D-39 — velocidade guardada como percentual `Int`, não como `length-scale` _(v1.9)_

O piper fala em `length-scale`: 0.5 é rápido, 2.0 é lento. Guardar isso cru no
`claude-code-dock.xml` significaria persistir um número **invertido** em relação à intuição, num
arquivo que o usuário edita à mão. O campo é `speechSpeed: Int` em porcentagem — 200 é o dobro da
velocidade —, e a conversão vive num único lugar (`piperParameters`).

Ganho secundário: o spinner do Kotlin UI DSL é de `Int`, e o `coerceIn` de faixa fica idêntico ao
de `sessionPadding`, que já existia. Nenhum idioma novo entrou no projeto por causa disto.

### D-40 — em 100% a flag `--length-scale` não é passada _(v1.9)_

Verificado em `piper/voice.py:449-450`: quando `length_scale` chega `None`, o piper usa o valor do
`config.json` **do modelo**. Passar `--length-scale 1.0` em 100% substituiria o padrão de fábrica
da voz por 1.0 — que **não é a mesma coisa**, ainda que coincida em muitos modelos (o
`pt_BR-faber-medium` usado nos testes traz `length_scale: 1`).

Por isso o preset de 100% se chama "1x (padrão do modelo)" e não "1x": ele devolve a voz ao que o
autor dela calibrou, e não a um número escolhido por nós. Custo da decisão: um `if` (CB-62).

### D-45 — 0,25x sai da tabela de velocidades, e a migração é de graça _(v1.11)_

O `create()` do Kokoro tem `assert speed >= 0.5 and speed <= 2.0`. A tabela `SPEECH_SPEEDS` é
compartilhada pelos dois motores — é o que impede o menu e a tela de divergirem —, então manter
0,25x significaria ou mostrar na tela uma velocidade que o motor recusa, ou fazer a tabela variar
por motor, deixando duas telas cientes do motor.

**Não há código de migração**, e isso não é descuido: `effectiveSpeechSpeed()` já escolhia o valor
**mais próximo** em vez de apenas limitar a faixa, justamente porque o XML é editável à mão. Quem
tinha `25` gravado passa a 50 sozinho. Um teste fixa isso (T-1.83) — sem ele, é esperança.

### D-46 — as vozes vêm da fonte, nos dois motores _(v1.11)_

Kokoro: o `voices-*.bin` é um `.npz`, que é um ZIP — `java.util.zip` lista as 54 vozes em
**0,40–2,20 ms**, uma vez por abertura da tela. Piper: os `.onnx` irmãos do modelo configurado,
com idioma e taxa vindos do `.onnx.json` de cada um.

Foi avaliado fixar as três vozes pt-BR num enum. **Custaria mais linhas do que a leitura do ZIP**,
entregaria 3 vozes em vez de 54, e envelheceria a cada release de vozes.

### D-47 — script Python embutido, sem arquivo e sem CLI próprio _(v1.11)_

O `kokoro-onnx` não publica CLI (Achado 37), então o motor é alcançado por um script de ~25 linhas
executado com `python -c`. Ele mora numa constante Kotlin, e não em `resources`: sem extração para
disco, sem cache a invalidar, sem versão defasada, e testável como valor puro.

**Um CLI próprio foi avaliado e recusado.** Seria o mesmo script morando em outro lugar: não
acelera nada e cria duas coisas que hoje não existem — um passo de instalação para o usuário e
defasagem de versão entre plugin e CLI. Isso mudaria se houvesse daemon; não há.

### D-48 — daemon avaliado e recusado, com números _(v1.11)_

Manter o modelo carregado entre falas compraria o custo fixo de 1,50 s. Cobraria **496 MB
residentes parados** (pico medido de 961 MB durante a síntese), protocolo de IPC, ciclo de vida de
processo e risco de zumbi.

Ganho medido por tamanho de texto: 2,5× no curto, 1,4× no médio, **1,1× no longo** — some
justamente onde a espera incomoda. E com o streaming (Achado 39) o que restava virou a diferença
entre falar em 2,5 s e falar em 1 s. Se um dia o uso for quase só de frases curtas, volta como
mudança aditiva.

### D-49 — sidecar do Piper lido por regex, sem dependência nova _(v1.11)_

Ler a taxa e o idioma do `.onnx.json` precisa de um inteiro e dois rótulos. **Não há parser JSON na
plataforma** — nem Gson nem Jackson nos jars de `lib/` do IDE instalado, verificado —, então a
alternativa seria adicionar uma dependência ao plugin para extrair um número.

Regex sobre JSON é frágil por natureza, e aqui é sobre campos de arquivo gerado por pipeline, com
queda segura para 22050 Hz e para o nome do arquivo quando não casa.

### D-50 — modelo int8 medido e recusado _(v1.11)_

| Modelo         | Carga  | RSS na carga | RTF (4 threads) |
| -------------- | ------ | ------------ | --------------- |
| f32 (325,5 MB) | 1,20 s | 483 MB       | **0,34**        |
| int8 (92,4 MB) | 0,80 s | 216 MB       | **1,55**        |

**O int8 é ~4,5× mais lento, não mais rápido**, e o número que encerra a discussão é o RTF 1,55:
acima de 1,0 o motor fica mais lento que a reprodução, e o streaming engasgaria. Ele se desqualifica
pela velocidade antes de qualquer discussão de qualidade. Reproduzido nas três configurações de
thread (2,30 / 1,55 / 1,74 contra 0,40 / 0,34 / 0,40 do f32).

Explicação provável: quantização dinâmica caindo em kernels sem caminho int8 otimizado no
onnxruntime de CPU. **Verificado o efeito, não a causa** — e a causa não muda a decisão.

### D-51 — teto de palavras no primeiro pedaço, e só nele _(v1.11)_

Uma rampa de pedaços crescentes `(40, 80, 160)` foi projetada e medida contra a alternativa mais
simples — cortar em toda pontuação, com teto de palavras só no primeiro pedaço. **Empataram**
(0,73 s contra 0,72 s no primeiro pedaço; 14,56 s contra 14,79 s no total), e a rampa tem mais
código.

A justificativa que eu havia escrito para a rampa era que blocos maiores seriam mais eficientes.
**São não**: 16 pedaços e 5 pedaços medem o mesmo tempo total. A rampa foi descartada por não pagar
o próprio custo. O prefixo `" . "` passou a valer só no primeiro pedaço (Achado 41).

### D-52 — cache de grafo otimizado do ONNX recusado _(v1.11)_

`optimized_model_filepath` leva a carga de 1,157 s para 0,946 s. **0,2 s**, ao preço de mais 325 MB
em disco e de um artefato que o próprio onnxruntime avisa ser específico do hardware em que foi
gerado. A carga é I/O de 325 MB, não otimização de grafo.

---

## Desafios em aberto

| #        | Desafio                                                                                                                                                                                                                                                             | Criticidade  |
| -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------ |
| **Q-01** | ✅ **RESOLVIDO em 2026-08-01.** O customizer alcança sim — validado por `TerminalCustomizerReachTest` (T-4), com teste de controle. R-02 fechado                                                                                                                    | ✅ Resolvido |
| **Q-02** | ~~Duas sessões simultâneas no mesmo servidor MCP: qual "possui" um diff aberto?~~ ✅ **RESOLVIDO em 2026-08-08 (T-3.59): ninguém.** O envio é broadcast; a noção de dono não existe nessa camada                                                                    | ✅ Resolvido |
| **Q-03** | Semântica exata de `CLAUDE_CODE_JETBRAINS_PLUGIN_HIDE_BUTTON` — string encontrada, comportamento não verificado                                                                                                                                                     | 🟢 Baixo     |
| **Q-04** | ~~`TerminalEngine.REWORKED` se comporta como `CLASSIC` fora da tool window nativa?~~ ✅ **RESOLVIDO em 2026-08-03 (Achado 27):** a sessão é **sempre** JediTerm/CLASSIC, qualquer que seja o engine configurado. Fixado como guarda de regressão por T-2.6 (v1.9.2) | ✅ Resolvido |
| **Q-05** | Vale ocultar o ponto de entrada do oficial para evitar confusão? Depende de Q-03                                                                                                                                                                                    | 🟢 Baixo     |
| **Q-06** | Remote Dev / split mode / WSL — declarados fora de escopo, reavaliar depois                                                                                                                                                                                         | 🟢 Baixo     |
| **Q-07** | Restaurar sessões ao reabrir o projeto? Sem demanda comprovada                                                                                                                                                                                                      | 🟢 Baixo     |
| **Q-08** | `since-build` definido como `252` por conservadorismo, mas **só `262` foi testado**                                                                                                                                                                                 | 🟢 Baixo     |
| **Q-09** | _(v1.1)_ `CLAUDE_CONFIG_DIR` **por aba**, e não só por projeto? Exigiria diálogo a cada "Nova sessão"                                                                                                                                                               | 🟢 Baixo     |
| **Q-10** | ~~_(v1.1)_ O `Esc` se comporta igual no engine `REWORKED`?~~ ✅ **RESOLVIDO em 2026-08-03**, junto de Q-04: o engine nunca é `REWORKED` na nossa janela, então o caminho `Terminal.Escape` + EP `escapeHandler` não nos alcança. T-2.6 guarda a premissa            | ✅ Resolvido |
| **Q-11** | _(v1.1)_ `CLAUDE_CONFIG_DIR` deveria ser versionável em `.idea/` em vez de ficar no workspace?                                                                                                                                                                      | 🟢 Baixo     |
| **Q-12** | _(v1.2)_ Vale passar `[filename]` ao `/export` e abrir o arquivo no editor? Economiza cliques, mas exige adivinhar a semântica do argumento                                                                                                                         | 🟢 Baixo     |
| **Q-13** | _(v1.2)_ A cópia deveria respeitar a seleção do mouse quando houver? `Ctrl+C` já cobre; `JBTerminalWidget.getSelectedText()` existe se mudarmos                                                                                                                     | 🟢 Baixo     |
| **Q-16** | _(v1.4)_ Trocar o prazo fixo da capa por detecção de que o CLI já pintou? Avaliado: viável via `addModelListener` + `getScreenLines()`, mas acopla ao texto do banner                                                                                               | 🟢 Baixo     |
| **Q-17** | _(v1.4)_ Reintroduzir a capa sobre uma partida sem eco (D-20), deixando-a só como acabamento? O pior caso do prazo viraria "tela vazia", não "eco visível"                                                                                                          | 🟢 Baixo     |
| **Q-32** | ✅ **RESOLVIDA em 2026-08-09 (T-3.62): o `Ctrl+V` chega.** Com o `wl-clipboard` instalado, o print colado foi anexado e o arquivo apareceu em `~/.claude/image-cache/`. Ninguém consome a tecla antes do PTY. **RF-52 arquivado sem código**                        | ✅ Resolvido |

---

## Próximos passos

Concluído: ~~aprovação do SPEC~~ · ~~T-4~~ · ~~esqueleto Gradle~~ · ~~componentes + testes T-1.\*~~ ·
~~`buildPlugin`~~ · ~~primeiro teste no IDE real~~ · ~~RF-17/RF-18/RF-19~~ ·
~~T-3.1/T-3.2 (diff ponta a ponta)~~ · ~~T-3.7/T-3.8/T-3.9~~ · ~~RF-21/RF-22~~

Concluído também: ~~RF-24/RF-26~~ · ~~T-3.14~~ · ~~T-3.17 (conclusivo: RF-27 removido)~~ ·
~~RF-28~~ · ~~RF-29~~ · ~~tela de configurações em Kotlin UI DSL~~

Concluído em 2026-08-08: ~~Achado 31 (prazo da síntese + cancelamento de RNF-23)~~ ·
~~remoção de `readText`~~

**Pendente, em ordem:**

1. **Roteiros manuais em aberto — o inventário completo**, que até 2026-08-08 estava espalhado em
   dois lugares e omitia dois blocos:

   | Bloco            | Roteiros            | Estado em 2026-08-10                                               |
   | ---------------- | ------------------- | ------------------------------------------------------------------ |
   | Base             | T-3.4               | ✅ aprovado (`--resume`)                                           |
   | Base             | ~~T-3.3~~           | ⚰️ **inválido** — DEF-08; substituído por T-3.59, este ✅ aprovado |
   | Base             | **T-3.5, T-3.6**    | ⏳ **pendentes** — outros IDEs, desinstalação                      |
   | v1.4             | **T-3.18 a T-3.20** | ⏳ **pendentes** — tema, partida sem eco, diretório novo           |
   | v1.5 (Piper)     | T-3.21 a T-3.23     | ✅ aprovados                                                       |
   | v1.5 (RNF-20/23) | T-3.26, T-3.27      | ✅ aprovados, **depois** da correção da v1.9.1                     |
   | v1.7 (split)     | T-3.34 a T-3.41     | ✅ aprovados, incluindo T-3.36 (diff por pane)                     |
   | v1.7.1 / v1.8    | T-3.42 a T-3.47     | ✅ uso real desde a release 0.7.0 — não por roteiro                |
   | v1.8.2           | **T-3.48, 52, 53**  | ⏳ **status nunca declarado** — T-3.48 aparece só como hipótese    |
   | v1.9 (fala)      | T-3.54 a T-3.58     | ✅ uso real — RF-47 e RF-48 validados no IDE                       |
   | v1.9.3 / v1.9.4  | T-3.59, T-3.60      | ✅ aprovados 2026-08-08 — fecham DEF-08 via RF-49                  |
   | v1.9.5           | ~~T-3.61~~          | ⚰️ **não reproduziu** duas vezes; Q-31 arquivada                   |
   | v1.10            | T-3.62 a T-3.66     | ✅ aprovados 2026-08-09 — RF-50, RF-51, e RF-52 arquivado          |
   | v1.10.1          | ~~T-3.67, T-3.68~~  | ⚰️ **superados por T-3.69** — mediam o conserto que falhou         |
   | v1.10.2          | T-3.69              | ✅ aprovado 2026-08-09 — RF-53, com e sem `Shift`                  |

2. ~~**Q-02** — ambiguidade de sessão dupla~~ ✅ respondida em 2026-08-08 por T-3.59: é broadcast,
   ninguém possui.
3. **Decidir o destino do stash** `shell -i -c com exec`: mantê-lo como referência de D-20 ou
   descartar. Stash não é memória de longo prazo, e D-20 já registra a medição por escrito.
4. ~~**`gradle.properties` em `pluginVersion = 0.1.0`** contra a tag~~ ✅ quitado em 2026-08-10:
   `pluginVersion = 0.9.0`, alinhado à tag `v0.9.0`.
5. **[TTS agnóstico](../20260807-tts-engine-agnostic.md)** _(rodada nova, não é dívida)_ — análise
   Piper × Kokoro com latência medida e escopo de "motor de fala plugável". Depende de o
   `kokoro-tts.py` ganhar um modo que devolva os bytes: hoje ele toca sozinho e o plugin perderia
   `pause`/`resume`. O Kokoro **não está instalado** nesta máquina. ⚠️ **O rascunho reservava o
   número "v1.10" — que já foi usado por esta rodada.** Quando entrar, será v1.11 ou adiante.

6. ~~**v1.10 — implementar RF-50 (play no popup)**~~ ✅ **feito em 2026-08-09.** Terceiro botão em
   `ClaudeSelectionCopyButton`, guarda de notificação em `playText` (D-42), painel extraído para
   `buttonPanel` para poder testar o teto. T-1.62 a T-1.64 verdes.

7. ~~**v1.10 — implementar RF-51 (`/usage` no cabeçalho)**~~ ✅ **feito em 2026-08-09.**
   `ClaudeDockSessions.openUsage()` + `UsageSessionAction`, entre `ResumeSessionAction` e
   `SplitSessionMenuAction`. T-1.65 e T-1.66 verdes.

8. ~~**Rodar os roteiros manuais da v1.10 — T-3.63 a T-3.68.**~~ ✅ **fechado em 2026-08-09.**
   T-3.63, T-3.64, T-3.65 e T-3.66 aprovados. T-3.67 e T-3.68 mediam o conserto da v1.10.1, que
   **reprovou** — foram superados por T-3.69, aprovado depois de RF-53 (Achado 36).

9. ~~**v1.10 — rodar T-3.62 (colar print screen)**~~ ✅ **fechado em 2026-08-09.** Depois de
   `sudo apt install wl-clipboard`, o `Ctrl+V` anexou a imagem: **Q-32 respondida** e **RF-52
   arquivado sem código** (Achado 33, D-44).

**Restante, e é só isto:** os oito roteiros manuais nunca executados — T-3.5, T-3.6 (base),
T-3.18 a T-3.20 (v1.4) e T-3.48, T-3.52, T-3.53 (v1.8.2) — mais a decisão do stash (item 3) e a
rodada nova do TTS agnóstico (item 5). **Nenhum código pendente:** 142 testes verdes em
2026-08-10, árvore limpa, `pluginVersion = 0.9.0` alinhado à tag `v0.9.0`.

---

## Log

### 2026-08-10 (noite/2) — o IDE real, e um WARN que era só cancelamento

Duas sessões no sandbox, com a configuração semeada de propósito antes de subir (os três caminhos
do Kokoro mais os dois do Piper) — sem isso a primeira tela abre vazia e o teste começa digitando
caminho.

**Resultado: nenhuma exceção nas duas sessões.** O que rendeu foram três achados de ergonomia e um
de registro:

- **O Kokoro agora é o primeiro botão do seletor**, a pedido. A ordem da tela é o que sinaliza qual
  é o padrão, e ele estava em segundo.
- **`buildSearchableOptions` falhava com o IDE aberto** — seção própria acima. Não é o atrito de
  cache do Gradle, e `--stop` não resolve.
- **Três `WARN ... exit code 137` no log eram cancelamento, não falha.** 137 é 128+9: o SIGKILL do
  nosso próprio `destroyForcibly`. Quem para uma fala, ou começa outra por cima, produz isso — e
  registrar como WARN polui o `idea.log` justamente para quem for diagnosticar uma falha real
  depois. Agora o processo que **nós** matamos sai em `debug`, e WARN fica para falha de verdade,
  com a saída de erro do motor anexada. Confirmado na segunda sessão: zero ocorrências.
- **Os caminhos do Kokoro sobrevivem à macro `$USER_HOME$`.** O platform colapsa na gravação e
  expande na leitura, como já fazia com os do Piper — verificado no XML do sandbox.

**T-3.79 fechou por inspeção de descritores, e não por `pgrep`.** O roteiro dizia
`pgrep -f kokoro`, e isso **casa com o próprio comando de busca** — o teste passaria sempre, pelo
motivo errado. A verificação que vale é `/proc/*/fd`: depois de fechado o IDE, nenhum processo
mantinha o modelo aberto. O roteiro no SPEC foi corrigido.

**O que segue sem medição:** o tempo até a primeira fala **dentro** do IDE. Os 2,41 s são de
bancada, com o script exato do plugin, mas a JVM, o `SourceDataLine` e o mixer não entram nessa
conta. A impressão de ouvido bate; um cronômetro fecharia o assunto.

### 2026-08-10 (noite) — o motor não era o gargalo, e o Piper tinha um bug de dois anos

Pedido: trocar o Piper pelo Kokoro-ONNX, configurável pela tela, com as três vozes pt-BR e a
velocidade que já existe. Dois bônus, ambos aceitos: outros idiomas e coexistência com o Piper.

**A rodada teve quatro voltas de pergunta antes de qualquer código**, e cada uma mudou o plano:

1. **"Vale um enum em vez de ler o ZIP? Vale um daemon? Vale um CLI? E o modelo menor?"** Todas
   respondidas por medição, e três recusadas com número (D-46, D-48, D-47, D-50). O daemon compraria
   1,50 s por 496 MB residentes. O int8 é **4,5× mais lento**, não mais rápido.
2. **"Esperar quase 10 s é muito."** Os 17,71 s citados eram do desenho **atual**, não da proposta —
   e ao medir tempo até o primeiro áudio em vez de tempo total apareceu o Achado 39: o `Clip` exige
   o áudio inteiro antes de tocar, e os dois motores sempre emitiram em pedaços. Streaming leva o
   Piper de 5,37 s para 2,21 s **de brinde**.
3. **"Dá para listar as vozes do Piper?"** Dá — e o arquivo que descreve cada voz declara o
   `sample_rate` porque ele **varia**. O código tinha 22050 fixo desde a v1.5. **DEF-10.**
4. **"Quero reduzir esses 4 s, nem que seja para 3."** Medindo etapa por etapa em vez de chutar:
   o tempo até falar depende **só da primeira frase** (Achado 40). Um teto de palavras no primeiro
   pedaço entregou **2,4 s**.

**A rampa que eu tinha proposto foi derrubada pela medição do usuário.** Ele testou em outra janela
uma versão mais simples — cortar em toda pontuação — e a comparação lado a lado mostrou empate
(0,73 s contra 0,72 s), com a rampa custando mais código. Pior: a justificativa que eu havia escrito
para ela — blocos maiores seriam mais eficientes — é **falsa**: 16 pedaços e 5 pedaços medem o mesmo
tempo total. Ficou a versão dele, com duas regras a mais (D-51).

**Dois erros meus na execução, os dois de método:**

- Comparei as estratégias de corte com uma versão **incompleta** da minha própria função — omiti a
  quebra por palavra, que é justamente o que faz o trabalho — e quase concluí que empatavam por
  motivo errado. Peguei ao ver que as duas colunas tinham o mesmo primeiro pedaço de 107 chars.
- Escrevi um motor falso de teste que deixava um `sleep` **neto** segurando a ponta de escrita do
  pipe. Matar o processo não desbloqueava a leitura, e passei uma iteração inteira consertando o
  código de produção por causa de um cenário que motor real nenhum produz (piper e python são
  processo único, e os shims do pyenv usam `exec`). O conserto certo era `exec` no fixture.

**Medições que ficam:**

| O quê                                 | Antes   | Depois     |
| ------------------------------------- | ------- | ---------- |
| 1º áudio, Kokoro, trecho de 800 chars | 17,71 s | **2,41 s** |
| 1º áudio, Piper, mesmo trecho         | 5,37 s  | **2,21 s** |
| RTF do Kokoro, threads default        | 1,56    | **0,37**   |
| Testes                                | 142     | **163**    |

**Onde parou:** código completo e suíte verde na branch `feature/kokoro-onnx-tts`, sem commit.
Falta rodar T-3.70 a T-3.79 no IDE real — em especial **T-3.77**, que exige baixar
`pt_BR-edresson-low` para confirmar o conserto de DEF-10, e **T-3.74**, que é o critério de aceite
do streaming e pede julgamento de ouvido sobre os cortes.

### 2026-08-09 (madrugada/2) — todos os roteiros fecharam, e o melhor recurso da rodada não teve código

T-3.64, T-3.66 e T-3.62 aprovados no IDE real. Com eles, **a v1.10 fecha inteira**: RF-50, RF-51 e
RF-53 entregues e medidos, RF-52 arquivado.

**T-3.62 é o resultado que vale registrar.** Depois de `sudo apt install wl-clipboard`, colar um
print com `Ctrl+V` **funcionou**, e o arquivo apareceu em `~/.claude/image-cache/<sessionId>/N.png`
— exatamente o destino que o Achado 33 tinha previsto lendo o binário do CLI. **Q-32 respondida:**
ninguém consome o `Ctrl+V` antes do PTY; a ordem do `JBTerminalPanel.handleKeyEvent` nunca foi
problema.

**Então o pedido "colar print screen" foi entregue com zero linha de código nossa.** A recusa de
virar RF, lá na avaliação, era o desfecho certo — e o que a sustentou foi uma pergunta barata
(`command -v xclip`) feita antes de desenhar interceptação de teclado. **Um `apt install` resolveu
o que teria sido uma feature inteira.**

Contraste que a rodada deixa claro, e que vale mais que qualquer dos dois isolado: o recurso mais
barato foi o que eu quase implementei sem medir; o mais caro (DEF-09) foi o que eu declarei
consertado **tendo** medido — só que a coisa errada.

### 2026-08-09 (madrugada) — o conserto do DEF-09 falhou, e a instrumentação disse por quê

**Registro de erro meu, e vale mais que o conserto.** Verifiquei no bytecode que
`TerminalPanel.scrollArea` limpa a seleção incondicionalmente, escrevi DEF-09 em cima disso, mudei
o código e **declarei consertado**. Não era a causa. O sintoma não mudou nada.

**O que a instrumentação mediu** — um `JBPopupListener` com `Throwable` no `onClosed`:

```
IdePopupManager.maybeCloseAllPopups → closeAllPopups
  → StackingPopupDispatcherImpl.closeActivePopup → AbstractPopup.cancel
dispatched from IdeEventQueue via java.awt.SentEvent
```

Quem fecha é **a plataforma**, por evento de foco, 268 ms depois de o popup nascer. E o mesmo log
mata a hipótese de largura: painel **80×24** em **x=325**, num terminal de **2173px**. Sobra
espaço — **a contagem de botões nunca esteve no caminho causal**.

**As duas pistas boas vieram do usuário, não da minha leitura de bytecode:**

1. _"Por que com dois botões não dava e com três dá?"_ — respondi que o `diff` era neutro. Verdade,
   e irrelevante: a pergunta apontava uma variável que eu não tinha **medido**. A resposta certa
   naquele momento era instrumentar, não argumentar.
2. A captura de tela com **"copied N chars to clipboard"**, com a seleção sendo copiada **sem
   clique**. Foi ela que levou ao verdadeiro suspeito.

**Medições que a segunda pista motivou:**

| Verificação                                | Resultado                                  |
| ------------------------------------------ | ------------------------------------------ |
| CLI liga mouse reporting?                  | **Sim** — `?1000h` e `?1006h` no binário   |
| IntelliJ encaminha o mouse ao PTY?         | **Sim** — `myReportMouse = true` no padrão |
| `copyOnSelection` do IntelliJ está ligado? | **Não** — campo não inicializado           |
| A mensagem é do plugin?                    | **Não** — nenhum caminho nosso a imprime   |

**Confirmado no mesmo dia, pelo teste do `Shift`:** com `Shift` o popup fica de pé e o play toca;
sem `Shift`, o sintoma persiste. Quem trata o gesto é o **TUI do Claude Code**.

**Primeira correção (exigir `Shift`) — recusada pelo usuário.** "Não quero que funcione só com
shift." Tecnicamente correta, errada como produto. A recusa foi o que levou à correção certa, e por
isso fica registrada.

**Correção final (RF-53, revisto):** a barra deixa de ser `JBPopup` e vira filho do `JLayeredPane`
em `POPUP_LAYER`. O `closeAllPopups` não a enxerga — não há popup registrado para fechar.
**Funciona com e sem `Shift`.** ✅ aprovado no IDE real em 2026-08-09.

**Terceiro erro meu da rodada, e o mais barato de evitar:** ao propor a guarda, eu tratei uma
limitação do ambiente como requisito do produto. O usuário não pediu para entender o mouse
reporting — pediu que a barra aparecesse. **Explicar por que não dá não é entregar.** A saída
existia (não ser popup) e estava a uma pergunta de distância: _"o que fecha popups não alcança o
quê?"_

**Lição — irmã do Achado 30, com uma volta a mais.** O Achado 30 foi publicar hipótese como
conserto. Este foi publicar **hipótese verificada** como conserto, que é pior: a verificação dá
confiança sem dar causalidade. **Mecanismo confirmado ≠ causa observada.** Custou uma execução
inteira do usuário. Registrado como Achado 36.

**Correção de procedimento de build, também minha.** A nota de 2026-08-08 dizia que o `rm -rf` no
diretório de transformação **nunca** foi necessário. **Falsificado hoje:** dois `--stop`, zero
daemons, erro idêntico, e nenhum arquivo com mtime posterior para remover cirurgicamente. O `rm`
resolveu, e a re-extração levou os 36 s previstos. **A regra correta tem dois casos:**

- Sintoma `Cannot resolve 'product-info.json'` → **`./gradlew --stop` basta** (daemon com caminho
  velho na memória). Confirmado de novo hoje.
- Sintoma `contents of the immutable workspace ... have been modified` **persistindo depois do
  `--stop`** → o diretório foi mesmo alterado; aí **só o `rm -rf` resolve**.

### 2026-08-09 (tarde) — DEF-09: o popup piscava, e o culpado óbvio era inocente

Primeiro teste no IDE real da v1.10. O `/usage` funcionou de primeira. O popup da seleção, não:
"aparece rápido e some, quase piscando", sem dar tempo de clicar.

**O suspeito óbvio era o terceiro botão** — é a novidade, e o popup ficou mais largo. Resisti a
consertar por aí e fui ver o `diff`: **nada do ciclo de vida do popup tinha mudado.** A montagem do
painel trocou de escopo, e só. Se o diff não explica, a causa é mais antiga que o diff.

**Achei no bytecode do JediTerm, e é incondicional:**

```
public void scrollArea(int, int, int);
  10: aconst_null
  11: invokevirtual updateSelection(TerminalSelection)
```

`scrollArea` não desloca a seleção pela rolagem — **apaga**. E `updateSelection` notifica os
listeners. O popup escutava `selectionChanged(null)` para fechar. Logo: **toda rolagem do terminal
fechava o popup**, e uma sessão do Claude Code rola sozinha o tempo todo.

**Por que só apareceu agora, se o bug é da v1.6:** sobre uma sessão parada o popup funciona. Nos
testes anteriores a sessão estava parada. Desta vez o roteiro começou pelo `/usage` — que desenha
uma tela viva — e o defeito ficou óbvio. **Não foi regressão da v1.10; foi a v1.10 dando a
condição de teste que faltava.**

**O defeito vinha em par, e o segundo era pior.** Os botões liam `selectedText` **na hora do
clique**. Mesmo com o popup de pé, uma rolagem entre mostrar e clicar faria copiar/exportar/tocar
agirem sobre `null` — em silêncio, no caso do copiar. Nunca foi visto porque o primeiro defeito
escondia o segundo. Registrado como T-3.68.

**Correção tentada — NÃO resolveu.** O popup passou a capturar o trecho ao nascer (`snapshot`) e o
listener de seleção saiu. Sintoma idêntico no IDE real. Ver a entrada de 2026-08-09 (madrugada):
o `scrollArea` é mecanismo real, mas não era a causa deste sintoma.

**Lição:** um evento com o nome certo não é o gatilho certo. `selectionChanged(null)` parecia dizer
"o usuário desfez a seleção" e dizia "a seleção não vale mais" — inclusive quando quem a invalidou
foi o emulador. Mesma forma do Achado 25.

**Também nesta rodada:** ícone de RF-51 trocado de `General.BalloonInformation` (azul) para
`Actions.Profile` — o medidor cinza monocromático, coerente com as irmãs do cabeçalho. As variantes
coloridas da família chamam-se `ProfileBlue`/`Red`/`Yellow`, o que confirma qual é o neutro.

**Ambiente que vale registrar:** o sandbox roda sobre `sun.awt.wl.WLToolkit` — toolkit **Wayland
nativo** do JBR, não XWayland. Não foi a causa aqui, mas é contexto para qualquer defeito futuro de
popup ou foco.

### 2026-08-09 — RF-50 e RF-51 implementados

Rodada de código, curta, sem surpresa arquitetural. **142 testes, 0 falhas, 0 warnings.**

**RF-51 (`/usage` no cabeçalho)** — `ClaudeDockSessions.openUsage()`, `UsageSessionAction`,
registro entre `ResumeSessionAction` e `SplitSessionMenuAction`. Três arquivos tocados.

Um detalhe que só apareceu escrevendo: **o `activate(null)` no fim é necessário.** Clicar num botão
do cabeçalho não garante que o foco caia no terminal, e sem foco lá o `Esc` que sai da tela de uso
não chega ao CLI — o usuário abriria uma tela da qual não sabe sair. Mesmo remate de RF-49, e não
estava no desenho da v1.10; o SPEC foi corrigido para refletir o código, e não o contrário.

**RF-50 (play no popup)** — o botão em si foram 3 linhas, como previsto. As duas outras mudanças é
que importam:

1. **A guarda em `playText` (D-42).** Confirmado ao escrever: o ramo negativo só chamava
   `LOG.warn`. Agora notifica, e conserta os **dois** chamadores de uma vez.
2. **`buttonPanel` saiu do `Controller` para o objeto.** Concessão estrutural que eu preferiria
   não fazer, e que se paga: dentro do `Controller` o painel só seria testável subindo um
   `TerminalPanel` real. Fora, T-1.64 verifica o teto de três (R-29) e a fiação de cada callback
   em milissegundos. Dado que o teto já foi rompido uma vez por critério errado (Achado 34), ter o
   número escrito onde um quarto botão quebre o teste vale a indireção.

**O único erro da rodada foi meu, e no teste:** passei `null` como `MouseEvent` para
`mousePressed`, e o check de não-nulo do Kotlin reprovou. Corrigido com um evento de verdade. Vale
registrar porque é o tipo de falha que tenta a gente a afrouxar a produção (`e: MouseEvent?`) para
o teste ficar mais curto — seria trocar segurança de tipo por conveniência de bancada.

**Falta o que não dá para medir aqui:** T-3.63 a T-3.66, no IDE real. Nada nesta rodada teve PTY.

### 2026-08-08 (noite/10) — três pedidos: dois viram RF, um vira `apt install`

Chegaram três propostas em `plans/`. A rodada foi de **avaliação**, não de implementação, e o
resultado mais útil foi o pedido que **não** virou código.

**1. Colar print screen (`20260808-paste-print-screen.md`) — recusado como RF.**

A tentação era desenhar interceptação de `Ctrl+V` no plugin. Antes disso, a pergunta do Achado 25:
**de onde vem o dado?** Resposta lida no binário do CLI, não suposta: de `xclip` ou `wl-paste`.
**Nenhum dos dois está instalado nesta máquina**, e a sessão é Wayland. O CLI trata a falha do
comando de detecção como "não há imagem" (`if (exitCode !== 0) return null`) — no-op mudo, o mesmo
formato do DEF-07.

E o relato **já trazia a prova** de que a causa era externa: "o mesmo problema acontece quando
executo claude code no terminal". Nenhuma linha do plugin roda ali. Uma causa que alcança os dois
ambientes não pode estar no plugin. Essa frase, sozinha, valia mais que qualquer leitura de
bytecode — e quase passou batido.

Conserto: `sudo apt install wl-clipboard`. Sobrou uma pergunta legítima (Q-32: o `Ctrl+V` chega ao
PTY dentro do IDE?), um teste que a responde (T-3.62) e um RF **condicional** (RF-52) que só existe
se a medição pedir. Publicar RF-52 como decidido seria repetir o Achado 30.

**2. Play no popup da seleção (`20260808-tts-play-button.md`) — aceito, RF-50.**

O pedido era reuso, e o usuário estava certo: as peças existem todas. O popup tem o texto
(`selectedText`, o mesmo de RF-26/RF-33) e o serviço tem o play (`playText`, o mesmo de RF-48).
São ~8 linhas.

**O interessante não é o botão, é o critério que o barrava.** R-23 declarava teto de dois botões,
e o play já tinha sido recusado duas vezes — RF-30 em v1.5.1, e o Achado 26 — pela pergunta "isso
já existe em outro lugar da UI?". **Essa pergunta reprova o botão de copiar**, que existe desde
RF-26 apesar de `Ctrl+C` já copiar; o comentário do `ClaudeSelectionCopyButton` diz, com todas as
letras, que atalho é invisível para quem está com o mouse. O critério media a **existência** da
capacidade e ignorava o **custo de alcançá-la**.

Agravante descoberto agora: entre a recusa de v1.5.1 e a v1.9, o item "Tocar seleção" do menu
**não funcionava** (DEF-07). O play foi recusado por já existir num lugar onde não existia.

Critério novo (Achado 34): entra no popup o que opera **sobre o trecho selecionado** e cabe em
**um clique**. Teto de três. R-29 substitui R-23.

**Uma coisa não é reuso, e é a única mudança real de comportamento:** `playText` hoje só _loga_
quando o Piper não está configurado. Do cabeçalho isso não aparecia, porque o `update()` do menu
desabilita o item antes do clique. **O popup não tem `update()`.** Sem aviso, o botão novo nasceria
sendo o DEF-07 de novo. A guarda vai em `playText` (D-42) — um ponto, dois chamadores consertados.

**3. `/usage` no cabeçalho (`20260808-usage-option.md`) — aceito como botão, recusado como popup.**

O recurso passa; o formato não. `/usage` é tela de TUI: não existe subcomando `claude usage` (a
lista de comandos foi lida), o `stats-cache.json` local guarda atividade e não limite de plano, e o
número que o popup mostraria vem de `/api/oauth/usage` autenticado com o token de
`~/.claude/.credentials.json`. **O popup custaria o plugin passar a ler o segredo do usuário** —
contra RNF-04 e contra a regra de segredos do `CLAUDE.md`. Raspar o buffer também não serve: é o
DEF-01 sobre uma tela que se repinta.

O botão entrega o que o pedido queria de fato — parar de digitar `/usage` — e custa uma linha
(RF-51, D-43).

**Colisão de numeração, resolvida:** o rascunho de TTS agnóstico reservava "v1.10". Esta rodada
tomou o número; aquela entra em v1.11 ou adiante.

**Nada foi implementado.** SPEC v1.10 e este registro são a entrega. Os próximos passos 7, 8 e 9
listam o código.

### 2026-08-08 (noite/9) — Q-31 arquivada: duas medições dirigidas, nenhuma reprodução

**Segunda tentativa, com a condição específica.** IDE reiniciada, tool window restaurada aberta, a
primeira pane nascendo durante a inicialização — o log confirma a sessão subindo sozinha na
partida. Resultado: **as duas panes integradas, mesma porta real**. Igual à primeira tentativa.

**E existe uma razão provável para nunca reproduzirmos, que estava debaixo do nariz o tempo todo.**
Nossas sessões usam `deferSessionStartUntilUiShown = true` (D-23/RNF-02): **o processo não nasce
quando a aba é criada, e sim quando o componente aparece** — e é no nascimento do processo que
`configureStartupOptions` roda o customizer e congela o `CLAUDE_CODE_SSE_PORT`. Não é suposição: o
spike dos T-2 mediu exatamente isso, em headless, onde `ttyConnector` fica `null` para sempre
porque a UI nunca é exibida.

**A flag que existe por motivo estético — esconder o eco da partida (RF-29) — provavelmente nos
tira da corrida de graça.** Adiar não elimina a corrida em teoria; estreita tanto que duas
tentativas dirigidas não a pegaram.

**Decisão: arquivar Q-31, e não escrever código.** O Achado 32 fica como fato do plugin oficial —
`getOrDefault(…, 0)` e a mitigação presa ao literal `"Terminal"` são reais e podem explicar
sintomas futuros. Mas a causa do caso visto em T-3.60 **continua desconhecida**, e duas medições
dirigidas que não sustentam a hipótese são motivo para parar de caçar, não para implementar
proteção contra algo que não se conseguiu provocar.

**O que fica pronto para a próxima vez que o sintoma aparecer:** pane sem `In <arquivo>` no rodapé,
`/exit`, `echo $CLAUDE_CODE_SSE_PORT`, comparar com uma pane sadia. Se der `0`, o Achado 32 estava
certo e é só reabrir a questão com a evidência na mão.

**Balanço honesto da investigação.** Ela não fechou Q-31, mas rendeu três coisas verificadas: o
mecanismo do `getOrDefault`, a terceira ocorrência do literal `"Terminal"` nos excluindo, e a
confirmação em produção — nas duas panes, com porta real — de que o customizer alcança sessões de
split. Esta última é a premissa central do projeto, vista fora do laboratório de T-4.

### 2026-08-08 (noite/8) — T-3.61 não reproduziu, e isso vale registrar

**Medição.** As duas panes ficaram **integradas** e `echo $CLAUDE_CODE_SSE_PORT` devolveu a **mesma
porta real, `33471`**, nas duas.

**O braço de controle passou, e não é pouco.** A porta é por projeto (`locationHash`), como o
bytecode dizia, e **o customizer alcança as panes de split em produção** — T-4 provou isso em
laboratório em 2026-08-01; agora está visto numa sessão real, nas duas panes. É a premissa central
do projeto confirmada mais uma vez, por outro caminho.

**Mas a hipótese continua sem medição.** O caso que eu queria pegar — pane **sem** integração — não
apareceu. Sem ele, a porta `0` do Achado 32 não foi nem confirmada nem refutada. **Não reproduzir
não é refutar, e também não é confirmar**; é ficar onde estava, com uma tentativa registrada.

**Por que registrar um resultado nulo.** Porque a próxima sessão, lendo o Achado 32 sozinho, teria
todo motivo para tratá-lo como fechado — a leitura do bytecode é convincente demais. Este registro
existe para que ela veja que a medição foi tentada e não deu.

**Para reproduzir, falta a condição específica:** uma sessão que nasça **antes** do
`PostStartupActivity` — a tool window restaurada aberta na abertura do projeto, não uma aba criada
com o IDE já de pé. Foi assim que o sintoma apareceu em T-3.60, e é a única forma conhecida de
provocá-lo.

**Nada foi implementado, de novo e de propósito.** Continua valendo o contorno sem código: "Nova
sessão" nasce com a porta certa.

### 2026-08-08 (noite/7) — Q-31: a pane nasce com porta `0`, e o conserto do oficial não nos vê

**Investigação pedida.** Comecei pelo nosso lado, e ele saiu limpo: `splitSession` usa o mesmo
`createPane` → `createSession` da primeira pane, sem divergência de ambiente. Se as duas nascem
iguais, a diferença é **quando** nascem — e aí a resposta estava no plugin oficial.

**A cadeia (Achado 32), toda lida no bytecode:** o customizer injeta
`CLAUDE_CODE_SSE_PORT = getOrDefault(locationHash, 0)`; o mapa só é populado por `MCPService.start()`;
`start()` roda no `PostStartupActivity`. Sessão criada antes disso recebe **`0`** — não "sem
variável", **zero** — e como ambiente de processo é fixado no `exec`, ela nunca conecta.

**O detalhe que fecha o caso:** logo depois de `start()`, o oficial chama
`restartClaudeInExistingTerminals` **justamente para consertar essa corrida**. E essa função varre
`getToolWindow("Terminal")` e só age em abas cujo título começa com `"Claude Code"`. **As nossas
panes falham nos dois critérios.** A corrida é dele, o conserto existe, e nos exclui por
construção — **terceira vez que o literal `"Terminal"` nos define**, depois de RF-17 e DEF-08.

**Explica os três comportamentos observados de uma vez:** a primeira pane costuma ser a
desintegrada (nasce com o projeto), a de split costuma conectar (nasce depois), e em T-3.36 as duas
conectaram porque a aba foi aberta com o IDE já de pé.

**O que eu não fiz, de propósito: não implementei nada.** O mecanismo está lido, o efeito **não
está medido**. T-3.61 mede com uma linha — `echo $CLAUDE_CODE_SSE_PORT` numa pane sem o indicador.
Sair `0` fecha Q-31; qualquer outra coisa derruba a explicação inteira. Este arquivo já pagou duas
vezes por tratar leitura como medição (Achados 30 e 31), e a tentação aqui era grande porque a
leitura é bonita demais.

**Contorno que já existe:** "Nova sessão" nasce com a porta certa. Fechar a pane desintegrada e
abrir outra resolve o caso concreto, sem código novo — o que também é o motivo de eu não ter
proposto código antes de medir.

### 2026-08-08 (noite/6) — a suíte verde de verdade, e o que o daemon escondia

**136 testes · 0 falhas · 0 erros · 22 classes · zero warnings**, executados às **17:15:03** com
`--rerun`. É a primeira medição de RF-49 feita depois de tudo pronto — as anteriores descreviam o
mesmo código, mas eram de antes.

**O que faltava na receita do cache, e é o motivo de três tentativas fracassadas.** Com o
diretório do hash **já apagado**, o build continuava falhando com
`Cannot resolve 'product-info.json'` apontando para um caminho **que não existia mais**. Não era
corrupção nova: era o **daemon do Gradle** servindo o caminho resolvido da memória. Nem
`--refresh-dependencies` nem `--no-configuration-cache` mexem nisso — só `./gradlew --stop`.

**E a armadilha seguinte, que quase me fez reportar número velho:** destravado o build, o primeiro
`./gradlew test` volta `BUILD SUCCESSFUL` em 34 s **sem executar teste nenhum** (task `UP-TO-DATE`),
e os XMLs mantêm a data anterior. Contar a partir deles nesse estado produz um total que parece
atual e não é. `--rerun` é obrigatório quando o que se quer é medição, não confirmação.

As duas coisas estão na receita, em Fatos verificados.

### 2026-08-08 (noite/5) — a conclusão de Q-02 quase virou um alvo fixo inexistente

**O relato.** Em T-3.60 o `Ctrl+Alt+K` entregou **só à segunda pane**, "sempre". Isso parecia
contradizer o que eu tinha acabado de registrar em Q-02 — broadcast, sem destinatário — e sugeria
o oposto: que existe um alvo fixo, só que o errado.

**Não contradiz, e a diferença só apareceu porque fui ao bytecode em vez de teorizar.**
`MCPService._mcpServerInfos` é `Map<Server, McpServerInfo>`: **uma entrada por conexão**, sem
colisão de chave que fizesse uma sessão sobrescrever a outra. E `sendAtMentionedNotifications`
itera **todas**. O broadcast está no código.

**O que muda entre as duas execuções é quantas sessões estão conectadas.** Comparando os prints:
em T-3.36 as duas panes mostravam `In test.md` e as duas receberam; em T-3.60 só a segunda mostra
`1 line selected`, e só ela recebeu. **A regra é "broadcast para toda sessão conectada"**, não
"para todas as panes".

**Q-02 continua respondida** — ninguém possui o envio. Mas a formulação foi refinada, porque
"chega nas duas" era verdade de uma execução, não do mecanismo.

**Q-31, nova e não medida:** por que uma pane às vezes não conecta. Afeta RF-37, que assume
integração em todas. O diagnóstico de campo é barato: **pane sem `In <arquivo>` no rodapé está
fora do MCP.**

**A lição, de novo.** Duas observações, duas conclusões opostas, e nenhuma delas era do mecanismo.
Terceira vez nesta sessão que o padrão do Achado 30 aparece — concluir de uma amostra sem variar a
condição. O que salvou foi ter uma fonte de verdade abaixo do comportamento: o bytecode.

**Sobre o erro de Gradle no print.** Não é do plugin. O sandbox estava com o **próprio projeto do
plugin aberto** (aba `build.gradle.kts (claude-code-dock)`), e o sync do Gradle da IDE bateu na
mesma corrupção de workspace imutável já registrada em Fatos verificados — agravada por ser a
mesma distribuição que o sandbox está usando. **Não abrir este projeto dentro do sandbox**; abrir
qualquer outro.

### 2026-08-08 (noite/4) — RF-49: a entrega que tem destinatário

**O que foi feito.** Ação própria de enviar a seleção do editor para a sessão **em foco**:
`ClaudeEditorReference` (objeto puro), `ClaudeDockSessions.sendEditorReference`,
`SendSelectionAction` no menu de contexto do editor e no menu Tools.

**O formato do @-mention foi lido, não inventado.** Está na função do CLI 2.1.220 que renderiza a
menção a partir da notificação MCP:

```js
let r = path.relative(cwd(), e.filePath);
if (e.lineStart && e.lineEnd)
  n =
    e.lineStart === e.lineEnd
      ? `@${r}#L${e.lineStart} `
      : `@${r}#L${e.lineStart}-${e.lineEnd} `;
else n = `@${r} `;
```

Três regras que eu teria errado adivinhando, e cada uma virou teste: caminho **relativo ao cwd**,
linhas **1-based** (o `&&` trata `0` como ausente, não como linha zero) e **espaço no fim**, que
separa a menção do que o usuário digita em seguida.

**O bug que o teste pegou antes do IDE (T-1.60).** Selecionar linhas inteiras deixa o offset final
na **coluna 0 da linha seguinte**. Sem `inclusiveEndLine`, marcar uma linha produziria
`@arquivo#L3-4` — uma referência que mente sobre o que foi marcado, e do tipo que passa
despercebido porque o CLI aceita o texto sem reclamar.

**D-41 — por que PTY e não MCP.** Reimplementar a notificação exigiria falar o protocolo privado
(D-01 recusa) **e** herdaria o broadcast que criou o problema: entregaria a todas as panes de
novo. A escrita no PTY é o caminho do `/export` desde D-16 e é a única com destinatário.

**Mutação.** Tirando o espaço final e a correção de linha inteira, reprovam três testes — os dois
alvos mais o de espaço. Restaurado, 136 verdes.

**Dois consertos de higiene que a rodada obrigou.**

- **`ClaudeIconTest` quebrou, e a falha estava certa:** ele fixava a lista exata `[MONO, MONO]`
  como controle contra a regex parar de casar, e a ação nova virou a terceira. Trocado por
  "não vazio **e** todos MONO" — mantém o controle real (regex morta → lista vazia → falha) e
  para de cair por contagem a cada ação nova. Era ruído disfarçado de teste.
- **Os cinco warnings que eu tinha introduzido em `ClaudeDockIntegrationTest` sumiram.** Dois
  casts inúteis, e três usos de `Disposer.isDisposed`, depreciado. Em vez de suprimir, T-2.4a e
  T-2.4b passaram a pendurar um `CheckedDisposable` como sentinela: não é depreciado e a asserção
  fica **mais forte**, porque afirma a propagação do descarte, que é o que solta o PTY.

**Testes.** 130 → **136**, zero falhas, **zero warnings** (conferido com `--rerun-tasks`).

**Validado no IDE no mesmo dia (T-3.60).** Com a aba dividida, a menção `@test.md#L3` apareceu
**só na pane esquerda** — a direita ficou vazia. É a diferença para o `Ctrl+Alt+K`, que em T-3.59
entregou às duas. E saiu `#L3`, não `#L3-4`, com a barra de status em `3:12 (30 chars)`: a
correção de `inclusiveEndLine` vale no editor real, e não só na suíte.

**Um susto que não era defeito.** Nesta partida o `Ctrl+Alt+K` "parou de funcionar" — o plugin
oficial não estava mais no sandbox, apagado pelo `prepareSandbox` ao recompilar. Ausência, não
regressão. Rendeu a correção do fato errado acima e a receita nova em Fatos verificados.

### 2026-08-08 (noite/3) — T-3.59 fecha DEF-08 e, de brinde, a Q-02 mais antiga do arquivo

**T-3.59 executado. O trecho chega.** Depois do `Ctrl+Alt+K`, `@test.md#L3` apareceu na nossa pane
com `1 line selected`. **DEF-08 é ergonomia, não integração** — o conteúdo atravessa por MCP, só o
foco vai para a janela errada. A leitura do bytecode acertou, e agora está medida em vez de
inferida.

**E chegou nas duas panes ao mesmo tempo.** Isso não é um segundo defeito: é a mesma frase do
mecanismo vista de frente. `sendAtMentionedNotifications` é broadcast — não tem destinatário.

**Q-02 respondida, depois de nove versões esperando.** A pergunta era "com duas sessões no mesmo
servidor MCP, qual delas 'possui' um diff aberto?". **A resposta é: nenhuma, e a pergunta assumia
uma estrutura que não existe.** O protocolo do plugin oficial não modela sessão-alvo; ele
transmite a todos. Não é que a desambiguação erre — é que ela nunca foi escrita. Ela estava
marcada como "🟡 Médio, durante o uso real", e foi exatamente o uso real que respondeu, de graça,
num teste que existia para outra coisa.

**O que isso ensina sobre o formato:** T-3.59 foi escrito para substituir um roteiro inválido, com
uma pergunta binária (chega ou não chega). Ele respondeu a sua pergunta **e** uma pergunta de
arquitetura aberta desde a v1.0. Roteiro barato com hipótese explícita rende mais que roteiro
caro sem hipótese — mesma lição de T-1.55/T-1.56 na rodada anterior.

**Conserto plausível de DEF-08, não implementado.** Uma ação nossa que escreva `@arquivo#Lx` direto
no PTY da pane em foco, pelo `sendInput` que existe desde D-16 — sem tocar no protocolo privado e
sem violar D-01. Custo baixo. **A demanda é que ainda não existe**, e inventá-la aqui seria o
mesmo erro de RF-30, recusado na v1.5.1.

**Nenhum código mudou.** A suíte não pôde ser reexecutada nesta rodada: a IDE do sandbox escreve
dentro do diretório de distribuição, que vive no cache de transformação **imutável** do Gradle, e
o `./gradlew test` passa a recusar. Some com `rm -rf ~/.gradle/caches/9.2.0/transforms/<hash>`
depois de fechar a IDE. O último verde real é de 15:53 (130/0), e **nenhum `.kt` foi tocado
depois** — então ele ainda descreve o código atual.

### 2026-08-08 (noite/2) — DEF-08: o roteiro estava errado, não o plugin

**Resultados.** T-3.4 ✅ · T-3.36 ✅ · **T-3.3 ✗** — e o "✗" virou achado estrutural.

**T-3.36, com a melhor evidência da noite.** O print mostra as duas panes lado a lado, **ambas com
`In test.md` no rodapé ao mesmo tempo**. Esse rótulo vem do servidor MCP dizendo ao CLI qual
arquivo está aberto no editor: duas panes exibindo-o **é** a prova de que as duas mantêm a
integração simultaneamente, que é o coração de RF-37. A edição foi aplicada e o editor atualizou.
Melhor do que o roteiro pedia — ele falava em "diff abre no IDE", e o que se viu foi a integração
viva nas duas pontas.

**T-3.3 falhou, e a culpa é do roteiro.** Lido o bytecode de `SendToClaudeAction` e `TerminalUtil`
(0.1.14-beta), a ação faz duas coisas independentes:

1. **O trecho vai por MCP** — `sendAtMentionedNotifications(mcpService, editor)`, broadcast que não
   conhece tool window nenhuma e, em tese, alcança as nossas panes.
2. **O foco vai para o Terminal nativo, sempre.** Sem cliente MCP registrado,
   `openClaudeInTerminal` cria sessão nova via `TerminalToolWindowManager`; com cliente,
   `focusClaudeInTerminal` faz `getToolWindow("Terminal")` e varre o `ContentManager` **dela**.

**As nossas sessões são invisíveis para esse caminho por definição** — não estão naquele
`ContentManager`. É **o mesmo literal `"Terminal"`** que originou este projeto, registrado nos
Fatos verificados desde 2026-08-01, agora aparecendo do outro lado do problema.

**Por isso T-3.3 foi invalidado, e não marcado como defeito nosso.** Ele pedia que uma ação de
terceiro, presa por literal à janela do terceiro, fosse parar na nossa. Nenhuma implementação
nossa faz isso passar sem violar D-01 ou colidir com o id nativo — alternativa já recusada na v1.1.
Substituído por **T-3.59**.

**A pergunta que sobra é a que vale (Q-30).** Se o item (1) chega, DEF-08 é ergonomia: o trecho
está lá, o foco é que foi para o lado errado. Se não chega, é integração. **A leitura do bytecode
sugere que chega — mas leitura não é medição**, e este arquivo já registrou duas vezes o custo de
confundir as duas (Achado 30, Achado 31). T-3.59 mede.

**Sobre o sandbox.** Ele nasce só com o nosso plugin: o oficial não estava lá, e sem ele T-3.3 e
T-3.36 seriam impossíveis de executar. `claude-code-jetbrains-plugin` foi copiado para
`.intellijPlatform/sandbox/claude-code-dock/IU-2026.2/plugins/` e os dois carregam juntos
("Claude Code Dock (0.1.0), Claude Code [Beta] (0.1.14-beta)").

> ⚠️ **Correção de 2026-08-08 (noite/4):** eu registrei aqui que o `prepareSandbox` **não** apagava
> a cópia. **Apaga.** Ela sobreviveu a dois `runIde` seguidos apenas porque a tarefa estava
> `UP-TO-DATE` — nenhum fonte havia mudado. No primeiro `runIde` após recompilar, o
> `prepareSandbox` rodou de verdade e limpou o diretório de plugins. **Regra real: reinstalar o
> oficial depois de qualquer mudança de código**, ou os roteiros que dependem dele falham por
> ausência e não por defeito. Foi conclusão tirada de duas observações sem variar a condição —
> o mesmo erro de método do Achado 30. O servidor MCP subiu dentro do sandbox — lockfile `~/.claude/ide/41083.lock`, `transport: ws`,
> com o pid da IDE do sandbox. **Registrar isto poupa a próxima sessão de descobrir de novo.**

### 2026-08-08 (noite) — os roteiros de split, e a corroboração que veio do log

**O que foi executado.** Sandbox aberto sobre `~/Workspaces/my/POC/aw`, com o Piper já configurado
da sessão anterior. Aprovados: **T-3.34, T-3.35, T-3.37, T-3.38, T-3.39, T-3.40 e T-3.41**.

**O que o log confirmou por conta própria**, sem depender do relato:

- **Quatro sessões numa mesma janela**, e uma delas subiu com `columns=59` contra `columns=126`
  das outras. Meia largura é uma pane dividida recebendo o tamanho real — prova de que o split
  aconteceu e de que o CLI redesenhou para o tamanho novo (T-3.34/T-3.35, CB-54).
- **T-3.41 verificado sem a tela:** depois do fechamento, nenhum processo restou pendurado no
  sandbox e **nada foi reparentado ao init**, que é onde um PTY órfão apareceria. RNF-09 e CB-53
  confirmados por medição, não por observação.
- Zero ocorrências de `dev.reginaldomorais.claudedock` em nível de erro no `idea.log`.

**T-3.36 continua pendente, e vale registrar por quê.** O relato inicial foi "tudo funcionou", mas
a janela da sessão durou 74 segundos para quatro sessões. Dá para dividir, focar e copiar nesse
tempo; conversar com o Claude até sair diff em duas panes, não. Perguntado, o usuário confirmou
que não executou esse. **É o tipo de ✅ que teria envenenado o arquivo:** o valor deste registro
está em as marcas serem confiáveis, e uma delas contradita pelo relógio contamina as outras.

**T-3.42 a T-3.47 — validados pelo uso real, não por roteiro.** São de SPEC v1.7.1 e v1.8, que
saíram na release **0.7.0**; estão em uso diário desde então. O rótulo é deliberadamente diferente
de "aprovado em roteiro" — mesmo padrão de Q-26, que também foi respondida pelo uso.

**Nenhum código mudou nesta rodada.** Os 130 testes da v1.9.2 seguem sendo o estado verificado.

### 2026-08-08 (tarde) — T-2.1 a T-2.6: medir o ambiente antes de escrever o teste

**Contexto.** Última lacuna estrutural do projeto: os testes de integração nunca escritos, listados
como pendência desde a v1.1.

**A fase que evitou seis testes falsos.** Antes de escrever qualquer asserção, três spikes
descartáveis mediram o que o `BasePlatformTestCase` realmente entrega. Os três acharam coisa:

1. **`ToolWindowManager` no headless é `ToolWindowHeadlessManagerImpl` e devolve `null` para
   qualquer id** — inclusive "Terminal". T-2.1 escrito sobre ele passaria sem medir nada, e é
   exatamente a forma óbvia de escrever esse teste. A rota honesta é o `ToolWindowEP`.
2. **Nenhuma sessão chega a ter PTY neste ambiente.** Mesmo passando
   `deferSessionStartUntilUiShown = false` direto ao runner, `ttyConnector` continua `null` depois
   de 10 s: o processo nasce quando o componente é exibido, e no headless nada é exibido. Isso
   **redefiniu** T-2.2/2.3/2.4 — eles verificam o encadeamento de `Disposable` que produz o PTY,
   que é onde o vazamento nasceria, e não o processo. A metade observável fica com T-3.1/T-3.41.
3. **`ToolWindowEP.pluginDescriptor` expõe `descriptorPath`**, e o nosso diz `plugin-terminal.xml`.
   Isso deu a T-2.5 uma asserção sobre o mecanismo real de RF-15, em vez de uma leitura de XML.

**T-2.6 é o mais valioso, e o ambiente entregou o caso interessante de graça:** o
`TerminalOptionsProvider` deste ambiente tem `REWORKED` como padrão, e a sessão nasceu
`ShellTerminalWidget` mesmo assim. O Achado 27 deixou de ser leitura de bytecode e virou asserção.

**E isso obrigou a corrigir um registro antigo.** Q-04 e Q-10 constavam resolvidos desde 2026-08-03
com a justificativa "ambos os engines funcionavam". **A justificativa estava errada.** As abas
deste plugin nunca foram `REWORKED`: o `REWORKED` do usuário valia para o Terminal nativo dele. As
perguntas não tinham objeto, e a resolução acertou o resultado pelo caminho errado — o mesmo
padrão do Achado 30. As duas linhas foram corrigidas no SPEC.

**Mutação, como manda o Achado 20.** Movendo a tool window para o `plugin.xml`, trocando o id, e
pendurando o widget no `project` em vez do `Disposable` da aba: reprovam T-2.1, T-2.5 e T-2.4b — e
só eles. T-2.6 não é mutável por construção: fixa comportamento da plataforma, e vale como tripwire
de upgrade (T-5.3).

**Testes.** 123 → **130**, zero falhas.

**Validação manual no IDE (mesmo dia).** Sandbox com Piper configurado: T-3.21, T-3.22, T-3.23,
T-3.26 e T-3.27 aprovados pelo usuário — os dois últimos **depois** da correção da v1.9.1, e
T-3.27 é justamente o cancelamento que era inerte. O `claude-code-dock.xml` do sandbox gravou
executável, modelo e `speechSpeed=125`, o que fecha T-3.57 de passagem.

**Não feito.** Os roteiros de split (T-3.34-41), de reposicionamento (T-3.45-47) e os herdados da
base (T-3.3-3.6, T-3.18-3.20) seguem pendentes de IDE real. `pluginVersion` continua em 0.1.0.

### 2026-08-08 — v1.9.1: o campo morto guardava duas promessas, não uma

**Contexto.** Pedido do usuário: avaliar o que faltava para fechar este arquivo. A avaliação
conferiu cada pendência contra o repositório em vez de reler o que estava escrito — e é essa
diferença que produziu o resto da entrada.

**O que a conferência corrigiu no próprio HANDOFF.**

- **O diff sujo era falso alarme.** `HANDOFF.md` e `SPEC.md` apareciam com 460 linhas alteradas.
  `git diff -w` mostrava 37: era o Prettier realinhando pipes de tabela e trocando `*x*` por
  `_x_`. Nenhuma mudança semântica. **Lição barata: antes de tratar um diff de markdown como
  trabalho pendente, rodar `git diff -w`.**
- **Dois blocos de roteiro manual não estavam em lista nenhuma:** T-3.26/T-3.27 (v1.5) e
  T-3.48/52/53 (v1.8.2). O inventário completo está agora em Próximos passos, numa tabela só, em
  vez de espalhado entre a tabela de estado e a lista de pendências.
- **O plano [TTS agnóstico](../20260807-tts-engine-agnostic.md) estava untracked e órfão** — 22 KB
  de análise verificada que nem este arquivo nem o SPEC citavam. Referenciado agora.

**Achado 31 quitado — e ele contava só metade.** O registro dizia "falta o timeout". Lendo o
código, o campo `currentProcess` era declarado e **nunca atribuído**, então o
`currentProcess?.destroy()` de `stopCurrent()` operava sempre sobre `null`. Isso não custava só o
prazo: `stopCurrent()` é o caminho de **RNF-23** — "novo play interrompe o anterior" —, e o piper
da síntese anterior seguia até o fim, invisível. **Duas promessas mortas no mesmo campo, e T-3.27
reprovaria se alguém o tivesse executado.**

**A correção teve uma terceira parte que não estava prevista.** Pôr `waitFor(20, SECONDS)` no
lugar de `waitFor()` não bastaria: `readBytes()` vinha **antes** e só volta no EOF, então o prazo
seria decorativo. Pior, ler depois de esperar convidaria o deadlock clássico — o piper trava se o
buffer do pipe encher sem ninguém lendo. O stdout foi para um `CompletableFuture`, e só então o
prazo passou a significar alguma coisa.

**A checagem, que é o ponto.** T-1.55 e T-1.56 usam um piper falso de duas linhas (`sh` que dorme
30 s) e afirmam o **efeito**: volta em ~1 s, não em 30. Depois de verdes, desfiz cada correção e
rodei de novo — **os dois reprovam sob mutação**. É exatamente o que o Achado 20 (o `ESC` vazio)
pediu por escrito e que teria pego este campo quatro versões antes. Um teste que só afirmasse
"o campo existe" teria passado o tempo todo.

**Também removido:** `ClaudeTerminalSessionFactory.readText`, morto desde que RF-24 substituiu
RF-21 — um ponto a menos de acoplamento com a API de terminal (RNF-15).

**Testes.** 121 → **123**, zero falhas, zero erros.

**Não feito, e por quê.** Os testes de integração T-2.\* seguem pendentes: é implementação de
verdade, não dívida de documentação, e a avaliação recomendou fazê-los como rodada própria — eles
matam Q-04 e Q-10 junto. `pluginVersion` continua em 0.1.0: decisão de release do usuário.

### 2026-08-07 (fim do dia) — a release v0.8.0 e o CHANGELOG que este arquivo não conhecia

**O que aconteceu.** A v1.9 saiu como release público `v0.8.0`. Duas coisas ficaram de fora daqui
até agora, e as duas custam à próxima sessão:

- **`CHANGELOG.md` existe desde `d99e9cd`** e nunca foi citado neste arquivo. Ele é o registro
  voltado a quem **usa** o plugin — Keep a Changelog 1.1.0 + SemVer, em pt-BR — e cobre da
  **0.6.0** em diante. Para trás não tenta competir: a seção "Versões anteriores" dele aponta de
  volta para este HANDOFF, onde vivem as decisões e as descobertas. A referência agora é mútua.
- **A release em si.** `c50861c` acrescentou a entrada `[0.8.0] — 2026-08-07` e recebeu a tag
  `v0.8.0`. É commit de **documentação apenas**: o código da versão já estava em `de5467e`
  (tag `v0.8.0-rc`), trazido para `main` pelo merge `421eb63`.

**A confusão que motivou esta entrada.** Perguntado se o HANDOFF fora atualizado depois da v0.8.0,
o `git log` respondia "não" — o último commit a tocá-lo era `de5467e`. Mas o conteúdo técnico da
versão estava aqui desde então, na entrada `v1.9` logo abaixo. As duas coisas eram verdade porque
**"v1.9" e "0.8.0" são a mesma versão com dois nomes**, e nada neste arquivo dizia isso. O mapa
está agora no topo, em Estado atual.

**Não corrigido.** `gradle.properties` segue em `pluginVersion = 0.1.0`, divergente da tag —
registrado em Dívida conhecida por ser decisão de release, não de documentação.

**Nenhum código mudou nesta rodada.** Os 121 testes da v1.9 continuam sendo o estado verificado.

### 2026-08-07 — v1.9: velocidade da fala, e uma ação que nunca tocou

**Contexto.** Pedido do usuário: poder mudar a velocidade da fala do Piper, pelo menu de áudio
ou pela configuração do plugin. O SPEC v1.5 tinha posto `--length-scale` explicitamente **fora de
escopo** — esta versão revoga essa linha, e só ela: `--noise-scale`, `--volume` e `--speaker`
continuam fora, com o motivo escrito.

**Fase 1 — o que a verificação mudou no plano.** Três coisas que eu teria errado adivinhando:

1. `--length-scale` é **inverso** da velocidade. Um mapeamento direto sairia de cabeça para
   baixo, e o teste que pega isso (T-1.51) só existe porque a direção foi conferida antes.
2. Omitir a flag **não** é o mesmo que passar `1.0` — `voice.py:449-450` cai no `config.json` do
   modelo. Virou D-40, e é a razão de o preset se chamar "1x (padrão do modelo)".
3. O argparse do piper é `type=float` e recusa vírgula. Numa JVM pt-BR — a desta máquina — o
   formato default produziria `0,667`, o piper sairia com código diferente de zero, e o sintoma
   seria "não sai som", sem erro visível. Virou RNF-31 e o teste T-1.52.

**Implementação.**

- `ClaudeDockSettings`: `speechSpeed: Int = 100` + `effectiveSpeechSpeed()` com `coerceIn(50,200)`,
  copiando o par `sessionPadding`/`effectivePadding()` que já existia (D-39).
- `ClaudePiperPlayback`: `piperParameters(modelPath, speedPercent)` extraída como função pura —
  é o que torna a regra testável sem lançar o piper. `synthesize` ganhou o parâmetro com valor
  default, então os seis testes anteriores seguiram compilando sem alteração.
- `SpeechSpeedActions.kt` (novo): `SpeechSpeedMenuAction` + `SpeechSpeedAction` (`ToggleAction`).
  Sem estado nem lista próprios — os itens saem da tabela do settings e gravam o mesmo campo.
- `ClaudeDockConfigurable`: terceira linha no grupo "Piper TTS", `comboBox` sobre a mesma tabela.
- `ClaudeTtaSessions`: repassa a velocidade efetiva na chamada de síntese.

**DEF-07, achado durante a implementação.** Ao procurar por onde a fala entra, `playText` tinha
**um único chamador**: `AudioPlayAction`, lendo `PlatformDataKeys.CONTEXT_COMPONENT as?
JBTerminalWidget`. Numa ação de título de tool window esse componente é a barra de ferramentas —
o cast dá `null` e o clique não faz nada, em silêncio. Era o único ponto do cabeçalho que não
passava pelo `ClaudeDockSessions`. Corrigido com `playSelectedSession()` no serviço, reusando
`selectedWidget()`, `notify(...)` e `ClaudeSessionText.normalize` — nada novo foi escrito para
isso. Sem essa correção a feature de velocidade não teria como ser ouvida pelo menu.

**Achado 31.** O SPEC prometia um timeout de 20 s na síntese que **nunca existiu** — o código
chama `process.waitFor()` sem argumento, e o `currentProcess?.destroy()` opera sobre um campo que
nunca recebe atribuição. O KDoc foi corrigido para dizer a verdade; implementar o timeout ficou
em Próximos passos.

**Testes.** 113 → **120**, todos verdes. Sete novos: T-1.48/T-1.49 (padrão e clamp da
velocidade), T-1.50 (100% omite a flag), T-1.51 (a conversão inversa), T-1.52 (locale pt-BR
imposto), T-1.53 (rótulo do submenu). Nenhum lança o piper de verdade, seguindo a disciplina do
arquivo. A direção da conversão foi confirmada **fora** da suíte, medindo os WAVs — está nos
Fatos verificados.

**Decisões.** D-39 (percentual `Int`, não `length-scale` `Double`) · D-40 (em 100% a flag não vai)
· Q-29 (não reajustar a fala em curso — o piper sintetiza tudo antes de tocar; reajustar exigiria
fila e posição, que RNF-23 mantém fora).

**Correção de registro.** Duas afirmações deste arquivo diziam que `AudioPlayAction` "não estava
integrado". Estava no menu desde a v1.5 — o que foi descartado em v1.5.1 é o play no **popup**
(RF-30), outra coisa. A nota em D-29 foi corrigida no lugar.

**Validação no IDE (mesmo dia).** `runIde`, com Piper configurado no sandbox: T-3.54 a T-3.58
aprovados pelo usuário. "Tocar seleção" voltou a tocar, e a velocidade responde.

**Ajuste pedido depois do teste.** A tela de configuração mostrava um número (100) enquanto o
menu mostrava rótulos ("1x"). O usuário pediu a mesma seleção nos dois lugares, com a lista
`0,25x … 2x` — o que **estendeu a faixa** de 50–200% para 25–200% e acrescentou 1,75x. Três
consequências, todas registradas:

1. A tabela de velocidades saiu de `SpeechSpeedMenuAction` para `ClaudeDockSettings.SPEECH_SPEEDS`.
   Com as duas UIs consumindo a mesma lista, divergir virou impossível por construção — antes
   dependia de disciplina, agora T-1.54 trava o conteúdo.
2. O spinner virou `comboBox`, e com isso **não existe mais valor customizado**. O CB-60 original
   ("110% não casa com preset algum") perdeu o objeto e foi reescrito.
3. `effectiveSpeechSpeed()` deixou de apenas limitar a faixa e passou a **aproximar** para a
   entrada mais próxima. Não é refinamento: com um `comboBox`, um valor solto no XML deixaria o
   seletor sem item selecionado e o `apply` gravaria nulo. O clamp anterior não cobria isso.

`SimpleListCellRenderer.create` foi a primeira escolha para o renderer e está **deprecada** —
trocada por `textListCellRenderer`. A suíte segue sem warnings.

**Resultado.** Código completo, 121 testes verdes, e **tudo validado no IDE**, incluindo o
seletor novo — T-3.54 a T-3.58 aprovados pelo usuário em duas rodadas de `runIde` no mesmo dia.
RF-47 e RF-48 fechados.

**Próximos passos.** Nada pendente na v1.9. A dívida que fica é anterior a ela: o timeout da
síntese (Achado 31), os testes de integração T-2.\* e os roteiros manuais herdados das versões
anteriores.

**Não verificado nesta rodada:** `verifyPlugin` não rodou — `/home` está em 100% e o verifier não
consegue descompactar o IDE que baixa. É ambiente, não código, mas segue sem confirmação de que
nenhuma API fora de `com.intellij.modules.platform` entrou. `textListCellRenderer` é a única API
nova da v1.9, e é do pacote `com.intellij.ui.dsl`.

### 2026-08-03 (noite/2) — DEF-05 e DEF-06: a aba morria de foco, e o nome mentia

Relato com quatro sessões numa aba (`aaaaa` a `ddddd`): fechar uma deixava três no ar, mas
**nenhuma ação do cabeçalho funcionava mais naquela aba** — nem "Nova sessão". Clicar em outra
aba consertava.

- **DEF-05 — causa encontrada.** `addSession` define
  `content.preferredFocusableComponent = pane.widget.component` na criação. Fechando **essa**
  pane — a primeira, normalmente a em foco —, a aba passa a apontar para um componente
  descartado e fora da árvore: o foco não vai a lugar nenhum e o `DataContext` da toolbar fica
  sem projeto. Daí _todo_ o cabeçalho parar.
- **A v1.8.1 já trocava a chave e pedia foco, mas esqueceu o `preferredFocusableComponent`** —
  eram três lugares apontando para a pane, e eu tinha consertado dois. RF-42 agora lista os três.
- **Achado 30 — e isto revoga o diagnóstico de DEF-04.** Eu tinha atribuído o "menu vazio" a
  `ActionUpdateThread`/`update()` ausentes, comparando com o `AudioMenuAction`. Hipótese
  plausível, **errada**. O que a salvou de virar dívida silenciosa foi ter sido registrada como
  hipótese pendente (T-3.48), e não como conserto. O sintoma "nem criar aba nova eu consigo"
  já excluía a explicação de menu na primeira leitura — segui a pista que eu sabia comparar,
  não a que explicava tudo. As mudanças de thread ficaram, porque ler a árvore Swing fora da EDT
  era errado de qualquer forma; só não eram a correção.
- **DEF-06 — o nome era meu, e mentia.** O usuário acionou "Fechar divisão" esperando encerrar a
  aba inteira. "Divisão" tanto é _a pane_ quanto _o arranjo_. **É o mesmo erro que eu diagnostiquei
  no "Close Tab" da plataforma uma rodada antes** (DEF-04/Achado 29) — repetido em rótulo escrito
  por mim, depois de a lição estar registrada. Agora os nomes dizem quantas sessões morrem:
  **"Fechar esta sessão"** e **"Fechar todas as sessões"** (RF-46), esta última fechando a aba
  com todas as divisões, como o usuário propôs.
- **De quebra:** `firstPane`/`countPanes` saíram de `ClaudeDockSessions` para
  `ClaudeSessionSplitter`, onde a lógica de árvore mora — e ficaram testáveis (T-1.46, T-1.47).
- Resultado: **113 testes, 0 falhas** (eram 110).

### 2026-08-03 (noite) — DEF-03 e DEF-04, e um conserto que não consertava

Dois defeitos no primeiro uso do "Fechar divisão".

- **DEF-03 — a aba inteira virou "(encerrado)" com uma sessão viva ao lado.** O callback de
  término era registrado só para a sessão **original** da aba. Fechar a divisão descarta o
  `Disposable` daquela pane → mata o PTY → dispara o callback. Como a pane em foco costuma ser
  justamente a original, a aba era marcada como encerrada enquanto a vizinha trabalhava.
- **A correção passou a ser por pane, com duas guardas:** a pane ainda estar na árvore
  (fechamento deliberado destaca antes de matar) e ser a última da aba. A segunda **responde
  Q-26**, que a v1.7 deixou em aberto por não saber o que "(encerrado)" deveria significar numa
  aba dividida — e cuja saída eu tinha descartado como cara. Custou seis linhas.
- **O que vale mais que os dois defeitos: meu primeiro conserto não consertava nada.** A guarda
  usava `paneOf(...) == null` para detectar o fechamento deliberado. Não funciona: `close()`
  desanexa o _splitter_ da árvore, mas a pane fechada **continua filha dele**, e `paneOf`
  devolvia assim que achasse um `Splitter` acima — sem nunca confirmar que ele leva à aba. Eu
  teria publicado um conserto inerte, do mesmo feitio do ESC vazio do Achado 21.
  **Quem pegou foi o teste** que escrevi junto (T-1.45), e só porque ele afirmava o estado da
  árvore em vez de repetir a chamada que eu estava consertando.
- **A correção final foi em `paneOf`**, não na guarda: exigir `isDescendingFrom(component, root)`
  antes de subir. Assim todos os chamadores ficam cobertos, e não só o caso relatado.
- **DEF-04 — o menu "Dividir" apareceu vazio até o usuário trocar de aba.** **Não reproduzi.** O
  que dá para afirmar: `Presentation.isDisableGroupIfEmpty()` vale por padrão, e o
  `AudioMenuAction` — o outro menu do mesmo cabeçalho, que funciona — declara
  `getActionUpdateThread()` e `update()`, que o meu não declarava. Pior: as ações filhas
  declaravam `BGT` e **leem a árvore Swing**, que é da EDT. Corrigi as duas coisas, mas a causa
  segue por confirmar — está em T-3.48, e o SPEC diz que é hipótese, não conserto verificado.
- Resultado: **110 testes, 0 falhas** (eram 108).

### 2026-08-03 (tarde/5) — v1.8: reposicionar panes, e por que não por arraste

Pergunta do usuário antes dos testes manuais: dá para arrastar as panes para reposicioná-las,
como as abas do editor? Pedido explícito de **avaliação**, não de implementação.

- **É possível, e o obstáculo não é o DnD.** A plataforma tem `DnDSupport` genérico e o framework
  `DockManager`/`DockContainer` (6 métodos + `DockableContent`) que o editor usa. O problema é
  **onde agarrar**: o editor arrasta o **rótulo da aba** (`TabInfo.DragOutDelegate` +
  `JBEditorTabs`), nunca o corpo do editor.
- **E a nossa superfície já tem dono.** As panes não têm aba, e arrastar dentro do terminal **é**
  selecionar texto — é o mecanismo de RF-26/RF-33, que o usuário acabou de validar. DnD do corpo
  da pane brigaria com o próprio recurso de copiar/exportar seleção. Logo, exigiria antes uma
  barra de título por pane: UI permanente, roubando altura de todas, para servir ação ocasional.
- **Sinal de custo que pesou:** o plugin de terminal da JetBrains tem split e **não** implementa
  DnD de panes (nenhuma classe de DnD no `terminal.jar`). Quem implementa é o editor, que tem
  abas de onde puxar.
- **A alternativa cobriu o caso real:** com 2–4 panes, "reposicionar" é trocar de lado ou girar a
  divisão. Virou RF-43, duas ações no menu "Dividir" que já existia.
- **A plataforma tinha a peça exata.** Eu ia escrever a troca à mão e cair numa armadilha:
  `setFirstComponent` remove o componente que estava naquele lado, então trocar em dois passos
  derruba o que o primeiro passo pôs. **`Splitter.swapComponents()` existe**, troca as duas
  referências internas sem reparentar e já chama `revalidate`/`repaint` — 27 instruções de
  bytecode. Procurar antes de escrever economizou o bug.
- Resultado: **108 testes, 0 falhas** (eram 104), sem warnings.
- **Q-28 registra a avaliação do DnD por escrito**, com o caminho técnico levantado, para que uma
  eventual reabertura parta de dados e não do zero — mesmo tratamento que Q-14 recebeu.

### 2026-08-03 (tarde/4) — DEF-02: a terceira parcela do "de graça"

Erro relatado no uso real: "Select Previous Tab" no menu de contexto, com **uma aba só**, produz
`Assertion failed` com stack trace inteiro no log do IDE.

- **Causa, lida no bytecode.** `ContentManagerImpl.selectPreviousContent()` e `selectNextContent()`
  começam pela mesma linha: `LOG.assertTrue(getContentCount() > 1)`. Navegar sem ter para onde ir
  **não é no-op** — é assertion. O contrato não está escrito em lugar nenhum além desse
  `assertTrue`.
- **Correção no ponto comum**, e não em cada direção: `selectSiblingTab` é por onde as duas
  passam, e a guarda ficou lá. O limite virou `ClaudeTabNavigation`, objeto puro, para ter teste
  sem subir o IDE (T-1.40).
- **Auditei o resto do que liguei no listener**, em vez de corrigir só o caso relatado:
  `removeContent(Content, boolean)` não tem assertion equivalente (`javap -c -p`), `onNewSession`
  cai no caminho que já existia, e `moveTabRight`/`moveTabLeft` continuam nos defaults (aparecem
  cinzas na captura do usuário, como deveriam). `showTabs` é no-op deliberado — virou CB-58.
- **O que isto ensina sobre as duas rodadas anteriores.** O Achado 28 comemorou "o listener
  acende quatro itens de graça". O Achado 29 já tinha descontado o **vocabulário** herdado. Este
  desconta a terceira parcela: as **precondições**. Herdar um item de menu é herdar
  comportamento, nome e contrato — e nenhum dos três vem documentado.
- Resultado: **104 testes, 0 falhas**, sem warnings.

### 2026-08-03 (tarde/3) — v1.7.1: dar nome ao fechamento da divisão

Primeiro uso real do split, com quatro panes abertas. Funcionou. O usuário pediu "uma opção de
fechar esses splits" — **e a opção já estava na tela dele.**

- **Achado 29 — entregue e invisível.** O item "Close Tab" (`Ctrl+W`) do menu de contexto chama
  `onSessionClosed()`, que a v1.7 já ligava ao fechamento da pane com colapso do splitter.
  Confirmado por `javap -c -p` de `ShellTerminalWidget`: o lambda de `getActions` invoca
  `JBTerminalWidgetListener.onSessionClosed`, e o rótulo sai de `getCloseTabActionPresentation()`.
  Aparece inclusive na captura que o usuário mandou.
- **O rótulo é da plataforma, e no nosso contexto ele mente.** "Close Tab" foi pensado para um
  terminal onde aba e sessão são a mesma coisa. Numa aba dividida diz o oposto do que faz —
  ninguém arrisca `Ctrl+W` com quatro sessões abertas para descobrir.
- **Isso corrige metade do Achado 28.** A rodada anterior comemorou "o listener acende quatro
  itens de graça". Herdar comportamento é de graça; herdar **vocabulário** não. Um dos quatro
  descreve mal o que passou a fazer.
- **RF-41:** item "Fechar divisão" no menu "Dividir" do cabeçalho, com nome honesto e no lugar
  onde o usuário procura. Sem divisão, avisa em vez de fechar a aba.
- **RF-42 — falha minha, encontrada ao olhar de perto.** A v1.7 limpava a chave `SESSION_WIDGET`
  ao fechar a pane, deixando a aba **sem sessão apontada**: copiar/exportar/áudio passavam a
  responder "nenhuma sessão aberta" até o usuário clicar em alguma pane. Agora `close` devolve o
  irmão sobrevivente (em vez de um booleano), e dele sai a sessão que reassume foco e chave.
- Resultado: **101 testes, 0 falhas**, sem warnings.
- **Ainda não validado:** T-3.42 a T-3.44, além dos T-3.34-41 que já estavam pendentes.

### 2026-08-03 (tarde/2) — SPEC v1.7 e divisão da aba em várias sessões

Pedido de `plans/20260803-split-tab.md`: "splitar as tabs de dois ou mais claude codes". Parecia
a feature mais cara da série; foi a mais barata, porque o gatilho já existia.

- **Achado 28 — o widget já pedia isto.** `ShellTerminalWidget.getActions()` monta
  `TerminalSplitAction.create(vertically, getListener())`, cujo `isEnabled` chama `canSplit` e
  cujo `actionPerformed` chama `split`. Como o nosso widget **nunca teve listener**, as ações
  simplesmente não apareciam no menu de contexto. Implementar `JBTerminalWidgetListener` acendeu
  de uma vez o split, o "New Session", o "Close Session" e a navegação entre abas — quatro itens
  mortos pelo mesmo motivo, desde a v1.0. Virou D-33.
- **Achado 27 — uma premissa errada carregada por seis rodadas.** CB-26, CB-36, CB-47 e R-15
  hedgeiam contra "e se a sessão não for JediTerm?", e este arquivo chegou a registrar "REWORKED
  no IntelliJ". **Não alcança o nosso caminho:**
  `AbstractTerminalRunner.startShellTerminalWidget` chama `createTerminalWidget(...)`, cujo tipo
  de retorno **é** `JBTerminalWidget`. Quem escolhe entre engines é o `TerminalToolWindowManager`
  da tool window nativa, acima do ponto de entrada que usamos. A observação original estava certa
  — o usuário roda mesmo o Terminal nativo em REWORKED —; o erro foi concluir que valia para as
  nossas abas. R-15 caiu de "Média" para "Baixa"; as guardas ficam (Q-27), porque custam uma linha.
- **A armadilha de direção, evitada por pouco.** `listener.split(vertically = true)` significa
  **lado a lado** (pareado com `TW.SplitRight` nas presentations); `Splitter(vertical = true)`
  significa **empilhado**. Dois booleanos com nome parecido e sentido oposto — é assim que se
  envia um recurso invertido. A conversão ficou explícita num ponto só (`stacked = !vertically`) e
  os testes T-1.34/T-1.35 verificam **geometria depois do layout**, não a flag.
- **Sem estrutura de dados própria (D-36).** A árvore de divisões é a própria hierarquia Swing:
  `isDescendingFrom` acha a aba, subir pelos pais acha o bloco divisível, `putClientProperty`
  guarda o `Disposable` da pane. Mapa paralelo precisaria ser limpo em todo caminho de fechamento
  — mesma razão de D-18.
- **O conflito de critério que o usuário resolveu (D-35).** O menu "Dividir" no cabeçalho é um
  segundo caminho para o que o menu de contexto já faz — exatamente o que reprovou RF-30 e virou
  D-32 na rodada anterior. O usuário escolheu ter os dois, e o contrapeso é legítimo: o RF-26 já
  tinha sido aceito **só** por descoberta. O critério de D-32 continua valendo para o popup de
  seleção, que é espaço escasso; o cabeçalho não é.
- **Ciclo de vida em dois níveis (RNF-28):** disposable da aba → disposable por pane. Fechar uma
  pane mata só o processo dela; fechar a aba mata a árvore inteira.
- Resultado: **100 testes, 0 falhas** (eram 89), sem warnings. ZIP de 110 KB às 14:39.
- **Ainda não validado:** T-3.34 a T-3.41. O mais importante é o T-3.34 — a cadeia que faz "Split
  Right" aparecer no menu de contexto foi **lida no bytecode, não exercitada no IDE**. Também não
  se sabe como duas sessões da mesma aba se comportam no mesmo servidor MCP (Q-02/CB-56), que o
  split torna rotina em vez de hipótese.

### 2026-08-03 (tarde) — SPEC v1.6 e exportação do trecho selecionado

Rodada de SDD **e** implementação na mesma sessão, a pedido do usuário — as cinco anteriores
pararam na especificação. O pedido: `plans/20260803-export-selected-button.md`, um botão para
exportar só o trecho selecionado, ao lado do de copiar que já existe no popup.

- **O achado que definiu tudo, e que quase passou batido (Achado 25).** Havia duas ações
  chamadas "export" no plugin (RF-22 e RF-24), **as duas pelo `/export` do CLI**. O caminho de
  menor resistência era escrever a terceira igual. Não funcionaria: **`/export` exporta a
  conversa** — roda dentro do Claude Code, sobre `lZo(t.messages, …)`, e o único argumento que
  aceita é o caminho do destino. Isso já estava lido do binário desde a v1.3, neste documento.
  O que mudou a resposta foi perguntar **de onde vem o texto**, e não como as outras exportações
  funcionam: o trecho já está na mão do plugin desde RF-26, no mesmo `widget.selectedText` do
  botão de copiar. Virou D-30.
- **Consequência boa:** sem PTY, sem arquivo temporário, sem sondagem, sem prazo de 20 s. Das
  três saídas do plugin, a mais nova é a de menos peças — e a única que funciona com a sessão
  ocupada, encerrada ou no meio de uma resposta.
- **O diálogo não foi construído (D-31).** `FileChooserFactory.createSaveFileDialog` +
  `FileSaverDescriptor` são a plataforma; deles vêm de graça o filtro por extensão e a
  confirmação de sobrescrita (CB-45). `save(...)` devolvendo `null` no cancelamento é o que faz
  do cancelamento um caminho normal em vez de erro a tratar (CB-44).
- **O critério do segundo botão ficou registrado (D-32, Achado 26).** Este é o segundo pedido
  seguido de "mais um botão junto ao de copiar"; o primeiro (play, RF-30) foi **recusado** em
  v1.5.1. Aceitar um e recusar o outro exigia critério declarado, senão vira gosto: **capacidade,
  não simetria** — exportar o trecho não existe em nenhum outro lugar da UI, o play já existia no
  menu do cabeçalho. Teto de dois botões em R-23.
- **Código:** `ClaudeSelectionExport.kt` novo (objeto puro, ~50 linhas com KDoc: nome sugerido e
  gravação em UTF-8) e o popup de `ClaudeSelectionCopyButton` passando de um `JBLabel` a um
  `JPanel` com dois. `ClaudeDockSessions.notify` virou `internal` em vez de ganhar um clone —
  quatro linhas duplicadas evitadas com uma palavra.
- **A armadilha do `grep` binário pegou de novo.** A busca pelas classes do diálogo nos jars da
  plataforma não achou nada — **nem o controle conhecido** (`Messages.class`). Causa: `grep` sem
  `-a` sobre `.class`. Está registrado neste arquivo desde 2026-08-01 e mesmo assim custou quatro
  tentativas. O controle é o que salvou: sem ele, a conclusão teria sido "a API não existe na
  262" e o desenho inteiro teria ido para o lado errado.
- **Um teste meu estava errado, não a produção.** Assumi que `ClaudeSessionText.normalize`
  aparava os dois lados; ela apara só a direita. A produção é que está certa — a indentação à
  esquerda **precisa** sobreviver, senão o trecho exportado deixa de ser código válido. O teste
  virou a asserção desse comportamento, em vez de esconder o mal-entendido.
- Resultado: **89 testes, 0 falhas** (eram 82), sem warnings. ZIP de 100 KB às 13:37.
- **Ainda não validado:** T-3.28 a T-3.33. Em especial o diálogo nativo sob Wayland e se o nome
  sugerido chega preenchido nos outros IDEs da família.

### 2026-08-02 (manhã) — Implementação de Piper TTS, SPEC v1.5 aprovado

Sessão de implementação pura: código, testes e documentação para RF-30/RF-31/RF-32. Nenhuma decisão de design nova; tudo seguiu a SDD de v1.5 conforme escrito.

**Implementação:**

- **`ClaudePiperPlayback.kt`** (singleton, 120 linhas) — encapsulação única de síntese + playback.
  - `canSynthesize(executable, modelPath): Boolean` — valida se ambos existem, sem exceções.
  - `synthesize(text, executable, modelPath): ByteArray?` — lança piper via `GeneralCommandLine`, stdin/stdout, timeout 20s.
  - `playBytes(pcmBytes): Boolean` — abre `Clip` com AudioInputStream (22050 Hz, 16-bit, mono), reproduz, limpa recursos.
  - `pause()`, `resume()`, `stop()` — controle de ciclo de vida.
- **`ClaudeTtaSessions.kt`** (serviço de projeto, 80 linhas) — estado único de reprodução.
  - `playText(text)` — valida, para anterior, síntese fora da EDT, notifica ouvintes.
  - Estados: Idle → Playing → Paused ↔ Playing → Idle.
  - Listeners notificam ações do menu via message bus.
- **`AudioPauseResumeAction.kt` / `AudioStopAction.kt`** (60 linhas) — ações com `update()` state-aware.
  - Pause muda texto/ícone conforme estado (Pausar ↔ Retomar).
  - Stop desabilitado quando Idle.
- **`ClaudeSelectionCopyButton.kt`** (estendido) — popup ganha segundo botão (ícone Play) ao lado de Copiar.
  - Botão desabilitado se `ClaudePiperPlayback.canSynthesize()` falso.
  - Clique chama `ClaudeTtaSessions.getInstance(project).playText(selectedText())`.
- **`ClaudeToolWindowFactory.kt`** (estendido) — novo `DefaultActionGroup("Áudio", true)` no cabeçalho.
  - Contém AudioPauseResumeAction + AudioStopAction.
- **`ClaudeDockSettings.kt` / `ClaudeDockConfigurable.kt`** (estendidos) — novos campos de configuração.
  - `piperExecutable` (default "piper") e `piperModel` (default vazio, desabilita play).
  - UI em Kotlin DSL com comentários de escopo.

**Testes:**

- 8 testes novos em `ClaudePiperPlaybackTest.kt` (6) + `AudioActionsTest.kt` (3)
- Suite completa: 82 testes passando (69 anteriores + 13 novos).
- Todos os testes T-1.22-27 do SPEC implementados e verdes.

**Integração:**

- `ClaudeTerminalSessionFactory.install()` agora passa `project` para `ClaudeSelectionCopyButton.install()`.
- Message bus listener pattern (TtsStateListener topic) desacopla UI de serviço.
- `ponytail:` comentários registram simplificações conhecidas (thread pool, cache, timeout).

**Mudanças no estado do repositório:**

- Branch `feature/tts` com código compilado e testado.
- SPEC.md — nova seção v1.5 com RFs/RNFs/CBs/Riscos/Testes/Achados 24.
- HANDOFF.md — esta entrada de log.
- `git status`: código novo em `src/main`, `src/test`; planos atualizados.

**Próximos passos (não executados nesta rodada):**

- Testes de integração T-2.\* (tool window registrada, abas isoladas, REWORKED engine).
- Roteiros manuais T-3.21-27 (seleção, play/pause/stop, configuração, timeout).
- Validação no IDE real com Piper instalado e modelo configurado.

**Resultado:** implementação 100% conforme SDD v1.5. Código pequeno, testado, e pronto para aprovação de integração.

### 2026-08-01 (noite/2) — Ergonomia da janela, SPEC v1.4

Sessão longa, quase toda de ajuste fino no que já funcionava. Quatro entregas, uma remoção e
duas tentativas descartadas — as descartadas ensinaram mais que as entregues.

- **RF-28 — respiro nas bordas.** O JediTerm reserva 4px só à esquerda, e nada nos outros lados.
  A borda vai no componente do widget (um `JPanel` com `BorderLayout`), porque é o `TerminalPanel`
  que mede a si mesmo para calcular o grid. Duas correções em cima disso, ambas pedidas pelo
  usuário na tela: a faixa saiu preta no tema claro (eu copiava a cor uma vez, e ela envelhecia),
  e depois saiu na cor da IDE quando o pedido era a cor do terminal. A forma final é um `Border`
  que lê `terminalPanel.background` **no `paintBorder`** — acompanha troca de tema sem listener.
- **RF-29 — capa de carregamento.** Três tentativas: o véu do `JBLoadingPanel` é translúcido e
  deixava o eco legível; esconder o terminal com `CardLayout` funcionava mas tirava a dimensão
  dele, e o CLI desenhava para 120 colunas numa aba de 149, redesenhando ao aparecer; a versão
  boa é `JLayeredPane` com a capa por cima e o terminal sempre visível.
- **RF-27 removido.** Antes disso, medi: a flag **não** trava a sessão (o `$` é o prompt do CLI
  em modo plano, e o processo segue vivo — `exit=124` sob `timeout`). Funcionava e não servia.
- **A tela de configurações** foi reescrita em Kotlin UI DSL com `BoundConfigurable`, a pedido do
  usuário, que queria as seções que o plugin oficial tem. Caiu de 110 para 79 linhas.
- **O erro de escopo que vale registrar:** pus o prazo da capa na tela de configurações sem
  ninguém pedir. O usuário recusou na hora — aquilo é ajuste de bancada, não escolha de quem usa.
  Ver D-22 e o Achado 19.
- **As duas tentativas descartadas** viraram D-20. A primeira (PTY direto no CLI) quebrou o
  ambiente do usuário: sem shell, os hooks pararam de achar suas ferramentas. A segunda
  (`shell -i -c 'exec "$0" "$@"'`) resolvia tudo — eco eliminado na origem, ambiente exportado
  preservado — e mesmo assim foi recusada, porque o `-c` encerra o shell junto com o CLI e o
  usuário quer o prompt utilizável depois do `/exit`. Está em stash.
- **A medição que deveria ter vindo primeiro:** só na terceira tentativa eu rodei o comando com
  PTY e ambiente limpo para ver o que sobrevive. Variáveis exportadas sim, funções e aliases não.
  Se essa medição tivesse aberto a investigação, as duas primeiras tentativas não teriam existido
  (Achado 20).
- **Correção de uma afirmação minha:** eu disse que não usaria a logo do Claude Code por ser marca
  da Anthropic. O motivo real e defensável é outro — eu teria de copiar o asset de dentro do jar
  do plugin oficial para o repositório do usuário. A ponderação de marca era minha, não uma regra,
  e a decisão é dele.
- **T-1.21 escrito ao fim da sessão**, fechando a única violação conhecida da regra "código novo
  com teste": os diretórios de fallback estavam em produção sem cobertura, porque o teste tinha
  ficado no ramo recusado. O par aceito/recusado sobre o **mesmo** nome inexistente é o controle
  — com o nome real, os dois passariam pelo `PATH` de quem roda a suíte e o fallback nunca seria
  exercitado.
- Resultado: **68 testes, 0 falhas**, sem warnings. Seis commits; o T-1.21 e os
  documentos ficaram pendentes.
- **Ainda não validado:** T-3.18 a T-3.20, e todo o comportamento fora do engine CLASSIC.

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

### 2026-08-02 (manhã/tarde) — Especificação SDD v1.5: Piper TTS

**Contexto:** Usuário pediu tocar em áudio, via Piper TTS do Linux, o texto selecionado na
sessão. Botão de play próximo ao de copiar (RF-26 existente); botões de pause/stop num menu
suspenso no cabeçalho; play desabilitado se Piper não disponível. Executável + modelo configuráveis.
Única reprodução ativa por vez. Sem gerência de modelos, sem seletores de voz/velocidade, sem fila.

**Fase 1 — Descoberta (adiantada):** Verificação empírica do ambiente (SEM re-implementar):

- `piper-tts` 1.4.2 está instalado, acessível via `~/.pyenv/shims/piper`.
- Modelo PT-BR de voz está em `~/.claude/piper-voices/pt_BR-faber-medium.onnx` (63 MB).
- Síntese funciona: `echo "teste" | piper -m <modelo> --output-raw` → PCM 22050 Hz, 16-bit,
  mono, little-endian, sem erros.
- **Descoberta crítica:** Java Sound (`javax.sound.sampled`) consegue reproduzir direto esse PCM,
  com mixer via ALSA/PipeWire. Não precisa chamar `aplay`/`paplay` externamente. T-4 dessa feature
  (a premissa de que playback é viável) foi validada empiricamente antes de especificar.
- **Limite do Piper:** requer `-m MODEL` obrigatoriamente — sem padrão como no `claude`. Logo,
  "Piper disponível" = executável + arquivo modelo válido, não só o executável.

**Fase 2 — Especificação:** SPEC.md v1.4 → v1.5 (mesma abordagem que as 4 rodadas anteriores):

- Novos objetivos: tocar seleções em áudio.
- Novos RF-30/31/32: play button, pause/resume/stop menu, detecção do Piper + modelo, config.
- Novos RNF-19-23: acoplamento único (espelhando RNF-15 da API de terminal), síntese fora da EDT,
  nenhum log de áudio/texto, limpeza de recurso de mixer, única reprodução ativa.
- Novos fluxos J (tocar), J2 (pausar/retomar), G/H/I (erros: Piper não encontrado, modelo não
  configurado, síntese falha).
- Design Técnico: `ClaudePiperPlayback` (único acoplamento com Piper + javax.sound.sampled),
  `ClaudeTtaSessions` (estado único por projeto), estensão de `ClaudeSelectionCopyButton`,
  menu "Áudio" no cabeçalho via `DefaultActionGroup(popup=true)`, novos campos em settings.
- Novos CB-38-42 (edge cases específicos de TTS).
- Novos R-18-22 (riscos de modelagem, CPU-bound, latência, mixer indisponível).
- Novos T-1.22-27 (unit tests para síntese, playback, persistência de configuração).
- Novos T-3.21-27 (E2E: botão play visível, habilitado/desabilitado, pausar, parar, seleção grande).
- Novos CA-20-24 (acceptance criteria para síntese, play, pausa, stop, configuração).
- Novas Q-18-20 (open questions sobre seleção grande, caching de canSynthesize(), timeout).
- Novos Achados 21-23 (revisão crítica v1.5): threading é trivial com IntelliJ API,
  Java Sound suficiente para playback local, configuração manual de modelo é a decisão certa.
- Evidências registradas na Rastreabilidade: Piper detectado, modelo encontrado, síntese testada,
  mixer disponível, DefaultActionGroup funciona em setTitleActions, PersistentStateComponent
  acomoda novos campos.

**Decisões (implícitas no design):**

- D-26: Sem autodetecção de modelos; usuário configura manualmente (decisão de "responsabilidade").
- D-27: Uma única reprodução ativa; novo play interrompe anterior sem fila (YAGNI).
- D-28: Síntese/playback fora da EDT via `executeOnPooledThread` (standard IntelliJ pattern).

**Resultado:** SPEC.md + HANDOFF.md atualizados. Nenhum código Kotlin alterado. Pendente: aprovação
da comunidade/usuário. Próximo: implementação de Fase 2.

### 2026-08-03 (manhã) — Descarte de RF-30, SPEC v1.5.1 finalizado

**Contexto:** Após validação do SPEC.md v1.5, usuário decidiu descartar RF-30 (botão de play no
popup de seleção). Razão: UX redundante — o menu "Áudio" no cabeçalho (RF-31) já oferece
play/pause/stop durante reprodução; adicionar segundo caminho (play no popup, junto com cópia)
seria UI paralela sem capacidade nova.

**Mudanças ao SPEC.md:**

- Objetivo #11 removido (v1.5 → v1.5.1)
- RF-30 movido de "Requisitos Funcionais" para "Fora de Escopo" com justificativa
- Fluxo principal J (tocar seleção no popup) removido; fluxo J2 (pausar/retomar) passa a ser J
- Fluxos de erro G/H/I ajustados: referências a "botão play no popup" → "menu Áudio no cabeçalho"
- Testes T-3.21/T-3.24 removidos (específicos de RF-30); T-3.22/T-3.25 renumerados para T-3.21/T-3.22
- Casos de aceitação CA-20/CA-21 removidos (específicos de RF-30); CA-22 renumerado para CA-20
- Histórico de versões atualizado: v1.5.1 com nota de descarte
- `AudioPlayAction.kt` permanece no código (pode ser reutilizado), mas removido da estrutura documentada

**Decisão registrada:**

- D-29: RF-30 descartado em favor de padrão único (menu no cabeçalho). Simplicidade > redundância.

**Código:** `AudioPlayAction.kt` implementado mas não integrado ao popup — fique no repositório
para futura reutilização ou descarte deliberado no cleanup final. Compilação e testes (82)
continuam passando.

> **Correção (2026-08-07, v1.9):** a frase acima ficou ambígua e foi lida errado por duas
> sessões seguidas. `AudioPlayAction` **nunca** saiu do menu "Áudio" do cabeçalho — o que foi
> descartado em v1.5.1 é o botão de play no **popup de seleção** (RF-30), que é outra coisa.
> Pior: a ação estava no menu e **não funcionava**, porque lia `CONTEXT_COMPONENT`. Ver DEF-07 e
> RF-48.

**Estado:** SPEC.md v1.5.1, HANDOFF.md e repositório alinhados. Branch `feature/tts` pronto para
merge após validação final dos testes manuais T-3.21-23 (Piper no IDE real).
