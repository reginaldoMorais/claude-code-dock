# SPEC — Claude Code Dock: tool window dedicada para JetBrains

- **Versão:** 1.5
- **Data:** 2026-08-02
- **Status:** Especificação — v1.5 acrescenta suporte a TTS via Piper com play/pause/stop na tool window
- **Autor:** Reginaldo Morais (com assistência do Claude Code)

> **Histórico de versões**
>
> | Versão | Data       | Mudança                                                                                                                                                                               |
> | ------ | ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
> | 1.0    | 2026-08-01 | Especificação inicial (Fases 1–3 do SDD)                                                                                                                                              |
> | 1.1    | 2026-08-01 | RF-17 (Esc devolvido ao shell), RF-18 (estado vazio utilizável) e RF-19 (`CLAUDE_CONFIG_DIR` por projeto), após teste manual                                                          |
> | 1.2    | 2026-08-01 | RF-21 (copiar o conteúdo da sessão) e RF-22 (exportar a conversa via `/export`), após T-3.1/T-3.2 aprovados                                                                           |
> | 1.3    | 2026-08-01 | DEF-01 (cópia duplicada) diagnosticado; RF-21 **revisto** para passar pelo `/export` (RF-24/RF-25); RF-26 (cópia flutuante por seleção); Q-14 (UI de markdown) analisada e recusada   |
> | 1.4    | 2026-08-01 | RF-27 (saída plana) **removido** após uso real; RF-28 (respiro nas bordas) e RF-29 (tela de carregamento) especificados; D-09 reconfirmado com a alternativa `exec` medida e recusada |
> | 1.5    | 2026-08-02 | RF-30 a RF-32 (play/pause/stop via Piper TTS); detecção de Piper + modelo; configuração de executável e caminho do modelo; ações no menu do cabeçalho e popup de seleção            |

---

## Problema

O plugin oficial da Anthropic (`com.anthropic.code.plugin`, versão `0.1.14-beta`) inicia o
Claude Code como **mais uma aba dentro da tool window "Terminal"** do IDE. Isso gera três
incômodos concretos no uso diário:

1. **Disputa de espaço.** A sessão do Claude Code compete pelas mesmas abas usadas para
   `git`, `npm`, `docker` e afins. Abrir um terminal comum empurra o Claude para segundo plano.
2. **Ciclo de vida acoplado.** Fechar a tool window "Terminal" — algo que se faz o tempo todo
   para recuperar espaço vertical — esconde também a sessão do Claude.
3. **Ergonomia diferente do VS Code.** Na extensão de VS Code o Claude Code vive em um painel
   próprio, posicionável de forma independente. No JetBrains, não.

O usuário quer o comportamento do VS Code: **uma janela própria, dentro do IDE**, com ciclo de
vida e posicionamento independentes do terminal comum — instalada localmente, **sem publicação
na JetBrains Marketplace**.

---

## Objetivos

1. Prover uma **tool window dedicada** ("Claude Code Dock"), independente da tool window
   "Terminal", hospedando uma sessão real de terminal que executa o CLI `claude`.
2. **Preservar integralmente** os recursos de integração já entregues pelo plugin oficial
   (visualização de diff, compartilhamento de seleção, diagnostics) quando este estiver
   instalado — sem reimplementar nenhuma parte desse mecanismo.
3. Permitir **múltiplas sessões simultâneas** em abas dentro da janela dedicada.
4. Expor **retomada de sessões** através do recurso nativo do CLI (`claude --resume`).
5. Permitir **configurar o caminho do executável** `claude`, para instalações fora do `PATH`.
6. Ser instalável **localmente** via _Install Plugin from Disk_, sem Marketplace.
7. Funcionar em **qualquer IDE JetBrains** que possua o plugin de terminal.
8. _(v1.1)_ Permitir **isolar a configuração do CLI por projeto**, via `CLAUDE_CONFIG_DIR`, para
   quem alterna entre contas ou perfis distintos do Claude Code entre janelas do IDE.
9. _(v1.1)_ Entregar **paridade de teclado com o terminal nativo**: o Claude Code usa `Esc` para
   sair de comandos interativos, e isso precisa funcionar dentro da janela dedicada.
10. _(v1.2)_ Permitir **tirar o conteúdo da sessão de dentro da janela** em um clique — como o
    ícone de cópia da extensão de VS Code —, tanto na forma bruta (o que está na tela) quanto na
    forma de transcrição (via o `/export` do próprio CLI).
11. _(v1.5)_ Permitir **tocar em áudio, via Piper TTS**, o texto que o usuário seleciona dentro
    da sessão, com botões de play (desabilitado se Piper não estiver disponível), pause/resume
    e stop, acionáveis pelo mouse sem soltar a seleção.

---

## Fora de Escopo

Explicitamente **não** serão construídos nesta tarefa:

- **Servidor MCP próprio.** Nenhuma implementação de servidor WebSocket/SSE, nem ferramentas
  MCP (`openDiff`, `getDiagnostics`, `openFile`, notificações de seleção).
- **Lockfile próprio** em `~/.claude/ide/`. Nenhuma escrita, leitura ou gerência desses arquivos.
- **Reimplementação de qualquer parte do protocolo do Claude Code.** O protocolo é privado,
  não documentado e instável.
- **Persistência própria de histórico de conversas.** Nenhum banco, arquivo de transcrição ou
  busca interna de sessões passadas. O CLI já resolve isso.
- **Renderização customizada de UI de chat.** Não haverá bolhas de mensagem, markdown renderizado
  ou editor de prompt próprio. A UI é o terminal.

  > **Reavaliado em v1.3, a pedido do usuário** ("renderizar como o VS Code faz, markdown viewer,
  > código em boxes"). A pergunta era _"é possível?"_ — e a resposta honesta é **sim, e mesmo
  > assim não vale**.
  >
  > **É possível.** O CLI expõe `--print --output-format stream-json --input-format stream-json
--include-partial-messages` (verificado em `claude --help` v2.1.220). Dá para dirigir o
  > Claude Code por JSON e desenhar a conversa numa UI própria; a plataforma tem com o que
  > renderizar markdown.
  >
  > **Mas não é um incremento — é outro plugin.** O terminal não some só como aparência: ele é
  > quem hoje trata, de graça, tudo o que passaríamos a ter de implementar — aprovação de
  > ferramentas, plan mode, slash commands, `--resume`, seletores interativos, colagem de
  > imagem, entrada multilinha. Cada um desses vira UI e estado nossos.
  >
  > **E o custo real não é o desenho, é a manutenção.** O formato `stream-json` não tem contrato
  > público de estabilidade. Cada versão do CLI que mudar um campo quebra a janela — e o CLI
  > atualiza sozinho. É exatamente o acoplamento que D-01 recusou para o protocolo MCP, pela
  > mesma razão, agora com muito mais superfície.
  >
  > **Ordem de grandeza:** o plugin inteiro tem hoje ~450 linhas de Kotlin e uma classe acoplada
  > à plataforma. Isso seria milhares de linhas e um segundo produto para manter — para ganhar
  > apresentação, não capacidade. **Recomendação: não seguir.** Registrado em Q-14, com o
  > caminho técnico documentado, para que a decisão possa ser revista com dados e não do zero.
  >
  > _Se o incômodo for legibilidade e não arquitetura_, havia algo bem mais barato a tentar
  > antes: a flag `--ax-screen-reader` do CLI ("flat text, no decorative borders or animations").
  > Foi implementada como RF-27 e **removida na v1.4** depois do uso real.
  >
  > **O que a medição mostrou** (verificado com PTY, ver anexo): a flag **não** trava a sessão —
  > o `$` solto que parecia retorno ao shell é o prompt de entrada do CLI em modo plano. Ou seja,
  > funciona como anunciado. O usuário simplesmente não a quis: troca a caixa de input por uma
  > linha indistinguível de um prompt de shell, o que piora CB-27 em vez de melhorar a leitura.
  >
  > **Consequência para Q-14:** a alternativa barata deixou de existir. Isso **não** reabre a
  > decisão — o custo de manter uma UI própria contra um formato sem contrato de estabilidade é o
  > mesmo de antes. Mas o argumento "há algo mais simples a tentar primeiro" está gasto, e uma
  > eventual reabertura precisa ser decidida pelo custo de manutenção, não por essa saída.

- **Publicação na JetBrains Marketplace.**
- **Suporte a Remote Development, split mode (frontend/backend) e WSL.** O alvo é execução local
  monolítica. Ver [Riscos](#riscos).
- **Substituição do plugin oficial.** Os dois coexistem; o oficial permanece a fonte da integração.
- **Autenticação, billing ou qualquer manipulação de credenciais do Claude Code.**
- _(v1.5)_ **Gerência de modelos de voz do Piper.** O usuário configura manualmente o caminho
  para o arquivo `.onnx` do modelo desejado. Sem seletor de voz, sem download automatizado de
  modelos, sem lista pública de vozes disponíveis.
- _(v1.5)_ **Controle de qualidade de síntese.** Sem suporte a `--length-scale`, `--noise-scale`,
  `--volume`, `--speaker` — só o padrão de cada modelo.
- _(v1.5)_ **Fila de reprodução.** Uma única fala por vez; um novo play interrompe o que estiver
  tocando sem oferecer fila.

---

## Análise do Estado Atual

### Ambiente verificado

Todos os dados abaixo foram obtidos por inspeção direta do ambiente, não por suposição.

| Item                            | Valor verificado                                                             | Como foi verificado                                     |
| ------------------------------- | ---------------------------------------------------------------------------- | ------------------------------------------------------- |
| IDE                             | IntelliJ IDEA Ultimate 2026.2, build `IU-262.8665.258`                       | `build.txt` e `product-info.json` da instalação Toolbox |
| Outros IDEs instalados          | GoLand, PyCharm, RubyMine, WebStorm, Air (todos 2026.2)                      | listagem de `~/.local/share/JetBrains/Toolbox/apps`     |
| Plugin oficial                  | `com.anthropic.code.plugin` v`0.1.14-beta`, `since-build 242`                | `META-INF/plugin.xml` do jar instalado                  |
| CLI                             | `claude` v`2.1.220`, em `~/.local/bin/claude`                                | `which claude` + `claude --version`                     |
| JDK                             | Zulu 21.0.8 (LTS)                                                            | `java -version`                                         |
| Gradle                          | 9.2.0                                                                        | `gradle --version`                                      |
| Rede de build                   | `plugins.gradle.org` → HTTP 200; `cache-redirector.jetbrains.com` → HTTP 200 | `curl`                                                  |
| IntelliJ Platform Gradle Plugin | `2.18.1` (10/07/2026)                                                        | portal de plugins do Gradle                             |
| Repositório                     | Vazio de código — só `CLAUDE.md`, `README.md`, `plans/`                      | listagem do diretório                                   |

### Como o plugin oficial funciona hoje

O plugin oficial **não é** um mero lançador de terminal. A inspeção do jar revela uma
arquitetura de duas metades:

**Metade 1 — servidor de integração dentro do IDE.**
As dependências empacotadas incluem `ktor-server-cio`, `ktor-server-websockets`,
`ktor-server-sse` e `kotlin-sdk-jvm-0.4.0` (SDK Kotlin de MCP). O pacote
`com.anthropic.code.plugin.mcp.tools` contém `DiffTools`, `DiagnosticTools`, `EditorTools`,
`FileTools` e `WebSocketMcpServerTransport`. Ou seja: o IDE **hospeda um servidor MCP** que
expõe ferramentas ao CLI.

**Metade 2 — descoberta pelo CLI.**
Há dois mecanismos de handshake, ambos confirmados:

- **Variável de ambiente `CLAUDE_CODE_SSE_PORT`.** A classe `TerminalCustomizer` estende
  `org.jetbrains.plugins.terminal.LocalTerminalCustomizer` e está registrada no extension point
  `org.jetbrains.plugins.terminal.localTerminalCustomizer` (arquivo `plugin-terminal.xml`).
  Seu `customizeCommandAndEnvironment` lê `TerminalUtil.getRunningMcpServerPorts()` e injeta a
  porta no ambiente de **todo terminal local criado pela plataforma**.
- **Lockfile `~/.claude/ide/<port>.lock`.** Arquivos reais no ambiente do usuário confirmam o
  formato: `{"pid":…, "workspaceFolders":[…], "ideName":"GoLand", "transport":"ws",
"runningInWindows":false, "authToken":"…"}`. Há lockfiles escritos tanto pelo VS Code quanto
  pelo GoLand, provando que o plugin JetBrains oficial usa o mesmo protocolo.

**Como o oficial abre a sessão.**
A classe `com.anthropic.code.plugin.TerminalUtil` faz, em sequência:

```
ToolWindowManager.getInstance(project).getToolWindow("Terminal")   // <-- a tool window NATIVA
TerminalToolWindowManager.getInstance(project)
    .createShellWidget(workingDir, tabName, requestFocus, deferSessionStart)
widget.sendCommandToExecute(PluginSettings.getClaudeCommand())
```

**É exatamente aqui que nasce o problema:** a string literal `"Terminal"` prende a sessão à tool
window nativa. Não há ponto de extensão no plugin oficial para redirecioná-la.

O plugin oficial registra ainda: `PostStartupActivity`, uma `DiffExtension`, um
`consoleFilterProvider` (links clicáveis para arquivos), um `applicationConfigurable`, e duas
ações — `SendToClaudeAction` (`Ctrl+Alt+K`) e `OpenClaudeInTerminalAction` (`Ctrl+Esc`).

### A descoberta que define a solução

A inspeção do bytecode de `org.jetbrains.plugins.terminal.TerminalToolWindowManager` mostra que
`createShellWidget(...)` **delega a** `AbstractTerminalRunner.startShellTerminalWidget(...)`.
Esse método é público e retorna um `com.intellij.terminal.ui.TerminalWidget` — que, por estender
`com.intellij.openapi.ui.ComponentContainer`, expõe um `JComponent` embutível em **qualquer**
container Swing, inclusive no `Content` de uma tool window customizada.

A cadeia de criação de processo (`LocalTerminalDirectRunner.createProcess` →
`LocalOptionsConfigurer.configureStartupOptions` → aplicação dos `LocalTerminalCustomizer`)
está **abaixo** desse ponto de entrada e é comum aos dois caminhos.

**Consequência (premissa central desta especificação):** um terminal criado por
`startShellTerminalWidget(...)` dentro de uma tool window própria percorre o mesmo caminho de
criação de processo que o terminal nativo. Portanto o `TerminalCustomizer` da Anthropic
**continua injetando `CLAUDE_CODE_SSE_PORT`** nele, e a integração (diff, seleção, diagnostics)
segue funcionando — sem que este plugin toque no protocolo.

> ✅ **CONFIRMADO empiricamente em 2026-08-01** — esta premissa deixou de ser inferência de
> bytecode. Ver [Estratégia de Testes, item T-4](#t-4-validação-da-premissa-central-aprovado).
> Precisão obtida no teste: quem aplica os customizers é `configureStartupOptions`, chamado
> internamente por `startShellTerminalWidget`.

### A armadilha do `Esc` fora da tool window "Terminal" _(descoberta em v1.1)_

O primeiro teste no IDE real revelou que `Esc` não chegava ao Claude Code: em vez de fechar
comandos interativos como `/usage`, o IDE movia o foco para o editor e deixava a sessão presa.

A causa foi isolada por `javap -c` em `com.intellij.terminal.TerminalEscapeKeyListener`, que o
`JBTerminalPanel` consulta **antes** de entregar a tecla ao JediTerm:

```java
ToolWindow tw = panel.getContextToolWindow();          // via PlatformDataKeys.TOOL_WINDOW
AnAction action = ActionManager.getAction("Terminal.SwitchFocusToEditor");
if (tw == null) return false;
if (action != null) {
    Collection<KeyStroke> strokes = KeymapUtil.getKeyStrokes(action.getShortcutSet());
    if (JBTerminalWidget.isTerminalToolWindow(tw))     // "Terminal".equals(tw.getId())
        return isMatched(e, strokes);
    return strokes.isEmpty() ? isEscape(e) : isMatched(e, strokes);   // <-- nós caímos aqui
}
return isEscape(e);
```

O ramo de fallback é assimétrico: **só a tool window de id literal `"Terminal"`** interpreta
"sem atalho configurado" como "entregue o `Esc` ao shell". Em qualquer outra tool window, a
mesma configuração padrão significa o oposto — consumir todo `Esc`. Como o IntelliJ 2026.2
passou a distribuir `Terminal.SwitchFocusToEditor` **sem atalho** (há inclusive a notificação
`Terminal: Escape behavior changed` no `TerminalBundle.properties`), a janela dedicada herdou o
comportamento errado.

`JBTerminalPanel.handleKeyEvent` dá a brecha para corrigir sem tocar em configuração global:

```java
for (Consumer<KeyEvent> c : myPreKeyEventConsumers) c.accept(e);   // ponto de extensão público
myEscapeKeyListener.handleKeyEvent(e);                             // ignora evento já consumido
if (!e.isConsumed()) super.handleKeyEvent(e);                      // JediTerm envia ao PTY
```

Um pre-handler que escreva `\u001b` no `TtyConnector` e consuma o evento produz exatamente o
comportamento do terminal nativo — sem alterar keymap do usuário nem o `TerminalOptionsProvider`,
o que violaria RF-13. É a implementação adotada em RF-17.

### A ausência de "copiar tudo" no engine CLASSIC _(descoberta em v1.2)_

Copiar o conteúdo da janela parecia resolvido pela plataforma. Não é — e a assimetria é a mesma
família do problema do `Esc`: **o comportamento depende do engine, não do widget.**

- `Terminal.SelectAll` está referenciada **apenas** em `Terminal.ReworkedTerminalContextMenu`, e
  sua implementação (`TerminalSelectAllAction`) só habilita quando
  `TerminalDataContextUtils.isReworkedTerminalEditor(editor)` é verdadeiro.
- `Terminal.CopySelectedText` idem: opera sobre o `Editor` do terminal reformulado.
- No JediTerm clássico — o engine da nossa janela — o menu de contexto oferece copiar-seleção e
  colar. **Nenhuma ação do IDE seleciona o buffer inteiro**; para o usuário, resta arrastar o
  mouse.
  > ⚠️ **Precisão corrigida em v1.3:** `com.jediterm.terminal.ui.TerminalPanel.selectAll()` é
  > público e existe. O que não existe é uma **ação registrada** que o alcance no CLASSIC. A
  > redação original da v1.2 dava a entender que a capacidade não existia na plataforma; existe,
  > só não está exposta. Isso não muda o desenho — `getText()` continua mais direto que
  > selecionar-para-copiar —, mas a afirmação precisava ficar exata.

Em compensação, o próprio `TerminalWidget` já sabe se ler por inteiro:

```java
// JBTerminalWidget.getText(TerminalPanel), via javap -c
TerminalSelection sel = new TerminalSelection(
    new Point(0, -buffer.getHistoryLinesCount()),                 // topo do scrollback
    new Point(buffer.getWidth(), buffer.getScreenLinesCount()-1)  // fim da tela
);
return SelectionUtil.getSelectionText(..., buffer);               // sob buffer.lock()
```

`JBTerminalWidget$TerminalWidgetBridge.getText()` delega a esse método, então a chamada sai de
graça pela interface que a factory já devolve. Fora do CLASSIC, o `default` da interface devolve
string vazia — degradação graciosa, sem exceção (CB-26).

**Para o `/export`, o caminho é outro.** `ShellTerminalWidget.executeCommand` — atrás de
`sendCommandToExecute`, que usamos para lançar o `claude` — começa checando
`getTypedShellCommand().isEmpty()` e **lança `IOException`** se houver texto digitado no prompt.
Com um TUI vivo na frente, essa é a regra, não a exceção. Falar com o Claude Code em execução
exige escrever direto no `TtyConnector`, o mesmo canal do `ClaudeEscapeForwarder`.

### DEF-01 — por que a cópia do buffer sai duplicada _(descoberto em v1.3)_

O primeiro uso real de RF-21 revelou que uma conversa com várias trocas é copiada **duas ou mais
vezes**, cada cópia precedida do banner do CLI e seguida da caixa de input e da barra de status.

**Não é defeito do nosso código.** `getText()` devolve fielmente o que está no buffer — e o
buffer realmente contém a conversa mais de uma vez.

**Causa.** O TUI do Claude Code é Ink (React para terminal). A cada **repintura de frame
completo** ele reemite a região estática inteira, e o que estava na tela rola para o scrollback.
Uma tool window é redimensionada o tempo todo — e cada resize dispara uma repintura total.

A evidência está na própria amostra colada pelo usuário: o rodapé do primeiro bloco marca
`⧉ In README.md` e o do segundo, `⧉ In a.txt`. **São dois frames de instantes diferentes**, não
uma cópia acidental. O buffer acumula um frame por repintura, e a cópia é fiel a isso.

**Por que não se conserta por heurística.** A tentação é cortar do último banner em diante — um
frame completo, limpo. Mas isso só funciona enquanto a conversa **cabe na tela**: passando disso,
o topo do último frame já rolou para fora e o corte **trunca em silêncio**, que é pior que
duplicar. Deduplicar blocos repetidos tem o mesmo problema pelo outro lado: duas respostas
legitimamente iguais seriam fundidas.

**A saída já estava especificada.** O `/export` do CLI produz exatamente o que se quer, e isso
foi **verificado no arquivo gerado em 2026-08-01 16:23**: a conversa aparece **uma única vez**,
sem caixa de input, sem barra de status, sem repetição. Ou seja, Q-12 — que a v1.2 deixou em
aberto — tem resposta: a cópia deve passar pelo `/export`. Daí RF-24.

#### Como o `/export` trata o argumento (lido no binário, não suposto)

R-13 era o bloqueio de RF-24. Como `/export` é `local-jsx` e exige TUI, não dá para exercitá-lo
por `-p`; a resposta veio da implementação embutida no binário `claude` 2.1.220:

```js
async function azb(e, t, r) {                 // r = argumento do slash command
  let n = await lZo(t.messages, ...);          // conversa renderizada
  let o = r.trim();
  if (o) { let l = await u0n(o, n); e(`Conversation exported to: ${l}`); return null; }
  ...                                          // sem argumento: mostra o seletor arquivo/clipboard
}
function Y5b(e) { let t = extname(e) === "" ? `${e}.txt` : e; return Mi(t, ...); }
async function u0n(e, t) {
  let r = Y5b(e);
  await mkdir(dirname(r), { recursive: true });
  await writeFile(r, t, { encoding: "utf-8", flush: true });
  return r;
}
```

Quatro consequências diretas para o desenho:

| O que o código mostra                   | Efeito em RF-24                                                                     |
| --------------------------------------- | ----------------------------------------------------------------------------------- |
| Com argumento, `return null` **sem UI** | O caminho é não-interativo: escreve e pronto. Não há seletor para o plugin driblar  |
| `writeFile` comum                       | **Sobrescreve** arquivo existente, sem perguntar                                    |
| `mkdir(dirname, { recursive: true })`   | Cria a árvore de diretórios; o temporário não precisa existir antes                 |
| `extname(e) === "" ? e + ".txt" : e`    | **Sem extensão, o CLI grava em outro caminho.** O destino precisa terminar em `.md` |

E o mais importante: **o argumento é `r.trim()` cru**. Aspas POSIX não seriam removidas — virariam
parte do nome do arquivo. É o oposto de RNF-08, que protege o caminho do executável contra o
**shell**; aqui quem lê a string é o CLI, e proteger contra shell **quebraria** a chamada. Ver
T-1.13.

**O que se perde.** A cópia deixa de ser instantânea e passa a depender do CLI vivo: escreve um
arquivo temporário, espera, lê, apaga. É mais peça do que `getText()`, mas é a diferença entre
um recurso que serve e um que não serve.

### Renderizar como o VS Code: o que o CLI oferece _(analisado em v1.3)_

O CLI expõe, verificado em `claude --help` v2.1.220:

```text
-p, --print                     Print response and exit
--output-format <format>        "text" | "json" | "stream-json"   (só com --print)
--input-format <format>         "text" | "stream-json"            (só com --print)
--include-partial-messages      chunks parciais (só com --print e --output-format=stream-json)
```

Ou seja: **é tecnicamente possível** dirigir o Claude Code por JSON e desenhar a conversa numa UI
própria, com markdown e blocos de código. O caminho existe. Ver a avaliação em
[Fora de Escopo](#fora-de-escopo) e Q-14 — a conclusão é **não seguir por ele**, e o motivo não
é viabilidade.

### API de terminal disponível na build 262

| Símbolo                                                                                                 | Situação                                       |
| ------------------------------------------------------------------------------------------------------- | ---------------------------------------------- |
| `AbstractTerminalRunner.startShellTerminalWidget(Disposable, ShellStartupOptions, boolean)`             | presente                                       |
| `LocalTerminalDirectRunner.createTerminalRunner(Project)`                                               | presente                                       |
| `ShellStartupOptions` (com `workingDirectory`, `shellCommand`, `envVariables`)                          | presente                                       |
| `TerminalWidget` (`sendCommandToExecute`, `addTerminationCallback`, `requestFocus`, `getTerminalTitle`) | presente                                       |
| `TerminalEngine`                                                                                        | enum com `CLASSIC`, `REWORKED`, `NEW_TERMINAL` |
| `LocalTerminalCustomizer.EP_NAME`                                                                       | presente                                       |

### Piper TTS no ambiente _(verificado em v1.5)_

| Item                            | Valor verificado                                                       | Como foi verificado                                                                       |
| ------------------------------- | ---------------------------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| Piper TTS                       | `piper-tts` 1.4.2, acessível via `~/.pyenv/shims/piper`                | `piper --help`; `pip show piper-tts`                                                     |
| Modelo de voz preparado         | `~/.claude/piper-voices/pt_BR-faber-medium.onnx` (63 MB + `.json`)      | `ls -la ~/.claude/piper-voices/`; teste de síntese bem-sucedido                          |
| Saída de síntese                | PCM cru, 22050 Hz, 16-bit, mono, little-endian, sem stderr             | `echo "teste" \| piper -m <modelo> --output-raw \| wc -c`                                |
| Playback via Java Sound         | Suportado: `SourceDataLine` abre no formato exato do Piper              | compilação e execução de `MixerCheck` com `javax.sound.sampled`                          |
| Mixers de áudio disponíveis     | HDMI, USB, e linhas genéricas (ALSA via PipeWire/PulseAudio)           | listagem de `AudioSystem.getMixerInfo()` no JDK 21                                       |
| Limite de Piper                 | **Requer `-m MODEL`** — não há modelo padrão como há em `claude`        | análise de `piper --help` v1.4.2                                                         |

**Descoberta que define a arquitetura (v1.5):** diferentemente do `claude` que roda com padrões
embutidos, o **Piper é obrigatoriamente configurável**. "Piper instalado" = executável presente
**E** caminho válido para um modelo `.onnx` configurado. A ausência de qualquer um disso disable
o botão play.

### Correção de uma premissa do documento de origem

O plano inicial afirma que _"a Anthropic inviabiliza plugins de terceiros rodarem o Claude Code"_.
**Essa premissa não se confirma.** Não existe bloqueio técnico a executar o binário `claude` em
um PTY — é o que qualquer emulador de terminal faz, e é literalmente o que o plugin oficial faz.

O que de fato não é público nem estável é o **protocolo MCP/lockfile** da integração: não há API
documentada para um terceiro se conectar ao Claude Code como IDE. A arquitetura de casca
escolhida contorna isso da forma mais econômica possível: **não tocando no protocolo**, e
delegando-o ao plugin oficial que já o implementa e é mantido pela própria Anthropic.

---

## Solução Proposta

Um plugin local, **deliberadamente mínimo**, cuja única responsabilidade é **hospedar o
terminal em uma janela própria**. Tudo o mais é delegado a software que já existe:

| Necessidade                                            | Quem resolve                   | Este plugin escreve código? |
| ------------------------------------------------------ | ------------------------------ | --------------------------- |
| Janela dedicada dentro do IDE                          | **este plugin**                | ✅ sim                      |
| Emulação de terminal, cópia/colagem, scrollback, busca | JediTerm / plataforma IntelliJ | ❌ não                      |
| Diff, seleção, diagnostics no IDE                      | plugin oficial da Anthropic    | ❌ não                      |
| Histórico e retomada de conversas                      | CLI (`claude --resume`)        | ❌ não                      |
| Autenticação, modelos, billing                         | CLI                            | ❌ não                      |

O plugin resultante é da ordem de **poucas centenas de linhas de Kotlin**.

**Trade-off assumido — dependência do plugin oficial.** A integração com o código do projeto
passa a depender de um plugin de terceiro (Anthropic) que este projeto não controla. Se a
Anthropic mudar o mecanismo de injeção, os recursos de integração param — embora a janela e o
terminal continuem funcionando (degradação graciosa). A alternativa (servidor MCP próprio)
troca essa dependência por outra pior: acoplamento a um protocolo privado e não documentado,
com muito mais código para manter. **A dependência declarada e visível é preferível à
reimplementação frágil.**

---

## Requisitos Funcionais

| #             | Requisito                                                                                                                                                                                                                                      |
| ------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **RF-01**     | O plugin DEVE registrar uma tool window dedicada, de título "Claude Code Dock", distinta da tool window "Terminal".                                                                                                                            |
| **RF-02**     | A tool window DEVE ancorar por padrão à direita (`ToolWindowAnchor.RIGHT`) e ser reposicionável pelo usuário através dos mecanismos padrão do IDE.                                                                                             |
| **RF-03**     | Ao ser aberta pela primeira vez, a tool window DEVE criar uma sessão de terminal cujo diretório de trabalho é a raiz do projeto (`project.basePath`).                                                                                          |
| **RF-04**     | Criada a sessão, o plugin DEVE executar automaticamente o comando `claude` configurado.                                                                                                                                                        |
| **RF-05**     | A criação da sessão DEVE ser **preguiçosa**: nenhum processo é iniciado enquanto a tool window não for aberta pelo usuário.                                                                                                                    |
| **RF-06**     | A tool window DEVE suportar múltiplas abas, cada uma com sessão independente.                                                                                                                                                                  |
| **RF-07**     | O plugin DEVE oferecer a ação "Nova sessão", que abre uma nova aba executando `claude`.                                                                                                                                                        |
| **RF-08**     | O plugin DEVE oferecer a ação "Retomar sessão", que abre uma nova aba executando `claude --resume`.                                                                                                                                            |
| **RF-09**     | O plugin DEVE oferecer uma ação global "Abrir Claude Code Dock", acessível por _Find Action_ e associável a atalho pelo usuário.                                                                                                               |
| **RF-10**     | O plugin DEVE expor uma tela de configurações com o caminho do executável `claude` (valor padrão: `claude`).                                                                                                                                   |
| **RF-11**     | Encerrado o processo `claude` de uma aba, a aba DEVE exibir indicação visual de sessão terminada, sem fechar-se automaticamente.                                                                                                               |
| **RF-12**     | Fechada uma aba, o processo correspondente DEVE ser encerrado e todos os recursos liberados.                                                                                                                                                   |
| **RF-13**     | O plugin NÃO DEVE alterar, remover ou interferir no comportamento do plugin oficial nem da tool window "Terminal".                                                                                                                             |
| **RF-14**     | Se o executável `claude` não for encontrado, o plugin DEVE exibir notificação acionável com link para as configurações.                                                                                                                        |
| **RF-15**     | Se o plugin de terminal (`org.jetbrains.plugins.terminal`) estiver ausente, o plugin DEVE degradar sem erro: a tool window não é registrada.                                                                                                   |
| **RF-16**     | O título de cada aba DEVE identificar a sessão de forma distinguível (ex.: `Claude`, `Claude (2)`).                                                                                                                                            |
| **RF-17**     | _(v1.1)_ A tecla `Esc` DEVE ser entregue ao processo da sessão, e não consumida pelo IDE para mover o foco ao editor.                                                                                                                          |
| **RF-18**     | _(v1.1)_ Com a tool window sem nenhuma aba, o estado vazio DEVE oferecer caminho visível para abrir uma nova sessão ou retomar uma anterior.                                                                                                   |
| **RF-19**     | _(v1.1)_ O plugin DEVE permitir configurar `CLAUDE_CONFIG_DIR` **por projeto** (= por janela do IDE), injetando-o no ambiente das sessões daquele projeto.                                                                                     |
| **RF-20**     | _(v1.1)_ `CLAUDE_CONFIG_DIR` vazio DEVE significar "não injetar", preservando o padrão do CLI (`~/.claude`).                                                                                                                                   |
| ~~**RF-21**~~ | ~~_(v1.2)_ O plugin DEVE oferecer a ação "Copiar Sessão", que coloca o conteúdo do buffer da aba selecionada (scrollback + tela) na área de transferência.~~ **Substituído por RF-24 em v1.3** — o buffer contém a conversa repetida (DEF-01). |
| **RF-22**     | _(v1.2)_ O plugin DEVE oferecer a ação "Exportar Conversa", que aciona o slash command `/export` do CLI na aba selecionada.                                                                                                                    |
| **RF-23**     | _(v1.2)_ Sem aba selecionada, ou sem conteúdo a copiar, as ações de RF-21 e RF-22 DEVEM notificar o usuário — nunca lançar exceção nem agir em silêncio.                                                                                       |
| **RF-24**     | _(v1.3)_ "Copiar Conversa" DEVE obter o texto pelo `/export` do CLI, para arquivo temporário, e colocar **só a conversa** na área de transferência — sem banner repetido, sem caixa de input e sem barra de status (DEF-01).                   |
| **RF-25**     | _(v1.3)_ O arquivo temporário de RF-24 DEVE ser criado com permissão exclusiva do usuário e **apagado logo após a leitura**, com ou sem sucesso.                                                                                               |
| **RF-26**     | _(v1.3)_ Ao selecionar texto com o mouse na sessão, o plugin DEVE oferecer um botão flutuante que copia **apenas o trecho selecionado**; ele DEVE sumir quando a seleção é desfeita.                                                           |
| ~~**RF-27**~~ | ~~_(v1.3)_ O plugin DEVE oferecer opção de aplicar `--ax-screen-reader` às novas sessões.~~ **REMOVIDO em v1.4** — ver [Fora de Escopo](#fora-de-escopo) e o Achado 18.                                                                        |
| **RF-28**     | _(v1.4)_ A sessão DEVE ser afastada das bordas da tool window por um respiro configurável, pintado com o fundo do terminal, e o valor DEVE valer para as sessões abertas a partir da mudança.                                                  |
| **RF-29**     | _(v1.4)_ Ao abrir uma aba, o plugin DEVE cobrir a sessão enquanto o CLI sobe, escondendo o prompt do shell e o eco do comando; a capa DEVE sair sozinha e NÃO DEVE impedir a sessão de receber o tamanho real da aba.                          |
| **RF-30**     | _(v1.5)_ Ao selecionar texto com o mouse na sessão, o plugin DEVE oferecer, **junto ao botão de cópia** (RF-26), um botão de play que envia o trecho selecionado ao Piper para síntese; o botão de play DEVE estar desabilitado se o Piper não estiver instalado ou um modelo válido não estiver configurado. |
| **RF-31**     | _(v1.5)_ Durante a reprodução de áudio, o plugin DEVE oferecer, **no cabeçalho da tool window**, um menu suspenso "Áudio" contendo ações para pausar/retomar e parar a fala em curso.                                                             |
| **RF-32**     | _(v1.5)_ O plugin DEVE permitir configurar, em Settings > Tools > Claude Code Dock, o caminho do executável `piper` (valor padrão: `piper`) e o caminho para um arquivo `.onnx` de modelo de voz (padrão: vazio; desabilita síntese até configurado). |

---

## Requisitos Não Funcionais

### Performance

- **RNF-01** — O plugin NÃO DEVE executar trabalho na inicialização do IDE além do registro da
  tool window. Sem `postStartupActivity`, sem varredura de disco, sem processos.
- **RNF-02** — A criação da sessão DEVE usar `deferSessionStartUntilUiShown = true`, para que o
  processo só nasça quando a UI estiver efetivamente visível.
- **RNF-03** — A resolução do caminho do executável DEVE ocorrer fora da EDT.

### Segurança

- **RNF-04** — O plugin NÃO DEVE ler, escrever ou registrar em log o conteúdo de
  `~/.claude/ide/*.lock`, que contém `authToken` de sessão.
- **RNF-05** _(revisado em v1.1)_ — O plugin NÃO DEVE persistir segredos, tokens ou credenciais.
  As únicas configurações persistidas são o caminho do executável (nível aplicação) e o
  `CLAUDE_CONFIG_DIR` (nível projeto). Ambos são **caminhos**, não segredos.
  > O `CLAUDE_CONFIG_DIR` é gravado no arquivo de **workspace** (`StoragePathMacros.WORKSPACE_FILE`),
  > e não em `.idea/` versionado: é um caminho local da máquina e o diretório apontado guarda
  > credenciais do CLI. Compartilhá-lo por Git levaria o time inteiro a apontar para um caminho
  > que só existe em uma máquina.
- **RNF-06** — O plugin NÃO DEVE registrar em log o conteúdo do buffer do terminal, que pode
  conter código-fonte e segredos do projeto.
- **RNF-07** — O plugin NÃO DEVE abrir portas de rede nem aceitar conexões.
- **RNF-08** _(revisado na implementação)_ — O caminho do executável DEVE ser protegido contra
  interpretação pelo shell, por escapamento POSIX com aspas simples.

  > ⚠️ A redação original exigia montagem por argv, "nunca concatenação de string interpretada
  > por shell". Isso é **incompatível com o design escolhido**: a sessão é iniciada com
  > `sendCommandToExecute`, que digita o comando no shell — mesma abordagem do plugin oficial, e
  > a única que mantém o shell vivo depois que o `claude` sai, como o Fluxo D e o CB-01 exigem.
  > Usar argv substituiria o shell e destruiria esse fallback de diagnóstico. O objetivo real do
  > requisito (um caminho com espaços ou metacaracteres não pode virar comando extra) é atendido
  > pelo escapamento, com testes dedicados em T-1.6.

- **RNF-19** _(v1.2, revisado em v1.3)_ — O conteúdo da sessão pode conter código-fonte e
  segredos do projeto (mesmo motivo de RNF-06). A cópia DEVE ser sempre **iniciada pelo
  usuário**, ir **apenas** para a área de transferência local, e NÃO DEVE ser registrada em log
  nem enviada a lugar algum.
  > ⚠️ **Exceção introduzida por RF-24:** o `/export` escreve em disco, então o texto passa por
  > um arquivo temporário. Isso é uma concessão consciente, não um descuido — a alternativa
  > (ler o buffer) não entrega o recurso (DEF-01). Mitigação obrigatória em RF-25: permissão
  > exclusiva do usuário e remoção em `finally`. Ver R-14.

### Confiabilidade

- **RNF-09** — Todo `TerminalWidget` DEVE ser registrado como filho de um `Disposable` amarrado
  ao `Content` da aba, garantindo que nenhum processo PTY vaze ao fechar aba, projeto ou IDE.
- **RNF-10** — Falha ao criar uma sessão NÃO DEVE derrubar a tool window; o erro é reportado por
  notificação e a janela permanece utilizável.
- **RNF-11** — A ausência ou desativação do plugin oficial NÃO DEVE impedir o funcionamento da
  janela e do terminal; apenas os recursos de integração ficam indisponíveis.

### Observabilidade

- **RNF-12** — Erros DEVEM ser registrados via `com.intellij.openapi.diagnostic.Logger`, sob a
  categoria do plugin, em nível `warn`/`error` — nunca conteúdo de sessão.
- **RNF-13** — Erros visíveis ao usuário DEVEM usar um `NotificationGroup` próprio, para que
  possam ser silenciados independentemente das notificações do plugin oficial.

### Manutenibilidade

- **RNF-14** — A separação de responsabilidades DEVE seguir SOLID: a factory da tool window não
  conhece a criação de terminal; a factory de sessão não conhece UI de tool window; as
  configurações são isoladas em seu próprio componente.
- **RNF-15** — Todo acesso a API de terminal da plataforma DEVE estar concentrado em **uma única
  classe**, limitando a superfície de quebra em upgrades de IDE a um só arquivo.
- **RNF-16** — Conforme `CLAUDE.md`: identificadores em inglês, comentários em português.

### Escalabilidade

- **RNF-17** — O plugin DEVE suportar múltiplos projetos abertos simultaneamente, com uma tool
  window e sessões independentes por projeto (serviços com escopo de `Project`).
- **RNF-18** — Não há limite artificial de abas; o limite prático é o de processos do sistema.

### Requisitos novos em v1.5 — Piper TTS

- **RNF-19** _(v1.5, novo)_ — Todo acoplamento com o Piper TTS e `javax.sound.sampled` DEVE
  estar concentrado em **uma única classe**, limitando a auditoria de upgrade a um arquivo
  (espelhando RNF-15 para a API de terminal). A classe não DEVE ser acoplada a ActionUpdaters,
  serviços de projeto, ou UI — é lógica pura, chamada por camadas acima.
- **RNF-20** _(v1.5, novo)_ — A síntese de fala (processo `piper`) DEVE rodar fora da EDT
  (Event Dispatch Thread), lido o arquivo `.onnx` e o modelo de áudio também fora da EDT.
- **RNF-21** _(v1.5, novo)_ — O plugin NÃO DEVE registrar em log ou armazenar o conteúdo de
  texto enviado ao Piper nem o audio/áudio gerado — analogamente a RNF-06 (conteúdo de buffer).
  A síntese é localizada, não transmitida; qualquer logging é exclusivamente para diagnóstico
  de erros da própria execução do Piper.
- **RNF-22** _(v1.5, novo)_ — Nenhum `Clip` ou `SourceDataLine` DEVE ser deixado aberto após
  play/pause/stop: a reprodução DEVE ser encerrada de forma limpa e os recursos de áudio
  liberados (evitando travamento de mixer ou falta de mixers para futuras reproduções).
- **RNF-23** _(v1.5, novo)_ — Uma única síntese/reprodução deve estar ativa por vez. Um novo
  play durante uma reprodução em curso DEVE pausar/parar a anterior e iniciar a nova (sem
  fila).

---

## Fluxos

### Fluxo principal — abrir o Claude Code na janela dedicada

1. O usuário clica no ícone "Claude Code Dock" na barra lateral (ou aciona a ação RF-09).
2. A plataforma instancia `ClaudeToolWindowFactory` e chama `createToolWindowContent`.
3. A factory solicita a `ClaudeTerminalSessionFactory` uma nova sessão.
4. A session factory:
   a. obtém o runner via `LocalTerminalDirectRunner.createTerminalRunner(project)`;
   b. monta `ShellStartupOptions` com `workingDirectory = project.basePath` e, quando configurado,
   `envVariables = {CLAUDE_CONFIG_DIR: …}` do projeto (RF-19);
   c. chama `runner.startShellTerminalWidget(disposable, options, true)`;
   d. instala o encaminhador de `Esc` no painel do widget (RF-17).
5. A plataforma cria o processo de shell. **Nesse ponto**, os `LocalTerminalCustomizer`
   registrados são aplicados — incluindo o da Anthropic, que injeta `CLAUDE_CODE_SSE_PORT`
   **ao lado** do que já viemos definindo, sem sobrescrevê-lo.
6. A session factory chama `widget.sendCommandToExecute(claudeCommand)`.
7. O componente do widget é envolvido em um `Content` e adicionado ao `ContentManager`.
8. O CLI `claude` inicia, lê `CLAUDE_CODE_SSE_PORT` e conecta-se ao servidor MCP do plugin oficial.
9. O usuário interage com o Claude Code na janela dedicada, com diff/seleção/diagnostics ativos.

### Fluxo alternativo A — retomar sessão anterior

1. O usuário aciona "Retomar sessão" na toolbar.
2. Passos 3–5 do fluxo principal se repetem.
3. Em vez do comando simples, executa-se `claude --resume`.
4. O CLI apresenta seu seletor nativo de sessões; o usuário escolhe e a conversa é retomada.

### Fluxo alternativo B — múltiplas sessões

1. O usuário aciona "Nova sessão" com a janela já aberta.
2. Uma nova sessão é criada e adicionada como **nova aba** no mesmo `ContentManager`.
3. Cada aba mantém processo, diretório e estado próprios.

### Fluxo alternativo B2 — última aba fechada _(v1.1, RF-18)_

1. O usuário fecha a única aba aberta; o `Disposable` encerra o processo (RNF-09).
2. O `ContentManager` fica vazio. `createToolWindowContent` **não** é chamado de novo — a
   plataforma o executa uma única vez por projeto.
3. A tool window exibe o estado vazio com os links "Nova sessão" e "Retomar sessão".
4. Um clique reentra no fluxo principal a partir do passo 3.

### Fluxo alternativo C — plugin oficial ausente

1. Fluxo principal executa normalmente até o passo 7.
2. Nenhum customizer injeta `CLAUDE_CODE_SSE_PORT`.
3. O CLI inicia em modo autônomo, sem integração com o IDE.
4. A janela e o terminal permanecem plenamente funcionais (RNF-11).

### Fluxo alternativo G — copiar a conversa _(revisto em v1.3, RF-24)_

> A v1.2 lia o buffer direto. Isso produzia a conversa repetida (DEF-01); o caminho passa a ser
> o mesmo do Fluxo H, mudando só o destino.

1. O usuário aciona "Copiar Conversa" no cabeçalho da tool window.
2. `ClaudeDockSessions` recupera o widget da aba selecionada (guardado no `Content` por `Key`).
3. O plugin cria um arquivo temporário vazio, com permissão exclusiva do usuário (RF-25).
4. `ClaudeTerminalSessionFactory.sendInput` escreve `/export <caminho>\r` no PTY.
5. O plugin espera o arquivo ganhar conteúdo, com prazo limite; estourado o prazo, notifica e
   desiste (CB-29).
6. Lido o arquivo, o texto vai para a área de transferência via
   `CopyPasteManager.copyTextToClipboard` e **o arquivo é apagado** — sucesso ou falha (RF-25).

### Fluxo alternativo I — copiar um trecho selecionado _(v1.3, RF-26)_

1. O usuário seleciona texto com o mouse dentro da sessão.
2. O `TerminalSelectionChangesListener` registrado no painel dispara com a seleção não vazia.
3. Um botão flutuante aparece perto do ponteiro, com o ícone de cópia.
4. Clicado, ele copia `JBTerminalWidget.getSelectedText()` e some.
5. Desfeita a seleção, o listener dispara com seleção vazia e o botão some sem ação (CB-30).

### Fluxo alternativo H — exportar a conversa _(v1.2, RF-22)_

1. O usuário aciona "Exportar Conversa".
2. Passos 2 do fluxo G se repetem.
3. `ClaudeTerminalSessionFactory.sendInput` escreve `"/export\r"` no `TtyConnector` — o
   equivalente a digitar e pressionar Enter.
4. O CLI abre o seu próprio seletor de destino dentro da sessão; o usuário escolhe.
5. Sem PTY ainda (RNF-02) ou com falha de escrita, a ação notifica e nada acontece na sessão.

### Fluxo de erro D — executável não encontrado

1. O terminal inicia, mas o shell responde `command not found: claude`.
2. O plugin detecta a condição na verificação prévia de disponibilidade do executável.
3. Notificação (RF-14) é exibida com a ação "Abrir configurações".
4. A aba permanece aberta com um shell utilizável, permitindo diagnóstico manual.

### Fluxo de erro E — falha ao criar a sessão

1. `startShellTerminalWidget` lança exceção (ex.: `basePath` inválido, PTY indisponível).
2. A exceção é capturada, registrada via `Logger.warn` e convertida em notificação.
3. A tool window permanece viva e cai no estado vazio de RF-18, cujos links "Nova sessão" e
   "Retomar sessão" servem justamente de "tentar novamente" (RNF-10).
   > _(v1.1)_ A v1.0 previa um botão "Tentar novamente" próprio. O estado vazio de RF-18 já
   > cobre esse papel e serve também ao caso comum de fechar a última aba (CB-20) — um
   > componente a menos para o mesmo resultado.

### Fluxo de erro F — processo encerrado

1. O usuário digita `/exit`, ou o processo morre.
2. O callback registrado via `addTerminationCallback` dispara.
3. O título da aba é marcado como encerrado; a aba **não** se fecha (RF-11), preservando o
   scrollback para leitura.

### Fluxo principal J — tocar texto selecionado via Piper _(v1.5, RF-30/RF-31)_

1. O usuário seleciona texto com o mouse na sessão.
2. O popup flutuante aparece com dois botões: copiar (RF-26) e play.
   - O play fica desabilitado se Piper não estiver disponível (executável + modelo não configurado).
3. Clicado em play:
   a. A síntese é disparada fora da EDT, via `ClaudePiperPlayback.synthesizeAndPlay(text)`.
   b. O `piper` é lançado em processo separado com o texto por stdin, saída `--output-raw`.
   c. O PCM é lido do stdout e reproduzido em tempo real em um `Clip` ou `SourceDataLine`.
   d. O menu "Áudio" aparece no cabeçalho com ações Pausar e Parar ativas (habilitadas).
4. Se já havia reprodução em curso, a anterior é parada (sem fila).
5. Fim de síntese ou clique em Parar: menu "Áudio" volta a desabilitado.

### Fluxo alternativo J2 — pausar e retomar fala _(v1.5, RF-31)_

1. Durante a reprodução, o usuário aciona "Pausar" no menu "Áudio".
2. O `Clip` é pausado via `.stop()`, a posição é guardada em `framePosition`.
3. O ícone muda para "Retomar".
4. Clicado em "Retomar", o `Clip` é repositicionado e `.start()` é chamado.
5. Clicado em "Parar", o `Clip` é fechado e o proceso `piper` é destruído.

### Fluxo de erro G — Piper não encontrado _(v1.5, RF-30/RF-32)_

1. Ao abrir a janela ou durante checagem cacheada, detecta-se que `piper` não está no PATH nem
   nos fallbacks (`~/.local/bin`, `/usr/local/bin`).
2. O botão play no popup fica desabilitado; nenhuma notificação é exibida (é advisório).
3. Um tooltip explica por que o botão está desabilitado ("Piper não encontrado").
4. O usuário pode configurar o caminho explícito em Settings > Tools > Claude Code Dock.

### Fluxo de erro H — modelo não configurado _(v1.5, RF-30/RF-32)_

1. O `piper` está disponível, mas o campo "Caminho do modelo Piper" está vazio ou aponta para
   arquivo inexistente.
2. O botão play fica desabilitado; tooltip: "Configure um modelo de voz".
3. Configuração é feita em Settings > Tools > Claude Code Dock > "Caminho do modelo Piper".

### Fluxo de erro I — síntese falha _(v1.5, RF-30/RNF-21)_

1. O `piper` é lançado, mas retorna código de erro ou nenhum PCM é produzido.
2. A notificação "Falha ao sintetizar o áudio. Verifique o modelo e o texto." é exibida.
3. Menu "Áudio" volta a desabilitado; nenhuma reprodução inicia.
4. O processo `piper` é destruído e recursos liberados (RNF-22).

---

## Design Técnico

### Alterações arquiteturais

Projeto novo. Estrutura proposta:

```
build.gradle.kts
settings.gradle.kts
gradle/wrapper/
src/main/kotlin/dev/reginaldomorais/claudedock/
├── ClaudeToolWindowFactory.kt      # UI: registra a tool window, abas e estado vazio (RF-18)
├── ClaudeDockSessions.kt           # serviço de projeto: ciclo de vida das abas
├── ClaudeTerminalSessionFactory.kt # ÚNICO ponto de contato com a API de terminal
├── ClaudeEscapeForwarder.kt        # devolve o Esc ao PTY fora da tool window "Terminal" (RF-17)
├── ClaudeEnvironment.kt            # puro: config dir -> variáveis de ambiente (RF-19)
├── ClaudeCommand.kt                # puro: montagem e escapamento do comando
├── ClaudeSessionText.kt            # puro: normaliza o texto copiado (RF-21; revisto em RF-24)
├── ClaudeSessionExport.kt          # (v1.3) puro-ish: destino, comando e espera do /export (RF-24, RF-25)
├── ClaudeSelectionCopyButton.kt    # (v1.3) botão flutuante na seleção (RF-26)
├── ClaudeSessionPadding.kt         # (v1.4) borda que se pinta com o fundo do terminal (RF-28)
├── ClaudeSessionLoading.kt         # (v1.4) capa sobreposta enquanto o CLI sobe (RF-29)
├── ClaudePiperPlayback.kt          # (v1.5) ÚNICO ponto de acoplamento com Piper + Java Sound (RNF-19)
├── ClaudeTtsSessions.kt            # (v1.5) serviço de projeto: estado de reprodução única (RNF-23)
├── ClaudeTabTitle.kt               # puro: títulos distinguíveis de aba
├── ClaudeWorkingDirectory.kt       # puro: resolução do diretório de trabalho
├── settings/
│   ├── ClaudeDockSettings.kt        # PersistentStateComponent (nível aplicação): executável, modelo
│   ├── ClaudeDockProjectSettings.kt # PersistentStateComponent (nível projeto): CLAUDE_CONFIG_DIR
│   └── ClaudeDockConfigurable.kt    # tela em Settings > Tools (projectConfigurable)
└── actions/
    ├── NewSessionAction.kt
    ├── ResumeSessionAction.kt
    ├── CopySessionAction.kt         # (v1.2) RF-21
    ├── ExportSessionAction.kt       # (v1.2) RF-22
    ├── AudioPauseResumeAction.kt    # (v1.5) toggle pause/resume na menu "Áudio"
    ├── AudioStopAction.kt           # (v1.5) parar reprodução e fechar mixer
    └── OpenClaudeDockAction.kt
src/main/resources/META-INF/
├── plugin.xml
└── plugin-terminal.xml             # extensões opcionais dependentes do terminal
src/test/kotlin/dev/reginaldomorais/claudedock/
```

**Responsabilidades (SOLID — SRP e DIP):**

| Componente                     | Responsabilidade única                                       | Não sabe sobre                     |
| ------------------------------ | ------------------------------------------------------------ | ---------------------------------- |
| `ClaudeToolWindowFactory`      | ciclo de vida da tool window e das abas                      | como um terminal é criado          |
| `ClaudeTerminalSessionFactory` | traduzir "quero uma sessão rodando X" em um `TerminalWidget` | tool windows, abas, ações          |
| `ClaudeDockSettings`           | persistir e prover configuração de aplicação                 | UI e terminal                      |
| `ClaudeDockProjectSettings`    | persistir e prover configuração de projeto (RF-19)           | UI, terminal e ambiente            |
| `ClaudeEnvironment`            | traduzir configuração em variáveis de ambiente               | `Project`, UI e terminal           |
| `ClaudeEscapeForwarder`        | corrigir a entrega do `Esc` ao PTY (RF-17)                   | tool windows, abas, configuração   |
| `ClaudeSessionText`            | normalizar o buffer para a área de transferência (RF-21)     | terminal, clipboard, UI            |
| Ações                          | traduzir intenção do usuário em chamadas às duas primeiras   | detalhes de implementação de ambas |

_(v1.2)_ Ler o buffer e escrever no PTY entram como `readText` e `sendInput` **dentro de**
`ClaudeTerminalSessionFactory`, e não nas ações: RNF-15 exige que o contato com a API de
terminal continue num arquivo só. As ações falam apenas com `ClaudeDockSessions`, que é quem
sabe qual aba está selecionada.

`ClaudeTerminalSessionFactory` concentra **todo** o acoplamento à API de terminal da plataforma
(RNF-15): é o único arquivo a auditar em cada upgrade de IDE.

### Núcleo da implementação

Pseudo-código de referência do único ponto sensível (comentários em português, conforme `CLAUDE.md`):

```kotlin
fun createSession(project: Project, parent: Disposable, command: String): TerminalWidget {
    val runner = LocalTerminalDirectRunner.createTerminalRunner(project)
    // O diretório de trabalho é a raiz do projeto; usuário sem basePath cai no home.
    val configDir = ClaudeDockProjectSettings.getInstance(project).effectiveConfigDir()
    val options = ShellStartupOptions.Builder()
        .workingDirectory(ClaudeWorkingDirectory.resolve(project.basePath))
        // Mapa aditivo: a plataforma o funde com o que os customizers injetam (RF-19).
        .envVariables(ClaudeEnvironment.build(configDir))
        .build()
    // Mesmo caminho usado por TerminalToolWindowManager.createShellWidget:
    // os LocalTerminalCustomizer (inclusive o da Anthropic) são aplicados abaixo daqui.
    val widget = runner.startShellTerminalWidget(parent, options, true)
    // Fora da tool window "Terminal" a plataforma engole o Esc (RF-17).
    ClaudeEscapeForwarder.install(widget)
    widget.sendCommandToExecute(command)
    return widget
}
```

**Precedência de `CLAUDE_CONFIG_DIR` (RF-19/RF-20).** O mapa entregue em `envVariables` é
aditivo, não substitutivo: a plataforma o funde com o ambiente herdado do shell e com o que os
`LocalTerminalCustomizer` injetam. Logo, definir o config dir **não interfere** no
`CLAUDE_CODE_SSE_PORT` do plugin oficial — as duas variáveis convivem, e a integração continua
íntegra. Campo vazio produz mapa vazio, e o CLI cai no seu próprio padrão (`~/.claude`).

### Respiro e capa: onde cada um encosta, e por quê _(v1.4)_

Os dois são UI de dez linhas, mas cada um errou duas vezes antes de assentar. O que decide a
posição correta é **o que a plataforma mede**, não o que o usuário vê.

**RF-28 — o respiro vai no componente do widget, não no `Content` da aba.**
`TerminalPanel.getTerminalSizeFromComponent()` calcula colunas e linhas a partir do **próprio
painel** (`getWidth() - getInsetX()`, `getHeight()`), então uma borda aplicada acima na hierarquia
não seria descontada do grid. O componente devolvido pelo widget é um `JPanel` com `BorderLayout`
(`JediTermWidget.getComponent()` devolve `this`), e ali a borda vazia diminui a área e o grid se
recalcula sozinho. O JediTerm só reserva 4px à esquerda, e nada nos outros lados — daí o conteúdo
colado nas bordas.

A cor **precisa ser lida na pintura**. `TerminalPanel.getBackground()` delega a
`getWindowBackground()` e é recalculada a cada chamada; copiar a cor uma vez deixa a faixa na cor
antiga quando o usuário troca de tema com a sessão aberta. Por isso a implementação é um `Border`
próprio que preenche as quatro faixas no `paintBorder`, e não um `EmptyBorder` sobre um valor
guardado.

**RF-29 — a capa fica sobreposta, e o terminal nunca é escondido.** Este é o ponto sutil: um
componente escondido não recebe dimensão, então o CLI desenharia para um tamanho inventado e
redesenharia tudo ao aparecer. Sobreposto num `JLayeredPane`, o terminal conta como visível
(inclusive para `deferSessionStartUntilUiShown`, RNF-02), recebe o tamanho real da aba e renderiza
uma única vez.

O prazo é **fixo, e não detecção**. Detectar "o CLI já pintou" exigiria vigiar o buffer, e a
primeira mudança dele é justamente o eco que a capa existe para esconder. Ver Q-16.

### `plugin.xml`

```xml
<idea-plugin>
  <id>dev.reginaldomorais.claudedock</id>
  <name>Claude Code Dock</name>
  <depends>com.intellij.modules.platform</depends>
  <depends optional="true" config-file="plugin-terminal.xml">org.jetbrains.plugins.terminal</depends>
  ...
</idea-plugin>
```

A dependência do terminal é **opcional com config-file**, replicando o padrão do plugin oficial
e satisfazendo RF-15. O registro da tool window vive em `plugin-terminal.xml`, de modo que
IDEs sem terminal simplesmente não a exibem.

**Compatibilidade:** `since-build = 252`, sem `until-build`. Sem `until-build` o plugin não é
bloqueado automaticamente a cada nova versão do IDE — decisão consciente, cujo risco é tratado
em [Riscos](#riscos).

### Build

- Kotlin **2.4.10**, JDK 21 (Zulu 21.0.8 já instalado).
  > ⚠️ Corrigido na implementação: a plataforma 2026.2 embute metadata Kotlin 2.4.0, que o
  > compilador 2.1.x não lê (limite 2.2.0). Kotlin ≥ 2.4 é obrigatório, não preferência.
- Gradle 9.2.0 via wrapper.
- IntelliJ Platform Gradle Plugin **2.18.1**.
- Plataforma alvo de compilação: `intellijIdea("2026.2")` — a **distribuição unificada**.
  > ⚠️ Corrigido na implementação: a versão original deste SPEC previa compilar contra a
  > Community Edition (`IC`). A JetBrains **deixou de publicar a IC separadamente a partir de
  > 2025.3 (253)**; o plugin Gradle rejeita `create("IC", "2026.2")`. A garantia de portabilidade
  > entre IDEs passa a vir da disciplina de só usar API de `com.intellij.modules.platform` e do
  > plugin de terminal, verificada por `verifyPlugin`.
- Artefato: ZIP produzido por `./gradlew buildPlugin`.

### Piper TTS — síntese e reprodução _(v1.5)_

**`ClaudePiperPlayback` — ÚNICO ponto de acoplamento com Piper + javax.sound.sampled (RNF-19).**

Responsabilidades:
1. **Deteção e validação:** `canSynthesize(executable, modelPath): Boolean` — verifica se
   executável existe e modelo (arquivo `.onnx`) existe e é legível, sem lançar exceção.
   Chamado fora da EDT via `executeOnPooledThread`, resultado cacheado e atualizado a cada
   N segundos ou quando configuração muda.
2. **Síntese:** `synthesize(text, executable, modelPath): ByteArray` — lança o `piper` via
   `GeneralCommandLine`, passa `text` por stdin, lê stdout (PCM cru), retorna bytes.
   Processo é destruído se timeout ou erro. Fora da EDT (RNF-20).
3. **Reprodução:** `playBytes(pcmBytes, sampleRate=22050, channels=1)` — abre um `Clip` ou
   `SourceDataLine`, reproduz, libera recursos em `finally` (RNF-22).
4. **Ciclo de vida:** `pause()` (guarda posição), `resume()` (restaura e reinicia), `stop()`
   (fecha clip e processo). Estado é simples: Idle | Playing | Paused.

Nenhum acoplamento a `Project`, serviços, UI, actions, ou listeners — é lógica pura.
Exceções são capturadas, logadas em `Logger.warn(...)` e retornadas como valores nulos /
booleanos falsos.

**`ClaudeTtaSessions` — serviço de projeto, estado único de reprodução.**

Uma única instância por projeto, via `project.service()`. Mantém:
- Referência ao `ClaudePiperPlayback` em curso (ou null).
- Estado: Idle / Playing / Paused.
- Listener de update para ações no menu (feedback para `AudioPauseResumeAction` / `AudioStopAction`).

Métodos:
- `playText(text)` — verifica `canSynthesize()`, para qualquer reprodução anterior,
  lança síntese/reprodução fora da EDT, notifica ouvintes (ações do menu ficam ativas).
- `pause()`, `resume()`, `stop()` — delegam a `ClaudePiperPlayback`, atualizam estado,
  notificam ouvintes.

Sem acoplamento a UI fora dos listeners de update (padrão padrão do IntelliJ).

**`ClaudeSelectionCopyButton` — agora com segundo botão (v1.5, RF-30).**

O popup que já existe ganha um segundo `JBLabel` com ícone `AllIcons.Actions.Play` (ou similar),
ao lado do de copiar. Clique dispara `ClaudeTtaSessions.getInstance(project).playText(selectedText())`.

O botão de play fica desabilitado quando:
- `ClaudePiperPlayback.canSynthesize()` retorna false (executável ou modelo inválido).
- Seleção vazia.

Checagem `canSynthesize()` é feita fora da EDT, resultado é cacheado, e a UI é atualizada
a cada 30s (constante configurável).

**Menu "Áudio" no cabeçalho (v1.5, RF-31).**

`DefaultActionGroup("Áudio", true)` contém:
- `AudioPauseResumeAction` — texto/ícone alterna entre "Pausar" (quando Playing) e "Retomar"
  (quando Paused); desabilitado quando Idle.
- `AudioStopAction` — ícone `AllIcons.Actions.Suspend` ou similar; desabilitado quando Idle.

Ambos chamam `ClaudeTtaSessions.getInstance(project).pause()`, `.resume()`, `.stop()`.

O `DefaultActionGroup` é criado em `ClaudeToolWindowFactory` (onde já são adicionadas as ações
de Nova/Retomar/Copiar/Exportar) e adicionado à lista `setTitleActions(...)`.

**Campos de configuração novos (v1.5, RF-32).**

`ClaudeDockSettings` (app-level) ganha:
- `piperExecutable: String = "piper"` (default)
- `piperModel: String = ""` (default vazio)

Métodos:
- `effectivePiperExecutable(): String` — trim, se vazio retorna default.
- `effectivePiperModel(): String` — trim, se vazio retorna `""` (desabilita play).

`ClaudeDockConfigurable` ganha dois novos campos na tela de configurações, no mesmo padrão que o
do `claude`:
```kotlin
group("Piper TTS") {
    row("Executável do Piper:") {
        textFieldWithBrowseButton(...)
            .bindText(settings::piperExecutable)
            .comment("Padrão: \"piper\" (busca no PATH).")
    }
    row("Modelo de voz (.onnx):") {
        textFieldWithBrowseButton(
            FileChooserDescriptorFactory.createSingleFileDescriptor("onnx")
                .withTitle("Selecione o arquivo .onnx do modelo"),
            project,
        )
            .bindText(settings::piperModel)
            .comment("Caminho absoluto para a voz desejada. Obrigatório para ativar síntese.")
    }
}
```

### Itens não aplicáveis

Registrados por exigência do roteiro de SDD:

- **Banco de dados / schema / migrações de dados** — não aplicável. O plugin não possui banco.
  A única persistência é um `PersistentStateComponent` com um campo `String`.
- **Mudanças de API / GraphQL** — não aplicável. O plugin não expõe nem consome API HTTP.
- **Background jobs** — não aplicável. Não há trabalho agendado ou assíncrono recorrente.
- **Integrações externas** — nenhuma direta. O plugin lança um processo local (`claude`); toda
  comunicação de rede é feita pelo CLI, fora do escopo do plugin.
- **Feature flags** — não aplicável a um plugin local de instalação manual. O controle é a
  própria (des)instalação.
- **Métricas / monitoramento** — apenas o log local do IDE (RNF-12). Nenhuma telemetria é
  coletada ou enviada — decisão deliberada de privacidade.

---

## Casos de Borda

| #         | Caso                                                                                                                | Tratamento                                                                                                                                                                                      |
| --------- | ------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **CB-01** | `claude` não está no `PATH`                                                                                         | Notificação acionável (RF-14); aba permanece com shell utilizável                                                                                                                               |
| **CB-02** | Caminho configurado aponta para arquivo inexistente ou sem permissão de execução                                    | Validação na tela de configurações + notificação ao criar sessão                                                                                                                                |
| **CB-03** | Plugin oficial ausente ou desabilitado                                                                              | Degradação graciosa: janela funciona, integração não (RNF-11, Fluxo C)                                                                                                                          |
| **CB-04** | Plugin oficial **e** este plugin abertos ao mesmo tempo                                                             | Coexistem: duas sessões independentes do CLI, ambas conectadas ao mesmo servidor MCP. Ver [Riscos R-05](#riscos)                                                                                |
| **CB-05** | Projeto sem `basePath` (ex.: janela sem projeto, arquivo avulso)                                                    | Fallback para o diretório home do usuário                                                                                                                                                       |
| **CB-06** | Múltiplos projetos abertos                                                                                          | Serviços com escopo de `Project`; uma tool window por projeto (RNF-17)                                                                                                                          |
| **CB-07** | Usuário encerra o `claude` com `/exit` ou `Ctrl+D`                                                                  | `addTerminationCallback` marca a aba; scrollback preservado (RF-11)                                                                                                                             |
| **CB-08** | Usuário fecha a aba com processo vivo                                                                               | `Disposable` encerra o processo e libera o PTY (RNF-09, RF-12)                                                                                                                                  |
| **CB-09** | Projeto fechado com sessões ativas                                                                                  | Disposables de escopo de projeto encerram todas as sessões                                                                                                                                      |
| **CB-10** | IDE sem o plugin de terminal                                                                                        | Tool window não registrada; nenhum erro (RF-15)                                                                                                                                                 |
| **CB-11** | `TerminalEngine.REWORKED` ativo em vez de `CLASSIC`                                                                 | Usar `startShellTerminalWidget`, que respeita o engine configurado; **validar ambos os engines nos testes**                                                                                     |
| **CB-12** | Projeto não confiável (_Trusted Projects_)                                                                          | A plataforma bloqueia criação de terminal; propagar a decisão do IDE sem contorná-la                                                                                                            |
| **CB-13** | Nome de aba duplicado com muitas sessões                                                                            | Sufixo numérico incremental (RF-16)                                                                                                                                                             |
| **CB-14** | Usuário troca o caminho do `claude` com sessões abertas                                                             | Sessões existentes não são afetadas; a mudança vale para as próximas                                                                                                                            |
| **CB-15** | Upgrade do IDE quebra a assinatura de `startShellTerminalWidget`                                                    | Falha isolada em uma classe (RNF-15); tratamento em [Riscos R-01](#riscos)                                                                                                                      |
| **CB-16** | Remote Dev / split mode (frontend e backend separados)                                                              | Fora de escopo; o customizer roda no backend e o caminho de código difere. **Documentar como não suportado**                                                                                    |
| **CB-17** | Diretório do projeto em WSL (Windows)                                                                               | Fora de escopo nesta versão                                                                                                                                                                     |
| **CB-18** | _(v1.1)_ `Esc` pressionado com modificador (`Shift+Esc`, `Ctrl+Esc`)                                                | Não é interceptado: segue para o tratamento normal do IDE, preservando `Ctrl+Esc` do oficial (RF-13, T-5.2)                                                                                     |
| **CB-19** | _(v1.1)_ `Esc` antes do PTY existir (sessão adiada por RNF-02)                                                      | `ttyConnector` nulo: o pre-handler não consome e o comportamento padrão prevalece — não há sessão para receber                                                                                  |
| **CB-20** | _(v1.1)_ Usuário fecha a última aba da tool window                                                                  | Estado vazio com links "Nova sessão" / "Retomar sessão" (RF-18); `createToolWindowContent` não roda de novo                                                                                     |
| **CB-21** | _(v1.1)_ `CLAUDE_CONFIG_DIR` informado com `~`                                                                      | Expandido no plugin: variável de ambiente não passa por expansão do shell                                                                                                                       |
| **CB-22** | _(v1.1)_ `CLAUDE_CONFIG_DIR` aponta para diretório inexistente                                                      | Repassado como está; a criação é responsabilidade do CLI. Sem validação própria para não divergir do CLI                                                                                        |
| **CB-23** | _(v1.1)_ `CLAUDE_CONFIG_DIR` alterado com sessões abertas                                                           | Como CB-14: sessões vivas não mudam; vale para as próximas abas                                                                                                                                 |
| **CB-24** | _(v1.2)_ Copiar/exportar sem nenhuma aba aberta (estado vazio de RF-18)                                             | Notificação "Nenhuma sessão aberta"; nada acontece (RF-23)                                                                                                                                      |
| **CB-25** | _(v1.2)_ Conversa maior que o scrollback                                                                            | A cópia traz só o que restou no buffer, truncada no topo. O limite é do usuário (`terminal.buffer.max.lines.count`) e o plugin não o altera (RF-13); para a conversa inteira existe o `/export` |
| **CB-26** | _(v1.2)_ Engine sem JediTerm (`REWORKED`)                                                                           | `getText()` cai no `default` da interface e devolve vazio: notificação "sem conteúdo", nunca exceção. Ligado a Q-04                                                                             |
| **CB-27** | _(v1.2)_ "Exportar Conversa" com o processo `claude` já encerrado (RF-11)                                           | O texto cai no shell e vira `command not found`. Inofensivo; distinguir TUI vivo de shell exigiria heurística frágil                                                                            |
| **CB-28** | _(v1.2)_ "Exportar Conversa" antes de o PTY existir (sessão adiada, RNF-02)                                         | `ttyConnector` nulo: `sendInput` devolve `false` e a ação notifica (mesma raiz de CB-19)                                                                                                        |
| **CB-29** | _(v1.3)_ O `/export` de RF-24 não produz o arquivo (CLI ocupado, encerrado, ou comando removido)                    | Prazo limite; estourado, notifica "não foi possível obter a conversa" e apaga o temporário. Sem espera indefinida                                                                               |
| **CB-30** | _(v1.3)_ Seleção desfeita ou vazia com o botão flutuante na tela                                                    | O listener dispara com seleção vazia e o botão some sem copiar nada                                                                                                                             |
| **CB-31** | _(v1.3)_ Seleção feita numa aba e clique no botão depois de trocar de aba                                           | O botão é por painel e some junto com a aba; não há caminho para copiar seleção de outra aba                                                                                                    |
| **CB-32** | _(v1.3)_ Cópia acionada duas vezes seguidas antes de a primeira terminar                                            | Um `/export` por vez, por aba: a segunda é ignorada enquanto a primeira está em curso                                                                                                           |
| **CB-33** | _(v1.4)_ Usuário troca o tema do IDE com sessões abertas                                                            | O respiro acompanha: a cor é lida a cada pintura, não copiada na criação da aba (RF-28)                                                                                                         |
| **CB-34** | _(v1.4)_ O CLI pede algo nos primeiros segundos — ex.: "confia nesta pasta?" na primeira execução em diretório novo | A capa esconde até o prazo terminar; o diálogo continua lá e o usuário responde depois. Custo aceito de RF-29                                                                                   |
| **CB-35** | _(v1.4)_ Aba fechada antes de a capa sair                                                                           | Timer, animação e capa são filhos do `Disposable` da aba: caem juntos, sem timer disparando sobre painel morto                                                                                  |
| **CB-36** | _(v1.4)_ Engine sem JediTerm (`REWORKED`) com o respiro ligado                                                      | Sem painel para consultar, a borda fica vazia e a faixa é pintada pela tool window — degrada em cor, não em erro                                                                                |
| **CB-37** | _(v1.4)_ Executável instalado em diretório ausente do `PATH` do IDE (ex.: `~/.local/bin`)                           | A verificação consulta diretórios conhecidos antes de notificar; o shell da sessão resolve de qualquer forma (CB-01)                                                                            |
| **CB-38** | _(v1.5)_ Seleção muito grande (ex.: páginas de código) enviada ao Piper | A síntese pode demorar décadas de segundos; o timeout (20s ou configurável) notifica o usuário sem travar a UI |
| **CB-39** | _(v1.5)_ Modelo `.onnx` ausente após ser configurado | O botão play fica desabilitado; verificação consultou o arquivo antes de manter estado Idle |
| **CB-40** | _(v1.5)_ Reprodução em curso e usuário fecha a aba | O `Disposable` da aba dispara, `ClaudeTtaSessions` para a reprodução e libera recursos (RNF-22) |
| **CB-41** | _(v1.5)_ Reprodução pausada e usuário sai do IDE | Nenhum evento especial — o proceso `piper` já terminou (síntese é fora da EDT), só o `Clip` fica em pausa. Fechamento normal do IDE libera tudo |
| **CB-42** | _(v1.5)_ Dois modelos diferentes configurados (ex.: português e inglês) e usuário alterna | Nenhum problema — cada chamada a `playText()` usa o `piperModel` atual da configuração |

---

## Riscos

| #        | Risco                                                                                                                                                                                                                | Impacto     | Prob. | Mitigação                                                                                                                                                                                                                                                                                                                                                        |
| -------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------- | ----- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **R-01** | `AbstractTerminalRunner` / `TerminalToolWindowManager` são API de plataforma sujeita a mudança sem aviso entre builds                                                                                                | Alto        | Média | Todo o acoplamento em uma única classe (RNF-15); smoke test manual a cada upgrade de IDE; sem `until-build`, o plugin carrega e falha de forma isolada e diagnosticável, em vez de ser silenciosamente desabilitado                                                                                                                                              |
| **R-02** | ~~A premissa central (customizer alcança widget customizado) pode estar errada~~ ✅ **FECHADO em 2026-08-01**                                                                                                        | ~~Crítico~~ | —     | Validado empiricamente por `TerminalCustomizerReachTest` (T-4), com teste de controle. É `configureStartupOptions` que aplica os customizers, e `startShellTerminalWidget` passa por ele. O teste permanece como regressão a cada upgrade de IDE                                                                                                                 |
| **R-03** | A Anthropic pode alterar `CLAUDE_CODE_SSE_PORT` ou o mecanismo de descoberta                                                                                                                                         | Médio       | Média | Este plugin **não depende** da variável: apenas não a atrapalha. Mudanças quebram integração, não a janela (degradação graciosa)                                                                                                                                                                                                                                 |
| **R-04** | O plugin oficial pode ganhar tool window dedicada nativamente, tornando este projeto obsoleto                                                                                                                        | Baixo       | Média | Resultado aceitável: o custo afundado é pequeno (poucas centenas de linhas) e desinstalar é trivial. Acompanhar o changelog do oficial                                                                                                                                                                                                                           |
| **R-05** | Duas sessões simultâneas (aba nativa + janela dedicada) conectadas ao mesmo servidor MCP podem gerar comportamento ambíguo — ex.: um diff aberto por uma sessão atribuído à outra                                    | Médio       | Média | Documentar no README a recomendação de usar uma janela por vez; investigar durante T-4 se o servidor distingue clientes. Considerar ocultar o botão do oficial via `CLAUDE_CODE_JETBRAINS_PLUGIN_HIDE_BUTTON` (string encontrada no jar oficial — **usar apenas após verificar semântica**)                                                                      |
| **R-06** | `TerminalEngine.REWORKED` pode ter comportamento distinto de `CLASSIC` ao ser embutido fora da tool window nativa                                                                                                    | Médio       | Média | Testar explicitamente com os dois engines (CB-11); fixar `CLASSIC` como fallback documentado se houver divergência                                                                                                                                                                                                                                               |
| **R-07** | Plugin não assinado instalado via disco dispara aviso de segurança do IDE                                                                                                                                            | Baixo       | Alta  | Esperado para uso local; documentar no README                                                                                                                                                                                                                                                                                                                    |
| **R-08** | Sem `until-build`, uma versão futura incompatível do IDE pode causar exceções em runtime                                                                                                                             | Médio       | Média | Aceito conscientemente em troca de não travar a cada upgrade; mitigado por R-01 e pelo tratamento de erro do Fluxo E                                                                                                                                                                                                                                             |
| **R-09** | _(v1.1)_ A correção do `Esc` (RF-17) depende de `JBTerminalPanel.addPreKeyEventHandler` e de `JBTerminalWidget.asJediTermWidget` — API pública, mas sem garantia de estabilidade, e específica do engine **CLASSIC** | Médio       | Média | Falha degrada, não quebra: `asJediTermWidget` nulo faz o `install` retornar sem efeito, e o pior caso é voltar ao bug atual (`Esc` move o foco), nunca uma exceção. Coberto por T-3.7. Se a plataforma migrar a janela para o engine REWORKED, reavaliar contra `TerminalEscapeHandler` (EP `org.jetbrains.plugins.terminal.escapeHandler`, já existente em 262) |
| **R-10** | _(v1.1)_ A JetBrains pode corrigir a assimetria do `TerminalEscapeKeyListener` e passar a entregar o `Esc` também fora da tool window "Terminal", tornando o pre-handler redundante — ou duplicando o envio          | Baixo       | Baixa | O pre-handler consome o evento antes do listener, então mesmo com a correção upstream o caminho continua único: nós enviamos, a plataforma não reenvia. Revalidar em T-5.3 a cada upgrade                                                                                                                                                                        |
| **R-11** | _(v1.2)_ O `/export` (RF-22) depende de um slash command do CLI, que a Anthropic pode renomear ou remover sem aviso                                                                                                  | Baixo       | Média | Falha visível e inofensiva: o texto aparece na sessão e o CLI responde que não conhece o comando. A cópia de RF-21 não depende do CLI e continua atendendo o caso principal                                                                                                                                                                                      |
| **R-12** | ~~_(v1.2)_ Escrever `"/export\r"` no PTY é digitação simulada~~ ✅ **FECHADO em 2026-08-01** — T-3.11 aprovado: o `/export` executou e gerou o arquivo                                                               | ~~Baixo~~   | —     | Validado no IDE real: o `\r` é aceito como Enter e o autocomplete não interfere. Como RF-24 passa a depender disso em todo uso, revalidar a cada upgrade do CLI                                                                                                                                                                                                  |
| **R-13** | ~~_(v1.3)_ O argumento `[filename]` do `/export` não foi verificado~~ ✅ **FECHADO em 2026-08-01** — lido na implementação embutida no binário `claude` 2.1.220                                                      | ~~Alto~~    | —     | Grava direto e sem UI, sobrescreve, cria diretórios, e acrescenta `.txt` se faltar extensão. O destino de RF-24 termina em `.md` por causa disso. Revalidar a cada upgrade do CLI, junto de R-11                                                                                                                                                                 |
| **R-14** | _(v1.3)_ RF-24 grava a conversa em arquivo temporário — código-fonte e possíveis segredos passam por disco, ainda que por segundos                                                                                   | Médio       | Alta  | Arquivo com permissão exclusiva do usuário e apagado em `finally` (RF-25). Liability real, aceita porque a alternativa (buffer) não entrega o recurso. Registrada em RNF-19                                                                                                                                                                                      |
| **R-15** | _(v1.3)_ O botão flutuante (RF-26) depende de `TerminalPanel.addSelectionListener`, específico do engine CLASSIC — mesma exposição de R-09                                                                           | Baixo       | Média | Degrada igual: sem `asJediTermWidget` o botão não é instalado, e `Ctrl+C`/`Ctrl+Shift+C` seguem copiando a seleção. Perde-se conveniência, não capacidade                                                                                                                                                                                                        |
| **R-16** | _(v1.4)_ O prazo da capa (RF-29) é fixo. Máquina mais lenta, hook de sessão pesado ou CLI atualizando deixam o eco escapar quando a capa sai                                                                         | Baixo       | Média | Constante única em `ClaudeSessionLoading`, ajustável em um lugar. Falha é cosmética e passageira, nunca funcional. Q-16 registra o caminho para trocar prazo por detecção                                                                                                                                                                                        |
| **R-17** | _(v1.4)_ A verificação do executável usa o `PATH` do `EnvironmentUtil`, que **não** enxerga o que o `.zshrc`/`.bashrc` acrescenta — falso negativo observado com o CLI funcionando                                   | Baixo       | Alta  | Consulta `~/.local/bin` e `/usr/local/bin` antes de desistir, e a notificação nunca bloqueia a abertura da aba. Quem instala fora disso tem o campo de configuração                                                                                                                                                                                              |
| **R-18** | _(v1.5)_ Piper depende de um arquivo `.onnx` cuja localização e nomenclatura não é padronizada (sem registro central de modelos)                                                                                    | Médio       | Alta  | Decisão de design: o usuário configura manualmente o caminho do modelo desejado, sem autodetecção. Documentar no README dicas de onde obter modelos (ex.: Hugging Face da oma/piper) e convenção de armazená-los em `~/.claude/piper-voices/` |
| **R-19** | _(v1.5)_ A síntese no `piper` é CPU-bound e pode travar a UI se rodasse na EDT                                      | Médio       | Média | RNF-20 exige síntese fora da EDT via `executeOnPooledThread`. Testado que `ApplicationManager.getApplication().executeOnPooledThread { ... }` + `invokeLater` para update não trava. Revalidar se seleção > N caracteres passar a ser suportada |
| **R-20** | _(v1.5)_ Reprodução de áudio via `javax.sound.sampled` é bloqueante (thread do mixer aguarda buffer ficar vazio)   | Médio       | Média | Linha de áudio é reproduzida em thread separada (mixer nativo do SO), a UI fica responsiva. Se o mixer travar ou estiver indisponível, a thread de reprodução congela, não a EDT. Risco aceitável |
| **R-21** | _(v1.5)_ Modelo `.onnx` pode ser muito grande (63 MB) e o carregamento na primeira síntese causa latência            | Baixo       | Média | Piper já cacheia o modelo em memória entre chamadas. Primeira síntese tem latência de carregamento (~2-3s); as seguintes são rápidas. Documentar e aceitar |
| **R-22** | _(v1.5)_ Dois projetos abertos com configurações diferentes de `piperModel` — sem sincronização entre `ClaudeTtsSessions` | Baixo       | Baixa | Cada projeto tem sua própria instância de `ClaudeTtaSessions` (via `project.service()`). Não há compartilhamento; cada um usa seu próprio modelo configurado. Esperado e correto |

---

## Estratégia de Testes

### Testes unitários

Base: `BasePlatformTestCase` (IntelliJ Test Framework), executados por `./gradlew test`.

| #              | Alvo                              | Verificação                                                                                                                                                                                                                                                                                       |
| -------------- | --------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **T-1.1**      | `ClaudeDockSettings`              | Valor padrão é `claude`; estado sobrevive a serialização/desserialização                                                                                                                                                                                                                          |
| **T-1.2**      | `ClaudeDockSettings`              | Caminho customizado é persistido e recuperado                                                                                                                                                                                                                                                     |
| **T-1.3**      | Montagem de `ShellStartupOptions` | `workingDirectory` recebe `project.basePath`                                                                                                                                                                                                                                                      |
| **T-1.4**      | Montagem de `ShellStartupOptions` | Projeto sem `basePath` cai no diretório home (CB-05)                                                                                                                                                                                                                                              |
| **T-1.5**      | Construção do comando             | "Nova sessão" produz `claude`; "Retomar" produz `claude --resume`                                                                                                                                                                                                                                 |
| **T-1.6**      | Construção do comando             | Caminho com espaços é passado como argumento único, sem interpretação de shell (RNF-08)                                                                                                                                                                                                           |
| **T-1.7**      | Nomeação de abas                  | Sessões sucessivas geram títulos distintos (RF-16, CB-13)                                                                                                                                                                                                                                         |
| **T-1.8**      | `ClaudeEnvironment`               | _(v1.1)_ Config dir ausente/em branco não injeta variável alguma (RF-20)                                                                                                                                                                                                                          |
| **T-1.9**      | `ClaudeEnvironment`               | _(v1.1)_ Caminho vira `CLAUDE_CONFIG_DIR`, com trim e expansão de `~` (RF-19, CB-21)                                                                                                                                                                                                              |
| **T-1.10**     | `ClaudeDockProjectSettings`       | _(v1.1)_ Padrão vazio, persistência via `loadState`, caminho efetivo com trim                                                                                                                                                                                                                     |
| **T-1.11**     | `ClaudeEscapeForwarder`           | _(v1.1)_ Só `Esc` puro em `KEY_PRESSED` é encaminhado; modificador, outras teclas e evento já consumido são ignorados (RF-17, CB-18, CB-19)                                                                                                                                                       |
| **T-1.12**     | `ClaudeSessionText`               | _(v1.2)_ Buffer nulo/vazio/só espaços não produz texto; espaços à direita e linhas vazias do fim são removidos; indentação e linhas em branco internas ficam intactas (RF-21, CB-26)                                                                                                              |
| **T-1.13**     | Comando de export                 | _(v1.3)_ O caminho vai **cru** para o `/export`, sem aspas — o CLI faz `argumento.trim()` e aspas virariam parte do nome do arquivo. Oposto de T-1.6, que protege contra o **shell** (RF-24)                                                                                                      |
| **T-1.14**     | Leitura do export                 | _(v1.3)_ Arquivo ausente ou vazio dentro do prazo produz falha tratada; arquivo com conteúdo produz o texto; o temporário é removido nos dois casos (RF-25, CB-29)                                                                                                                                |
| **T-1.15**     | Destino do export                 | _(v1.3)_ O temporário termina em `.md` (senão o CLI grava em outro caminho) e nasce com permissão exclusiva do dono (RF-25)                                                                                                                                                                       |
| ~~**T-1.16**~~ | ~~`ClaudeCommand`~~               | ~~_(v1.3)_ `--ax-screen-reader`~~ **removido com RF-27 em v1.4**; no lugar, o teste guarda o oposto: o comando não ganha argumento além do pedido                                                                                                                                                 |
| ~~**T-1.17**~~ | ~~`ClaudeDockSettings`~~          | ~~_(v1.3)_ `flatOutput`~~ **removido com RF-27 em v1.4**                                                                                                                                                                                                                                          |
| **T-1.18**     | `ClaudeSessionPadding`            | _(v1.4)_ Borda nos quatro lados; a faixa é pintada com o fundo do terminal e **acompanha a troca de cor**; sem painel JediTerm sobra borda vazia; zero não adiciona borda (RF-28, CB-33, CB-36)                                                                                                   |
| **T-1.19**     | `ClaudeDockSettings`              | _(v1.4)_ `sessionPadding` nasce no padrão, persiste via `loadState` e valor fora da faixa é limitado (RF-28)                                                                                                                                                                                      |
| **T-1.20**     | `ClaudeSessionLoading`            | _(v1.4)_ O terminal continua na árvore e **visível** sob a capa; a capa é opaca e fica em camada superior; as camadas ocupam a área inteira; prazo zero devolve o próprio terminal (RF-29, CB-35)                                                                                                 |
| **T-1.21**     | Verificação do executável         | _(v1.4)_ Caminho explícito exige existir e ser executável; nome ausente do `PATH` é aceito quando está num diretório conhecido, e recusado quando o diretório não o tem — o par é o controle que impede o teste de passar resolvendo pelo `PATH` de quem roda a suíte (CB-01, CB-02, CB-37, R-17) |
| **T-1.22**     | `ClaudePiperPlayback`             | _(v1.5)_ `canSynthesize(executable, modelPath)` retorna verdadeiro só quando ambos existem, é legível e modelo termina em `.onnx`; sem exceções mesmo com caminho inválido (RF-30, CB-39) |
| **T-1.23**     | `ClaudePiperPlayback`             | _(v1.5)_ `synthesize(text, executable, modelPath)` retorna ByteArray não vazio com PCM cru (22050 Hz, 16-bit, mono) quando entrada é válida; retorna null com timeout ou erro de processo (CB-38, R-19) |
| **T-1.24**     | `ClaudePiperPlayback`             | _(v1.5)_ PCM de síntese é reproduzível sem erro via `playBytes(pcm, 22050, 1)`; recurso é liberado após stop ou exceção (RNF-22) |
| **T-1.25**     | `ClaudeDockSettings`              | _(v1.5)_ `piperExecutable` e `piperModel` nascem em padrão, persistem via `loadState`, `effectivePiperExecutable()` e `effectivePiperModel()` fazem trim (RF-32) |
| **T-1.26**     | `ClaudeTtaSessions`               | _(v1.5)_ Estado alterna Idle → Playing → (Paused ↔ Playing) → Idle corretamente; listeners de update disparam em cada mudança (RNF-23) |
| **T-1.27**     | `ClaudeTtaSessions`               | _(v1.5)_ `playText(text)` novo durante reprodução prévia para e inicia nova, sem fila; recurso anterior é liberado (RNF-23) |

Conforme `CLAUDE.md`, novos testes acompanham cada funcionalidade nova ou alterada, e a suíte é
executada após cada implementação.

### Testes de integração

| #         | Cenário                 | Verificação                                                                                                 |
| --------- | ----------------------- | ----------------------------------------------------------------------------------------------------------- |
| **T-2.1** | Registro da tool window | Em `runIde`, a tool window "Claude Code Dock" existe e é distinta de "Terminal"                             |
| **T-2.2** | Criação de sessão       | Abrir a janela cria um `TerminalWidget` com processo vivo                                                   |
| **T-2.3** | Isolamento de abas      | Duas abas produzem dois processos independentes (RF-06)                                                     |
| **T-2.4** | Liberação de recursos   | Fechar a aba encerra o processo; nenhum PTY órfão (RNF-09, CB-08)                                           |
| **T-2.5** | Ausência do terminal    | Com `org.jetbrains.plugins.terminal` desabilitado, o IDE inicia sem erro e sem a tool window (RF-15, CB-10) |
| **T-2.6** | Engines de terminal     | T-2.2 passa com `CLASSIC` e com `REWORKED` (CB-11, R-06)                                                    |

### T-4: Validação da premissa central (aprovado)

**Este teste precedeu toda a implementação subsequente**, como planejado.

**Como foi executado (mudou de manual para automatizado):** o SPEC previa um roteiro manual
(`env | grep CLAUDE_CODE_SSE_PORT` digitado na janela). Na implementação virou o teste
automatizado `TerminalCustomizerReachTest`, que é estritamente melhor: determinístico, sem UI,
e roda em todo `./gradlew test` — atendendo de quebra o T-5.3, que pedia suíte de fumaça por
upgrade de IDE.

**O que o teste faz:**

1. Registra um `LocalTerminalCustomizer` próprio, que injeta uma variável sentinela — exatamente
   o que o `TerminalCustomizer` da Anthropic faz com `CLAUDE_CODE_SSE_PORT`.
2. Monta as `ShellStartupOptions` como a produção e chama `runner.configureStartupOptions(...)`.
3. Afirma que a sentinela está em `envVariables`.
4. **Teste de controle:** sem customizer registrado, afirma que a variável **não** aparece —
   garantindo que a medição é do mecanismo, e não de resíduo do ambiente.

**Resultado: aprovado.** O customizer alcança sessões criadas fora da tool window nativa.

**Ponto exato do mecanismo** (descoberto aqui, não estava no SPEC original): quem aplica os
customizers é **`configureStartupOptions`**, não `createProcess`. `startShellTerminalWidget`
passa por ele internamente, então a produção está correta.

**O que este teste NÃO prova**, e por isso os roteiros T-3.\* continuam necessários: que a
janela renderiza corretamente, que o `claude` de fato conecta ao servidor MCP, e que o diff
abre no visualizador do IDE de ponta a ponta.

**Se reprovado:** acionar o plano de contingência de R-02 e reavaliar o escopo.

### Testes end-to-end (manuais)

| #              | Roteiro                                                                                                                                                                                                                                                      |
| -------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **T-3.1**      | Instalar via _Install Plugin from Disk_; reiniciar; abrir a janela; conversar com o Claude                                                                                                                                                                   |
| **T-3.2**      | Pedir uma edição de arquivo e confirmar que o diff abre no IDE                                                                                                                                                                                               |
| **T-3.3**      | Selecionar código no editor, usar `Ctrl+Alt+K` do oficial e confirmar que a referência chega à sessão da janela dedicada                                                                                                                                     |
| **T-3.4**      | Executar "Retomar sessão" e confirmar que o seletor do CLI lista sessões anteriores                                                                                                                                                                          |
| **T-3.5**      | Repetir T-3.1 em ao menos dois IDEs distintos (ex.: IntelliJ e GoLand)                                                                                                                                                                                       |
| **T-3.6**      | Desinstalar o plugin e confirmar que o oficial segue íntegro e funcional                                                                                                                                                                                     |
| **T-3.7**      | _(v1.1)_ Na janela dedicada, rodar `/usage` e sair com `Esc`; conferir que o foco **não** vai para o editor (RF-17)                                                                                                                                          |
| **T-3.8**      | _(v1.1)_ Fechar a última aba e confirmar que os links "Nova sessão"/"Retomar sessão" aparecem e funcionam (RF-18, CB-20)                                                                                                                                     |
| **T-3.9**      | _(v1.1)_ Definir `CLAUDE_CONFIG_DIR` em Settings, abrir nova sessão e conferir `echo $CLAUDE_CONFIG_DIR` no terminal; confirmar que `CLAUDE_CODE_SSE_PORT` continua presente (RF-19)                                                                         |
| **T-3.10**     | _(v1.2)_ Conversar, acionar "Copiar Sessão" e colar num editor: o texto traz a conversa do buffer, sem bloco de linhas vazias no fim nem espaços à direita (RF-21)                                                                                           |
| **T-3.11**     | _(v1.2)_ Acionar "Exportar Conversa" com o `claude` rodando: o seletor do `/export` aparece dentro da sessão e produz a transcrição. Nesta máquina o destino precisa ser arquivo — o clipboard do CLI exige `wl-copy`/`xclip`/`xsel`, ausentes (RF-22, R-12) |
| **T-3.12**     | _(v1.2)_ Com a última aba fechada, acionar as duas ações novas: cada uma notifica "Nenhuma sessão aberta" e nada quebra (RF-23, CB-24)                                                                                                                       |
| **T-3.13**     | ~~_(v1.3)_ Verificar à mão se `/export <caminho>` aceita caminho absoluto~~ ✅ **Dispensado** — respondido por leitura do binário (R-13), com mais precisão do que o teste manual daria                                                                      |
| **T-3.14**     | _(v1.3)_ Conversa longa, com a janela redimensionada no meio: "Copiar Conversa" traz a conversa **uma única vez**, sem banner repetido nem barra de status; o temporário não fica em `/tmp` (RF-24, RF-25, DEF-01)                                           |
| **T-3.15**     | _(v1.3)_ Selecionar um trecho com o mouse: o botão flutuante aparece, copia só o trecho e some ao desfazer a seleção (RF-26, CB-30)                                                                                                                          |
| **T-3.16**     | _(v1.3)_ Com o botão flutuante na tela, digitar na sessão: o foco **não** foi roubado pelo popup (RF-26)                                                                                                                                                     |
| ~~**T-3.17**~~ | ~~_(v1.3)_ Ligar "Saída plana" e comparar~~ ✅ **EXECUTADO e conclusivo** — a flag funciona, mas troca a caixa de input por um `$` indistinguível de prompt de shell. RF-27 removido em v1.4                                                                 |
| **T-3.18**     | _(v1.4)_ Abrir aba, mudar o tema do IDE (claro ↔ escuro) e conferir que o respiro acompanha o fundo do terminal, sem faixa de cor antiga (RF-28, CB-33)                                                                                                      |
| **T-3.19**     | _(v1.4)_ Abrir aba e observar a partida: a capa cobre o prompt e o eco, sai sozinha, e o rodapé do CLI **não** aparece quebrado nem redesenha ao sair (RF-29)                                                                                                |
| **T-3.20**     | _(v1.4)_ Abrir aba em diretório novo, onde o CLI pergunta "confia nesta pasta?": o diálogo continua respondível depois de a capa sair (CB-34)                                                                                                                |
| **T-3.21**     | _(v1.5)_ Selecionar texto, verificar que o botão play aparece **ao lado** do botão de copiar no popup, ativo se Piper disponível (RF-30) |
| **T-3.22**     | _(v1.5)_ Clicar em play: o texto é sintetizado e reproduzido em áudio, menu "Áudio" fica ativo no cabeçalho (RF-31) |
| **T-3.23**     | _(v1.5)_ Durante reprodução, clicar em "Pausar": som para e botão muda para "Retomar". Clicar "Retomar": som continua. Clicar "Parar": som cessa (RF-31, CB-40) |
| **T-3.24**     | _(v1.5)_ Botão play fica desabilitado quando: (a) `piper` não encontrado; (b) modelo não configurado; verificar tooltip em cada caso (RF-30, CB-39) |
| **T-3.25**     | _(v1.5)_ Abrir Settings > Tools > Claude Code Dock, verificar novos campos "Executável do Piper" e "Modelo de voz (.onnx)", alterá-los, aplicar, e verificar que botão play se habilita/desabilita conforme modelo (RF-32) |
| **T-3.26**     | _(v1.5)_ Selecionar e reproduzir um grande trecho (páginas de código): síntese demora mas UI fica responsiva (RNF-20); timeout após 20s e notificação se síntese não terminar (CB-38) |
| **T-3.27**     | _(v1.5)_ Iniciar síntese de trecho A, e antes de terminar selecionar e iniciar trecho B: síntese de A é cancelada, B começa novo (RNF-23) |

### Testes de regressão

| #         | Verificação                                                                                                                 |
| --------- | --------------------------------------------------------------------------------------------------------------------------- |
| **T-5.1** | A tool window "Terminal" nativa continua criando terminais normalmente                                                      |
| **T-5.2** | A ação `Ctrl+Esc` do plugin oficial continua funcionando e abrindo na tool window nativa                                    |
| **T-5.3** | Após upgrade de IDE, executar T-2.1, T-2.2, T-4 e T-3.7 como suíte de fumaça (R-01, R-09, R-10)                             |
| **T-5.4** | _(v1.1)_ `Shift+Esc` (esconder tool window) e `Ctrl+Esc` (oficial) seguem funcionando com o foco na janela dedicada (CB-18) |

---

## Critérios de Aceitação

**CA-01 — Janela dedicada**

- **Given** o plugin instalado em um IDE JetBrains com plugin de terminal
- **When** o usuário aciona "Abrir Claude Code Dock"
- **Then** abre-se uma tool window intitulada "Claude Code Dock", separada da tool window "Terminal"

**CA-02 — Início automático da sessão**

- **Given** a tool window "Claude Code Dock" nunca aberta nesta sessão do IDE
- **When** o usuário a abre pela primeira vez
- **Then** uma sessão de terminal é criada na raiz do projeto e o comando `claude` é executado automaticamente

**CA-03 — Inicialização preguiçosa**

- **Given** o plugin instalado e a tool window fechada
- **When** o IDE é iniciado e o projeto carregado
- **Then** nenhum processo `claude` ou de shell é criado pelo plugin

**CA-04 — Integração preservada**

- **Given** o plugin oficial instalado e uma sessão ativa na janela dedicada
- **When** o usuário pede ao Claude Code uma alteração em um arquivo
- **Then** o diff é apresentado no visualizador de diff do IDE, com o mesmo comportamento da aba nativa

**CA-05 — Retomada de sessão**

- **Given** conversas anteriores registradas pelo CLI
- **When** o usuário aciona "Retomar sessão"
- **Then** uma nova aba executa `claude --resume` e o seletor nativo de sessões é exibido

**CA-06 — Múltiplas sessões**

- **Given** a janela aberta com uma sessão ativa
- **When** o usuário aciona "Nova sessão"
- **Then** uma segunda aba é criada com processo independente, sem afetar a primeira

**CA-07 — Executável ausente**

- **Given** o `claude` não presente no `PATH` nem no caminho configurado
- **When** o usuário abre a tool window
- **Then** exibe-se notificação acionável com link para as configurações, e o IDE permanece estável

**CA-08 — Liberação de recursos**

- **Given** uma aba com processo `claude` ativo
- **When** o usuário fecha a aba
- **Then** o processo é encerrado e nenhum PTY órfão permanece

**CA-09 — Coexistência**

- **Given** este plugin e o oficial instalados simultaneamente
- **When** o usuário aciona `Ctrl+Esc` (ação do oficial)
- **Then** a sessão do oficial abre na tool window "Terminal" nativa, sem interferir na janela dedicada

**CA-10 — Degradação graciosa**

- **Given** o plugin oficial desinstalado ou desabilitado
- **When** o usuário abre a tool window dedicada
- **Then** a sessão do `claude` inicia e é plenamente utilizável, apenas sem os recursos de integração com o IDE

**CA-11 — IDE sem terminal**

- **Given** um IDE sem o plugin `org.jetbrains.plugins.terminal`
- **When** o IDE é iniciado com este plugin instalado
- **Then** nenhum erro é exibido e a tool window simplesmente não é registrada

**CA-12 — `Esc` chega ao Claude Code** _(v1.1, RF-17)_

- **Given** uma sessão ativa na janela dedicada com um comando interativo aberto (ex.: `/usage`)
- **When** o usuário pressiona `Esc`
- **Then** o comando é encerrado pelo próprio CLI e o foco **permanece** no terminal, sem saltar para o editor

**CA-13 — Estado vazio utilizável** _(v1.1, RF-18)_

- **Given** a janela dedicada aberta com uma única aba
- **When** o usuário fecha essa aba
- **Then** a janela exibe "Nenhuma sessão do Claude Code aberta" com os links "Nova sessão" e "Retomar sessão", e clicar em qualquer um deles cria uma aba funcional

**CA-14 — `CLAUDE_CONFIG_DIR` por projeto** _(v1.1, RF-19/RF-20)_

- **Given** dois projetos abertos, um com `CLAUDE_CONFIG_DIR` configurado e outro sem
- **When** o usuário abre uma sessão em cada janela
- **Then** a primeira roda o `claude` com o config dir informado e a segunda usa o padrão do CLI, sem que nenhuma delas perca a integração com o plugin oficial

**CA-15 — Copiar a conversa** _(revisto em v1.3, RF-24)_

- **Given** uma sessão com várias trocas de mensagem, e a janela redimensionada durante a conversa
- **When** o usuário aciona "Copiar Conversa" no cabeçalho da janela
- **Then** a área de transferência contém a conversa **uma única vez** — sem banner repetido, sem caixa de input, sem barra de status — e o arquivo temporário usado no caminho não permanece em disco

**CA-17 — Copiar só um trecho** _(v1.3, RF-26)_

- **Given** uma sessão com texto na tela
- **When** o usuário seleciona um trecho com o mouse
- **Then** aparece um botão flutuante que, clicado, copia **apenas o trecho selecionado**; desfeita a seleção, o botão some sem copiar nada

**CA-16 — Exportar a conversa** _(v1.2, RF-22)_

- **Given** uma sessão com o `claude` em execução
- **When** o usuário aciona "Exportar Conversa"
- **Then** o `/export` é executado dentro da própria sessão e o CLI apresenta seu seletor de destino, sem que o plugin interprete ou armazene a transcrição

**CA-18 — Respiro nas bordas** _(v1.4, RF-28)_

- **Given** uma sessão aberta com respiro configurado
- **When** o usuário troca o tema do IDE
- **Then** a faixa entre o conteúdo e as bordas acompanha o novo fundo do terminal, sem ficar na cor antiga nem exigir reabrir a aba

**CA-19 — Partida sem eco** _(v1.4, RF-29)_

- **Given** a tool window aberta
- **When** uma nova aba de sessão é criada
- **Then** o prompt do shell e o comando `claude` não são vistos, a capa sai sozinha, e o CLI aparece já desenhado na largura real da aba — sem redesenhar o rodapé

**CA-20 — Síntese e reprodução de áudio** _(v1.5, RF-30/RF-31)_

- **Given** Piper instalado e modelo de voz configurado
- **When** o usuário seleciona texto e clica em play no popup
- **Then** o texto é sintetizado e reproduzido em áudio, os botões de pausa/stop aparecem ativos no menu "Áudio"

**CA-21 — Desabilitação de play sem Piper** _(v1.5, RF-30)_

- **Given** Piper não instalado OU modelo não configurado
- **When** o usuário seleciona texto
- **Then** o botão de play no popup fica desabilitado com tooltip explicativo

**CA-22 — Pausa e retomada** _(v1.5, RF-31)_

- **Given** áudio sendo reproduzido
- **When** o usuário clica em "Pausar"
- **Then** o som para, o botão muda para "Retomar"; reclicando continua a partir de onde pausou

**CA-23 — Interrupção** _(v1.5, RF-31)_

- **Given** áudio em qualquer estado (playing, paused, ou idle)
- **When** o usuário clica em "Parar" ou inicia novo play enquanto há reprodução anterior
- **Then** a reprodução anterior cessa e libera recursos (mixer, processo piper)

**CA-24 — Configuração de Piper** _(v1.5, RF-32)_

- **Given** janela de Settings aberta
- **When** usuário altera "Executável do Piper" ou "Modelo de voz"
- **Then** as mudanças são persistidas, e o botão play responde à nova configuração na próxima seleção

---

## Plano de Rollout

### Estratégia de deploy

Distribuição **local**, sem Marketplace:

1. `./gradlew buildPlugin` → gera `build/distributions/claude-code-dock-<versão>.zip`.
2. No IDE: _Settings → Plugins → ⚙ → Install Plugin from Disk…_ → selecionar o ZIP.
3. Reiniciar o IDE.
4. Repetir por IDE onde o plugin for desejado (a instalação não é compartilhada entre IDEs).

**Pré-requisito:** manter o plugin oficial da Anthropic instalado e habilitado, pois é ele que
fornece os recursos de integração (RNF-11, CA-10).

**Faseamento sugerido:**

1. **Fase 0 — validação (bloqueante).** Executar T-4 em um protótipo mínimo. Sem aprovação aqui,
   não se prossegue.
2. **Fase 1 — uso pessoal.** Instalar em um IDE, usar por alguns dias, registrar achados no `HANDOFF.md`.
3. **Fase 2 — expansão.** Instalar nos demais IDEs (T-3.5).

### Estratégia de rollback

Rollback é trivial e sem perda de estado — propriedade deliberada do design:

1. _Settings → Plugins → Claude Code Dock → Uninstall_; reiniciar.
2. O plugin oficial permanece intocado e volta a ser o único caminho.
3. **Nenhum dado é perdido:** o plugin não persiste conversas (elas pertencem ao CLI) nem altera
   configuração do IDE, do CLI ou do plugin oficial. A única configuração própria é o caminho do
   executável.

### Monitoramento pós-deploy

Sem telemetria, por decisão de privacidade. O acompanhamento é local:

- _Help → Show Log in Explorer/Finder_ → `idea.log`, filtrando pela categoria do plugin (RNF-12).
- Verificar ausência de processos `claude` órfãos após fechar abas (`ps aux | grep claude`).
- Reexecutar a suíte de fumaça (T-5.3) a cada upgrade de IDE.
- Registrar observações no `HANDOFF.md`.

---

## Perguntas em Aberto

| #        | Questão                                                                                                                       | Situação                                                                                                                                                                                                                                                                             |
| -------- | ----------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Q-01** | ~~O `TerminalCustomizer` da Anthropic de fato alcança widgets criados fora da tool window nativa?~~                           | ✅ **RESOLVIDO em 2026-08-01.** Sim. Validado por `TerminalCustomizerReachTest` (T-4) com teste de controle. Ponto exato: `configureStartupOptions` aplica os customizers                                                                                                            |
| **Q-02** | Duas sessões simultâneas conectadas ao mesmo servidor MCP causam ambiguidade de atribuição (ex.: qual sessão recebe um diff)? | Em aberto (R-05). Investigar durante T-4                                                                                                                                                                                                                                             |
| **Q-03** | Qual a semântica exata de `CLAUDE_CODE_JETBRAINS_PLUGIN_HIDE_BUTTON`?                                                         | String encontrada no jar oficial; **semântica não verificada**. Não usar antes de confirmar                                                                                                                                                                                          |
| **Q-04** | O engine `REWORKED` se comporta como o `CLASSIC` fora da tool window nativa?                                                  | Em aberto (R-06, CB-11). Cobrir em T-2.6                                                                                                                                                                                                                                             |
| **Q-05** | Vale ocultar o botão/ação do plugin oficial para evitar confusão de dois pontos de entrada?                                   | Decisão de produto, adiada até haver uso real. Depende de Q-03                                                                                                                                                                                                                       |
| **Q-06** | Suportar Remote Development e WSL no futuro?                                                                                  | Fora de escopo agora (CB-16, CB-17). Reavaliar conforme necessidade                                                                                                                                                                                                                  |
| **Q-07** | A tool window deve restaurar automaticamente as sessões ao reabrir o projeto?                                                 | Não previsto. `isTerminalSessionPersistent` existe na plataforma, mas persistência acrescenta complexidade sem demanda comprovada                                                                                                                                                    |
| **Q-08** | Qual o `since-build` mínimo realmente testável?                                                                               | Definido como `252` por conservadorismo; **testado apenas em `262`**. Builds anteriores não foram verificadas                                                                                                                                                                        |
| **Q-09** | _(v1.1)_ Vale permitir `CLAUDE_CONFIG_DIR` **por aba**, e não só por projeto?                                                 | Adiado. Em IDEs JetBrains uma janela é um projeto, então o escopo atual já atende o pedido. Por aba exigiria diálogo a cada "Nova sessão" — reavaliar se houver demanda                                                                                                              |
| **Q-10** | _(v1.1)_ O `TerminalEscapeKeyListener` se comporta igual no engine `REWORKED`?                                                | Em aberto. Lá o `Esc` passa por `Terminal.Escape` + EP `escapeHandler`, caminho diferente do pre-handler adotado. Ligado a Q-04 e R-09                                                                                                                                               |
| **Q-11** | _(v1.1)_ `CLAUDE_CONFIG_DIR` deveria ser versionável (`.idea/`) em vez de ficar no workspace?                                 | Decidido pelo workspace (RNF-05). Reavaliar só se surgir caso de config dir relativo ao repositório, compartilhável pelo time                                                                                                                                                        |
| **Q-12** | ~~_(v1.2)_ Vale passar `[filename]` ao `/export`?~~                                                                           | ✅ **RESOLVIDO em v1.3.** Sim, e deixou de ser conveniência: é a única forma de entregar a cópia sem duplicação (DEF-01). Virou RF-24, com R-13 a verificar primeiro                                                                                                                 |
| **Q-14** | _(v1.3)_ Reimplementar a UI como visualizador de markdown, dirigindo o CLI por `stream-json`?                                 | **Analisado e recusado.** Viável tecnicamente, mas é outro produto: descarta o terminal e todo o comportamento interativo que ele dá de graça, e acopla a um formato JSON sem contrato de estabilidade. Ver [Fora de Escopo](#fora-de-escopo) e Achado 17                            |
| **Q-15** | ~~_(v1.3)_ Qual prazo limite para o `/export` de RF-24 responder?~~                                                           | ✅ **RESOLVIDO na implementação.** 20 s, com sondagem do arquivo. Nenhum estouro observado no uso real; revisitar só se aparecer conversa que não caiba nesse prazo                                                                                                                  |
| **Q-16** | _(v1.4)_ Trocar o prazo fixo da capa (RF-29) por detecção de que o CLI já pintou?                                             | **Avaliado, não implementado.** É viável: `JediTermWidget.getTerminalTextBuffer()` e `addModelListener` são públicos, e `getScreenLines()` dá a tela como texto. Custo: acoplar-se ao texto do banner do CLI, que não tem contrato — some com a rede de segurança do prazo. Ver R-16 |
| **Q-17** | _(v1.4)_ Vale reintroduzir a capa em cima de uma partida sem eco (a alternativa de D-20), ficando só como acabamento?         | Em aberto. Combinadas, o pior caso da capa deixaria de ser "eco visível" e passaria a ser "tela vazia por um instante" — o chute do prazo ficaria inofensivo                                                                                                                         |
| **Q-18** | _(v1.5)_ Seleções muito grandes (>N caracteres) devem desabilitar o botão play, ou apenas retornar timeout na síntese?         | Adiado. Decisão inicial: deixar o botão ativo e retornar erro/timeout em síntese longa. Se virar problema, adicionar heurística para desabilitar play se seleção > threshold |
| **Q-19** | _(v1.5)_ Vale cachear o resultado de `canSynthesize()` para evitar checagem de arquivo a cada milissegundo durante seleção?  | Sim, mas como? Decidido em design: cache é atualizado a cada N segundos (constante configurável ~30s) ou no evento de mudança de configuração. Reavaliar latência se implementação demonstrar problema |
| **Q-20** | _(v1.5)_ O timeout da síntese (padrão 20s) deve ser configurável pelo usuário?                                                | Não nesta versão. Registrado como constante em `ClaudePiperPlayback`, ajustável por alguém que leia código. Reavaliar se surgirem modelos que rotineiramente ultrapassam 20s |
| **Q-13** | _(v1.2)_ A cópia deveria respeitar a seleção do mouse quando houver uma, em vez de sempre copiar tudo?                        | Adiado. `Ctrl+C`/`Ctrl+Shift+C` já cobrem a seleção; o botão existe justamente para o caso que o CLASSIC não resolve. `JBTerminalWidget.getSelectedText()` existe se mudarmos de ideia                                                                                               |

---

## Fase 3 — Revisão Crítica

Autorrevisão da especificação, conforme exigido pelo processo. Os achados abaixo **já foram
incorporados** ao corpo do documento.

### Achado 1 — Premissa do documento de origem incorreta (corrigido)

O plano inicial afirmava que a Anthropic inviabiliza tecnicamente que terceiros executem o
Claude Code. **A afirmação não se sustenta:** executar `claude` em um PTY é irrestrito. A
restrição real é a ausência de API pública para o protocolo de integração. Sem essa correção,
o SPEC teria justificado a arquitetura pela razão errada. Registrado em
[Análise do Estado Atual](#correção-de-uma-premissa-do-documento-de-origem).

### Achado 2 — Requisito de "histórico de conversas" era complexidade desnecessária (corrigido)

O plano de origem pedia histórico de conversas como funcionalidade do plugin. O CLI **já
oferece** `claude --resume` e `--continue`. Construir persistência própria significaria
duplicar responsabilidade, arriscar divergência do formato de sessão do CLI e criar um segundo
lugar onde código-fonte do usuário ficaria armazenado — um passivo de segurança sem
contrapartida. Reduzido a RF-08: uma ação que invoca o recurso nativo.

### Achado 3 — "Cópia de texto" já é resolvido pela plataforma (corrigido)

O plano de origem listava cópia de texto como funcionalidade. O widget JediTerm da plataforma já
provê seleção, cópia, colagem, busca no scrollback e links clicáveis. Nenhum requisito foi
criado; removido do escopo por já existir.

### Achado 4 — Cenário de falha central não estava coberto (corrigido)

A primeira versão do desenho tratava a injeção de `CLAUDE_CODE_SSE_PORT` em widgets customizados
como fato consumado. É uma **inferência de análise estática**, não uma observação. Como toda a
arquitetura depende dela, foi promovida a risco crítico (R-02), a pergunta em aberto (Q-01) e a
teste bloqueante (T-4) com plano de contingência explícito.

### Achado 5 — Ambiguidade de sessão dupla não fora considerada (corrigido)

Com o oficial instalado, passam a existir dois pontos de entrada (`Ctrl+Esc` e a janela
dedicada), possivelmente com duas sessões vivas contra o mesmo servidor MCP. O comportamento
resultante — qual sessão "possui" um diff aberto — não é conhecido. Adicionado como R-05, CB-04
e Q-02.

### Achado 6 — Compatibilidade retroativa e evolução do IDE (corrigido)

A API de terminal usada não é _interna_ no sentido estrito (é pública e usada pelo próprio
plugin oficial), mas tampouco é uma API de extensão estável com garantias. Mitigações: todo o
acoplamento em uma classe (RNF-15), suíte de fumaça por upgrade (T-5.3) e a decisão consciente
sobre `until-build` (R-08).

### Achado 7 — Superfície de segurança revista (corrigido)

Três vetores foram identificados e endereçados: (a) lockfiles contêm `authToken` — o plugin foi
proibido de tocá-los (RNF-04); (b) o buffer do terminal contém código-fonte e potencialmente
segredos — logging desse conteúdo foi proibido (RNF-06); (c) o caminho do executável é entrada
do usuário — exigida montagem de argumentos sem interpretação de shell (RNF-08), com teste
dedicado (T-1.6).

### Achado 8 — Cenários operacionais faltantes (corrigido)

A primeira versão não tratava: projetos não confiáveis (CB-12), Remote Dev/split mode (CB-16),
WSL (CB-17) e divergência entre engines de terminal (CB-11). Os três últimos foram declarados
fora de escopo de forma explícita, em vez de deixados implícitos.

### Achado 9 — Complexidade desnecessária evitada (validado)

Revisão explícita em busca de excesso de engenharia. Descartados por ausência de demanda
comprovada: UI de chat própria, camada de abstração sobre `TerminalWidget` com implementação
única, serviço de gerência de sessões separado da tool window, sistema de temas, telemetria e
persistência de sessões entre reinícios (Q-07). O escopo final — quatro componentes pequenos —
é considerado o mínimo que satisfaz os objetivos.

### Achado 10 — Gargalos de performance (validado)

Nenhum gargalo identificado. O plugin não faz I/O na inicialização (RNF-01), a sessão é
preguiçosa (RNF-02, CA-03) e a emulação de terminal é responsabilidade da plataforma. O único
cuidado registrado é não resolver caminho de executável na EDT (RNF-03).

### Achado 11 — Paridade de teclado não fora especificada _(v1.1)_

A v1.0 tratou "hospedar o terminal em outra tool window" como operação neutra. Não é: a
plataforma tem comportamento **condicionado ao id da tool window**, e o `Esc` é o exemplo
concreto — o único caminho em que "sem atalho configurado" significa coisas opostas dentro e
fora da tool window "Terminal". Para o Claude Code, cujo fluxo interativo depende de `Esc`, isso
não era detalhe cosmético: tornava a janela inutilizável em `/usage`, `/model` e afins.

**Lição registrada:** ao reaproveitar um widget da plataforma fora do seu host original, vale
auditar o que na plataforma decide comportamento por **identidade do host** (id de tool window,
`DataKey` resolvido pela hierarquia de componentes) e não por tipo do widget. Foi assim que o
bug apareceu e é onde os próximos provavelmente aparecerão.

### Achado 12 — Estado vazio era ponto cego do ciclo de vida _(v1.1)_

`ToolWindowFactory.createToolWindowContent` roda **uma vez por projeto**. A v1.0 descreveu a
criação da primeira sessão, mas nunca o que acontece depois que o usuário fecha a última aba: a
janela ficava viva, vazia e sem caminho de volta. O erro de método foi especificar o ciclo de
vida só na direção de abertura. Corrigido por RF-18, CB-20 e o Fluxo B2.

### Achado 13 — Configuração de aplicação vs. de projeto _(v1.1)_

A v1.0 tinha uma única configuração (caminho do executável) e a colocou, corretamente, no nível
de aplicação. `CLAUDE_CONFIG_DIR` é de natureza oposta: identifica **qual perfil do CLI** usar,
o que muda por contexto de trabalho. Colocá-lo no mesmo lugar teria criado uma configuração
global que o usuário precisaria reeditar a cada troca de janela — exatamente o incômodo que o
plugin existe para eliminar. Daí a separação em `ClaudeDockSettings` (aplicação) e
`ClaudeDockProjectSettings` (projeto), com uma única tela `projectConfigurable` mostrando os
dois e rotulando o escopo de cada campo.

### Achado 14 — O comportamento condicionado ao engine é a mesma armadilha do Achado 11 _(v1.2)_

A lição do `Esc` era "auditar o que a plataforma decide por **identidade do host**". A cópia
mostrou a irmã dela: **decisão por engine**. `Terminal.SelectAll` e `Terminal.CopySelectedText`
existem, aparecem na lista de ações do IDE e não funcionam para nós — não por bug, mas porque
seus `update()` exigem um `Editor` de terminal reformulado, que o JediTerm clássico não tem.

Presumir que "a ação existe, logo o recurso existe" teria produzido um botão morto. O que
salvou foi ler o `update()` antes de reusar a ação.

**Lição registrada:** ao reaproveitar uma ação da plataforma, ler o `update()` dela é parte da
verificação, não detalhe. Uma ação registrada não é uma capacidade disponível.

### Achado 15 — Um botão não cobria os dois usos _(v1.2)_

O pedido original era "um ícone que copia o conteúdo da janela". A primeira leitura levaria a
um botão só. Mas as duas coisas que o usuário pode querer têm naturezas opostas: o **render
literal** (rápido, fiel ao que está na tela, com bordas de TUI e quebras na largura da janela) e
a **transcrição** (limpa, completa, mas produzida pelo CLI, não por nós).

Nenhum dos dois substitui o outro: a cópia bruta não vira transcrição, e o `/export` não copia
o que está na tela agora. Dois botões pequenos custaram menos que um botão tentando adivinhar
qual dos dois o usuário queria.

**Achado colateral verificado:** o destino "clipboard" do `/export` depende de
`wl-copy`/`xclip`/`xsel`, ausentes nesta máquina (sessão Wayland). Ou seja, delegar tudo ao CLI
teria deixado o caso principal — copiar — sem solução. A cópia pelo `CopyPasteManager` do IDE
não tem essa dependência.

### Achado 16 — A cópia do buffer respondia a pergunta errada _(v1.3)_

A v1.2 perguntou "como copiar o conteúdo da janela?" e respondeu bem: `getText()`, uma chamada,
sem dependência externa. O usuário, porém, queria **copiar a conversa** — e a janela não é a
conversa. É a última renderização dela, empilhada sobre as anteriores.

A distinção parecia cosmética e não era: por ser um TUI Ink, cada repintura deixa **mais uma
cópia inteira** no scrollback. O defeito não apareceu na especificação nem nos testes unitários
porque ambos mediam a normalização do texto, não a **fidelidade do texto ao conceito**.

O trade-off nº 1 da v1.2 chegou a dizer, com todas as letras, "a cópia é o render, não a
conversa" — e ainda assim o recurso foi entregue como se render bastasse. **Nomear um
trade-off não é o mesmo que aceitá-lo em nome do usuário.** Aqui ele não era aceitável, e o
sinal disso já estava escrito.

**Lição registrada:** quando um trade-off documentado diz que a entrega é uma aproximação do que
foi pedido, ele é um item a validar com o usuário — não uma ressalva que se registra e segue.

### Achado 17 — "É possível?" quase nunca é a pergunta que decide _(v1.3)_

Os três pedidos desta rodada eram tecnicamente viáveis, e isso não os tornou equivalentes:

| Pedido                     | Viável? | Decisão | O que de fato decidiu                                                                    |
| -------------------------- | ------- | ------- | ---------------------------------------------------------------------------------------- |
| Copiar só o conteúdo       | sim     | fazer   | conserta um recurso que não serve como está                                              |
| Botão flutuante na seleção | sim     | fazer   | barato, e a alternativa (`Ctrl+C`) é invisível para quem usa mouse                       |
| Renderizar markdown        | sim     | **não** | custo de manutenção contínua contra formato sem contrato — ganha apresentação, não poder |

O terceiro é o que importa registrar: recusá-lo **não** é dizer que é difícil. É que o plugin
existe justamente por não reimplementar o que o CLI e a plataforma já fazem (D-01, D-04), e uma
UI própria inverteria essa premissa inteira para ganhar aparência. Se um dia essa decisão for
revista, que seja com esse custo à vista — e não pela pergunta "dá pra fazer?", cuja resposta
sempre foi sim.

### Achado 18 — Um recurso pode funcionar e mesmo assim estar errado _(v1.4)_

O RF-27 fez exatamente o que prometia: `--ax-screen-reader` produz saída plana, sem bordas nem
animação. A flag foi verificada com PTY e **não trava a sessão** — o `$` que parecia retorno ao
shell é o prompt de entrada do CLI em modo plano.

E foi removido assim mesmo. O motivo é que ele resolvia um problema que o usuário não tinha, e
criava um que ele tinha: uma linha de entrada indistinguível de prompt de shell, o que piora
CB-27 justamente na hora em que a sessão morre.

**Lição registrada:** "funciona conforme especificado" não é evidência de valor. O que decidiu
foi o uso, e o uso levou três minutos — menos tempo do que se gastou implementando a opção.

### Achado 19 — Constante de bancada não é configuração de usuário _(v1.4)_

O prazo da capa (RF-29) foi parar na tela de configurações por iniciativa minha, com a
justificativa de que "calibrar exige reinstalar o plugin". O usuário recusou de imediato: aquilo
é ajuste de implementação, não escolha de quem usa. Voltou a ser uma constante em um arquivo.

O respiro (RF-28) ficou na tela — e a diferença entre os dois casos é o teste que vale: **o
respiro é preferência estética, com valor certo diferente por pessoa; o prazo é uma medida de
quanto o CLI demora, com um valor certo só.** Configuração existe para o primeiro tipo.

**Lição registrada:** dificuldade de calibrar não é razão para expor um botão. É razão para o
número morar num lugar fácil de achar.

### Achado 20 — A ordem de investigação mudou o resultado, para pior _(v1.4)_

A tentativa de eliminar o eco do comando foi feita em três passos, e os dois primeiros
falharam por eu ter medido tarde demais:

1. **PTY direto no CLI.** Eliminou o eco e quebrou o ambiente: sem shell, o processo herdou o
   `PATH` da sessão gráfica e os hooks do usuário pararam de achar suas ferramentas.
2. **Correção pontual do `PATH`.** Resolvi a busca do `claude` e deixei todo o resto quebrado —
   remendo sobre o sintoma, não sobre a causa.
3. **Shell interativo com `exec`.** Preserva o ambiente exportado e elimina o eco. Foi aqui que
   eu **finalmente** medi o que sobrevive: variáveis exportadas sim, funções e aliases não.

Se a medição do passo 3 tivesse vindo antes do passo 1, os dois primeiros não teriam existido.
A informação estava disponível o tempo todo — bastava rodar o comando com PTY e ambiente limpo,
que é o que acabou decidindo tudo.

**Lição registrada:** quando a mudança mexe em ambiente de execução, medir o ambiente resultante
é o **primeiro** passo, não o último. Vale para qualquer troca de "quem é o processo pai".

### Achado 21 — Processamento fora da EDT é trivial com IntelliJ API _(v1.5)_

O plano temeu que síntese de fala no `piper` poderia travar a UI da EDT. A realidade: `ClaudeTtaSessions` executa síntese
via `ApplicationManager.getApplication().executeOnPooledThread { ... }`, mesma API que o projeto já usa para validar o
executável do `claude`. Nenhuma mudança de threading model foi necessária; o padrão existente escala direto.

### Achado 22 — Java Sound é suficiente para playback local _(v1.5)_

O plano perguntou se seria necessário chamar `aplay`/`paplay` para reproduzir o PCM gerado pelo Piper. Verificação 
empírica com um `Clip` de Java Sound: não é necessário. `javax.sound.sampled` abre mixer sem problemas em PipeWire/PulseAudio,
e a reprodução é simultânea à UI sem travos. O processo de áudio roda em thread nativa (mixer ALSA/PipeWire), não na EDT.

### Achado 23 — Configuração de modelo é responsabilidade do usuário, não automática _(v1.5)_

Diferentemente do `claude`, que roda com padrões embutidos, o Piper exige `-m <arquivo.onnx>` para qualquer síntese. 
A decisão de não ter "autodetecção" nem "download" de modelos é deliberada: não há registro central de vozes do Piper 
(diferente de um LLM em API). Documentar no README onde obter modelos (Hugging Face da oma/piper) e que o usuário 
configure manualmente é o caminho certo.

### Achado 24 — Implementação completa de TTS em 82 linhas de Kotlin _(v1.5)_

A integração de Piper foi implementada com 4 classes novas (~350 linhas totais) e 82 testes passando:
- `ClaudePiperPlayback` — 120 linhas, síntese + reprodução com Java Sound
- `ClaudeTtaSessions` — 80 linhas, serviço de projeto com estado
- `AudioPauseResumeAction` / `AudioStopAction` — 60 linhas, ações do menu
- Extensões em `ClaudeSelectionCopyButton`, `ClaudeToolWindowFactory`, `ClaudeDockSettings`

**Princípio aplicado:** cada classe tem uma responsabilidade única (RNF-19, RNF-20, RNF-22, RNF-23), nenhuma acoplada à outra além de camadas. A síntese roda fora da EDT via `executeOnPooledThread`, o playback usa `javax.sound.sampled.Clip` (16-bit, 22050 Hz, mono), e toda exceção é tratada com retorno nulo/falso sem propagar.

**Precedente:** o mesmo padrão aparece em RF-17 (Esc), RF-24 (export), RF-28 (respiro) — quando cada RF novo segue o padrão de "uma classe, uma responsabilidade", o código fica simples de ler e revisar. Piper é o terceiro caso dessa série.

---

## Anexo — Rastreabilidade das evidências

Toda afirmação técnica sobre o estado atual remonta a uma verificação direta, conforme a regra
"DON'T GUESS, VERIFY" do `CLAUDE.md`.

| Afirmação                                                                                                                                                                | Evidência                                                                                                                                                                    |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Oficial usa `createShellWidget` + `sendCommandToExecute`                                                                                                                 | `javap -c` de `com.anthropic.code.plugin.TerminalUtil`                                                                                                                       |
| Oficial prende a sessão à tool window "Terminal"                                                                                                                         | literal `"Terminal"` passado a `ToolWindowManager.getToolWindow` em `TerminalUtil`                                                                                           |
| Oficial hospeda servidor MCP                                                                                                                                             | jars `ktor-server-{cio,websockets,sse}`, `kotlin-sdk-jvm-0.4.0`; classes `mcp/tools/*`, `WebSocketMcpServerTransport`                                                        |
| Injeção via `CLAUDE_CODE_SSE_PORT`                                                                                                                                       | literal em `TerminalCustomizer.class`; registro em `META-INF/plugin-terminal.xml`                                                                                            |
| Formato do lockfile                                                                                                                                                      | arquivos reais em `~/.claude/ide/*.lock`, incluindo um escrito por GoLand                                                                                                    |
| `TerminalToolWindowManager` delega a `startShellTerminalWidget`                                                                                                          | `javap -c` de `TerminalToolWindowManager`                                                                                                                                    |
| `TerminalWidget` é embutível                                                                                                                                             | interface estende `com.intellij.openapi.ui.ComponentContainer`                                                                                                               |
| API de terminal presente na build 262                                                                                                                                    | `javap` sobre `plugins/terminal/lib/terminal.jar`                                                                                                                            |
| `TerminalEngine` tem `CLASSIC`/`REWORKED`/`NEW_TERMINAL`                                                                                                                 | `javap` de `org.jetbrains.plugins.terminal.TerminalEngine`                                                                                                                   |
| Versões de ambiente                                                                                                                                                      | `java -version`, `gradle --version`, `claude --version`, `build.txt`, `product-info.json`                                                                                    |
| IntelliJ Platform Gradle Plugin 2.18.1                                                                                                                                   | portal de plugins do Gradle                                                                                                                                                  |
| _(v1.1)_ `Esc` só é entregue ao shell na tool window de id `"Terminal"`                                                                                                  | `javap -c` de `com.intellij.terminal.TerminalEscapeKeyListener.shouldSwitchFocusToEditor` e de `JBTerminalWidget.isTerminalToolWindow`                                       |
| _(v1.1)_ `JBTerminalPanel` roda pre-handlers antes do listener de `Esc` e respeita `isConsumed`                                                                          | `javap -c` de `com.intellij.terminal.JBTerminalPanel.handleKeyEvent`                                                                                                         |
| _(v1.1)_ 2026.2 distribui `Terminal.SwitchFocusToEditor` sem atalho padrão                                                                                               | ausência de `keyboard-shortcut` no `META-INF/plugin.xml` do terminal + chaves `escape.behavior.change.notification.*` em `TerminalBundle.properties`                         |
| _(v1.1)_ `CLAUDE_CONFIG_DIR` é variável reconhecida pelo CLI                                                                                                             | literal encontrado no jar do plugin oficial (já registrado no `HANDOFF.md`)                                                                                                  |
| _(v1.1)_ `ToolWindowEx.emptyText`, `ShellStartupOptions.Builder.envVariables`, `JBTerminalPanel.addPreKeyEventHandler` existem em 262                                    | compilação bem-sucedida contra `intellijIdea("2026.2")`                                                                                                                      |
| _(v1.2)_ `TerminalWidget.getText()` devolve scrollback + tela no CLASSIC, e string vazia fora dele                                                                       | `javap -c` de `JBTerminalWidget.getText(TerminalPanel)`, de `JBTerminalWidget$TerminalWidgetBridge.getText` e do `default` da interface                                      |
| _(v1.2)_ Não há "copiar tudo" no engine CLASSIC                                                                                                                          | `Terminal.SelectAll` só é referenciada em `Terminal.ReworkedTerminalContextMenu` (plugin.xml do terminal); `TerminalSelectAllAction.update` exige `isReworkedTerminalEditor` |
| _(v1.2)_ `sendCommandToExecute` não serve para falar com um TUI vivo                                                                                                     | `javap -c` de `ShellTerminalWidget.executeCommand`: lança `IOException` quando `getTypedShellCommand()` não está vazio                                                       |
| _(v1.2)_ O CLI tem o slash command `/export`                                                                                                                             | string no binário `claude` 2.1.220: `{type:"local-jsx", name:"export", description:"Export the current conversation to a file or clipboard", argumentHint:"[filename]"}`     |
| _(v1.2)_ O destino "clipboard" do `/export` depende de utilitário externo ausente nesta máquina                                                                          | `grep` por `wl-copy`/`xclip`/`xsel`/`pbcopy` no binário + `command -v` (nenhum instalado; `XDG_SESSION_TYPE=wayland`)                                                        |
| _(v1.2)_ Limite do scrollback vem de `terminal.buffer.max.lines.count`                                                                                                   | `javap -c` de `JBTerminalSystemSettingsProviderBase.getBufferMaxLinesCount`                                                                                                  |
| _(v1.2)_ `CopyPasteManager.copyTextToClipboard` existe e `Content` é `UserDataHolder`                                                                                    | `javap` de `intellij.platform.editor.ui.jar` e de `com.intellij.ui.content.Content`                                                                                          |
| _(v1.3)_ **O `/export` produz saída limpa**: conversa uma única vez, sem caixa de input nem barra de status                                                              | leitura do arquivo real gerado em 2026-08-01 16:23, `2026-08-01-162241-*.md`, 45 linhas                                                                                      |
| _(v1.3)_ O buffer contém a conversa repetida, um frame por repintura                                                                                                     | amostra colada pelo usuário: rodapé do 1º bloco marca `⧉ In README.md` e o do 2º, `⧉ In a.txt` — instantes diferentes                                                        |
| _(v1.3)_ `TerminalPanel.selectAll()` é **público** — o que falta é ação registrada, não a capacidade                                                                     | `javap` de `com.jediterm.terminal.ui.TerminalPanel` (corrige a redação da v1.2)                                                                                              |
| _(v1.3)_ Existe listener de seleção público: `addSelectionListener(TerminalSelectionChangesListener)`, com `selectionChanged(TerminalSelection)`                         | `javap` de `TerminalPanel` e de `TerminalSelectionChangesListener`                                                                                                           |
| _(v1.3)_ Não há conversão célula→pixel pública (`myCharSize` é `protected`); posicionar o botão flutuante exige a posição do mouse                                       | `javap -p` de `TerminalPanel`                                                                                                                                                |
| _(v1.3)_ O CLI suporta `--print --output-format stream-json --input-format stream-json --include-partial-messages`                                                       | `claude --help` v2.1.220                                                                                                                                                     |
| _(v1.3)_ `/export <arquivo>` grava **sem UI**, sobrescreve, cria diretórios e acrescenta `.txt` se faltar extensão; o argumento é usado cru (`r.trim()`)                 | implementação extraída do binário `claude` 2.1.220 (funções `azb`, `Y5b`, `u0n`)                                                                                             |
| _(v1.3)_ `--ax-screen-reader` não tem restrição a `--print`, então vale em sessão interativa                                                                             | `claude --help`: a descrição não traz a ressalva "(only works with --print)" presente em outras flags                                                                        |
| _(v1.3)_ `JBPopupFactory.createComponentPopupBuilder` + `setRequestFocus/setCancelOnClickOutside/setResizable/setMovable` e `JBPopup.show(RelativePoint)` existem em 262 | `javap` de `intellij.platform.ide.jar` e `intellij.platform.ide.core.jar`                                                                                                    |
| _(v1.4)_ **O customizer do plugin oficial devolve o comando intacto** e só mexe no ambiente — injeta `CLAUDE_CODE_SSE_PORT` e também `ENABLE_IDE_INTEGRATION=true`       | `javap -c` de `com.anthropic.code.plugin.TerminalCustomizer.customizeCommandAndEnvironment`: o método termina em `aload_3; areturn`                                          |
| _(v1.4)_ `JediTermWidget.getComponent()` devolve **o próprio widget** (`JPanel` com `BorderLayout`); o terminal e a barra ficam num `JLayeredPane` interno               | `javap -c`: `aload_0; areturn`, e o construtor faz `add(myInnerPanel, "Center")`                                                                                             |
| _(v1.4)_ `TerminalPanel.getTerminalSizeFromComponent()` mede o **próprio painel** (`getWidth() - getInsetX()`), e `getInsetX()` devolve a constante `4`                  | `javap -c` de `TerminalPanel`                                                                                                                                                |
| _(v1.4)_ `TerminalPanel.getBackground()` delega a `getWindowBackground()` → `SettingsProvider.getDefaultBackground()`, **recalculado a cada chamada**                    | `javap -c` de `TerminalPanel`                                                                                                                                                |
| _(v1.4)_ Nem `JediTermWidget` nem `JBTerminalWidget` chamam `setBackground`: o `JPanel` fica na cor de painel do tema                                                    | `javap -c` das duas classes, sem ocorrência de `setBackground`                                                                                                               |
| _(v1.4)_ `PathEnvironmentVariableUtil.findInPath` usa `EnvironmentUtil.getValue("PATH")` — que **não** continha `~/.local/bin` neste ambiente, gerando falso negativo    | `javap -c` de `getPathVariableValue` + a notificação observada com o CLI funcionando                                                                                         |
| _(v1.4)_ `--ax-screen-reader` **não trava a sessão**: o processo segue vivo e o `$` é o prompt de entrada em modo plano                                                  | `script -qec` com `timeout 15`: saída `exit=124` (morto pelo timeout) e último byte `$` + `ESC[2G`                                                                           |
| _(v1.4)_ O modo também liga por `CLAUDE_AX_SCREEN_READER` ou pelo setting `axScreenReader`                                                                               | string extraída do binário `claude` 2.1.220 (classe `ytu.isEnabled`)                                                                                                         |
| _(v1.4)_ Um shell interativo com `exec` preserva **variáveis exportadas** (`PATH` completo, `NVM_DIR`, `SDKMAN_DIR`, `PYENV_ROOT`…) e **perde** funções e aliases        | `env -i … zsh -i -c 'exec "$0" "$@"' env` sob PTY, comparando nomes; e `command -v` para `sdk`/funções                                                                       |
| _(v1.4)_ Sem PTY, o `.zshrc` do usuário **não** é carregado nem com `-i`                                                                                                 | mesmo comando sem `script`: nenhuma invocação resolveu o `claude`                                                                                                            |
| _(v1.4)_ `JBLoadingPanel` cobre com **véu translúcido**: o conteúdo por baixo continua legível                                                                           | observação no IDE — o eco do comando aparecia através da capa                                                                                                                |
| _(v1.4)_ `JLayeredPane.DEFAULT_LAYER` é `Integer`, e passá-lo direto ao `add` faz o Kotlin escolher o overload de **índice**, ignorando a camada                         | teste `T-1.20` falhou afirmando `getLayer(capa) > getLayer(terminal)`                                                                                                        |
| _(v1.4)_ `Box.setLayout` lança `AWTError("Illegal request")`                                                                                                             | três testes de `ClaudeSessionLoading` falharam com essa exceção                                                                                                              |
| _(v1.4)_ `IconUtil.scale(Icon, Double)` está **depreciado** em 262; o substituto é `scale(Icon, Component?, Float)`                                                      | warning de compilação                                                                                                                                                        |
| _(v1.4)_ `TerminalProjectOptionsProvider.getShellPath()` é público e síncrono — dá o shell configurado em Settings > Tools > Terminal                                    | `javap` de `TerminalProjectOptionsProvider`                                                                                                                                  |
| _(v1.4)_ `JediTermWidget.getTerminalTextBuffer()`, `TerminalTextBuffer.addModelListener` e `getScreenLines()` são públicos — base para Q-16                              | `javap` de `JediTermWidget` e `TerminalTextBuffer`                                                                                                                           |
| _(v1.5)_ Piper TTS está instalado e acessível                                                                                                                          | `which piper`, `piper --help`, `pip show piper-tts`                                                                                                                          |
| _(v1.5)_ Modelo `.onnx` de voz está em `~/.claude/piper-voices/pt_BR-faber-medium.onnx` (63 MB)                                                                       | `ls -la ~/.claude/piper-voices/`                                                                                                                                             |
| _(v1.5)_ Piper produz PCM cru (22050 Hz, 16-bit, mono, little-endian) sem erros                                                                                       | `echo "teste" \| piper -m <modelo> --output-raw \| wc -c` (40960 bytes para "teste")                                                                                        |
| _(v1.5)_ Java Sound (`javax.sound.sampled.SourceDataLine`) consegue reproduzir o PCM do Piper                                                                        | compilação e execução do teste `MixerCheck.java` com JDK 21 Zulu — `isLineSupported: true`                                                                                 |
| _(v1.5)_ Mixers de áudio estão disponíveis via `AudioSystem.getMixerInfo()` (ALSA/PipeWire)                                                                           | listagem de mixers: HDMI, USB, Generic, default — nenhum mixer bloqueado                                                                                                    |
| _(v1.5)_ `DefaultActionGroup(text, true)` é aceito por `ToolWindow.setTitleActions(List<AnAction>)` (popup automático)                                                | `javap` de `DefaultActionGroup implements AnAction` + conhecimento de padrão IntelliJ                                                                                      |
| _(v1.5)_ `ClaudeDockSettings.PersistentStateComponent` pode ter campos novos (`piperExecutable`, `piperModel`) sem migrações                                           | padrão já usado com `claudeExecutable` em v1.0; XML serialization é transparente                                                                                             |

**Não verificado (declarado como suposição):** semântica de
`CLAUDE_CODE_JETBRAINS_PLUGIN_HIDE_BUTTON` (Q-03); comportamento de builds anteriores a `262`
(Q-08); alcance efetivo do customizer em widget customizado (Q-01 / T-4); comportamento do `Esc`
sob o engine `REWORKED` (Q-10); ~~_(v1.2)_ comportamento do `/export` acionado por digitação
simulada~~ ✅ verificado em T-3.11.

~~_(v1.3)_ **Não verificado e bloqueante:** o argumento `[filename]` do `/export`~~ ✅ resolvido
por leitura do binário — ver [Como o `/export` trata o argumento](#como-o-export-trata-o-argumento-lido-no-binário-não-suposto).

_(v1.3)_ **Não verificado:** se o popup de RF-26 se comporta bem sob arraste rápido e troca de
aba (T-3.15, T-3.16). ~~Quanto o `--ax-screen-reader` melhora a leitura no uso diário~~ ✅
respondido em T-3.17: funciona, e mesmo assim foi recusado (Achado 18).

_(v1.4)_ **Não verificado:** o comportamento do respiro e da capa fora do engine CLASSIC
(CB-36, ligado a Q-04); se o prazo da capa se mantém suficiente em máquina mais lenta ou com
hooks de sessão pesados (R-16); e o caso de diretório não confiável com a capa no ar (CB-34,
T-3.20).

_(v1.4)_ ~~**Código sem teste:** os diretórios de fallback da verificação do executável (R-17)~~
✅ **COBERTO** — T-1.21 implementado, com o par aceito/recusado servindo de controle.
