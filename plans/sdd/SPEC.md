# SPEC — Claude Code Dock: tool window dedicada para JetBrains

- **Versão:** 1.10
- **Data:** 2026-08-08
- **Status:** Especificação — v1.10 avalia três pedidos: aceita dois (RF-50, RF-51) e recusa o
  terceiro como requisito, por ter causa medida fora do plugin (Achado 33)
- **Autor:** Reginaldo Morais (com assistência do Claude Code)

> **Histórico de versões**
>
> | Versão | Data       | Mudança                                                                                                                                                                                                                                                 |
> | ------ | ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
> | 1.0    | 2026-08-01 | Especificação inicial (Fases 1–3 do SDD)                                                                                                                                                                                                                |
> | 1.1    | 2026-08-01 | RF-17 (Esc devolvido ao shell), RF-18 (estado vazio utilizável) e RF-19 (`CLAUDE_CONFIG_DIR` por projeto), após teste manual                                                                                                                            |
> | 1.2    | 2026-08-01 | RF-21 (copiar o conteúdo da sessão) e RF-22 (exportar a conversa via `/export`), após T-3.1/T-3.2 aprovados                                                                                                                                             |
> | 1.3    | 2026-08-01 | DEF-01 (cópia duplicada) diagnosticado; RF-21 **revisto** para passar pelo `/export` (RF-24/RF-25); RF-26 (cópia flutuante por seleção); Q-14 (UI de markdown) analisada e recusada                                                                     |
> | 1.4    | 2026-08-01 | RF-27 (saída plana) **removido** após uso real; RF-28 (respiro nas bordas) e RF-29 (tela de carregamento) especificados; D-09 reconfirmado com a alternativa `exec` medida e recusada                                                                   |
> | 1.5    | 2026-08-02 | RF-31/RF-32 (pause/stop via Piper TTS); detecção de Piper + modelo; configuração de executável e caminho do modelo; ações no menu do cabeçalho                                                                                                          |
> | 1.5.1  | 2026-08-03 | RF-30 (botão play no popup) **descartado** — complexidade UX no popup para pouco ganho vs. menu de cabeçalho já existente (RF-31)                                                                                                                       |
> | 1.6    | 2026-08-03 | RF-33/RF-34 (exportar o trecho selecionado para arquivo, pelo popup de seleção); D-30 registra por que isso **não** passa pelo `/export` do CLI                                                                                                         |
> | 1.7    | 2026-08-03 | RF-36 a RF-39 (dividir a aba em várias sessões); D-33 (o gatilho nativo já existia), D-34 (foco define a sessão ativa); Achado 27 corrige a premissa de engine de CB-26/CB-36/R-15                                                                      |
> | 1.7.1  | 2026-08-03 | RF-41 (fechar a divisão pelo cabeçalho) após uso real: a capacidade já existia sob o rótulo "Close Tab" da plataforma, que numa aba dividida diz o oposto do que faz (Achado 29)                                                                        |
> | 1.7.2  | 2026-08-03 | DEF-02 corrigido: "Select Previous/Next Tab" com uma aba só disparava assertion da plataforma — `selectNextContent`/`selectPreviousContent` exigem mais de uma aba                                                                                      |
> | 1.8    | 2026-08-03 | RF-43 (trocar de lado e girar a divisão); DnD de panes **avaliado e recusado** por custo de UI, não de mecanismo — registrado em Q-28                                                                                                                   |
> | 1.8.1  | 2026-08-03 | DEF-03 (aba marcada "encerrado" com sessão viva ao lado) e DEF-04 (menu "Dividir" vazio); RF-44 e RF-45 especificam o comportamento correto                                                                                                             |
> | 1.8.2  | 2026-08-03 | DEF-05 (cabeçalho inteiro morria ao fechar a primeira pane — `preferredFocusableComponent` órfão) e DEF-06 (nome ambíguo); RF-46. **Revoga o diagnóstico de DEF-04** (Achado 30)                                                                        |
> | 1.9    | 2026-08-07 | RF-47 (velocidade da fala, 0,25x–2x, lista fechada em menu e tela) **revoga** parte da exclusão de v1.5 sobre `--length-scale`; RNF-31 (locale em argumento de processo); DEF-07 — "Tocar seleção" nunca tocou (RF-48); Achado 31                       |
> | 1.9.1  | 2026-08-08 | **Achado 31 quitado**: `synthesize` ganha o prazo de 20 s que o SPEC prometia desde a v1.5 e passa a registrar o processo em `currentProcess` — sem isso o cancelamento de RNF-23 era inerte. T-1.55/T-1.56; remoção de `readText` (morto desde a v1.3) |
> | 1.9.2  | 2026-08-08 | **T-2.1 a T-2.6 implementados** — a lacuna de integração mais antiga do projeto. T-2.6 fixa o Achado 27 como guarda de regressão e **corrige a razão pela qual Q-04/Q-10 estavam resolvidos**. Registra o que o ambiente headless não entrega (PTY) e o que fica com os roteiros manuais |
> | 1.9.3  | 2026-08-08 | **DEF-08**: o `Ctrl+Alt+K` do plugin oficial nunca poderia chegar à nossa janela — `focusClaudeInTerminal` e `openClaudeInTerminal` estão presos ao literal `"Terminal"`. T-3.3 reescrito: era roteiro sobre premissa falsa. T-3.36 e T-3.4 aprovados |
> | 1.9.4  | 2026-08-08 | **RF-49** — ação própria de enviar a seleção do editor para a pane **em foco**, fechando DEF-08 sem tocar no protocolo privado. D-41. T-1.57 a T-1.61 |
> | 1.10   | 2026-08-08 | Três pedidos avaliados. **RF-50** — play do trecho no popup da seleção, **revogando o teto de dois botões de R-23** com critério novo (Achado 34). **RF-51** — `/usage` no cabeçalho, entregue como envio à sessão e **não** como popup, porque o popup exigiria o token do usuário (Achado 35). **Colar print screen recusado como RF**: o CLI já implementa o caminho e o que falta é um pacote do sistema (Achado 33, Q-32, T-3.62) |
> | 1.9.5  | 2026-08-08 | **Q-31 explicada no mecanismo (Achado 32)**: `CLAUDE_CODE_SSE_PORT` vem de `getOrDefault(locationHash, 0)`, e a mitigação de corrida do plugin oficial ignora as nossas panes. T-3.61 mede |

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
11. _(v1.6)_ Permitir **gravar em arquivo apenas o trecho selecionado**, sem passar pelo CLI —
    o complemento natural da cópia por seleção (RF-26), para quando o destino é um arquivo e
    não a área de transferência.
12. _(v1.7)_ Permitir **dividir uma aba em duas ou mais sessões visíveis ao mesmo tempo**, lado a
    lado ou empilhadas, para acompanhar mais de um Claude Code sem alternar de aba.

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
- _(v1.5, ~~parcialmente revogado~~ em v1.9)_ **Controle de qualidade de síntese.** ~~Sem suporte a
  `--length-scale`~~, `--noise-scale`, `--volume`, `--speaker` — só o padrão de cada modelo.
  > **v1.9:** `--length-scale` sai da exclusão e vira RF-47. A velocidade é a única destas que o
  > usuário pediu depois de conviver com a feature, e é a única cujo efeito ele consegue julgar de
  > ouvido sem entender o modelo. As outras três continuam fora: `--noise-scale`/`--noise-w-scale`
  > são parâmetros de treino expostos, `--volume` duplica o mixer do sistema, e `--speaker` só
  > existe em modelos multi-voz, que a exclusão de gerência de vozes já mantém fora.
- _(v1.5)_ **Fila de reprodução.** Uma única fala por vez; um novo play interrompe o que estiver
  tocando sem oferecer fila.
- _(v1.5)_ **Botão de play no popup de seleção (RF-30).** O menu "Áudio" no cabeçalho já oferece
  play/pause/stop enquanto a fala está em curso. Adicionar play também no popup de seleção
  (junto com cópia) adicionaria UI paralela sem capacidade nova, apenas um segundo caminho para
  a mesma ação. Decisão: manter play só no cabeçalho (RF-31) e cópia no popup (RF-26). RF-30
  descartado em v1.5.1.
- _(v1.6)_ **Conversão de formato na exportação do trecho.** O que sai no arquivo é o texto do
  terminal como está, apenas com os espaços à direita aparados (`ClaudeSessionText.normalize`).
  Sem envolver em cerca de código markdown, sem detectar linguagem, sem converter as sequências
  ANSI que a seleção já não traz. Ver Q-22.
- _(v1.6)_ **Abrir no editor o arquivo exportado.** Gravar e abrir são intenções diferentes; o
  usuário que quer ver o resultado tem o arquivo no caminho que ele mesmo escolheu. Ver Q-21.
- _(v1.6)_ **Exportação do trecho pelo `/export` do CLI.** Não é uma escolha de simplicidade, é
  uma impossibilidade: o `/export` é executado pelo Claude Code e exporta **a conversa**, sem
  qualquer forma de restringi-lo a um trecho da tela. Ver D-30.
- _(v1.7)_ **Persistência do layout de divisão.** Fechar o projeto descarta as divisões, como já
  descarta as abas (Q-07). Restaurar exigiria persistir a árvore de panes e recriar sessões —
  contra D-02, que deixa o histórico com o CLI.
- _(v1.7, reavaliado em v1.8)_ **Arrastar panes com o mouse para reorganizá-las.** Reposicionar
  passou a existir por ações (RF-43); o que continua fora é o **arraste**. A recusa não é por
  dificuldade de DnD — a plataforma tem `DnDSupport` e o framework `DockManager`/`DockContainer`
  que o editor usa. É por falta de onde agarrar:

  > O editor arrasta o **rótulo da aba** (`TabInfo.DragOutDelegate`), nunca o corpo do editor.
  > As nossas panes não têm aba, e a superfície delas já é do terminal: arrastar dentro da pane
  > **é** selecionar texto, que é o mecanismo de RF-26/RF-33. Logo, DnD exigiria antes uma barra
  > de título por pane — UI permanente, roubando altura de todas, para servir uma ação ocasional.
  > Sinal de custo: o terminal da própria JetBrains tem split e **não** implementa DnD de panes
  > (nenhuma classe de DnD em `terminal.jar`). Ver Q-28.

- _(v1.7)_ **Limite de panes por aba.** Não há teto artificial: a divisão é recursiva e aninha à
  vontade. O limite prático é a legibilidade, e quem decide é quem divide (mesma postura de
  RNF-18 para abas).

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

| Item                        | Valor verificado                                                   | Como foi verificado                                             |
| --------------------------- | ------------------------------------------------------------------ | --------------------------------------------------------------- |
| Piper TTS                   | `piper-tts` 1.4.2, acessível via `~/.pyenv/shims/piper`            | `piper --help`; `pip show piper-tts`                            |
| Modelo de voz preparado     | `~/.claude/piper-voices/pt_BR-faber-medium.onnx` (63 MB + `.json`) | `ls -la ~/.claude/piper-voices/`; teste de síntese bem-sucedido |
| Saída de síntese            | PCM cru, 22050 Hz, 16-bit, mono, little-endian, sem stderr         | `echo "teste" \| piper -m <modelo> --output-raw \| wc -c`       |
| Playback via Java Sound     | Suportado: `SourceDataLine` abre no formato exato do Piper         | compilação e execução de `MixerCheck` com `javax.sound.sampled` |
| Mixers de áudio disponíveis | HDMI, USB, e linhas genéricas (ALSA via PipeWire/PulseAudio)       | listagem de `AudioSystem.getMixerInfo()` no JDK 21              |
| Limite de Piper             | **Requer `-m MODEL`** — não há modelo padrão como há em `claude`   | análise de `piper --help` v1.4.2                                |

**Descoberta que define a arquitetura (v1.5):** diferentemente do `claude` que roda com padrões
embutidos, o **Piper é obrigatoriamente configurável**. "Piper instalado" = executável presente
**E** caminho válido para um modelo `.onnx` configurado. A ausência de qualquer um disso disable
o botão play.

### Gravar um trecho em arquivo: o que a plataforma já dá _(verificado em v1.6)_

O diálogo de "salvar como" não precisa ser construído — a plataforma tem o nativo, e ele é o
mesmo que o IDE usa em _File → Save As_. Assinaturas conferidas por `javap` sobre
`intellij.platform.ide.jar` da distribuição 2026.2 (build `IU-262.8665.337`):

| Símbolo                                                                      | Assinatura verificada                                                |
| ---------------------------------------------------------------------------- | -------------------------------------------------------------------- |
| `FileSaverDescriptor(String title, String description, String... ext)`       | construtor público — as extensões filtram e sugerem o sufixo         |
| `FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)` | devolve `FileSaverDialog`                                            |
| `FileSaverDialog.save(Path baseDir, String filename)`                        | sobrecarga com `java.nio.file.Path` — dispensa achar o `VirtualFile` |
| `VirtualFileWrapper.getFile()`                                               | devolve `java.io.File`; `null` do `save(...)` significa cancelado    |

Três consequências para o desenho:

| O que a API mostra                          | Efeito em RF-33/RF-34                                                           |
| ------------------------------------------- | ------------------------------------------------------------------------------- |
| `save(...)` devolve `null` no cancelamento  | Cancelar é caminho normal, não erro: nada a gravar e nada a notificar (CB-44)   |
| O diálogo nativo já trata arquivo existente | A confirmação de sobrescrita vem de graça; não escrevemos essa pergunta (CB-45) |
| A sobrecarga aceita `Path`                  | O diretório inicial sai direto de `project.basePath`, sem passar pelo VFS       |

> ⚠️ **Nota de método.** A primeira busca por essas classes nos jars da plataforma deu
> **falso negativo**: `grep` sem `-a` trata `.class` como binário e não reporta as linhas. É
> exatamente a armadilha já registrada no `HANDOFF.md` da rodada de 2026-08-01 — e ela pegou de
> novo. Com `grep -a`, as classes apareceram.

### O gatilho do split já existia na plataforma _(verificado em v1.7)_

A suposição natural era que dividir a aba exigiria inventar a ação, o atalho e o menu. Não
exige: **o widget de terminal já pede a divisão a quem o hospeda**, e até agora não havia
ninguém atendendo.

A cadeia, conferida por `javap` sobre `terminal.jar` da 2026.2:

```
ShellTerminalWidget.getActions()
  └─ TerminalSplitAction.create(vertically, getListener())     // duas: right e down
       ├─ isEnabled(KeyEvent)      → listener.canSplit(vertically)
       └─ actionPerformed(KeyEvent) → listener.split(vertically)
```

`getActions()` é o que alimenta o menu de contexto do JediTerm e o tratamento de teclas do
painel. Como o nosso widget nunca teve `listener`, as duas ações simplesmente não apareciam.
Implementar `JBTerminalWidgetListener` faz surgirem "Split Right" e "Split Down" no botão
direito, com os atalhos do keymap — **sem uma linha de UI nossa** (D-33).

De quebra, o mesmo listener entrega outros itens que já estavam mortos pelo mesmo motivo:
`onNewSession`, `onSessionClosed`, `onPreviousTabSelected` e `onNextTabSelected`.

**Semântica da direção (fácil de inverter, então foi verificada).** Em
`TerminalSplitAction$Companion.create`, `vertically = true` é pareado com o texto
`action.SplitVertically.text` e o atalho `TW.SplitRight` — ou seja, **divisor vertical, panes
lado a lado**. `vertically = false` é o `TW.SplitDown`. Já o `Splitter(boolean vertical)` da
plataforma usa a convenção **oposta**: `true` empilha um sobre o outro. Os dois booleanos se
chamam parecido e significam o contrário, o que é exatamente como se envia um recurso invertido —
por isso a conversão é explícita no código e há teste de geometria (T-1.34, T-1.35).

### DEF-03 — "encerrado" dizia respeito à sessão errada _(v1.8.1)_

Primeiro uso do "Fechar divisão" com duas panes: uma fechou, a outra continuou viva — e a **aba
inteira** apareceu como `Claude (2) (encerrado)`.

**Causa.** O callback de término era registrado só para a sessão **original** da aba:

```kotlin
pane.widget.addTerminationCallback({ content.displayName = "$title (encerrado)" }, tabDisposable)
```

Fechar a divisão descarta o `Disposable` daquela pane, o que mata o PTY, o que dispara o
callback. Se a pane fechada fosse a original — e é a que está em foco na maioria das vezes —, a
aba era marcada como encerrada com uma sessão trabalhando ao lado.

**Correção (RF-44).** O callback passa a ser registrado **por pane**, com duas guardas:

| Guarda                      | Por quê                                                                                        |
| --------------------------- | ---------------------------------------------------------------------------------------------- |
| A pane ainda está na árvore | Fechar deliberadamente destaca a pane antes de matar o processo — não é "encerrada", é fechada |
| É a última pane da aba      | Numa aba dividida, uma sessão que acaba não encerra a aba; a vizinha segue                     |

A segunda guarda responde **Q-26**, que a v1.7 deixou em aberto justamente por não saber o que
"(encerrado)" deveria significar numa aba dividida. O uso real respondeu.

**O segundo defeito, que só o teste encontrou.** A primeira versão da guarda usava
`paneOf(...) == null` para detectar o fechamento deliberado — e **não funcionava**. `close()`
desanexa o _splitter_ da árvore, mas a pane fechada continua sendo filha dele; como `paneOf`
devolvia assim que encontrasse um `Splitter` acima, ela ainda parecia estar na aba. O conserto
teria sido publicado sem efeito nenhum.

A correção foi em `paneOf`, e não na guarda: ele passou a exigir
`SwingUtilities.isDescendingFrom(component, root)` antes de subir a árvore. Assim **todos** os
chamadores ficam protegidos, e não só o caso que o defeito relatou.

### DEF-05 — a aba inteira morria ao fechar a primeira pane _(v1.8.2)_

Sintoma relatado com quatro sessões (`aaaaa` a `ddddd`): fechar uma delas deixava as outras três
no ar, mas **nenhuma ação do cabeçalho funcionava mais naquela aba** — nem dividir, nem "Nova
sessão". Clicar em outra aba devolvia tudo ao normal.

**Causa.** `addSession` define o alvo de foco da aba na criação:

```kotlin
content.preferredFocusableComponent = pane.widget.component
```

Fechando **essa** pane — a primeira, que é a que costuma estar em foco —, a aba passa a apontar
para um componente descartado e fora da árvore. A tool window tenta focá-lo, o foco não vai a
lugar nenhum, e o `DataContext` da toolbar fica sem projeto: todas as ações do cabeçalho param.
Trocar de aba consertava porque o foco caía num componente válido.

**Correção (RF-42, ampliado):** ao fechar uma pane, a aba passa a apontar para a sobrevivente —
`SESSION_WIDGET`, **`preferredFocusableComponent`** e o foco efetivo, os três. A v1.8.1 já
trocava a chave e pedia foco, mas deixava o `preferredFocusableComponent` para trás, que era
justamente o que quebrava.

> ⚠️ **Isto revoga o diagnóstico de DEF-04.** A v1.8.1 atribuiu o "menu vazio" a
> `ActionUpdateThread`/`update()` ausentes, por comparação com o `AudioMenuAction`. A hipótese
> era plausível e **estava errada**: o menu não aparecia porque a toolbar inteira estava sem
> contexto, pelo motivo acima. As mudanças de thread foram mantidas — ler a árvore Swing fora da
> EDT era incorreto de qualquer forma —, mas não eram a correção. Ver o Achado 30.

### DEF-06 — "Fechar divisão" foi lido como "fechar as divisões" _(v1.8.2)_

O mesmo relato mostrou o item sendo acionado com a expectativa de encerrar a aba inteira, e não
uma pane. O nome permitia as duas leituras: "divisão" tanto é _a pane_ quanto _o arranjo_.

**É o mesmo erro que o DEF-04 diagnosticou na plataforma** — "Close Tab" fechando uma pane —,
repetido em rótulo escrito por este projeto uma rodada depois de a lição ter sido registrada.

**Correção (RF-46):** os dois nomes passam a dizer **quantas sessões morrem**:

| Antes            | Agora                         | O que faz                             |
| ---------------- | ----------------------------- | ------------------------------------- |
| "Fechar divisão" | **"Fechar esta sessão"**      | fecha a pane em foco, a vizinha ocupa |
| _(não existia)_  | **"Fechar todas as sessões"** | fecha a aba com todas as divisões     |

O `X` da aba já fazia o segundo, mas quem está no menu de divisões procura ali — e foi a
ausência do item que abriu espaço para a leitura errada.

### DEF-07 — "Tocar seleção" nunca tocou nada _(v1.9)_

O item existe no menu "Áudio" desde a v1.5 (`AudioMenuAction` o adiciona no `init`). Ele nunca
funcionou, e nunca reclamou: `actionPerformed` lia

```kotlin
val widget = e.getData(PlatformDataKeys.CONTEXT_COMPONENT) as? JBTerminalWidget ?: return
```

Numa ação de **título de tool window**, o componente de contexto é a barra de ferramentas do
cabeçalho — não o terminal. O cast dá `null`, o `?: return` engole, e o clique é um no-op mudo.

**O que o torna um defeito de arquitetura, e não um descuido:** é o **único** ponto do cabeçalho
que não passa pelo `ClaudeDockSessions`. Copiar, exportar, dividir, fechar — todos chamam um
método do serviço, que resolve a sessão em foco por `selectedWidget()`. Este quis atalhar pelo
`DataContext` e pegou o objeto errado. O caminho certo já estava provado no `ClaudeSelectionCopyButton`,
que lê a seleção com `JBTerminalWidget.asJediTermWidget(widget)?.selectedText` — mas ali o widget
vem instalado por baixo, não adivinhado pelo contexto.

**Por que passou despercebido tanto tempo:** as duas camadas de `update()` desabilitam o item
quando o Piper não está configurado, então "não acontece nada" tinha uma explicação pronta e
plausível para quem ainda não tinha terminado de configurar o modelo. Um no-op silencioso é
indistinguível de uma pré-condição não atendida — e é exatamente por isso que RF-48 exige aviso
nos dois casos negativos.

**Correção (RF-48):** `ClaudeDockSessions.playSelectedSession()`, junto de `copySelectedSession()`,
reusando `selectedWidget()`, o `notify(...)` do serviço e o `ClaudeSessionText.normalize`. A ação
volta a ser uma linha, como as irmãs.

### Achado 32 — a integração falha por porta `0`, e a mitigação da corrida nos exclui _(v1.9.5)_

**Pergunta de origem (Q-31):** por que uma pane às vezes não conecta ao MCP — sem `In <arquivo>` no
rodapé e sem receber o `Ctrl+Alt+K` — enquanto a irmã conecta?

**A cadeia, lida no bytecode do plugin oficial 0.1.14-beta:**

1. **`TerminalCustomizer.customizeCommandAndEnvironment`** injeta
   `CLAUDE_CODE_SSE_PORT = TerminalUtil.getRunningMcpServerPorts().getOrDefault(project.locationHash, 0)`.
   **O `getOrDefault(…, 0)` é o ponto.** Sessão criada antes de a porta ser registrada não fica sem
   a variável — fica com **`0`**. E ambiente de processo é fixado no `exec`: aquela sessão nunca
   mais conecta, por mais que o servidor suba depois.
2. **Quem popula o mapa é `MCPService.start()`**, que registra
   `runningMcpServerPorts[locationHash] = port` e só então sobe o ktor.
3. **`start()` é chamado do `PostStartupActivity`** — ou seja, existe uma janela real entre o
   projeto abrir e a porta existir.
4. **O oficial sabe da corrida e a mitiga:** logo depois de `start()`, o mesmo
   `PostStartupActivity` chama `TerminalUtil.restartClaudeInExistingTerminals(project)`, que
   reinicia o CLI nas sessões já abertas para que peguem a porta certa.

**E é aqui que nos perdemos.** `restartClaudeInExistingTerminals` faz
`ToolWindowManager.getToolWindow("Terminal")`, varre o `ContentManager` **dela** e só age em abas
cujo título começa com `"Claude Code"`. **As nossas panes falham nos dois critérios.** A corrida é
do oficial, a mitigação existe, e ela nos exclui estruturalmente — **terceira vez que o literal
`"Terminal"` decide o nosso comportamento**, depois de RF-17 (o `Esc`) e de DEF-08.

**O que isto explica de uma vez:** por que a primeira pane costuma ser a desintegrada (nasce com o
projeto, antes do `PostStartupActivity`) e a criada por split costuma conectar (nasce depois); e
por que em T-3.36 as duas conectaram — a aba foi aberta com o IDE já de pé.

**Não é defeito do nosso código.** O caminho de split usa exatamente o mesmo `createPane` →
`ClaudeTerminalSessionFactory.createSession` da primeira pane; não há divergência de ambiente entre
elas. O que difere é **o instante** em que cada uma nasce.

**Estado: mecanismo real no plugin oficial, mas NÃO observado nos afetando. Hipótese arquivada
após duas não-reproduções.**

T-3.61 rodou em 2026-08-08 e deu o **oposto da condição necessária**: as duas panes ficaram
integradas e `echo $CLAUDE_CODE_SSE_PORT` devolveu a **mesma porta real (`33471`)** nas duas.

**O que isso prova, e é ganho real:** a porta é **por projeto** (`locationHash`), como o bytecode
dizia, e **o customizer alcança as panes de split em produção** — T-4 tinha provado isso em
laboratório, agora está visto na sessão real, nas duas panes. É o braço de controle do teste, e
ele passou.

**O que isso NÃO prova:** nada sobre a causa da desintegração, porque **a pane desintegrada não
apareceu**. A hipótese da porta `0` segue de pé e sem evidência — não confirmada e não refutada.

**Segunda tentativa, com a condição específica — também não reproduziu.** A IDE foi reiniciada
com a tool window restaurada, de modo que a primeira pane nasceu durante a inicialização, e o log
confirma a sessão subindo sozinha na partida. Mesmo assim: as duas panes integradas, **mesma porta
real**.

**E há uma razão provável para nunca reproduzirmos, que estava debaixo do nariz.** Nossas sessões
usam `deferSessionStartUntilUiShown = true` (D-23/RNF-02): **o processo não nasce quando a aba é
criada, e sim quando o componente é exibido** — e é no nascimento do processo que
`configureStartupOptions` roda o customizer e congela o `CLAUDE_CODE_SSE_PORT`. Isso não é
suposição: o spike de T-2 mediu justamente isso, em headless, onde o `ttyConnector` fica `null`
para sempre porque a UI nunca aparece.

Ou seja, **a flag que existe por motivo estético — esconder o eco da partida — provavelmente nos
tira da corrida de graça**, adiando a leitura da porta para depois de a janela estar montada.
Adiar não elimina a corrida em teoria; só a estreita tanto que duas tentativas dirigidas não a
pegaram.

**Decisão: arquivar.** O mecanismo do `getOrDefault(…, 0)` e a mitigação presa a `"Terminal"` ficam
documentados porque são fatos do plugin oficial e podem explicar sintomas futuros. Mas **a causa do
caso visto em T-3.60 permanece desconhecida**, e não se escreve código sobre hipótese que duas
medições dirigidas não sustentaram. **Se o sintoma voltar, o diagnóstico já está pronto** — pane
sem `In <arquivo>`, `echo $CLAUDE_CODE_SSE_PORT`, comparar com uma pane sadia.

**Contorno que já existe no produto:** "Nova sessão" cria uma sessão nova, que pega a porta certa.
Fechar a pane desintegrada e abrir outra resolve o caso concreto — sem código novo.

### Colar print screen: o CLI já faz isso, e o que falta é um pacote do sistema _(v1.10)_

**Pedido de origem.** Colar um print screen direto na janela do plugin, como no VS Code, em vez de
abrir a pasta de capturas, copiar o arquivo e colar o caminho.

**A primeira pergunta decide o resto: o CLI sabe ler imagem da área de transferência?** **Sabe.**
Lido no binário `claude` 2.1.226 instalado neste ambiente, o bloco por sistema operacional é
literal:

```js
linux: {
  checkImage: `xclip -selection clipboard -t TARGETS -o 2>/dev/null | grep -E "image/(png|jpeg|jpg|gif|webp|bmp)" || wl-paste -l 2>/dev/null | grep -E "image/(png|jpeg|jpg|gif|webp|bmp)"`,
  saveImage:  `xclip -selection clipboard -t image/png -o > ${i} 2>/dev/null || wl-paste --type image/png > ${i} 2>/dev/null || xclip -selection clipboard -t image/bmp -o > ${i} 2>/dev/null || wl-paste --type image/bmp > ${i}`,
}
```

E o chamador trata a falha do `checkImage` como "não há imagem":

```js
if ((await Tni(r.checkImage)).exitCode !== 0) return null;
```

Há ainda `~/.claude/image-cache/<sessionId>/N.png` no disco desta máquina, com PNGs reais — o
destino já existe e já foi usado.

**No Linux o caminho inteiro depende de `xclip` ou `wl-paste` estarem no `PATH`.** Neste ambiente
**nenhum dos dois está instalado** — `xclip`, `xsel`, `wl-paste` e `wl-copy`, todos ausentes — e a
sessão é **Wayland** (`XDG_SESSION_TYPE=wayland`). Os dois comandos falham, o `exitCode` nunca é
`0`, e o `return null` transforma tudo num **no-op mudo**: o mesmo formato de falha do DEF-07, e
pela mesma razão — um caminho negativo sem aviso é indistinguível de um recurso que não existe.

**Isto explica o sintoma inteiro, inclusive a parte que não é nossa.** O relato diz que falha
**também no terminal comum**, fora do IDE, onde nenhuma linha do plugin roda. Uma causa que
alcança os dois casos é do ambiente, não do plugin — e o pacote ausente é exatamente essa causa.
Funcionar no VS Code do mesmo usuário não contradiz nada: a extensão de lá não passa pela área de
transferência do sistema para entregar a imagem ao modelo.

**Conserto de ambiente, sem uma linha de código nosso:**

```bash
sudo apt install wl-clipboard   # sessão Wayland; `xclip` também serve, via XWayland
```

**O que sobra para o plugin, e ainda não está medido.** No terminal do IDE, `Ctrl+V` é ação da
plataforma. `JBTerminalPanel.handleKeyEvent` roda, nesta ordem, os `preKeyEventConsumers`, o
`TerminalEscapeKeyListener` e — só se o evento não tiver sido consumido — o
`TerminalPanel.handleKeyEvent` do JediTerm, que trata `PASTE` como colagem **de texto**
(`handlePaste` → `pasteFromClipboard`). Se a tecla for consumida antes de virar bytes no PTY, o CLI
nunca fica sabendo que houve um `Ctrl+V`, e aí o pacote instalado não basta. **Isso é pergunta, não
fato** (Q-32); T-3.62 existe para respondê-la **depois** de o pacote estar instalado.

**Por que não escrever o código antes de medir.** O plugin já tem o gancho que resolveria — o
`addPreKeyEventHandler` do `ClaudeEscapeForwarder` — e um plano B melhor que o do próprio CLI:
`Toolkit.getDefaultToolkit().getSystemClipboard()` lê `DataFlavor.imageFlavor` **sem depender de
`xclip`**, e gravar um PNG temporário e colar o caminho é literalmente o que o usuário já faz à
mão hoje, com sucesso. Mas metade do problema é ambiental e está comprovada; a outra metade é
hipótese. Especificar RF sobre a metade não medida repetiria o Achado 30 — publicar hipótese
plausível como se fosse conserto.

### `/usage` é tela de TUI, e não saída de texto _(verificado em v1.10)_

**Pedido de origem.** Um botão no cabeçalho que abra, em popup, o resumo de uso hoje obtido com
`/usage`.

**O que foi verificado, e não suposto:**

1. **Não existe subcomando.** `claude --help` lista `agents`, `auth`, `auto-mode`, `doctor`,
   `gateway`, `import`, `install`, `mcp`, `plugin`, `project`, `setup-token`, `ultrareview` e
   `update`. **`usage` não está lá**, e `claude usage --help` cai no help geral.
2. **`/usage` é comando de sessão interativa.** Este próprio documento já sabia disso desde a v1.1:
   o `ClaudeEscapeForwarder` existe, entre outras coisas, para **sair** de telas como o `/usage`
   com `Ctrl+Backspace` onde o `Esc` é capturado pelo IDE (RF-17).
3. **O dado não está em cache local utilizável.** `~/.claude/stats-cache.json` existe, mas guarda
   **atividade** (`dailyActivity`, `modelUsage`, `totalSessions`) — não os limites de plano que a
   tela de `/usage` mostra.
4. **O dado vem da rede, autenticado.** O binário traz o endpoint `/api/oauth/usage`, e o token
   está em `~/.claude/.credentials.json` (modo `600`).

**A consequência é direta:** um popup com o resumo exigiria o plugin **ler o token do usuário** e
falar com um endpoint não documentado. Isso colide com RNF-04 e com a regra de segredos do
`CLAUDE.md`, e quebraria em qualquer mudança de formato do CLI. **O popup é recusado; o botão
não** — ver Achado 35 e RF-51.

### DEF-08 — `Ctrl+Alt+K` foca o Terminal nativo, e sempre foi assim _(v1.9.3)_

**Sintoma.** Em T-3.3, `Ctrl+Alt+K` com um trecho selecionado no editor abriu uma sessão na tool
window "Terminal" nativa, em vez de usar a sessão da janela dedicada.

**Causa, lida no bytecode de `SendToClaudeAction` e `TerminalUtil` (0.1.14-beta).** A ação faz
**duas coisas independentes**, e só uma delas nos alcança:

1. **O trecho vai por MCP**, não pelo terminal: `MCPServiceKt.sendAtMentionedNotifications(mcpService, editor)`.
   Esse caminho é de broadcast e **não conhece tool window nenhuma** — ele chega a qualquer CLI
   conectado, inclusive o das nossas panes.
2. **O foco vai para o Terminal nativo**, sempre. Nas duas saídas possíveis:
   - `mcpServerInfos` vazio → `openClaudeInTerminal`, que chama
     `TerminalToolWindowManager.createShellWidget(basePath, "Claude Code", …)` e ativa
     `TerminalToolWindowManager.getToolWindow()` — **cria sessão nova na nativa**;
   - `mcpServerInfos` não vazio → `focusClaudeInTerminal`, que faz
     `ToolWindowManager.getToolWindow("Terminal")` e varre o `ContentManager` **dela** com
     `findClaudeTerminal(toolWindow, runningProcessIds)`.

**Consequência: as nossas sessões são estruturalmente invisíveis para esse caminho.** Elas não
estão no `ContentManager` da tool window "Terminal", então `findClaudeTerminal` nunca as acha —
não por bug, mas por definição. É **o mesmo literal `"Terminal"`** de `TerminalUtil` que originou
este projeto, já registrado nos Fatos verificados desde 2026-08-01, agora aparecendo do outro lado.

**Não é regressão, e não é conserto nosso.** É consequência direta de **D-01** (casca fina): o
plugin oficial permanece dono das ações dele. **T-3.3 foi escrito sobre uma premissa falsa** — ele
pedia que uma ação de terceiro, presa por literal à janela do terceiro, fosse parar na nossa.
Nenhuma implementação nossa faria esse roteiro passar sem violar D-01 ou colidir com o id nativo
(alternativa já recusada em v1.1).

**Medido em T-3.59 (2026-08-08): o trecho chega.** `@test.md#L3` apareceu na nossa pane, com
`1 line selected`. **DEF-08 é, portanto, defeito de ergonomia** — o conteúdo atravessa por MCP; só
o foco vai para a janela errada. A leitura do bytecode acertou, e agora está medida.

**E chegou nas duas panes ao mesmo tempo**, o que não é um segundo defeito: é a mesma frase do
item (1) vista de frente. Broadcast não tem destinatário. **Isso responde Q-02**, aberta desde a
v1.0 esperando exatamente uso real: ninguém "possui" o envio, porque a noção de dono não existe
nessa camada.

> **Refinamento de 2026-08-08 (T-3.60), e o que quase virou conclusão errada.** Numa execução
> seguinte o `Ctrl+Alt+K` entregou **só à segunda pane**, o que parecia contradizer o broadcast e
> sugerir um alvo fixo. Não contradiz. `MCPService._mcpServerInfos` é
> `Map<Server, McpServerInfo>` — **uma entrada por conexão**, sem colisão de chave — e
> `sendAtMentionedNotifications` itera **todas**. O que muda entre as duas execuções é quantas
> sessões estão **conectadas**: nos prints, a pane que recebeu mostra o indicador de integração
> (`In <arquivo>` / `N line selected`) e a que não recebeu não mostra nada.
>
> **A regra correta, então, é "broadcast para toda sessão conectada"** — e não "para todas as
> panes". O indicador de integração no rodapé do CLI é o diagnóstico: pane sem ele está fora do
> MCP e não recebe. **Por que uma pane às vezes não conecta é pergunta nova (Q-31)**, e não foi
> medida. Registrar isto importa porque a leitura ingênua — "sempre vai para a segunda" — teria
> virado um alvo fixo inexistente, no mesmo formato do Achado 30.

**Consertado em v1.9.4 por RF-49**, e não no lugar do oficial: a ação dele continua fazendo o que
faz. O que passa a existir é uma entrega **com destinatário** — `@arquivo#Lx-y` escrito no PTY da
pane em foco pelo `sendInput` de D-16, sem tocar no protocolo privado e sem violar D-01 (D-41).
O `Ctrl+Alt+K` segue levando o foco para a janela nativa; quem não quiser isso usa a ação nova.

### Achado 31 — o SPEC afirmava um timeout que nunca existiu _(v1.9)_

Ao abrir `ClaudePiperPlayback` para acrescentar `--length-scale`, a documentação não bateu com o
código. O SPEC dizia "Processo é destruído se timeout ou erro" e o KDoc do método dizia
"Timeout 20s". O código chamava `process.waitFor()`, **sem argumento** — espera indefinida. E o
`stopCurrent()` chama `currentProcess?.destroy()` sobre um campo que nunca recebe atribuição:
código morto guardando uma promessa que ninguém cumpriu.

Não houve regressão. O timeout **nunca** foi implementado, e o SPEC descreveu a intenção como se
fosse o estado. Nada quebrou porque o piper local termina rápido e o único chamador está fora da
EDT — o custo ficou latente, não ausente: um modelo corrompido ou um `.onnx` gigante travaria a
pooled thread para sempre.

**O que isto ensina sobre o formato:** uma seção de Design que descreve o que a classe _fará_ e
uma implementação que entrega parte disso divergem em silêncio, porque nenhum teste olha para a
prosa. Os requisitos têm ID e são rastreados; as frases de Design não. A defesa barata é o que
esta versão fez em outro ponto: quando a regra couber numa função pura, extrair e testar
(`piperParameters`) — aí a prosa passa a ter uma asserção atrás.

**Estado:** ✅ **quitado em v1.9.1.** `synthesize` recebeu o prazo de 20 s e a atribuição de
`currentProcess`. A leitura do stdout foi para outra thread no mesmo movimento — sem isso o prazo
seria decorativo, porque `readBytes()` bloqueia até o EOF.

**O que a correção revelou, e o Achado 31 não tinha registrado:** o campo nunca atribuído não
custava só o timeout. `stopCurrent()` era o caminho de **RNF-23** — "novo play interrompe o
anterior" —, e com `currentProcess` sempre `null` ele parava o `Clip` e deixava o piper anterior
sintetizando até o fim. O requisito estava escrito, testado à mão em T-3.27, e inerte no código.
Duas promessas no mesmo campo morto, e o achado só tinha visto uma.

**A defesa que ficou:** T-1.55 e T-1.56 afirmam o **efeito** com um piper falso que dorme 30 s.
Sob mutação — desfazendo cada uma das duas correções — os dois testes reprovam; é a checagem que
o Achado 20 pediu e que teria pego este campo quatro versões antes.

### Achado 30 — uma hipótese plausível publicada como conserto _(v1.8.2)_

A v1.8.1 não conseguiu reproduzir DEF-04 (menu vazio) e mesmo assim mexeu no código: atribuiu a
causa a `ActionUpdateThread`/`update()` ausentes, por comparação com o `AudioMenuAction`, que
funciona. O raciocínio era razoável e **a conclusão estava errada** — o menu não aparecia porque
a toolbar inteira estava sem contexto, por causa do `preferredFocusableComponent` órfão de
DEF-05, que só o exemplo com quatro sessões deixou visível.

**O que salvou o registro:** o SPEC e o HANDOFF disseram, na hora, que era hipótese pendente de
confirmação (T-3.48), e não conserto. Foi por isso que a correção real pôde ser encontrada em
vez de o defeito ser dado por fechado.

**O que teria evitado o erro:** o sintoma relatado — "só funciona depois de clicar em outra aba"
— aponta para **contexto/foco**, não para cálculo de menu. Um menu mal atualizado não impede
"Nova sessão" de funcionar; um `DataContext` sem projeto impede tudo. A informação que
distinguia as duas hipóteses estava no relato original, e eu segui a pista errada por ela ser a
que eu sabia comparar (havia um menu vizinho funcionando).

**Lição registrada:** quando um defeito não é reproduzível, escolher a hipótese que explica
**todos** os sintomas, não a que é mais fácil de testar. "Nem criar aba nova eu consigo" já
excluía a explicação de menu na primeira leitura — e só apareceu no segundo relato porque o
primeiro não foi lido com essa pergunta em mente.

### DEF-02 — herdar um item de menu é herdar as precondições dele _(v1.7.2)_

O menu de contexto que o listener acendeu (D-33) traz também "Select Previous Tab" e "Select Next
Tab". Ligá-los ao `ContentManager` parecia trivial — e com **uma aba só** produz um stack trace
inteiro no log do IDE:

```
java.lang.Throwable: Assertion failed
  at com.intellij.ui.content.impl.ContentManagerImpl.selectPreviousContent(ContentManagerImpl.java:616)
  at dev.reginaldomorais.claudedock.ClaudeDockSessions.selectSiblingTab(...)
```

**Causa, lida no bytecode.** Os dois métodos começam pela mesma linha:

```java
int count = getContentCount();
LOG.assertTrue(count > 1);      // <- antes de qualquer outra coisa
```

Ou seja, navegar sem ter para onde ir **não é operação inócua**: é assertion. E o menu mostra os
itens sempre — `getActions()` os monta a partir de presentations, sem consultar o listener —,
então quem precisa segurar a chamada é o plugin.

**Correção:** uma guarda no ponto por onde as duas direções passam (`selectSiblingTab`), com o
limite em objeto puro (`ClaudeTabNavigation`) para ficar testável sem subir o IDE. Guardar em
cada direção seria duplicar a mesma regra em dois lugares.

**O que isto ensina sobre a rodada anterior.** O Achado 28 comemorou "o listener acende quatro
itens de graça", e o Achado 29 já havia descontado o vocabulário herdado. Este desconta a
terceira parcela: **as precondições**. Um item de menu herdado traz comportamento, nome **e**
contrato — e o contrato não estava escrito em lugar nenhum, só no `assertTrue` da implementação.

### Um só engine importa: as nossas sessões são sempre CLASSIC _(corrigido em v1.7)_

Várias hipóteses antigas — CB-26, CB-36, CB-47 e R-15 — hedgeiam contra "e se a sessão não for
JediTerm?". A pergunta foi levada a sério porque o `TerminalEngine` do IDE tem `CLASSIC`,
`REWORKED` e `NEW_TERMINAL`, e o `HANDOFF.md` chegou a registrar "REWORKED no IntelliJ".

**Essa premissa estava errada, e agora está verificada.** `AbstractTerminalRunner.startShellTerminalWidget`
faz, em sequência (`javap -c`):

```
createTerminalWidget(Disposable, String, boolean) → com.intellij.terminal.JBTerminalWidget
JBTerminalWidget.asNewWidget()                    → com.intellij.terminal.ui.TerminalWidget
```

O tipo de retorno de `createTerminalWidget` **é** `JBTerminalWidget`, o widget JediTerm — nenhuma
subclasse pode devolver outra coisa. Quem escolhe entre engines é o `TerminalToolWindowManager`
da tool window nativa, **acima** do ponto de entrada que usamos.

**Consequência:** toda sessão criada por este plugin é JediTerm clássico, em qualquer IDE da
família e independentemente do que o usuário configurou em Settings → Tools → Terminal. O
"REWORKED no IntelliJ" do HANDOFF descrevia o Terminal nativo do usuário, não as nossas abas.

Isso **não** é motivo para apagar as degradações graciosas: elas custam uma linha (`?: return`) e
protegem contra a plataforma mudar esse retorno num upgrade. Mas a redação que sugeria "o recurso
pode não estar disponível para você hoje" era falsa, e a probabilidade de R-15 cai de "Média"
para "Baixa". Ver o Achado 27.

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

| #             | Requisito                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| ------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **RF-01**     | O plugin DEVE registrar uma tool window dedicada, de título "Claude Code Dock", distinta da tool window "Terminal".                                                                                                                                                                                                                                                                                                                                                                                               |
| **RF-02**     | A tool window DEVE ancorar por padrão à direita (`ToolWindowAnchor.RIGHT`) e ser reposicionável pelo usuário através dos mecanismos padrão do IDE.                                                                                                                                                                                                                                                                                                                                                                |
| **RF-03**     | Ao ser aberta pela primeira vez, a tool window DEVE criar uma sessão de terminal cujo diretório de trabalho é a raiz do projeto (`project.basePath`).                                                                                                                                                                                                                                                                                                                                                             |
| **RF-04**     | Criada a sessão, o plugin DEVE executar automaticamente o comando `claude` configurado.                                                                                                                                                                                                                                                                                                                                                                                                                           |
| **RF-05**     | A criação da sessão DEVE ser **preguiçosa**: nenhum processo é iniciado enquanto a tool window não for aberta pelo usuário.                                                                                                                                                                                                                                                                                                                                                                                       |
| **RF-06**     | A tool window DEVE suportar múltiplas abas, cada uma com sessão independente.                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| **RF-07**     | O plugin DEVE oferecer a ação "Nova sessão", que abre uma nova aba executando `claude`.                                                                                                                                                                                                                                                                                                                                                                                                                           |
| **RF-08**     | O plugin DEVE oferecer a ação "Retomar sessão", que abre uma nova aba executando `claude --resume`.                                                                                                                                                                                                                                                                                                                                                                                                               |
| **RF-09**     | O plugin DEVE oferecer uma ação global "Abrir Claude Code Dock", acessível por _Find Action_ e associável a atalho pelo usuário.                                                                                                                                                                                                                                                                                                                                                                                  |
| **RF-10**     | O plugin DEVE expor uma tela de configurações com o caminho do executável `claude` (valor padrão: `claude`).                                                                                                                                                                                                                                                                                                                                                                                                      |
| **RF-11**     | Encerrado o processo `claude` de uma aba, a aba DEVE exibir indicação visual de sessão terminada, sem fechar-se automaticamente.                                                                                                                                                                                                                                                                                                                                                                                  |
| **RF-12**     | Fechada uma aba, o processo correspondente DEVE ser encerrado e todos os recursos liberados.                                                                                                                                                                                                                                                                                                                                                                                                                      |
| **RF-13**     | O plugin NÃO DEVE alterar, remover ou interferir no comportamento do plugin oficial nem da tool window "Terminal".                                                                                                                                                                                                                                                                                                                                                                                                |
| **RF-14**     | Se o executável `claude` não for encontrado, o plugin DEVE exibir notificação acionável com link para as configurações.                                                                                                                                                                                                                                                                                                                                                                                           |
| **RF-15**     | Se o plugin de terminal (`org.jetbrains.plugins.terminal`) estiver ausente, o plugin DEVE degradar sem erro: a tool window não é registrada.                                                                                                                                                                                                                                                                                                                                                                      |
| **RF-16**     | O título de cada aba DEVE identificar a sessão de forma distinguível (ex.: `Claude`, `Claude (2)`).                                                                                                                                                                                                                                                                                                                                                                                                               |
| **RF-17**     | _(v1.1)_ A tecla `Esc` DEVE ser entregue ao processo da sessão, e não consumida pelo IDE para mover o foco ao editor.                                                                                                                                                                                                                                                                                                                                                                                             |
| **RF-18**     | _(v1.1)_ Com a tool window sem nenhuma aba, o estado vazio DEVE oferecer caminho visível para abrir uma nova sessão ou retomar uma anterior.                                                                                                                                                                                                                                                                                                                                                                      |
| **RF-19**     | _(v1.1)_ O plugin DEVE permitir configurar `CLAUDE_CONFIG_DIR` **por projeto** (= por janela do IDE), injetando-o no ambiente das sessões daquele projeto.                                                                                                                                                                                                                                                                                                                                                        |
| **RF-20**     | _(v1.1)_ `CLAUDE_CONFIG_DIR` vazio DEVE significar "não injetar", preservando o padrão do CLI (`~/.claude`).                                                                                                                                                                                                                                                                                                                                                                                                      |
| ~~**RF-21**~~ | ~~_(v1.2)_ O plugin DEVE oferecer a ação "Copiar Sessão", que coloca o conteúdo do buffer da aba selecionada (scrollback + tela) na área de transferência.~~ **Substituído por RF-24 em v1.3** — o buffer contém a conversa repetida (DEF-01).                                                                                                                                                                                                                                                                    |
| **RF-22**     | _(v1.2)_ O plugin DEVE oferecer a ação "Exportar Conversa", que aciona o slash command `/export` do CLI na aba selecionada.                                                                                                                                                                                                                                                                                                                                                                                       |
| **RF-23**     | _(v1.2)_ Sem aba selecionada, ou sem conteúdo a copiar, as ações de RF-21 e RF-22 DEVEM notificar o usuário — nunca lançar exceção nem agir em silêncio.                                                                                                                                                                                                                                                                                                                                                          |
| **RF-24**     | _(v1.3)_ "Copiar Conversa" DEVE obter o texto pelo `/export` do CLI, para arquivo temporário, e colocar **só a conversa** na área de transferência — sem banner repetido, sem caixa de input e sem barra de status (DEF-01).                                                                                                                                                                                                                                                                                      |
| **RF-25**     | _(v1.3)_ O arquivo temporário de RF-24 DEVE ser criado com permissão exclusiva do usuário e **apagado logo após a leitura**, com ou sem sucesso.                                                                                                                                                                                                                                                                                                                                                                  |
| **RF-26**     | _(v1.3)_ Ao selecionar texto com o mouse na sessão, o plugin DEVE oferecer um botão flutuante que copia **apenas o trecho selecionado**; ele DEVE sumir quando a seleção é desfeita.                                                                                                                                                                                                                                                                                                                              |
| ~~**RF-27**~~ | ~~_(v1.3)_ O plugin DEVE oferecer opção de aplicar `--ax-screen-reader` às novas sessões.~~ **REMOVIDO em v1.4** — ver [Fora de Escopo](#fora-de-escopo) e o Achado 18.                                                                                                                                                                                                                                                                                                                                           |
| **RF-28**     | _(v1.4)_ A sessão DEVE ser afastada das bordas da tool window por um respiro configurável, pintado com o fundo do terminal, e o valor DEVE valer para as sessões abertas a partir da mudança.                                                                                                                                                                                                                                                                                                                     |
| **RF-29**     | _(v1.4)_ Ao abrir uma aba, o plugin DEVE cobrir a sessão enquanto o CLI sobe, escondendo o prompt do shell e o eco do comando; a capa DEVE sair sozinha e NÃO DEVE impedir a sessão de receber o tamanho real da aba.                                                                                                                                                                                                                                                                                             |
| **RF-31**     | _(v1.5)_ Durante a reprodução de áudio, o plugin DEVE oferecer, **no cabeçalho da tool window**, um menu suspenso "Áudio" contendo ações para pausar/retomar e parar a fala em curso.                                                                                                                                                                                                                                                                                                                             |
| **RF-32**     | _(v1.5)_ O plugin DEVE permitir configurar, em Settings > Tools > Claude Code Dock, o caminho do executável `piper` (valor padrão: `piper`) e o caminho para um arquivo `.onnx` de modelo de voz (padrão: vazio; desabilita síntese até configurado).                                                                                                                                                                                                                                                             |
| **RF-33**     | _(v1.6)_ O popup flutuante de seleção (RF-26) DEVE oferecer, **ao lado** do botão de copiar, um botão que grava em arquivo **apenas o trecho selecionado**, com o destino escolhido pelo usuário no diálogo nativo de salvar do IDE.                                                                                                                                                                                                                                                                              |
| **RF-34**     | _(v1.6)_ O diálogo de RF-33 DEVE abrir na raiz do projeto, com nome sugerido distinguível e extensão `.md`. Cancelar o diálogo NÃO DEVE gravar nada nem notificar; falha de gravação DEVE virar notificação, nunca exceção.                                                                                                                                                                                                                                                                                       |
| **RF-35**     | _(v1.6)_ A ação "Exportar Conversa" do cabeçalho (RF-22) DEVE permanecer inalterada: o trecho e a conversa inteira são destinos distintos, e nenhum substitui o outro.                                                                                                                                                                                                                                                                                                                                            |
| **RF-36**     | _(v1.7)_ O plugin DEVE permitir dividir uma aba em duas ou mais sessões visíveis ao mesmo tempo, **à direita** (lado a lado) ou **abaixo** (empilhadas), acionável tanto pelo menu de contexto da sessão quanto por um menu "Dividir" no cabeçalho.                                                                                                                                                                                                                                                               |
| **RF-37**     | _(v1.7)_ Cada pane DEVE ser uma sessão independente, com processo, `CLAUDE_CODE_SSE_PORT` e ciclo de vida próprios — dividir é abrir uma sessão nova, não espelhar a existente.                                                                                                                                                                                                                                                                                                                                   |
| **RF-38**     | _(v1.7)_ As ações do cabeçalho que operam sobre "a sessão selecionada" (RF-22, RF-24, RF-31, RF-36) DEVEM agir sobre a **sessão em foco** da aba, e não sobre uma pane arbitrária.                                                                                                                                                                                                                                                                                                                                |
| **RF-39**     | _(v1.7)_ Fechar uma sessão dividida DEVE colapsar a divisão, com a irmã ocupando o espaço das duas, e encerrar **apenas** o processo daquela pane. Sendo a última sessão da aba, fechar a sessão DEVE fechar a aba.                                                                                                                                                                                                                                                                                               |
| **RF-40**     | _(v1.7)_ Fechar a aba DEVE encerrar **todas** as sessões dela, sem deixar PTY órfão, qualquer que seja a profundidade das divisões.                                                                                                                                                                                                                                                                                                                                                                               |
| **RF-41**     | _(v1.7.1)_ O menu "Dividir" do cabeçalho DEVE oferecer "Fechar divisão", que fecha a sessão em foco e devolve o espaço à vizinha, **sem** fechar a aba. Sem divisão, a ação DEVE avisar em vez de fechar a aba.                                                                                                                                                                                                                                                                                                   |
| **RF-42**     | _(v1.7.1, ampliado em v1.8.2)_ Fechada uma pane, a aba DEVE passar a apontar para uma sessão **viva** em **três** lugares: a chave da sessão selecionada, o `preferredFocusableComponent` do `Content` e o foco efetivo. Deixar qualquer um apontando para a pane fechada derruba o cabeçalho inteiro (DEF-05).                                                                                                                                                                                                   |
| **RF-44**     | _(v1.8.1)_ O sufixo "(encerrado)" no título da aba DEVE significar que a aba não tem mais nenhuma sessão viva. Fechar uma divisão deliberadamente NÃO DEVE marcar a aba, e uma sessão que acaba numa aba dividida também não, enquanto restar vizinha (DEF-03).                                                                                                                                                                                                                                                   |
| **RF-46**     | _(v1.8.2)_ O menu "Dividir" DEVE distinguir, **pelo nome**, quantas sessões cada fechamento encerra: "Fechar esta sessão" (a pane em foco) e "Fechar todas as sessões" (a aba inteira, com todas as divisões).                                                                                                                                                                                                                                                                                                    |
| **RF-45**     | _(v1.8.1)_ As ações que exigem divisão ("Trocar de lado", "Girar divisão", "Fechar divisão") DEVEM aparecer **desabilitadas** quando a aba não está dividida, em vez de avisar depois do clique.                                                                                                                                                                                                                                                                                                                  |
| **RF-43**     | _(v1.8)_ O menu "Dividir" DEVE oferecer "Trocar de lado" (inverte a sessão em foco com a vizinha) e "Girar divisão" (alterna lado a lado ↔ empilhado). Sem divisão, as duas DEVEM avisar em vez de agir.                                                                                                                                                                                                                                                                                                          |
| **RF-47**     | _(v1.9)_ O plugin DEVE permitir configurar a **velocidade da fala** do Piper em **dois lugares ligados ao mesmo valor e à mesma lista**: um submenu "Velocidade" dentro do menu "Áudio" e um seletor em Settings > Tools > Claude Code Dock. A lista é 0,25x / 0,5x / 0,75x / 1x / 1,25x / 1,5x / 1,75x / 2x, e os dois pontos DEVEM oferecer exatamente ela — sem campo numérico livre. O padrão, "1x", DEVE preservar o padrão **do próprio modelo**, não impor 1.0. A velocidade vale para a **próxima** fala. |
| **RF-48**     | _(v1.9)_ O menu "Áudio" DEVE tocar o trecho **selecionado** na sessão em foco, obtendo a seleção pelo mesmo caminho das demais ações do cabeçalho. Sem sessão aberta, ou sem seleção, DEVE avisar em vez de não fazer nada em silêncio (DEF-07).                                                                                                                                                                                                                                                                  |
| **RF-49**     | _(v1.9.4)_ O plugin DEVE oferecer ação própria que envie a referência do trecho selecionado no editor (`@arquivo#Lx-y`) para a sessão **em foco** da janela dedicada, e traga a janela à frente. Sem sessão aberta, DEVE avisar. **Não substitui o `Ctrl+Alt+K` do oficial** — resolve o que ele não faz: entregar a uma pane só, e nesta janela (DEF-08, Q-02). NÃO DEVE definir atalho padrão (RF-13). |
| **RF-50**     | _(v1.10)_ O popup flutuante da seleção DEVE oferecer um **terceiro** botão, "Tocar seleção", que sintetiza o trecho já selecionado chamando o **mesmo** `ClaudeTtaSessions.playText` do menu "Áudio" (RF-31) — sem caminho de síntese próprio. **Revoga o teto de dois botões de R-23**, sob critério novo e declarado (Achado 34). |
| **RF-51**     | _(v1.10)_ O cabeçalho DEVE oferecer, **depois** de "Retomar Sessão", uma ação "Uso" que envie `/usage` à sessão **em foco**, pelo mesmo `sendInput` de RF-24 e RF-49. O resultado aparece **na própria sessão**, como tela de TUI — **não** em popup (Achado 35). Sem sessão aberta, ou sem PTY ainda, DEVE avisar. |
| **RF-52**     | _(v1.10)_ **Condicional, não implementável ainda.** Se — e somente se — T-3.62 mostrar que o `Ctrl+V` não chega ao CLI dentro da janela dedicada, o plugin DEVE interceptar `Ctrl+V` pelo `addPreKeyEventHandler`, e, havendo imagem na área de transferência e **nenhum** texto, gravar um PNG temporário e enviar o caminho dele à sessão. Com texto na área de transferência, o comportamento padrão de colagem DEVE seguir intacto. Enquanto T-3.62 não rodar, **não há código a escrever** (Achado 33). |

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

### Requisitos novos em v1.9 — velocidade da fala

- **RNF-31** _(v1.9, novo)_ — Todo número que vire **argumento de linha de comando** DEVE ser
  formatado com `Locale.ROOT`. O `--length-scale` do piper é `type=float` do argparse e recusa
  vírgula decimal; numa JVM pt-BR — a do autor — o formato default produziria `0,667` e a síntese
  morreria com exit code diferente de zero, sem nada audível e sem erro claro. Vale para qualquer
  parâmetro numérico futuro, não só este.

### Requisitos novos em v1.6 — exportação do trecho

- **RNF-24** _(v1.6, novo)_ — O trecho selecionado pode conter código-fonte e segredos do
  projeto, pelo mesmo motivo de RNF-06 e RNF-19. A gravação DEVE ser **sempre iniciada pelo
  usuário**, ir **apenas** para o arquivo que ele escolheu, e o conteúdo NÃO DEVE ser
  registrado em log nem enviado a lugar algum.
  > Diferente de RF-24/RF-25, aqui **não há arquivo temporário**: o destino é o definitivo,
  > escolhido no diálogo. Some com ele a concessão que RNF-19 precisou abrir — não há nada a
  > apagar depois, porque nada foi escrito às escondidas.
- **RNF-25** _(v1.6, novo)_ — A escrita em disco DEVE ocorrer fora da EDT (mesma regra de
  RNF-03), e qualquer falha de E/S DEVE virar notificação pelo `NotificationGroup` próprio
  (RNF-13), nunca exceção propagada para a plataforma.
- **RNF-26** _(v1.6, novo)_ — A montagem do nome sugerido e a normalização do texto DEVEM ficar
  em objeto puro, testável sem subir o IDE — mesma regra que já vale para `ClaudeSessionText`,
  `ClaudeCommand` e `ClaudeTabTitle`.

### Requisitos novos em v1.7 — divisão da aba

- **RNF-27** _(v1.7, novo)_ — O acoplamento com `JBTerminalWidgetListener` DEVE ficar dentro de
  `ClaudeTerminalSessionFactory`, como qualquer outro contato com a API de terminal (RNF-15). A
  camada de cima conversa por uma interface própria, sem importar tipos do terminal.
- **RNF-28** _(v1.7, novo)_ — O ciclo de vida DEVE ser hierárquico: cada pane tem um `Disposable`
  filho do `Disposable` da aba. Fechar uma pane encerra **só** o processo dela; fechar a aba
  encerra todos (RF-39, RF-40).
- **RNF-29** _(v1.7, novo)_ — A árvore de divisões NÃO DEVE ser espelhada em estrutura de dados
  própria: a hierarquia de componentes Swing é a única fonte de verdade. Um mapa paralelo
  precisaria ser limpo em todo caminho de fechamento, e é exatamente aí que vazam referências
  para panes mortas (mesma razão de D-18).
- **RNF-30** _(v1.7, novo)_ — A lógica de divisão DEVE ficar em objeto puro de Swing, sem
  conhecer `Project`, terminal ou tool window, para poder ser testada sem subir o IDE (RNF-26).

### Requisitos novos em v1.10 — play no popup e `/usage` no cabeçalho

- **RNF-32** _(v1.10, novo)_ — O botão de play do popup NÃO DEVE ganhar caminho próprio de
  síntese. DEVE chamar `ClaudeTtaSessions.playText`, para que velocidade (RF-47),
  pausa/retomada (RF-31), exclusão mútua entre falas (RNF-23) e liberação de recursos (RNF-22)
  continuem tendo **um dono só**. Um segundo caminho de síntese seria um segundo estado de áudio.
- **RNF-33** _(v1.10, novo)_ — **Nenhum caminho negativo novo pode ser mudo.** Piper não
  configurado, síntese falhada, seleção vazia e sessão inexistente DEVEM notificar. É a
  generalização da lição do DEF-07, e alcança um ponto que hoje ainda falha em silêncio:
  `playText` apenas loga quando `canSynthesize` devolve `false`.
- **RNF-34** _(v1.10, novo)_ — A ação de `/usage` NÃO DEVE ler `~/.claude/.credentials.json` nem
  chamar `/api/oauth/usage`. O token de acesso é do CLI; o plugin não entra nesse caminho
  (RNF-04, e a regra de segredos do `CLAUDE.md`).

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

### Fluxo alternativo J — pausar e retomar fala _(v1.5, RF-31)_

1. Durante a reprodução, o usuário aciona "Pausar" no menu "Áudio".
2. O `Clip` é pausado via `.stop()`, a posição é guardada em `framePosition`.
3. O ícone muda para "Retomar".
4. Clicado em "Retomar", o `Clip` é repositicionado e `.start()` é chamado.
5. Clicado em "Parar", o `Clip` é fechado e o proceso `piper` é destruído.

### Fluxo de erro G — Piper não encontrado _(v1.5, RF-31/RF-32)_

1. Ao abrir a janela ou durante checagem cacheada, detecta-se que `piper` não está no PATH nem
   nos fallbacks (`~/.local/bin`, `/usr/local/bin`).
2. O menu "Áudio" no cabeçalho fica desabilitado; nenhuma notificação é exibida (é advisório).
3. Um tooltip explica por que o menu está desabilitado ("Piper não encontrado").
4. O usuário pode configurar o caminho explícito em Settings > Tools > Claude Code Dock.

### Fluxo de erro H — modelo não configurado _(v1.5, RF-31/RF-32)_

1. O `piper` está disponível, mas o campo "Caminho do modelo Piper" está vazio ou aponta para
   arquivo inexistente.
2. O menu "Áudio" no cabeçalho fica desabilitado; tooltip: "Configure um modelo de voz".
3. Configuração é feita em Settings > Tools > Claude Code Dock > "Caminho do modelo Piper".

### Fluxo alternativo K — exportar o trecho selecionado _(v1.6, RF-33/RF-34)_

1. O usuário seleciona texto com o mouse na sessão.
2. O popup flutuante aparece com dois botões: copiar (RF-26) e exportar.
3. Clicado em exportar:
   a. o texto selecionado é normalizado por `ClaudeSessionText.normalize` — o mesmo tratamento
   que a cópia recebe, aparando os espaços à direita do render do TUI;
   b. abre-se o diálogo nativo de salvar, com diretório inicial em `project.basePath` e nome
   sugerido `claude-selection-<AAAAMMDD-HHmmss>.md`;
   c. escolhido o destino, o popup se fecha e a gravação acontece fora da EDT (RNF-25).
4. Gravado o arquivo, nada é exibido — sucesso silencioso, como o botão de copiar.
5. O `/export` do CLI **não é acionado em momento algum**: o texto já estava na mão do plugin,
   e o CLI não tem como exportar um trecho (D-30).

### Fluxo alternativo L — dividir a aba _(v1.7, RF-36/RF-37)_

1. O usuário aciona a divisão, por um de dois caminhos equivalentes:
   a. botão direito na sessão → "Split Right" / "Split Down" (nativo do widget, D-33);
   b. menu "Dividir" no cabeçalho → "À direita" / "Abaixo".
2. Descobre-se a aba dona da sessão percorrendo a árvore de componentes — sem mapa (RNF-29).
3. Cria-se um `Disposable` filho do disposable da aba, e nele uma sessão nova, pelo mesmo
   caminho do Fluxo principal (mesmo working dir, mesmo `CLAUDE_CONFIG_DIR`, mesmos customizers).
4. O bloco da sessão de origem é substituído por um splitter contendo os dois blocos.
5. O foco vai para a sessão nova: quem dividiu quer digitar nela.
6. Dividir de novo repete o processo dentro de qualquer pane, aninhando à vontade.

### Fluxo alternativo M — fechar uma sessão dividida _(v1.7, RF-39)_

1. O usuário aciona "Close Session" no menu de contexto da pane.
2. Havendo divisão, o splitter é colapsado e a **irmã** ocupa o espaço das duas.
3. O `Disposable` daquela pane é descartado: só aquele processo morre (RNF-28).
4. Se a pane fechada era a que a aba apontava como "em foco", a marca é limpa — quem receber o
   foco assume (RF-38).
5. Sendo a única sessão da aba, não há divisão a colapsar: a **aba** é fechada, encerrando tudo.

### Fluxo de erro J — falha ao gravar o trecho _(v1.6, RF-34/RNF-25)_

1. O destino escolhido está em diretório sem permissão de escrita, o disco está cheio, ou o
   caminho deixou de existir entre a escolha e a gravação.
2. A `IOException` é capturada, registrada em `Logger.warn` **sem o conteúdo** (RNF-24) e
   convertida em notificação de erro.
3. A sessão não é afetada: nada foi enviado ao PTY, nenhum processo foi tocado.

### Fluxo de erro I — síntese falha _(v1.5, RF-31/RNF-21)_

1. O `piper` é lançado, mas retorna código de erro ou nenhum PCM é produzido.
2. A notificação "Falha ao sintetizar o áudio. Verifique o modelo e o texto." é exibida.
3. Menu "Áudio" volta a desabilitado; nenhuma reprodução inicia.
4. O processo `piper` é destruído e recursos liberados (RNF-22).

### Fluxo alternativo N — tocar o trecho pelo popup da seleção _(v1.10, RF-50)_

1. O usuário seleciona um trecho com o mouse e solta o botão.
2. O popup aparece com **três** botões: copiar, exportar, tocar.
3. O usuário clica em tocar; o popup fecha, como nos outros dois.
4. `ClaudeTtaSessions.playText(trecho)` roda o mesmo caminho do menu "Áudio", fora da EDT.
5. Sai som — e o menu "Áudio" já mostra pausa e parar, porque o estado de reprodução é o mesmo
   objeto, não uma cópia (RNF-32).

### Fluxo alternativo O — consultar o uso _(v1.10, RF-51)_

1. O usuário clica em "Uso", no cabeçalho, logo depois de "Retomar Sessão".
2. `sendInput(widget, "/usage\r")` escreve na sessão em foco — o mesmo mecanismo de RF-24.
3. O CLI abre a tela de uso **dentro da sessão**.
4. O usuário sai dela com `Esc`, ou com `Ctrl+Backspace` onde o `Esc` é capturado pelo IDE
   (RF-17) — que é, aliás, a razão original de o `ClaudeEscapeForwarder` existir.

### Fluxo de erro K — tocar sem Piper configurado _(v1.10, RF-50/RNF-33)_

1. O usuário clica no botão de tocar com o Piper ausente ou sem modelo.
2. `canSynthesize` devolve `false`.
3. O plugin **notifica** que o Piper não está configurado e aponta Settings > Tools > Claude Code
   Dock.
4. Nada toca — e, ao contrário de hoje, nada fica em silêncio. É a correção de forma que o DEF-07
   exigiu, aplicada no ponto por onde **todos** os chamadores passam.

### Fluxo de erro L — `/usage` sem sessão viva _(v1.10, RF-51)_

1. O usuário clica em "Uso" sem nenhuma sessão na janela, ou com a sessão ainda sem PTY.
2. `selectedWidget()` devolve `null`, ou `sendInput` devolve `false`.
3. O plugin notifica — "Nenhuma sessão aberta para consultar o uso." no primeiro caso, "A sessão
   ainda não iniciou; tente de novo em instantes." no segundo, reusando a mensagem de RF-24.

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
├── ClaudeSelectionCopyButton.kt    # (v1.3) popup flutuante na seleção: copiar (RF-26) e exportar (RF-33)
├── ClaudeSelectionExport.kt        # (v1.6) puro: nome sugerido e gravação do trecho (RF-33, RF-34)
├── ClaudeSessionSplitter.kt        # (v1.7) puro (Swing): árvore de panes da aba (RF-36, RF-39)
├── ClaudeSessionPadding.kt         # (v1.4) borda que se pinta com o fundo do terminal (RF-28)
├── ClaudeSessionLoading.kt         # (v1.4) capa sobreposta enquanto o CLI sobe (RF-29)
├── ClaudePiperPlayback.kt          # (v1.5) ÚNICO ponto de acoplamento com Piper + Java Sound (RNF-19)
├── ClaudeTtaSessions.kt            # (v1.5) serviço de projeto: estado de reprodução única (RNF-23)
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
    ├── AudioMenuAction.kt           # (v1.5) menu "Áudio" no cabeçalho com ações de TTS (RF-31)
    ├── AudioPauseResumeAction.kt    # (v1.5) toggle pause/resume na menu "Áudio"
    ├── AudioPlayAction.kt           # (v1.5) tocar a seleção; corrigido em v1.9 (RF-48, DEF-07)
    ├── AudioStopAction.kt           # (v1.5) parar reprodução e fechar mixer
    ├── SpeechSpeedActions.kt        # (v1.9) submenu "Velocidade" e seus presets (RF-47)
    ├── SplitSessionAction.kt        # (v1.7) menu "Dividir" no cabeçalho (RF-36)
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
2. **Argumentos:** `piperParameters(modelPath, speedPercent): List<String>` _(v1.9)_ — função
   pura, testável sem lançar o `piper`. Em 100% devolve só `-m … --output-raw`; fora disso
   acrescenta `--length-scale`, formatado com `Locale.ROOT` (RNF-31). Ver D-40.
3. **Síntese:** `synthesize(text, executable, modelPath, speedPercent, timeoutSeconds): ByteArray?`
   — lança o `piper` via `GeneralCommandLine`, passa `text` por stdin, lê stdout (PCM cru),
   retorna bytes. Fora da EDT (RNF-20). Prazo de **20 s** _(v1.9.1)_, com `destroyForcibly()` no
   estouro. O stdout é lido em outra thread de propósito: `readBytes()` só volta no EOF e o piper
   trava se o buffer do pipe encher, então um prazo aplicado depois de uma leitura bloqueante não
   limitaria nada. O processo fica em `currentProcess` enquanto roda — é o que dá efeito ao
   cancelamento de RNF-23.
4. **Reprodução:** `playBytes(pcmBytes, sampleRate=22050, channels=1)` — abre um `Clip` ou
   `SourceDataLine`, reproduz, libera recursos em `finally` (RNF-22).
5. **Ciclo de vida:** `pause()` (guarda posição), `resume()` (restaura e reinicia), `stop()`
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

**Menu "Áudio" no cabeçalho (v1.5, RF-31).**

`DefaultActionGroup("Áudio", true)` contém:

- `AudioPauseResumeAction` — texto/ícone alterna entre "Pausar" (quando Playing) e "Retomar"
  (quando Paused); desabilitado quando Idle.
- `AudioStopAction` — ícone `AllIcons.Actions.Suspend` ou similar; desabilitado quando Idle.

Ambos chamam `ClaudeTtaSessions.getInstance(project).pause()`, `.resume()`, `.stop()`.

O `DefaultActionGroup` é criado em `ClaudeToolWindowFactory` (onde já são adicionadas as ações
de Nova/Retomar/Copiar/Exportar) e adicionado à lista `setTitleActions(...)`.

**Submenu "Velocidade" (v1.9, RF-47).**

`SpeechSpeedMenuAction` é um `DefaultActionGroup("Velocidade", true)` pendurado no fim do menu
"Áudio", com um `SpeechSpeedAction` (`ToggleAction`) por entrada de `ClaudeDockSettings.SPEECH_SPEEDS`.
Não há estado nem lista próprios: `isSelected` lê `effectiveSpeechSpeed()` e `setSelected` grava
`speechSpeed`, e os itens saem da mesma tabela que a tela de configuração consome.

**A tabela mora no `ClaudeDockSettings`, não nas actions.** É o domínio do valor, e é o que
permite `effectiveSpeechSpeed()` aproximar para a velocidade oferecida mais próxima sem que o
pacote de settings passe a depender do de ações (CB-59, CB-60).

O `update()` do grupo reescreve o próprio rótulo para "Velocidade (…)", de modo que a velocidade
em vigor apareça sem abrir o submenu. Desabilitar sem Piper é herdado — o `AudioMenuAction` já
desabilita o grupo inteiro.

**Campos de configuração novos (v1.5, RF-32; v1.9, RF-47).**

`ClaudeDockSettings` (app-level) ganha:

- `piperExecutable: String = "piper"` (default)
- `piperModel: String = ""` (default vazio)
- _(v1.9)_ `speechSpeed: Int = 100` — percentual, com `effectiveSpeechSpeed()` aproximando para a
  entrada mais próxima de `SPEECH_SPEEDS`, a tabela que menu e tela compartilham (CB-59)

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
    // v1.9 — RF-47
    row("Velocidade da fala:") {
        comboBox(SPEECH_SPEEDS.keys.toList(), textListCellRenderer("") { speechSpeedLabel(it) })
            .bindItem({ settings.effectiveSpeechSpeed() }, { settings.speechSpeed = it ?: 100 })
            .comment("Mesmos valores do submenu \"Velocidade\". \"1x\" mantém o padrão do modelo.")
    }
}
```

### Exportação do trecho selecionado _(v1.6)_

**A decisão que define a seção: o CLI não participa (D-30).**

A ação de exportar do cabeçalho (RF-22) e a cópia da conversa (RF-24) passam ambas pelo
`/export`, escrito no `TtyConnector`. É tentador supor que "exportar o trecho" seja a mesma
coisa com um argumento a mais — e é aí que a suposição quebra: **`/export` exporta a conversa**.
Ele é executado dentro do Claude Code, sobre as mensagens que o CLI tem em memória
(`lZo(t.messages, …)`, lido do binário na v1.3), e o único argumento que aceita é o **nome do
arquivo de destino**. Não existe forma de pedir a ele um trecho da tela.

O trecho, por outro lado, **já está na mão do plugin**: é o mesmo `widget.selectedText` que
RF-26 usa para copiar. Exportá-lo é gravar um texto que já temos.

Consequência prática — o caminho de RF-33 é o mais curto dos três, e por isso o mais confiável:

| Etapa                    | RF-24 (conversa → clipboard) | RF-22 (conversa → arquivo) | **RF-33 (trecho → arquivo)** |
| ------------------------ | ---------------------------- | -------------------------- | ---------------------------- |
| Escreve no PTY           | sim                          | sim                        | **não**                      |
| Depende do CLI vivo      | sim                          | sim                        | **não**                      |
| Arquivo temporário       | sim (RF-25)                  | não                        | **não**                      |
| Espera/sondagem em disco | sim, até 20 s                | não                        | **não**                      |
| Pode estourar prazo      | sim (CB-29)                  | —                          | **não**                      |

**`ClaudeSelectionExport` — objeto puro (RNF-26).**

Duas responsabilidades, nenhuma delas ligada a UI:

- `suggestedFileName(now: LocalDateTime): String` — `claude-selection-<AAAAMMDD-HHmmss>.md`.
  O carimbo de tempo é o que distingue exportações sucessivas sem perguntar nada ao usuário; o
  `.md` acompanha o que RF-24/RF-25 já usam para o mesmo tipo de conteúdo.
- `write(target: Path, text: String)` — grava em UTF-8, deixando a `IOException` subir para
  quem sabe notificar. O objeto não conhece `Project`, `Notification` nem `Logger`.

A normalização **não** ganha código novo: é `ClaudeSessionText.normalize`, a mesma função que a
cópia usa desde RF-21. Trecho que normaliza para `null` (só espaços, ou só bordas do TUI) não
chega a abrir o diálogo (CB-43).

**O popup ganha o segundo botão (RF-33).**

`ClaudeSelectionCopyButton` passa a montar um `JPanel` com dois `JBLabel` — copiar e exportar —
em vez de um único label. O resto do mecanismo é o que já existe e já foi validado no uso real:
aparece no `mouseReleased`, some no `selectionChanged`, `setRequestFocus(false)` para não roubar
o foco da sessão, e nada é instalado fora do engine CLASSIC (R-15).

O diálogo é o nativo da plataforma, não um construído por nós:

```kotlin
// Extensão sugerida no descritor: o diálogo cuida de sufixo e de sobrescrita (CB-45).
val descriptor = FileSaverDescriptor("Exportar Seleção", "Grava o trecho selecionado", "md")
val wrapper = FileChooserFactory.getInstance()
    .createSaveFileDialog(descriptor, project)
    .save(baseDir, ClaudeSelectionExport.suggestedFileName())
    ?: return  // Cancelou: caminho normal, nada a fazer nem a avisar (CB-44).
```

**Teto deliberado de dois botões.** O popup nasceu com um (RF-26) e agora tem dois. RF-30 (play)
foi descartado em v1.5.1 justamente para não transformá-lo em barra de ferramentas. O critério
que separa os casos é capacidade, não simetria: exportar o trecho **não existe** em outro lugar
da UI, enquanto o play já existia no menu do cabeçalho. Ver R-23 e o Achado 25.

> **Superado em v1.10.** O teto virou três e o critério mudou: o de capacidade reprovava também o
> botão de copiar, que `Ctrl+C` já cobria — e o copiar existe. O critério em vigor é o do
> Achado 34 (opera sobre o trecho selecionado, e cabe em um clique). RF-50 entra por ele; R-29
> substitui R-23.

### Divisão da aba em várias sessões _(v1.7)_

**A árvore de componentes é a estrutura de dados (RNF-29).**

Cada aba tem um painel raiz com um filho só. Dividir troca esse filho por um `OnePixelSplitter`
que contém o bloco antigo e o novo. Dividir de novo repete o processo dentro de qualquer lado —
daí "duas ou mais" sem limite artificial e sem código de aninhamento.

```
Aba (Content)                       Depois de dividir à direita, e a de baixo abaixo:
└── raiz (BorderLayout)             └── raiz
    └── pane A                          └── Splitter(lado a lado)
                                            ├── pane A
                                            └── Splitter(empilhado)
                                                ├── pane B
                                                └── pane C
```

Não há mapa de panes, pelo mesmo motivo de D-18: um mapa paralelo precisaria ser limpo em todo
caminho de fechamento — e é exatamente aí que sobra referência para pane morta. As três consultas
necessárias saem da própria árvore:

| Pergunta                        | Como é respondida                                                      |
| ------------------------------- | ---------------------------------------------------------------------- |
| Qual aba contém esta sessão?    | `SwingUtilities.isDescendingFrom(widget.component, content.component)` |
| Qual bloco divisível é este?    | sobe pelos pais até achar o painel raiz ou um `Splitter`               |
| Qual o `Disposable` desta pane? | `putClientProperty` no próprio componente — morre junto com ele        |

**`ClaudeSessionSplitter` — objeto puro de Swing (RNF-30).** Quatro funções: `root` (embrulha a
primeira pane), `paneOf` (sobe até o bloco divisível), `split` (troca a pane por um splitter com
as duas) e `close` (colapsa o splitter, promovendo a irmã). Não conhece `Project`, terminal nem
tool window, e por isso a árvore inteira é testável sem subir o IDE.

**Ciclo de vida hierárquico (RNF-28).** O `Disposable` da aba é pai dos `Disposable` das panes:

```
tabDisposable                 → Content.setDisposer(...)  : fechar a aba mata todos (RF-40)
├── paneDisposable A          → widget A, capa, popup de seleção
└── paneDisposable B          → widget B, capa, popup de seleção   : fechar B mata só B (RF-39)
```

**Quem é "a sessão selecionada" (D-34).** Antes, a aba tinha uma sessão e a chave
`SESSION_WIDGET` apontava para ela. Com o split, a chave passa a significar **a última sessão com
foco naquela aba**, reescrita por um `FocusListener` no painel de cada pane. `selectedWidget()`
não mudou uma linha — mudou o que a chave quer dizer, e com isso RF-22, RF-24 e RF-31 passaram a
respeitar o foco sem saberem que o split existe.

**O gatilho não foi construído (D-33).** Ver
[O gatilho do split já existia na plataforma](#o-gatilho-do-split-já-existia-na-plataforma-verificado-em-v17):
implementar `JBTerminalWidgetListener` faz o próprio widget mostrar "Split Right"/"Split Down".
O menu "Dividir" do cabeçalho é o segundo caminho, pedido pelo usuário (D-35).

**A conversão de direção é explícita, e testada.** `listener.split(vertically = true)` significa
lado a lado; `Splitter(vertical = true)` significa empilhado. Os dois booleanos têm nome parecido
e sentido oposto, então o código converte num ponto só (`stacked = !vertically`) e T-1.34/T-1.35
verificam **geometria**, não a flag: depois do layout, a segunda pane está à direita ou abaixo.

### Play do trecho no popup da seleção _(v1.10, RF-50)_

**O pedido é de reuso, e o código já está todo escrito.** O popup tem o texto
(`widget.selectedText`, o mesmo que alimenta RF-26 e RF-33) e o serviço de áudio tem o play
(`ClaudeTtaSessions.playText`, o mesmo que RF-48 usa desde o cabeçalho). O que falta é um terceiro
`button(...)` no `JPanel` que já existe:

```kotlin
add(button(AllIcons.Actions.Execute, "Tocar seleção", ::playSelection))

private fun playSelection() {
    val text = ClaudeSessionText.normalize(selectedText())
    hide()
    if (text == null) return
    ClaudeTtaSessions.getInstance(project).playText(text)
}
```

O `project` já é campo do `Controller` — entrou com a exportação, na v1.6. **Nenhuma peça nova.**

**O que não é reuso, e é a única mudança de comportamento real.** Hoje `playText` falha em
silêncio quando o Piper não está configurado:

```kotlin
if (!ClaudePiperPlayback.canSynthesize(executable, model)) {
    LOG.warn("Piper not available for synthesis")   // <- e mais nada
    state = TtsState.Idle
    notifyStateChanged()
    return@executeOnPooledThread
}
```

Do cabeçalho isso passava despercebido, porque as duas camadas de `update()` do menu "Áudio"
desabilitam o item antes do clique. **O popup não tem `update()`** — o botão está sempre lá, e sem
aviso o clique vira exatamente o no-op mudo do DEF-07, no mesmo produto e pelo mesmo motivo.

**O aviso vai em `playText`, e não no botão** (RNF-33). Não é preferência de estilo: `playText` é o
ponto por onde **os dois** chamadores passam, então uma guarda ali conserta o botão novo e o item
de menu de uma vez, com um diff menor do que colocar a verificação nos dois lugares. É a mesma
forma do RF-48 — resolver no serviço, não no chamador.

### `/usage` no cabeçalho _(v1.10, RF-51)_

Uma ação irmã das outras do cabeçalho, e do mesmo tamanho:

```kotlin
// ClaudeDockSessions
fun openUsage() {
    val widget = selectedWidget()
        ?: return notify("Nenhuma sessão aberta para consultar o uso.", NotificationType.WARNING)

    if (!ClaudeTerminalSessionFactory.sendInput(widget, "/usage\r")) {
        notify("A sessão ainda não iniciou; tente de novo em instantes.", NotificationType.WARNING)
    }
}
```

O `\r` é a convenção já usada por `ClaudeSessionExport.command` — é o retorno que o TUI espera, e
não `\n`. A ação entra em `setTitleActions` **entre** `ResumeSessionAction` e
`SplitSessionMenuAction`, que é a posição pedida.

**O que este desenho deliberadamente não faz.** Não sonda o terminal esperando a resposta, como
RF-24 faz com o `/export`. Não há arquivo para aparecer: o `/usage` desenha uma tela e fica nela.
Tentar raspar o buffer para montar um popup traria de volta o DEF-01 (a conversa sai repetida, uma
cópia por repintura do TUI) sobre um conteúdo que muda a cada frame. **Entregar dentro da sessão é
o desenho, não uma limitação contornável.**

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

| #         | Caso                                                                                                                | Tratamento                                                                                                                                                                                                                                                  |
| --------- | ------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **CB-01** | `claude` não está no `PATH`                                                                                         | Notificação acionável (RF-14); aba permanece com shell utilizável                                                                                                                                                                                           |
| **CB-02** | Caminho configurado aponta para arquivo inexistente ou sem permissão de execução                                    | Validação na tela de configurações + notificação ao criar sessão                                                                                                                                                                                            |
| **CB-03** | Plugin oficial ausente ou desabilitado                                                                              | Degradação graciosa: janela funciona, integração não (RNF-11, Fluxo C)                                                                                                                                                                                      |
| **CB-04** | Plugin oficial **e** este plugin abertos ao mesmo tempo                                                             | Coexistem: duas sessões independentes do CLI, ambas conectadas ao mesmo servidor MCP. Ver [Riscos R-05](#riscos)                                                                                                                                            |
| **CB-05** | Projeto sem `basePath` (ex.: janela sem projeto, arquivo avulso)                                                    | Fallback para o diretório home do usuário                                                                                                                                                                                                                   |
| **CB-06** | Múltiplos projetos abertos                                                                                          | Serviços com escopo de `Project`; uma tool window por projeto (RNF-17)                                                                                                                                                                                      |
| **CB-07** | Usuário encerra o `claude` com `/exit` ou `Ctrl+D`                                                                  | `addTerminationCallback` marca a aba; scrollback preservado (RF-11)                                                                                                                                                                                         |
| **CB-08** | Usuário fecha a aba com processo vivo                                                                               | `Disposable` encerra o processo e libera o PTY (RNF-09, RF-12)                                                                                                                                                                                              |
| **CB-09** | Projeto fechado com sessões ativas                                                                                  | Disposables de escopo de projeto encerram todas as sessões                                                                                                                                                                                                  |
| **CB-10** | IDE sem o plugin de terminal                                                                                        | Tool window não registrada; nenhum erro (RF-15)                                                                                                                                                                                                             |
| **CB-11** | `TerminalEngine.REWORKED` ativo em vez de `CLASSIC`                                                                 | Usar `startShellTerminalWidget`, que respeita o engine configurado; **validar ambos os engines nos testes**                                                                                                                                                 |
| **CB-12** | Projeto não confiável (_Trusted Projects_)                                                                          | A plataforma bloqueia criação de terminal; propagar a decisão do IDE sem contorná-la                                                                                                                                                                        |
| **CB-13** | Nome de aba duplicado com muitas sessões                                                                            | Sufixo numérico incremental (RF-16)                                                                                                                                                                                                                         |
| **CB-14** | Usuário troca o caminho do `claude` com sessões abertas                                                             | Sessões existentes não são afetadas; a mudança vale para as próximas                                                                                                                                                                                        |
| **CB-15** | Upgrade do IDE quebra a assinatura de `startShellTerminalWidget`                                                    | Falha isolada em uma classe (RNF-15); tratamento em [Riscos R-01](#riscos)                                                                                                                                                                                  |
| **CB-16** | Remote Dev / split mode (frontend e backend separados)                                                              | Fora de escopo; o customizer roda no backend e o caminho de código difere. **Documentar como não suportado**                                                                                                                                                |
| **CB-17** | Diretório do projeto em WSL (Windows)                                                                               | Fora de escopo nesta versão                                                                                                                                                                                                                                 |
| **CB-18** | _(v1.1)_ `Esc` pressionado com modificador (`Shift+Esc`, `Ctrl+Esc`)                                                | Não é interceptado: segue para o tratamento normal do IDE, preservando `Ctrl+Esc` do oficial (RF-13, T-5.2)                                                                                                                                                 |
| **CB-19** | _(v1.1)_ `Esc` antes do PTY existir (sessão adiada por RNF-02)                                                      | `ttyConnector` nulo: o pre-handler não consome e o comportamento padrão prevalece — não há sessão para receber                                                                                                                                              |
| **CB-20** | _(v1.1)_ Usuário fecha a última aba da tool window                                                                  | Estado vazio com links "Nova sessão" / "Retomar sessão" (RF-18); `createToolWindowContent` não roda de novo                                                                                                                                                 |
| **CB-21** | _(v1.1)_ `CLAUDE_CONFIG_DIR` informado com `~`                                                                      | Expandido no plugin: variável de ambiente não passa por expansão do shell                                                                                                                                                                                   |
| **CB-22** | _(v1.1)_ `CLAUDE_CONFIG_DIR` aponta para diretório inexistente                                                      | Repassado como está; a criação é responsabilidade do CLI. Sem validação própria para não divergir do CLI                                                                                                                                                    |
| **CB-23** | _(v1.1)_ `CLAUDE_CONFIG_DIR` alterado com sessões abertas                                                           | Como CB-14: sessões vivas não mudam; vale para as próximas abas                                                                                                                                                                                             |
| **CB-24** | _(v1.2)_ Copiar/exportar sem nenhuma aba aberta (estado vazio de RF-18)                                             | Notificação "Nenhuma sessão aberta"; nada acontece (RF-23)                                                                                                                                                                                                  |
| **CB-25** | _(v1.2)_ Conversa maior que o scrollback                                                                            | A cópia traz só o que restou no buffer, truncada no topo. O limite é do usuário (`terminal.buffer.max.lines.count`) e o plugin não o altera (RF-13); para a conversa inteira existe o `/export`                                                             |
| **CB-26** | _(v1.2)_ Engine sem JediTerm (`REWORKED`)                                                                           | `getText()` cai no `default` da interface e devolve vazio: notificação "sem conteúdo", nunca exceção. Ligado a Q-04                                                                                                                                         |
| **CB-27** | _(v1.2)_ "Exportar Conversa" com o processo `claude` já encerrado (RF-11)                                           | O texto cai no shell e vira `command not found`. Inofensivo; distinguir TUI vivo de shell exigiria heurística frágil                                                                                                                                        |
| **CB-28** | _(v1.2)_ "Exportar Conversa" antes de o PTY existir (sessão adiada, RNF-02)                                         | `ttyConnector` nulo: `sendInput` devolve `false` e a ação notifica (mesma raiz de CB-19)                                                                                                                                                                    |
| **CB-29** | _(v1.3)_ O `/export` de RF-24 não produz o arquivo (CLI ocupado, encerrado, ou comando removido)                    | Prazo limite; estourado, notifica "não foi possível obter a conversa" e apaga o temporário. Sem espera indefinida                                                                                                                                           |
| **CB-30** | _(v1.3)_ Seleção desfeita ou vazia com o botão flutuante na tela                                                    | O listener dispara com seleção vazia e o botão some sem copiar nada                                                                                                                                                                                         |
| **CB-31** | _(v1.3)_ Seleção feita numa aba e clique no botão depois de trocar de aba                                           | O botão é por painel e some junto com a aba; não há caminho para copiar seleção de outra aba                                                                                                                                                                |
| **CB-32** | _(v1.3)_ Cópia acionada duas vezes seguidas antes de a primeira terminar                                            | Um `/export` por vez, por aba: a segunda é ignorada enquanto a primeira está em curso                                                                                                                                                                       |
| **CB-33** | _(v1.4)_ Usuário troca o tema do IDE com sessões abertas                                                            | O respiro acompanha: a cor é lida a cada pintura, não copiada na criação da aba (RF-28)                                                                                                                                                                     |
| **CB-34** | _(v1.4)_ O CLI pede algo nos primeiros segundos — ex.: "confia nesta pasta?" na primeira execução em diretório novo | A capa esconde até o prazo terminar; o diálogo continua lá e o usuário responde depois. Custo aceito de RF-29                                                                                                                                               |
| **CB-35** | _(v1.4)_ Aba fechada antes de a capa sair                                                                           | Timer, animação e capa são filhos do `Disposable` da aba: caem juntos, sem timer disparando sobre painel morto                                                                                                                                              |
| **CB-36** | _(v1.4)_ Engine sem JediTerm (`REWORKED`) com o respiro ligado                                                      | Sem painel para consultar, a borda fica vazia e a faixa é pintada pela tool window — degrada em cor, não em erro                                                                                                                                            |
| **CB-37** | _(v1.4)_ Executável instalado em diretório ausente do `PATH` do IDE (ex.: `~/.local/bin`)                           | A verificação consulta diretórios conhecidos antes de notificar; o shell da sessão resolve de qualquer forma (CB-01)                                                                                                                                        |
| **CB-38** | _(v1.5)_ Seleção muito grande (ex.: páginas de código) enviada ao Piper                                             | A síntese pode demorar décadas de segundos; o timeout (20s ou configurável) notifica o usuário sem travar a UI                                                                                                                                              |
| **CB-39** | _(v1.5)_ Modelo `.onnx` ausente após ser configurado                                                                | O botão play fica desabilitado; verificação consultou o arquivo antes de manter estado Idle                                                                                                                                                                 |
| **CB-40** | _(v1.5)_ Reprodução em curso e usuário fecha a aba                                                                  | O `Disposable` da aba dispara, `ClaudeTtaSessions` para a reprodução e libera recursos (RNF-22)                                                                                                                                                             |
| **CB-41** | _(v1.5)_ Reprodução pausada e usuário sai do IDE                                                                    | Nenhum evento especial — o proceso `piper` já terminou (síntese é fora da EDT), só o `Clip` fica em pausa. Fechamento normal do IDE libera tudo                                                                                                             |
| **CB-42** | _(v1.5)_ Dois modelos diferentes configurados (ex.: português e inglês) e usuário alterna                           | Nenhum problema — cada chamada a `playText()` usa o `piperModel` atual da configuração                                                                                                                                                                      |
| **CB-43** | _(v1.6)_ Seleção contém só espaços ou só bordas do TUI                                                              | `ClaudeSessionText.normalize` devolve `null` e o diálogo não chega a abrir; notificação explica que não há o que exportar (RF-34)                                                                                                                           |
| **CB-44** | _(v1.6)_ Usuário cancela o diálogo de salvar                                                                        | `save(...)` devolve `null`. É caminho normal, não erro: nada é gravado e **nada é notificado** (RF-34)                                                                                                                                                      |
| **CB-45** | _(v1.6)_ Destino escolhido já existe                                                                                | A confirmação de sobrescrita é do diálogo nativo — não escrevemos essa pergunta, e não a contornamos                                                                                                                                                        |
| **CB-46** | _(v1.6)_ Diretório sem permissão de escrita, disco cheio, ou caminho removido entre a escolha e a gravação          | `IOException` capturada, logada sem o conteúdo (RNF-24) e convertida em notificação de erro (Fluxo J)                                                                                                                                                       |
| **CB-47** | _(v1.6)_ Engine sem JediTerm (`REWORKED`/`NEW_TERMINAL`)                                                            | O popup inteiro não é instalado, exportar o trecho junto com copiar — mesma degradação de R-15. O export da conversa no cabeçalho (RF-22) continua valendo                                                                                                  |
| **CB-48** | _(v1.6)_ Projeto sem `basePath` (raro, mas possível)                                                                | O diálogo abre no diretório padrão da plataforma em vez da raiz do projeto; a gravação segue funcionando                                                                                                                                                    |
| **CB-49** | _(v1.6)_ Aba fechada com o popup aberto                                                                             | O popup é filho do `Disposable` da aba desde RF-26: cai junto, e o diálogo nem chega a existir                                                                                                                                                              |
| **CB-50** | _(v1.7)_ Aba fechada entre o clique em "Dividir" e a criação da pane                                                | `split` devolve `false` quando o componente não tem pai; a sessão recém-criada é descartada e nada fica pendurado (T-1.37)                                                                                                                                  |
| **CB-51** | _(v1.7)_ Falha ao criar a sessão da divisão (executável sumiu, PTY negado)                                          | A pane nova é descartada com seu `Disposable`, a divisão **não** acontece e a aba continua como estava (RNF-10)                                                                                                                                             |
| **CB-52** | _(v1.7)_ Sessão de uma pane encerra (`/exit`) sem fechar a pane                                                     | A pane continua no ar com o shell vivo, como qualquer aba encerrada (RF-11, D-09). Só a sessão original da aba renomeia o título — ver Q-26                                                                                                                 |
| **CB-53** | _(v1.7)_ Fechar a aba com várias divisões abertas                                                                   | O `Disposable` da aba é pai de todos: cai a árvore inteira, sem PTY órfão (RF-40, RNF-28)                                                                                                                                                                   |
| **CB-54** | _(v1.7)_ Divisão em janela estreita: as panes ficam com poucas colunas                                              | Cada terminal recebe seu tamanho real e o CLI redesenha para ele; o divisor é arrastável. Legibilidade é escolha de quem divide, não limite do plugin                                                                                                       |
| **CB-55** | _(v1.7)_ Ação do cabeçalho acionada sem nenhuma pane jamais focada                                                  | `selectedWidget()` cai na sessão registrada na criação da aba — a chave nasce preenchida e só é reescrita pelo foco (RF-38)                                                                                                                                 |
| **CB-56** | _(v1.7)_ Várias sessões da mesma aba conectadas ao mesmo servidor MCP do plugin oficial                             | É a Q-02 de sempre, agora mais provável: nada garante qual sessão "possui" um diff aberto. Não regride nada — duas abas já produziam o mesmo — mas passa a acontecer com mais frequência                                                                    |
| **CB-57** | _(v1.7.2)_ "Select Previous/Next Tab" acionado com uma aba só                                                       | A guarda de `ClaudeTabNavigation` segura a chamada: sem ela a plataforma dispara assertion, não um no-op (DEF-02). Os itens continuam visíveis no menu, porque quem os monta não nos consulta                                                               |
| **CB-58** | _(v1.7.2)_ "Show Tabs" acionado no menu de contexto                                                                 | No-op deliberado: as abas já estão visíveis no cabeçalho da tool window, e não há painel de abas a revelar como no terminal nativo                                                                                                                          |
| **CB-59** | _(v1.9)_ `speechSpeed` fora da tabela no `claude-code-dock.xml`, editado à mão (ex.: 110, 9999, 0)                  | `effectiveSpeechSpeed()` devolve a velocidade **mais próxima** das oferecidas — 110 vira 100, 9999 vira 200, 0 vira 25. Limitar a faixa não bastaria: um valor solto dentro dela deixaria o seletor da tela sem item selecionado, e o `apply` gravaria nulo |
| **CB-60** | _(v1.9)_ Menu e tela oferecendo listas diferentes                                                                   | Impossível por construção: as duas leem `ClaudeDockSettings.SPEECH_SPEEDS`, e T-1.54 fixa o conteúdo dela. A tabela mora no settings, e não nas actions, para que a tela não dependa do pacote de ações                                                     |
| **CB-61** | _(v1.9)_ Velocidade trocada durante uma fala em curso                                                               | A fala atual segue na velocidade com que foi sintetizada; a nova vale a partir do próximo play. O piper sintetiza o áudio inteiro antes de tocar, então não há o que reajustar (Q-29)                                                                       |
| **CB-63** | _(v1.10)_ Clique no play do popup com o Piper não configurado                                                       | Notifica e não toca (RNF-33). Antes de v1.10 este caminho era mudo, e do menu ficava escondido atrás do `update()` desabilitado                                                                                                                            |
| **CB-64** | _(v1.10)_ Clique no play do popup com uma fala já em curso                                                          | A anterior é interrompida e a nova toca: `playText` começa com `stop()`, e a regra de uma fala por vez (RNF-23) vale para o botão novo sem código novo                                                                                                     |
| **CB-65** | _(v1.10)_ Trecho selecionado longo demais para a síntese                                                            | Mesmo teto do menu "Áudio": o prazo de 20 s do `ClaudePiperPlayback` (RNF-20) corta, e a falha notifica. O botão novo não muda o limite nem o herda pela metade                                                                                            |
| **CB-66** | _(v1.10)_ "Uso" clicado com o CLI no meio de uma resposta                                                           | O `/usage` entra na fila de entrada do TUI como qualquer digitação. Não há garantia de que abra na hora, e o plugin não tenta dar uma — é sessão interativa, não RPC                                                                                       |
| **CB-67** | _(v1.10)_ `Ctrl+V` com imagem na área de transferência e `xclip`/`wl-paste` ausentes                                | Hoje: no-op mudo, dentro e fora do IDE (Achado 33). Conserto é de ambiente (`wl-clipboard`), não de código. O que o plugin pode fazer depende de T-3.62                                                                                                    |
| **CB-62** | _(v1.9)_ Modelo cujo `config.json` traz `length_scale` diferente de 1.0                                             | Em 100% a flag não é passada e o valor do modelo prevalece — é o comportamento correto, e é por isso que 100% se chama "padrão do modelo" e não "1x" (D-40)                                                                                                 |

---

## Riscos

| #        | Risco                                                                                                                                                                                                                | Impacto     | Prob.               | Mitigação                                                                                                                                                                                                                                                                                                                                                                       |
| -------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------- | ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **R-01** | `AbstractTerminalRunner` / `TerminalToolWindowManager` são API de plataforma sujeita a mudança sem aviso entre builds                                                                                                | Alto        | Média               | Todo o acoplamento em uma única classe (RNF-15); smoke test manual a cada upgrade de IDE; sem `until-build`, o plugin carrega e falha de forma isolada e diagnosticável, em vez de ser silenciosamente desabilitado                                                                                                                                                             |
| **R-02** | ~~A premissa central (customizer alcança widget customizado) pode estar errada~~ ✅ **FECHADO em 2026-08-01**                                                                                                        | ~~Crítico~~ | —                   | Validado empiricamente por `TerminalCustomizerReachTest` (T-4), com teste de controle. É `configureStartupOptions` que aplica os customizers, e `startShellTerminalWidget` passa por ele. O teste permanece como regressão a cada upgrade de IDE                                                                                                                                |
| **R-03** | A Anthropic pode alterar `CLAUDE_CODE_SSE_PORT` ou o mecanismo de descoberta                                                                                                                                         | Médio       | Média               | Este plugin **não depende** da variável: apenas não a atrapalha. Mudanças quebram integração, não a janela (degradação graciosa)                                                                                                                                                                                                                                                |
| **R-04** | O plugin oficial pode ganhar tool window dedicada nativamente, tornando este projeto obsoleto                                                                                                                        | Baixo       | Média               | Resultado aceitável: o custo afundado é pequeno (poucas centenas de linhas) e desinstalar é trivial. Acompanhar o changelog do oficial                                                                                                                                                                                                                                          |
| **R-05** | Duas sessões simultâneas (aba nativa + janela dedicada) conectadas ao mesmo servidor MCP podem gerar comportamento ambíguo — ex.: um diff aberto por uma sessão atribuído à outra                                    | Médio       | Média               | Documentar no README a recomendação de usar uma janela por vez; investigar durante T-4 se o servidor distingue clientes. Considerar ocultar o botão do oficial via `CLAUDE_CODE_JETBRAINS_PLUGIN_HIDE_BUTTON` (string encontrada no jar oficial — **usar apenas após verificar semântica**)                                                                                     |
| **R-06** | `TerminalEngine.REWORKED` pode ter comportamento distinto de `CLASSIC` ao ser embutido fora da tool window nativa                                                                                                    | Médio       | Média               | Testar explicitamente com os dois engines (CB-11); fixar `CLASSIC` como fallback documentado se houver divergência                                                                                                                                                                                                                                                              |
| **R-07** | Plugin não assinado instalado via disco dispara aviso de segurança do IDE                                                                                                                                            | Baixo       | Alta                | Esperado para uso local; documentar no README                                                                                                                                                                                                                                                                                                                                   |
| **R-08** | Sem `until-build`, uma versão futura incompatível do IDE pode causar exceções em runtime                                                                                                                             | Médio       | Média               | Aceito conscientemente em troca de não travar a cada upgrade; mitigado por R-01 e pelo tratamento de erro do Fluxo E                                                                                                                                                                                                                                                            |
| **R-09** | _(v1.1)_ A correção do `Esc` (RF-17) depende de `JBTerminalPanel.addPreKeyEventHandler` e de `JBTerminalWidget.asJediTermWidget` — API pública, mas sem garantia de estabilidade, e específica do engine **CLASSIC** | Médio       | Média               | Falha degrada, não quebra: `asJediTermWidget` nulo faz o `install` retornar sem efeito, e o pior caso é voltar ao bug atual (`Esc` move o foco), nunca uma exceção. Coberto por T-3.7. Se a plataforma migrar a janela para o engine REWORKED, reavaliar contra `TerminalEscapeHandler` (EP `org.jetbrains.plugins.terminal.escapeHandler`, já existente em 262)                |
| **R-10** | _(v1.1)_ A JetBrains pode corrigir a assimetria do `TerminalEscapeKeyListener` e passar a entregar o `Esc` também fora da tool window "Terminal", tornando o pre-handler redundante — ou duplicando o envio          | Baixo       | Baixa               | O pre-handler consome o evento antes do listener, então mesmo com a correção upstream o caminho continua único: nós enviamos, a plataforma não reenvia. Revalidar em T-5.3 a cada upgrade                                                                                                                                                                                       |
| **R-11** | _(v1.2)_ O `/export` (RF-22) depende de um slash command do CLI, que a Anthropic pode renomear ou remover sem aviso                                                                                                  | Baixo       | Média               | Falha visível e inofensiva: o texto aparece na sessão e o CLI responde que não conhece o comando. A cópia de RF-21 não depende do CLI e continua atendendo o caso principal                                                                                                                                                                                                     |
| **R-12** | ~~_(v1.2)_ Escrever `"/export\r"` no PTY é digitação simulada~~ ✅ **FECHADO em 2026-08-01** — T-3.11 aprovado: o `/export` executou e gerou o arquivo                                                               | ~~Baixo~~   | —                   | Validado no IDE real: o `\r` é aceito como Enter e o autocomplete não interfere. Como RF-24 passa a depender disso em todo uso, revalidar a cada upgrade do CLI                                                                                                                                                                                                                 |
| **R-13** | ~~_(v1.3)_ O argumento `[filename]` do `/export` não foi verificado~~ ✅ **FECHADO em 2026-08-01** — lido na implementação embutida no binário `claude` 2.1.220                                                      | ~~Alto~~    | —                   | Grava direto e sem UI, sobrescreve, cria diretórios, e acrescenta `.txt` se faltar extensão. O destino de RF-24 termina em `.md` por causa disso. Revalidar a cada upgrade do CLI, junto de R-11                                                                                                                                                                                |
| **R-14** | _(v1.3)_ RF-24 grava a conversa em arquivo temporário — código-fonte e possíveis segredos passam por disco, ainda que por segundos                                                                                   | Médio       | Alta                | Arquivo com permissão exclusiva do usuário e apagado em `finally` (RF-25). Liability real, aceita porque a alternativa (buffer) não entrega o recurso. Registrada em RNF-19                                                                                                                                                                                                     |
| **R-15** | _(v1.3, revisto em v1.7)_ O popup de seleção (RF-26/RF-33) e o listener de split (RF-36) dependem de o widget ser JediTerm — mesma exposição de R-09                                                                 | Baixo       | ~~Média~~ **Baixa** | **Probabilidade revista pelo Achado 27:** a sessão criada por este plugin é sempre JediTerm, porque `createTerminalWidget` devolve `JBTerminalWidget` por assinatura. O que resta é a plataforma mudar isso num upgrade. As guardas continuam (Q-27): sem `asJediTermWidget` nada é instalado, `Ctrl+C` segue copiando, e o menu "Dividir" do cabeçalho não depende do listener |
| **R-16** | _(v1.4)_ O prazo da capa (RF-29) é fixo. Máquina mais lenta, hook de sessão pesado ou CLI atualizando deixam o eco escapar quando a capa sai                                                                         | Baixo       | Média               | Constante única em `ClaudeSessionLoading`, ajustável em um lugar. Falha é cosmética e passageira, nunca funcional. Q-16 registra o caminho para trocar prazo por detecção                                                                                                                                                                                                       |
| **R-17** | _(v1.4)_ A verificação do executável usa o `PATH` do `EnvironmentUtil`, que **não** enxerga o que o `.zshrc`/`.bashrc` acrescenta — falso negativo observado com o CLI funcionando                                   | Baixo       | Alta                | Consulta `~/.local/bin` e `/usr/local/bin` antes de desistir, e a notificação nunca bloqueia a abertura da aba. Quem instala fora disso tem o campo de configuração                                                                                                                                                                                                             |
| **R-18** | _(v1.5)_ Piper depende de um arquivo `.onnx` cuja localização e nomenclatura não é padronizada (sem registro central de modelos)                                                                                     | Médio       | Alta                | Decisão de design: o usuário configura manualmente o caminho do modelo desejado, sem autodetecção. Documentar no README dicas de onde obter modelos (ex.: Hugging Face da oma/piper) e convenção de armazená-los em `~/.claude/piper-voices/`                                                                                                                                   |
| **R-19** | _(v1.5)_ A síntese no `piper` é CPU-bound e pode travar a UI se rodasse na EDT                                                                                                                                       | Médio       | Média               | RNF-20 exige síntese fora da EDT via `executeOnPooledThread`. Testado que `ApplicationManager.getApplication().executeOnPooledThread { ... }` + `invokeLater` para update não trava. Revalidar se seleção > N caracteres passar a ser suportada                                                                                                                                 |
| **R-20** | _(v1.5)_ Reprodução de áudio via `javax.sound.sampled` é bloqueante (thread do mixer aguarda buffer ficar vazio)                                                                                                     | Médio       | Média               | Linha de áudio é reproduzida em thread separada (mixer nativo do SO), a UI fica responsiva. Se o mixer travar ou estiver indisponível, a thread de reprodução congela, não a EDT. Risco aceitável                                                                                                                                                                               |
| **R-21** | _(v1.5)_ Modelo `.onnx` pode ser muito grande (63 MB) e o carregamento na primeira síntese causa latência                                                                                                            | Baixo       | Média               | Piper já cacheia o modelo em memória entre chamadas. Primeira síntese tem latência de carregamento (~2-3s); as seguintes são rápidas. Documentar e aceitar                                                                                                                                                                                                                      |
| **R-22** | _(v1.5)_ Dois projetos abertos com configurações diferentes de `piperModel` — sem sincronização entre `ClaudeTtsSessions`                                                                                            | Baixo       | Baixa               | Cada projeto tem sua própria instância de `ClaudeTtaSessions` (via `project.service()`). Não há compartilhamento; cada um usa seu próprio modelo configurado. Esperado e correto                                                                                                                                                                                                |
| **R-23** | _(v1.6; **substituído por R-29 em v1.10**)_ O popup de seleção vira barra de ferramentas: cada rodada acrescenta "só mais um botão" até ele atrapalhar a leitura do que foi selecionado                              | Baixo       | Média               | ~~Teto de dois botões, critério de capacidade~~ — o critério reprovava o próprio botão de copiar, que existe desde RF-26 (Achado 34). Substituído: teto de três, critério de escopo (opera sobre o trecho, um clique). Ver R-29                                                                                                                                                                               |
| **R-24** | _(v1.6)_ O trecho gravado é o render do terminal, com quebras de linha na largura da aba — o arquivo pode não conter o texto como o autor o escreveu                                                                 | Baixo       | Alta                | Inerente a exportar de um terminal, e o mesmo que a cópia por seleção (RF-26) já entrega há rodadas sem reclamação. Declarado em [Fora de Escopo](#fora-de-escopo); Q-22 registra a alternativa se incomodar                                                                                                                                                                    |
| **R-25** | _(v1.7)_ `JBTerminalWidgetListener` não tem contrato de estabilidade: um upgrade pode acrescentar método abstrato ou mudar a semântica de `split(vertically)`                                                        | Médio       | Média               | O acoplamento está num arquivo só (RNF-27), e a semântica da direção tem teste de **geometria** (T-1.34/T-1.35) — inverter a flag lá em cima quebra o teste em vez de sair invertido na tela. O menu do cabeçalho não depende do listener e continuaria funcionando                                                                                                             |
| **R-26** | _(v1.7)_ Várias sessões por aba multiplicam processos `claude`, cada um com seu consumo de memória e sua conexão ao servidor MCP                                                                                     | Baixo       | Alta                | É o custo pedido: dividir é para rodar mais de um Claude Code. Mesmo custo de abrir mais abas, que já não tem limite (RNF-18). Fechar a pane encerra o processo (RF-39)                                                                                                                                                                                                         |
| **R-28** | _(v1.9)_ Nos extremos da faixa (25% e 200%) a inteligibilidade cai, e o ponto em que cai varia por modelo                                                                                                            | Baixo       | Média               | É ajuste de gosto, reversível em dois cliques pelo menu, e o padrão continua sendo o do modelo. A lista é fechada e curta justamente para o usuário topar com o limite escolhendo, não digitando; quem quiser além disso está pedindo outra voz, não outra velocidade                                                                                                           |
| **R-29** | _(v1.10)_ O teto de dois botões no popup (R-23) foi revogado por RF-50; um terceiro pedido de botão volta a crescer o popup até atrapalhar a leitura do trecho                                                        | Baixo       | Média               | O critério não foi abandonado, foi **substituído** por um mais estreito e declarado no Achado 34: entra no popup o que opera **sobre o trecho selecionado** e cabe em um clique. Três é o novo teto, e o próximo pedido passa pela mesma pergunta                        |
| **R-30** | _(v1.10)_ `/usage` pode ser renomeado, removido ou mudar de forma em qualquer release do CLI, e a ação do cabeçalho vira um botão que não faz nada                                                                    | Baixo       | Média               | Mesma exposição que RF-24 já aceita com o `/export` (R-11), e mesma mitigação: o texto do comando fica num ponto só. A falha é visível — o usuário vê o comando entrar na sessão e o CLI reclamar —, ao contrário de um popup que ficaria vazio sem explicação           |
| **R-27** | _(v1.7)_ Com mais sessões simultâneas, a ambiguidade de Q-02 (qual sessão "possui" um diff) deixa de ser hipótese e vira rotina                                                                                      | Médio       | Alta                | Não é regressão — duas abas já bastavam. O split apenas torna o caso comum, o que **ajuda**: Q-02 passa a ser observável no uso real, que é a condição que faltava para decidi-la (CB-56)                                                                                                                                                                                       |

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
| **T-1.22**     | `ClaudePiperPlayback`             | _(v1.5)_ `canSynthesize(executable, modelPath)` retorna verdadeiro só quando ambos existem, é legível e modelo termina em `.onnx`; sem exceções mesmo com caminho inválido (CB-39)                                                                                                                |
| **T-1.23**     | `ClaudePiperPlayback`             | _(v1.5)_ `synthesize(text, executable, modelPath)` retorna ByteArray não vazio com PCM cru (22050 Hz, 16-bit, mono) quando entrada é válida; retorna null com timeout ou erro de processo (CB-38, R-19)                                                                                           |
| **T-1.24**     | `ClaudePiperPlayback`             | _(v1.5)_ PCM de síntese é reproduzível sem erro via `playBytes(pcm, 22050, 1)`; recurso é liberado após stop ou exceção (RNF-22)                                                                                                                                                                  |
| **T-1.25**     | `ClaudeDockSettings`              | _(v1.5)_ `piperExecutable` e `piperModel` nascem em padrão, persistem via `loadState`, `effectivePiperExecutable()` e `effectivePiperModel()` fazem trim (RF-32)                                                                                                                                  |
| **T-1.26**     | `ClaudeTtaSessions`               | _(v1.5)_ Estado alterna Idle → Playing → (Paused ↔ Playing) → Idle corretamente; listeners de update disparam em cada mudança (RNF-23)                                                                                                                                                            |
| **T-1.27**     | `ClaudeTtaSessions`               | _(v1.5)_ `playText(text)` novo durante reprodução prévia para e inicia nova, sem fila; recurso anterior é liberado (RNF-23)                                                                                                                                                                       |
| **T-1.28**     | `ClaudeSelectionExport`           | _(v1.6)_ O nome sugerido termina em `.md`, começa pelo prefixo do plugin e carrega o carimbo de tempo formatado; dois instantes diferentes produzem nomes diferentes (RF-34)                                                                                                                      |
| **T-1.29**     | `ClaudeSelectionExport`           | _(v1.6)_ `write` grava o texto em UTF-8 e o round-trip devolve exatamente o que entrou, inclusive acentuação e quebras de linha (RF-33)                                                                                                                                                           |
| **T-1.30**     | `ClaudeSelectionExport`           | _(v1.6)_ `write` em diretório inexistente lança `IOException` em vez de falhar em silêncio — é o que o chamador converte em notificação (CB-46, Fluxo J)                                                                                                                                          |
| **T-1.31**     | `ClaudeSessionText`               | _(v1.6)_ Seleção só de espaços normaliza para `null`, e é o que impede o diálogo de abrir (CB-43) — reuso verificado, não função nova                                                                                                                                                             |
| **T-1.32**     | `ClaudeSessionSplitter`           | _(v1.7)_ A raiz nasce com a sessão ocupando tudo; dividir troca a pane por um splitter contendo as duas, na ordem certa (RF-36)                                                                                                                                                                   |
| **T-1.33**     | `ClaudeSessionSplitter`           | _(v1.7)_ Dividir de novo **aninha** dentro do lado escolhido, sem mexer no splitter externo — é o que sustenta "duas ou mais" (RF-36)                                                                                                                                                             |
| **T-1.34**     | `ClaudeSessionSplitter`           | _(v1.7)_ "À direita" põe a segunda pane com `x` maior e mesmo `y` — asserção de **geometria depois do layout**, não da flag, porque é a flag que se inverte                                                                                                                                       |
| **T-1.35**     | `ClaudeSessionSplitter`           | _(v1.7)_ "Abaixo" põe a segunda pane com `y` maior e mesmo `x` — o par de T-1.34 é o que prova que a conversão `stacked = !vertically` não está trocada                                                                                                                                           |
| **T-1.36**     | `ClaudeSessionSplitter`           | _(v1.7)_ Fechar uma pane promove a irmã ao lugar do splitter; fechando a de dentro, só o splitter interno colapsa; fechando a única, devolve `false` para a aba ser fechada (RF-39)                                                                                                               |
| **T-1.37**     | `ClaudeSessionSplitter`           | _(v1.7)_ `paneOf` sobe do terminal até o bloco divisível, devolve `null` fora da aba, e dividir componente sem pai devolve `false` em vez de lançar (CB-50)                                                                                                                                       |
| **T-1.38**     | `ClaudeSessionSplitter`           | _(v1.7.1)_ `close` devolve **o irmão sobrevivente**, e não só um booleano — é dele que sai a sessão que reassume o foco (RF-42)                                                                                                                                                                   |
| **T-1.39**     | `ClaudeSessionSplitter`           | _(v1.7.1)_ Fechar em cadeia devolve a árvore ao estado de uma sessão só, sem splitter pendurado, e a última pane devolve `null` (RF-41)                                                                                                                                                           |
| **T-1.40**     | `ClaudeTabNavigation`             | _(v1.7.2)_ Navegar exige mais de uma aba: `canNavigate` é falso com 0 e com 1, verdadeiro de 2 em diante — o limite que DEF-02 violava                                                                                                                                                            |
| **T-1.41**     | `ClaudeSessionSplitter`           | _(v1.8)_ `swap` inverte as duas panes, e trocar duas vezes volta ao arranjo original (RF-43)                                                                                                                                                                                                      |
| **T-1.42**     | `ClaudeSessionSplitter`           | _(v1.8)_ `rotate` leva de lado a lado a empilhado — verificado por **geometria depois do layout**, como T-1.34/T-1.35 (RF-43)                                                                                                                                                                     |
| **T-1.43**     | `ClaudeSessionSplitter`           | _(v1.8)_ `swap` e `rotate` sem divisão devolvem `false` **sem tocar na árvore** (RF-43)                                                                                                                                                                                                           |
| **T-1.44**     | `ClaudeSessionSplitter`           | _(v1.8.1)_ `isSplit` distingue a pane sozinha da dividida — é o que habilita/desabilita as ações (RF-45)                                                                                                                                                                                          |
| **T-1.46**     | `ClaudeSessionSplitter`           | _(v1.8.2)_ `firstPane` acha a sobrevivente sob splitter aninhado — é dela que saem a chave, o `preferredFocusableComponent` e o foco (RF-42, DEF-05)                                                                                                                                              |
| **T-1.47**     | `ClaudeSessionSplitter`           | _(v1.8.2)_ `countPanes` acompanha divisões e fechamentos, e componente sem marca não conta — é o que decide se a aba ainda tem sessão viva (RF-44)                                                                                                                                                |
| **T-1.48**     | `ClaudeDockSettings`              | _(v1.9)_ `speechSpeed` nasce em 100 e sobrevive ao `loadState` (RF-47)                                                                                                                                                                                                                            |
| **T-1.49**     | `ClaudeDockSettings`              | _(v1.9)_ `effectiveSpeechSpeed()` aproxima para a velocidade oferecida mais próxima: 110 → 100, 9999 → 200, 0 → 25 (CB-59)                                                                                                                                                                        |
| **T-1.50**     | `ClaudePiperPlayback`             | _(v1.9)_ Em 100% os argumentos **não** trazem `--length-scale` — é o teste que protege o padrão do modelo (D-40, CB-62)                                                                                                                                                                           |
| **T-1.51**     | `ClaudePiperPlayback`             | _(v1.9)_ 200% → `0.500` e 50% → `2.000`: prova que a conversão é **inversa** e não está trocada de lado (RF-47)                                                                                                                                                                                   |
| **T-1.52**     | `ClaudePiperPlayback`             | _(v1.9)_ Com `Locale` pt-BR imposto, 150% ainda sai `0.667` com **ponto**. É o teste que falha se alguém trocar `String.format(Locale.ROOT, …)` por `"%.3f".format(…)` (RNF-31)                                                                                                                   |
| **T-1.53**     | `SpeechSpeedMenuAction`           | _(v1.9)_ O submenu monta **um item por velocidade oferecida**, e o rótulo de cada um vem da tabela compartilhada (CB-60)                                                                                                                                                                          |
| **T-1.54**     | `ClaudeDockSettings`              | _(v1.9)_ `SPEECH_SPEEDS` é exatamente `25…200` na ordem de exibição, e `speechSpeedLabel` cai em `"110%"` fora dela — é o teste que trava a lista contra divergir entre menu e tela                                                                                                               |
| **T-1.45**     | `ClaudeSessionSplitter`           | _(v1.8.1)_ **`paneOf` devolve `null` para pane já fechada.** Foi este teste que pegou DEF-03: `close` desanexa o splitter, mas a pane removida continua filha dele, então sem checar `isDescendingFrom` ela ainda "estava" na aba — e a guarda de RF-44 não teria efeito nenhum                   |
| **T-1.55**     | `ClaudePiperPlayback`             | _(v1.9.1)_ **A síntese respeita o prazo.** Com um piper falso que dorme 30 s e `timeoutSeconds = 1`, `synthesize` volta `null` em cerca de um segundo. É o teste que reprova se alguém devolver o `waitFor()` sem argumento (RNF-20, Achado 31)                                                   |
| **T-1.56**     | `ClaudePiperPlayback`             | _(v1.9.1)_ **`stop()` alcança a síntese em curso.** Com o mesmo piper falso, `stop()` faz `synthesize` voltar `null` de imediato em vez de esperar os 30 s. Afirma o efeito, não o campo: sem `currentProcess = process` o teste estoura o prazo (RNF-23)                                         |
| **T-1.57**     | `ClaudeEditorReference`           | _(v1.9.4)_ Uma linha só produz `@arquivo#L3 ` — a forma curta que o CLI usa quando `lineStart == lineEnd` (RF-49) |
| **T-1.58**     | `ClaudeEditorReference`           | _(v1.9.4)_ Várias linhas produzem a faixa `@arquivo#L3-10 ` |
| **T-1.59**     | `ClaudeEditorReference`           | _(v1.9.4)_ Sem seleção não vai `#L`. No CLI a guarda é `if (lineStart && lineEnd)` e `0` é falso em JS: zero significa **ausente**, não linha zero |
| **T-1.60**     | `ClaudeEditorReference`           | _(v1.9.4)_ **Seleção de linhas inteiras não conta a linha seguinte.** O offset final cai na coluna 0 da próxima, e sem correção marcar uma linha reportaria `#L3-4` |
| **T-1.62**     | `ClaudeTtaSessions`               | _(v1.10)_ Piper não configurado: `playText` **notifica** e deixa o estado em `Idle`. Antes de v1.10 só logava, e o clique era mudo (RF-50, RNF-33, DEF-07) |
| **T-1.63**     | `ClaudeTtaSessions`               | _(v1.10)_ Texto em branco continua saindo cedo, **sem** notificar — o popup nunca chega a chamar com branco, e um aviso aqui viraria ruído (RF-50) |
| **T-1.64**     | `ClaudeSelectionCopyButton`       | _(v1.10)_ O painel do popup monta **três** botões, nesta ordem: copiar, exportar, tocar. Guarda de regressão do teto revogado em R-29 (RF-50) |
| **T-1.65**     | `ClaudeDockSessions`              | _(v1.10)_ `openUsage()` sem sessão selecionada notifica e **não** escreve no PTY (RF-51) |
| **T-1.66**     | `ClaudeDockSessions`              | _(v1.10)_ O comando enviado é exatamente `/usage\r` — o `\r`, e não `\n`, é o que o TUI espera; a mesma convenção do `ClaudeSessionExport.command` (RF-51) |
| **T-1.61**     | `ClaudeEditorReference`           | _(v1.9.4)_ A referência **sempre termina em espaço** — é ele que separa a menção do que o usuário digita depois. Existe porque `trim()` é a limpeza mais tentadora do mundo |

Conforme `CLAUDE.md`, novos testes acompanham cada funcionalidade nova ou alterada, e a suíte é
executada após cada implementação.

### Testes de integração

✅ **Implementados em v1.9.2**, em `ClaudeDockIntegrationTest`.

> **O limite do ambiente, medido antes de escrever os testes.** Nenhuma sessão criada sob
> `BasePlatformTestCase` chega a ter PTY: mesmo passando `deferSessionStartUntilUiShown = false`
> direto ao runner, `ttyConnector` continua `null` depois de 10 s — o processo nasce quando o
> componente é exibido, e no headless nada é exibido. Isso **redefine** T-2.2, T-2.3 e T-2.4: eles
> verificam o encadeamento de `Disposable` que produz (ou vaza) o PTY, que é onde o defeito
> nasceria, e não o processo. A metade observável fica com T-3.1 e T-3.41, no IDE real.
>
> Pelo mesmo motivo T-2.1 **não** usa `ToolWindowManager`: no headless quem responde é
> `ToolWindowHeadlessManagerImpl`, e ele devolve `null` para qualquer id — inclusive "Terminal".
> Um teste escrito sobre ele passaria sem medir nada.

| #          | Cenário                 | O que o teste afirma                                                                                                                                                                       |
| ---------- | ----------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **T-2.1**  | Registro da tool window | O `ToolWindowEP` de id "Claude Code Dock" existe, aponta para `ClaudeToolWindowFactory`, ancora à direita e **não** compartilha factory com a "Terminal" nativa (RF-01)                   |
| **T-2.2**  | Criação de sessão       | `createSession` devolve um `ShellTerminalWidget` (JediTerm) **com listener instalado** — sem ele as ações de split do menu de contexto não existem (D-33)                                 |
| **T-2.3**  | Isolamento de abas      | Duas abas produzem componentes e `Disposable` distintos, uma pane cada (RF-06)                                                                                                            |
| **T-2.4a** | Liberação de recursos   | `removeContent` descarta o `Disposable` da aba — primeiro elo da corrente que mata o PTY (RNF-09, CB-08)                                                                                  |
| **T-2.4b** | Liberação de recursos   | O widget é **filho** desse `Disposable`: descartar o pai descarta o widget. Sem este elo o (a) não serviria de nada                                                                       |
| **T-2.5**  | Ausência do terminal    | A tool window é registrada pelo descritor **opcional** `plugin-terminal.xml` (verificado por `descriptorPath`), carregado só com `org.jetbrains.plugins.terminal` presente (RF-15, CB-10) |
| **T-2.6**  | Engines de terminal     | Em **todos** os `TerminalEngine` a sessão continua JediTerm/CLASSIC (Achado 27, CB-11, R-06)                                                                                              |

**Sobre T-2.5.** Desabilitar um plugin empacotado dentro do processo de teste não é possível, então
o que se verifica é o mecanismo que produz o efeito, não o efeito. Mover a declaração para o
`plugin.xml` principal quebraria IDEs sem terminal — e derruba este teste. O `descriptorPath` é
lido por reflexão **sem** `runCatching`: se a plataforma parar de expor o método num upgrade, é
melhor o teste explodir do que passar calado.

**Sobre T-2.6, que é o mais valioso do conjunto.** É a premissa que o SPEC errou por três versões
— CB-26, CB-36, CB-47 e R-15 supunham que o engine escolhido pelo usuário valia para as nossas
abas. Não vale: o engine é propriedade da tool window que cria o widget. O ambiente de teste tem
`REWORKED` como padrão, então **o caso interessante é o normal**, não uma exceção montada.

**Mutação, conforme o Achado 20.** Com a tool window movida para o `plugin.xml` e o id trocado, e
com o widget pendurado no `project` em vez do `Disposable` da aba, reprovam exatamente T-2.1,
T-2.5 e T-2.4b — e só eles. T-2.6 não é mutável por construção: ele fixa comportamento da
plataforma, e seu valor é servir de tripwire a cada upgrade (T-5.3).

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

| #              | Roteiro                                                                                                                                                                                                                                                                 |
| -------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **T-3.1**      | Instalar via _Install Plugin from Disk_; reiniciar; abrir a janela; conversar com o Claude                                                                                                                                                                              |
| **T-3.2**      | Pedir uma edição de arquivo e confirmar que o diff abre no IDE                                                                                                                                                                                                          |
| **T-3.3**      | Selecionar código no editor, usar `Ctrl+Alt+K` do oficial e confirmar que a referência chega à sessão da janela dedicada ⚰️ **INVÁLIDO — reescrito por DEF-08.** Pedia que o `Ctrl+Alt+K` do oficial focasse a nossa janela, o que o bytecode mostra ser impossível: `focusClaudeInTerminal` varre só o `ContentManager` da tool window `"Terminal"`. Substituído por **T-3.59** |
| **T-3.4**      | Executar "Retomar sessão" e confirmar que o seletor do CLI lista sessões anteriores ✅ **aprovado 2026-08-08** |
| **T-3.5**      | Repetir T-3.1 em ao menos dois IDEs distintos (ex.: IntelliJ e GoLand)                                                                                                                                                                                                  |
| **T-3.6**      | Desinstalar o plugin e confirmar que o oficial segue íntegro e funcional                                                                                                                                                                                                |
| **T-3.7**      | _(v1.1)_ Na janela dedicada, rodar `/usage` e sair com `Esc`; conferir que o foco **não** vai para o editor (RF-17)                                                                                                                                                     |
| **T-3.8**      | _(v1.1)_ Fechar a última aba e confirmar que os links "Nova sessão"/"Retomar sessão" aparecem e funcionam (RF-18, CB-20)                                                                                                                                                |
| **T-3.9**      | _(v1.1)_ Definir `CLAUDE_CONFIG_DIR` em Settings, abrir nova sessão e conferir `echo $CLAUDE_CONFIG_DIR` no terminal; confirmar que `CLAUDE_CODE_SSE_PORT` continua presente (RF-19)                                                                                    |
| **T-3.10**     | _(v1.2)_ Conversar, acionar "Copiar Sessão" e colar num editor: o texto traz a conversa do buffer, sem bloco de linhas vazias no fim nem espaços à direita (RF-21)                                                                                                      |
| **T-3.11**     | _(v1.2)_ Acionar "Exportar Conversa" com o `claude` rodando: o seletor do `/export` aparece dentro da sessão e produz a transcrição. Nesta máquina o destino precisa ser arquivo — o clipboard do CLI exige `wl-copy`/`xclip`/`xsel`, ausentes (RF-22, R-12)            |
| **T-3.12**     | _(v1.2)_ Com a última aba fechada, acionar as duas ações novas: cada uma notifica "Nenhuma sessão aberta" e nada quebra (RF-23, CB-24)                                                                                                                                  |
| **T-3.13**     | ~~_(v1.3)_ Verificar à mão se `/export <caminho>` aceita caminho absoluto~~ ✅ **Dispensado** — respondido por leitura do binário (R-13), com mais precisão do que o teste manual daria                                                                                 |
| **T-3.14**     | _(v1.3)_ Conversa longa, com a janela redimensionada no meio: "Copiar Conversa" traz a conversa **uma única vez**, sem banner repetido nem barra de status; o temporário não fica em `/tmp` (RF-24, RF-25, DEF-01)                                                      |
| **T-3.15**     | _(v1.3)_ Selecionar um trecho com o mouse: o botão flutuante aparece, copia só o trecho e some ao desfazer a seleção (RF-26, CB-30)                                                                                                                                     |
| **T-3.16**     | _(v1.3)_ Com o botão flutuante na tela, digitar na sessão: o foco **não** foi roubado pelo popup (RF-26)                                                                                                                                                                |
| ~~**T-3.17**~~ | ~~_(v1.3)_ Ligar "Saída plana" e comparar~~ ✅ **EXECUTADO e conclusivo** — a flag funciona, mas troca a caixa de input por um `$` indistinguível de prompt de shell. RF-27 removido em v1.4                                                                            |
| **T-3.18**     | _(v1.4)_ Abrir aba, mudar o tema do IDE (claro ↔ escuro) e conferir que o respiro acompanha o fundo do terminal, sem faixa de cor antiga (RF-28, CB-33)                                                                                                                 |
| **T-3.19**     | _(v1.4)_ Abrir aba e observar a partida: a capa cobre o prompt e o eco, sai sozinha, e o rodapé do CLI **não** aparece quebrado nem redesenha ao sair (RF-29)                                                                                                           |
| **T-3.20**     | _(v1.4)_ Abrir aba em diretório novo, onde o CLI pergunta "confia nesta pasta?": o diálogo continua respondível depois de a capa sair (CB-34)                                                                                                                           |
| **T-3.21**     | _(v1.5)_ Menu "Áudio" no cabeçalho fica desabilitado quando: (a) `piper` não encontrado; (b) modelo não configurado; verificar tooltip em cada caso (RF-31, CB-39)                                                                                                      |
| **T-3.22**     | _(v1.5)_ Durante reprodução de áudio, clicar em "Pausar": som para e botão muda para "Retomar". Clicar "Retomar": som continua. Clicar "Parar": som cessa (RF-31, CB-40)                                                                                                |
| **T-3.23**     | _(v1.5)_ Abrir Settings > Tools > Claude Code Dock, verificar novos campos "Executável do Piper" e "Modelo de voz (.onnx)", alterá-los, aplicar, e verificar que menu "Áudio" se habilita/desabilita conforme modelo (RF-32)                                            |
| **T-3.26**     | _(v1.5)_ Selecionar e reproduzir um grande trecho (páginas de código): síntese demora mas UI fica responsiva (RNF-20); timeout após 20s e notificação se síntese não terminar (CB-38) · _(v1.9.1)_ o prazo passou a existir de fato; T-1.55 cobre a parte automatizável |
| **T-3.27**     | _(v1.5)_ Iniciar síntese de trecho A, e antes de terminar selecionar e iniciar trecho B: síntese de A é cancelada, B começa novo (RNF-23) · _(v1.9.1)_ **antes deste release este roteiro reprovaria** — o cancelamento era inerte; T-1.56 cobre a parte automatizável  |
| **T-3.28**     | _(v1.6)_ Selecionar texto e conferir que o popup mostra **dois** botões, copiar e exportar, sem cobrir a seleção nem roubar o foco da sessão (RF-33)                                                                                                                    |
| **T-3.29**     | _(v1.6)_ Clicar em exportar: o diálogo abre na raiz do projeto com o nome sugerido preenchido; salvar e conferir que o arquivo tem **só o trecho**, sem banner nem barra de status (RF-33, RF-34)                                                                       |
| **T-3.30**     | _(v1.6)_ Cancelar o diálogo: nada é gravado e **nenhuma** notificação aparece (CB-44)                                                                                                                                                                                   |
| **T-3.31**     | _(v1.6)_ Escolher destino em diretório sem permissão (ex.: `/`): a notificação de erro aparece e a sessão continua utilizável (CB-46, Fluxo J)                                                                                                                          |
| **T-3.32**     | _(v1.6)_ Exportar duas vezes seguidas sem renomear: os nomes sugeridos diferem pelo carimbo de tempo, sem sobrescrever o primeiro arquivo (RF-34)                                                                                                                       |
| **T-3.33**     | _(v1.6)_ Conferir que "Exportar Conversa" no cabeçalho continua exportando a conversa inteira, inalterada (RF-35)                                                                                                                                                       |
| **T-3.34**     | _(v1.7)_ Botão direito na sessão: conferir que "Split Right" e "Split Down" **aparecem** no menu de contexto (não apareciam antes do listener) e que dividem na direção certa (RF-36, D-33) ✅ **aprovado 2026-08-08** — corroborado no log por uma pane com `columns=59` contra `columns=126` das irmãs |
| **T-3.35**     | _(v1.7)_ Menu "Dividir" no cabeçalho: "À direita" e "Abaixo" produzem o mesmo resultado do menu de contexto (RF-36) ✅ **aprovado 2026-08-08** |
| **T-3.36**     | _(v1.7)_ Cada pane roda um Claude Code independente: pedir algo em uma não afeta a outra, e as duas mantêm a integração (diff abre no IDE) (RF-37) ⏳ **NÃO executado** — é o único do bloco de split ainda pendente ✅ **aprovado 2026-08-08** — as **duas** panes mostraram `In test.md` ao mesmo tempo, prova de que ambas falam com o servidor MCP; a edição foi aplicada e o editor atualizou |
| **T-3.37**     | _(v1.7)_ Dividir três ou mais vezes, aninhando: as panes se acomodam e os divisores são arrastáveis (RF-36, CB-54) ✅ **aprovado 2026-08-08** |
| **T-3.38**     | _(v1.7)_ Clicar numa pane e acionar "Copiar Conversa"/"Exportar Conversa" no cabeçalho: a ação age sobre a pane **em foco**, não sobre outra (RF-38) ✅ **aprovado 2026-08-08** |
| **T-3.39**     | _(v1.7)_ "Close Session" numa pane dividida: a irmã ocupa o espaço, o processo daquela pane morre e o da irmã **continua vivo** (RF-39, RNF-28) ✅ **aprovado 2026-08-08** |
| **T-3.40**     | _(v1.7)_ "Close Session" na última pane fecha a aba inteira (RF-39) ✅ **aprovado 2026-08-08** |
| **T-3.41**     | _(v1.7)_ Fechar a aba com várias divisões: conferir com `ps aux \| grep claude` que **nenhum** processo sobrou (RF-40, CB-53) ✅ **aprovado 2026-08-08** — verificado por medição: nada reparentado ao init depois do fechamento |
| **T-3.42**     | _(v1.7.1)_ Menu "Dividir" → "Fechar divisão": a pane em foco sai, a vizinha ocupa o espaço, e a aba continua aberta (RF-41) ✅ uso real desde a release 0.7.0 |
| **T-3.43**     | _(v1.7.1)_ Depois de "Fechar divisão", acionar "Copiar Conversa" **sem clicar em nada**: a ação encontra a sessão sobrevivente, já com o foco (RF-42) ✅ uso real desde a release 0.7.0 |
| **T-3.44**     | _(v1.7.1)_ "Fechar divisão" numa aba sem divisão: aparece o aviso e a aba **não** é fechada (RF-41) ✅ uso real desde a release 0.7.0 |
| **T-3.45**     | _(v1.8)_ "Trocar de lado": as duas sessões trocam de posição **sem reiniciar** — o scrollback e o processo de cada uma seguem intactos (RF-43) ✅ uso real desde a release 0.7.0 |
| **T-3.46**     | _(v1.8)_ "Girar divisão": lado a lado vira empilhado e vice-versa, e o CLI redesenha para o novo tamanho sem quebrar o rodapé (RF-43, CB-54) ✅ uso real desde a release 0.7.0 |
| **T-3.47**     | _(v1.8)_ Numa aba com três panes aninhadas, trocar e girar agem sobre a divisão **da pane em foco**, não sobre a externa (RF-43) ✅ uso real desde a release 0.7.0 |
| **T-3.48**     | _(v1.8.2)_ Com quatro sessões numa aba, "Fechar esta sessão" na **primeira**: as outras três seguem, e **sem trocar de aba** o cabeçalho continua inteiro — dividir, nova sessão e o menu abrem normalmente (DEF-05, DEF-04)                                            |
| **T-3.52**     | _(v1.8.2)_ "Fechar todas as sessões" numa aba com quatro divisões: a aba some e `ps aux \| grep claude` não mostra processo sobrando (RF-46, RF-40)                                                                                                                     |
| **T-3.53**     | _(v1.8.2)_ Ler os dois itens do menu sem contexto e conferir que o nome já diz quantas sessões cada um encerra (RF-46, DEF-06)                                                                                                                                          |
| **T-3.49**     | _(v1.8.1)_ "Fechar divisão" numa aba com duas sessões: a aba **não** ganha o sufixo "(encerrado)", e a sessão sobrevivente segue viva (RF-44, DEF-03)                                                                                                                   |
| **T-3.50**     | _(v1.8.1)_ Numa aba dividida, encerrar uma sessão pelo `/exit`: a aba **não** é marcada enquanto a vizinha viver; encerrando as duas, aí sim aparece "(encerrado)" (RF-44, Q-26)                                                                                        |
| **T-3.51**     | _(v1.8.1)_ Numa aba sem divisão, o menu "Dividir" mostra "Trocar de lado", "Girar divisão" e "Fechar divisão" **cinzas**, e as duas de dividir habilitadas (RF-45)                                                                                                      |
| **T-3.54**     | _(v1.9)_ Selecionar um trecho e acionar "Tocar seleção": **sai som**. Antes de v1.9 não saía nada e nada era avisado (RF-48, DEF-07)                                                                                                                                    |
| **T-3.55**     | _(v1.9)_ Trocar para 2x no menu e tocar o mesmo trecho: a fala sai visivelmente mais rápida; voltar a 1x devolve o ritmo original (RF-47)                                                                                                                               |
| **T-3.56**     | _(v1.9)_ Trocar a velocidade **na tela de Settings** e conferir que o menu "Áudio" reflete a escolha, e vice-versa: as duas UIs mostram a mesma lista e o mesmo item marcado (RF-47)                                                                                    |
| **T-3.57**     | _(v1.9)_ Fechar e reabrir o IDE: a velocidade escolhida persiste no `claude-code-dock.xml` (RF-47)                                                                                                                                                                      |
| **T-3.58**     | _(v1.9)_ Acionar "Tocar seleção" **sem seleção** e com a aba vazia: aparece aviso nos dois casos, nenhum silêncio (RF-48)                                                                                                                                               |
| **T-3.59**     | _(v1.9.3)_ **Substitui T-3.3.** Com uma sessão viva na janela dedicada, selecionar código e acionar `Ctrl+Alt+K`: o foco vai para o Terminal nativo (esperado, DEF-08) — **a pergunta é se a referência do trecho aparece na nossa pane**. Voltar para a janela dedicada **sem** tocar na nativa e conferir. Responde Q-30 ✅ **executado 2026-08-08** — chegou, e chegou nas **duas** panes |
| **T-3.60**     | _(v1.9.4)_ Com a aba **dividida**, selecionar código e acionar "Enviar Seleção para o Claude Code" pelo menu de contexto do editor: a menção `@arquivo#Lx-y` aparece **só na pane em foco** — a diferença para o `Ctrl+Alt+K`, que entrega às duas —, a janela vem à frente e o cursor fica na pane certa (RF-49) ✅ **aprovado 2026-08-08** — print mostra a menção `@test.md#L3` **só na pane esquerda**, a direita vazia; e saiu `#L3`, não `#L3-4`, com a barra de status em `3:12 (30 chars)`: a correção de `inclusiveEndLine` vale no editor real, não só em T-1.60 |
| **T-3.62**     | _(v1.10)_ **Mede o Achado 33, e só depois de `sudo apt install wl-clipboard`.** Tirar um print screen, focar a sessão da janela dedicada e teclar `Ctrl+V`. Se a imagem for anexada, **não há RF-52 a implementar** e o caso fecha como ambiente. Se nada acontecer, conferir no terminal comum: funcionando lá e não aqui, o `Ctrl+V` está sendo consumido antes do PTY, e **aí** RF-52 vira código |
| **T-3.63**     | _(v1.10)_ Selecionar um trecho, clicar no play do popup: **sai som**, e o menu "Áudio" passa a oferecer pausa — a prova de que o estado é compartilhado e não duplicado (RF-50, RNF-32) |
| **T-3.64**     | _(v1.10)_ Com o Piper **desconfigurado** de propósito, clicar no play do popup: aparece **notificação**. É o teste que distingue "não configurado" de "quebrado em silêncio", e que só existe por causa do DEF-07 (RF-50, RNF-33) |
| **T-3.65**     | _(v1.10)_ Clicar em "Uso" no cabeçalho: a tela de uso do CLI abre **na sessão em foco**; `Esc` (ou `Ctrl+Backspace`) sai dela e devolve o prompt (RF-51, RF-17) |
| **T-3.66**     | _(v1.10)_ Com a aba dividida, selecionar a pane da direita e clicar em "Uso": a tela abre **nela**, não na irmã — mesma garantia de RF-38 |
| **T-3.61**     | _(v1.9.5)_ **Mede o Achado 32.** Numa pane **sem** o indicador de integração no rodapé (`In <arquivo>`), rodar `echo $CLAUDE_CODE_SSE_PORT`: se sair **`0`**, a causa da desintegração é a corrida do `getOrDefault`, e Q-31 fecha. Conferir numa pane **com** o indicador que sai a porta real — é o controle que impede concluir pelo motivo errado ⚠️ **executado 2026-08-08 — não reproduziu.** As duas panes ficaram integradas e o `echo` deu a **mesma porta real (`33471`)** nas duas. Isso **valida o controle** (o customizer alcança as panes de split em produção, não só em T-4) e **deixa a hipótese sem medição**: o caso da pane desintegrada não ocorreu nesta sessão · **2ª execução, com a IDE reiniciada e a pane nascida na inicialização: também não reproduziu** — mesma porta real nas duas. Hipótese arquivada |

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

**CA-20 — Pausa e retomada** _(v1.5, RF-31)_

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

**CA-25 — Exportação do trecho selecionado** _(v1.6, RF-33)_

- **Given** uma sessão com texto selecionado pelo mouse
- **When** o usuário clica no botão de exportar do popup e confirma o destino
- **Then** o arquivo contém **apenas o trecho selecionado**, normalizado, e nada foi enviado à sessão

**CA-26 — Cancelamento é caminho normal** _(v1.6, RF-34, CB-44)_

- **Given** o diálogo de salvar aberto
- **When** o usuário cancela
- **Then** nenhum arquivo é criado e nenhuma notificação é exibida

**CA-27 — Os dois destinos convivem** _(v1.6, RF-35)_

- **Given** o plugin instalado com a v1.6
- **When** o usuário aciona "Exportar Conversa" no cabeçalho
- **Then** a conversa inteira é exportada pelo `/export`, exatamente como antes — o botão do popup não o substituiu

**CA-28 — Duas sessões visíveis ao mesmo tempo** _(v1.7, RF-36, RF-37)_

- **Given** uma aba com uma sessão do Claude Code
- **When** o usuário divide à direita (pelo menu de contexto ou pelo cabeçalho)
- **Then** a aba passa a mostrar duas sessões lado a lado, cada uma com seu próprio Claude Code rodando e com a integração ativa

**CA-29 — A ação segue o foco** _(v1.7, RF-38)_

- **Given** uma aba dividida em duas sessões
- **When** o usuário clica em uma delas e aciona "Copiar Conversa" no cabeçalho
- **Then** a conversa copiada é a da sessão em que ele clicou

**CA-30 — Fechar uma pane não derruba a irmã** _(v1.7, RF-39, RF-40)_

- **Given** uma aba dividida em duas sessões
- **When** o usuário fecha uma delas
- **Then** a outra ocupa o espaço inteiro e continua rodando; fechando a aba depois, nenhum processo `claude` sobra

**CA-31 — A fala obedece à velocidade escolhida** _(v1.9, RF-47)_

- **Given** o Piper configurado e a velocidade em 1x
- **When** o usuário abre o menu "Áudio" → "Velocidade", escolhe 2x e toca um trecho selecionado
- **Then** o item 2x aparece marcado, o rótulo do grupo passa a "Velocidade (2x)", a fala sai mais
  rápida que antes, e a escolha continua valendo depois de reabrir o IDE

---

**CA-30 — Tocar o trecho selecionado pelo popup** _(v1.10, RF-50)_

- **Given** uma sessão com um trecho selecionado e o Piper configurado
- **When** o usuário clica no terceiro botão do popup flutuante
- **Then** o áudio do trecho é reproduzido, e o menu "Áudio" do cabeçalho passa a oferecer pausa e
  parar — o mesmo estado, não um segundo

**CA-31 — Play sem Piper avisa** _(v1.10, RF-50/RNF-33)_

- **Given** o Piper não configurado
- **When** o usuário clica no botão de tocar do popup
- **Then** uma notificação explica que o Piper não está configurado, e nada acontece em silêncio

**CA-32 — Consultar o uso pelo cabeçalho** _(v1.10, RF-51)_

- **Given** uma sessão viva na janela dedicada
- **When** o usuário clica em "Uso", logo depois de "Retomar Sessão"
- **Then** o `/usage` é enviado à sessão **em foco** e a tela de uso do CLI aparece nela

**CA-33 — Uso sem sessão avisa** _(v1.10, RF-51)_

- **Given** a janela dedicada sem nenhuma sessão aberta
- **When** o usuário clica em "Uso"
- **Then** uma notificação avisa que não há sessão, e nada é escrito em PTY nenhum

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

| #        | Questão                                                                                                                                                    | Situação                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| -------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Q-01** | ~~O `TerminalCustomizer` da Anthropic de fato alcança widgets criados fora da tool window nativa?~~                                                        | ✅ **RESOLVIDO em 2026-08-01.** Sim. Validado por `TerminalCustomizerReachTest` (T-4) com teste de controle. Ponto exato: `configureStartupOptions` aplica os customizers                                                                                                                                                                                                                                                                                                                  |
| **Q-02** | Duas sessões simultâneas conectadas ao mesmo servidor MCP causam ambiguidade de atribuição (ex.: qual sessão recebe um diff)?                              | Em aberto (R-05). Investigar durante T-4 · ✅ **RESPONDIDA em 2026-08-08, pelo uso real (T-3.59).** **Ninguém "possui".** Com duas panes abertas, um único `Ctrl+Alt+K` entregou `@test.md#L3` às **duas ao mesmo tempo**. `sendAtMentionedNotifications` é broadcast: o protocolo não tem noção de sessão-alvo, então a desambiguação que esta pergunta procurava **não existe na camada do plugin oficial** — não é que ela erre, é que ela não foi modelada. Consequência prática: a pane que não era o destino fica com um @-mention parado no input |
| **Q-03** | Qual a semântica exata de `CLAUDE_CODE_JETBRAINS_PLUGIN_HIDE_BUTTON`?                                                                                      | String encontrada no jar oficial; **semântica não verificada**. Não usar antes de confirmar                                                                                                                                                                                                                                                                                                                                                                                                |
| **Q-04** | ~~O engine `REWORKED` se comporta como o `CLASSIC` fora da tool window nativa?~~                                                                           | ✅ **RESOLVIDO em 2026-08-03.** Falsa alarme. O problema foi o ESC constant vazio em 097266b (Achado 20), não divergência de engine. Ambos (CLASSIC no WebStorm + REWORKED no IntelliJ) trabalhavam com o hotfix corrigido · _(v1.9.2)_ **A razão registrada estava errada.** Não é que "ambos os engines funcionavam": as abas deste plugin **nunca** foram `REWORKED` — o engine é propriedade da tool window que cria o widget (Achado 27). O `REWORKED` do usuário valia para o Terminal nativo dele, não para nós. A pergunta não tinha objeto, e T-2.6 é o teste que impede de ela voltar a ser feita |
| **Q-05** | Vale ocultar o botão/ação do plugin oficial para evitar confusão de dois pontos de entrada?                                                                | Decisão de produto, adiada até haver uso real. Depende de Q-03                                                                                                                                                                                                                                                                                                                                                                                                                             |
| **Q-06** | Suportar Remote Development e WSL no futuro?                                                                                                               | Fora de escopo agora (CB-16, CB-17). Reavaliar conforme necessidade                                                                                                                                                                                                                                                                                                                                                                                                                        |
| **Q-07** | A tool window deve restaurar automaticamente as sessões ao reabrir o projeto?                                                                              | Não previsto. `isTerminalSessionPersistent` existe na plataforma, mas persistência acrescenta complexidade sem demanda comprovada                                                                                                                                                                                                                                                                                                                                                          |
| **Q-08** | Qual o `since-build` mínimo realmente testável?                                                                                                            | Definido como `252` por conservadorismo; **testado apenas em `262`**. Builds anteriores não foram verificadas                                                                                                                                                                                                                                                                                                                                                                              |
| **Q-09** | _(v1.1)_ Vale permitir `CLAUDE_CONFIG_DIR` **por aba**, e não só por projeto?                                                                              | Adiado. Em IDEs JetBrains uma janela é um projeto, então o escopo atual já atende o pedido. Por aba exigiria diálogo a cada "Nova sessão" — reavaliar se houver demanda                                                                                                                                                                                                                                                                                                                    |
| **Q-10** | ~~_(v1.1)_ O `TerminalEscapeKeyListener` se comporta igual no engine `REWORKED`?~~                                                                         | ✅ **RESOLVIDO em 2026-08-03.** Falsa alarme. Q-04 acima. O hotfix 097266b tinha o ESC constant vazio — ao ser restaurado para `""`, ambos os engines (CLASSIC e REWORKED) funcionam normalmente (Achado 20) · _(v1.9.2)_ **A razão registrada estava errada.** Não é que "ambos os engines funcionavam": as abas deste plugin **nunca** foram `REWORKED` — o engine é propriedade da tool window que cria o widget (Achado 27). O `REWORKED` do usuário valia para o Terminal nativo dele, não para nós. A pergunta não tinha objeto, e T-2.6 é o teste que impede de ela voltar a ser feita |
| **Q-11** | _(v1.1)_ `CLAUDE_CONFIG_DIR` deveria ser versionável (`.idea/`) em vez de ficar no workspace?                                                              | Decidido pelo workspace (RNF-05). Reavaliar só se surgir caso de config dir relativo ao repositório, compartilhável pelo time                                                                                                                                                                                                                                                                                                                                                              |
| **Q-12** | ~~_(v1.2)_ Vale passar `[filename]` ao `/export`?~~                                                                                                        | ✅ **RESOLVIDO em v1.3.** Sim, e deixou de ser conveniência: é a única forma de entregar a cópia sem duplicação (DEF-01). Virou RF-24, com R-13 a verificar primeiro                                                                                                                                                                                                                                                                                                                       |
| **Q-14** | _(v1.3)_ Reimplementar a UI como visualizador de markdown, dirigindo o CLI por `stream-json`?                                                              | **Analisado e recusado.** Viável tecnicamente, mas é outro produto: descarta o terminal e todo o comportamento interativo que ele dá de graça, e acopla a um formato JSON sem contrato de estabilidade. Ver [Fora de Escopo](#fora-de-escopo) e Achado 17                                                                                                                                                                                                                                  |
| **Q-15** | ~~_(v1.3)_ Qual prazo limite para o `/export` de RF-24 responder?~~                                                                                        | ✅ **RESOLVIDO na implementação.** 20 s, com sondagem do arquivo. Nenhum estouro observado no uso real; revisitar só se aparecer conversa que não caiba nesse prazo                                                                                                                                                                                                                                                                                                                        |
| **Q-16** | _(v1.4)_ Trocar o prazo fixo da capa (RF-29) por detecção de que o CLI já pintou?                                                                          | **Avaliado, não implementado.** É viável: `JediTermWidget.getTerminalTextBuffer()` e `addModelListener` são públicos, e `getScreenLines()` dá a tela como texto. Custo: acoplar-se ao texto do banner do CLI, que não tem contrato — some com a rede de segurança do prazo. Ver R-16                                                                                                                                                                                                       |
| **Q-17** | _(v1.4)_ Vale reintroduzir a capa em cima de uma partida sem eco (a alternativa de D-20), ficando só como acabamento?                                      | Em aberto. Combinadas, o pior caso da capa deixaria de ser "eco visível" e passaria a ser "tela vazia por um instante" — o chute do prazo ficaria inofensivo                                                                                                                                                                                                                                                                                                                               |
| **Q-18** | _(v1.5)_ Seleções muito grandes (>N caracteres) devem desabilitar o botão play, ou apenas retornar timeout na síntese?                                     | Adiado. Decisão inicial: deixar o botão ativo e retornar erro/timeout em síntese longa. Se virar problema, adicionar heurística para desabilitar play se seleção > threshold                                                                                                                                                                                                                                                                                                               |
| **Q-19** | _(v1.5)_ Vale cachear o resultado de `canSynthesize()` para evitar checagem de arquivo a cada milissegundo durante seleção?                                | Sim, mas como? Decidido em design: cache é atualizado a cada N segundos (constante configurável ~30s) ou no evento de mudança de configuração. Reavaliar latência se implementação demonstrar problema                                                                                                                                                                                                                                                                                     |
| **Q-20** | _(v1.5)_ O timeout da síntese (padrão 20s) deve ser configurável pelo usuário?                                                                             | Não nesta versão. Registrado como constante em `ClaudePiperPlayback`, ajustável por alguém que leia código. Reavaliar se surgirem modelos que rotineiramente ultrapassam 20s                                                                                                                                                                                                                                                                                                               |
| **Q-13** | _(v1.2)_ A cópia deveria respeitar a seleção do mouse quando houver uma, em vez de sempre copiar tudo?                                                     | Adiado. `Ctrl+C`/`Ctrl+Shift+C` já cobrem a seleção; o botão existe justamente para o caso que o CLASSIC não resolve. `JBTerminalWidget.getSelectedText()` existe se mudarmos de ideia                                                                                                                                                                                                                                                                                                     |
| **Q-21** | _(v1.6)_ Depois de gravar o trecho, vale abrir o arquivo no editor?                                                                                        | Adiado. `FileEditorManager.openFile` custa duas linhas, mas gravar e abrir são intenções diferentes — quem exporta para colar em outro lugar não quer uma aba nova. Reavaliar se o uso real mostrar que abrir é o que sempre se faz em seguida                                                                                                                                                                                                                                             |
| **Q-22** | _(v1.6)_ O trecho deveria sair envolvido em cerca de código markdown, já que o destino é `.md`?                                                            | Adiado. O `.md` é sufixo de conveniência, herdado de RF-24, não uma promessa de formatação. Envolver em cerca exigiria decidir a linguagem e quebraria quem exporta prosa. Ver R-24                                                                                                                                                                                                                                                                                                        |
| **Q-23** | _(v1.6)_ Vale lembrar o último diretório usado, em vez de sempre sugerir a raiz do projeto?                                                                | Adiado. A raiz do projeto é o palpite certo na maioria dos casos e não custa persistência nenhuma. Guardar o último diretório significa mais um campo em settings — só com demanda real                                                                                                                                                                                                                                                                                                    |
| **Q-24** | _(v1.7)_ A divisão deveria oferecer "retomar sessão" (`--resume`), e não só sessão nova?                                                                   | Adiado. Dividir hoje abre sempre uma sessão nova. Retomar dentro da divisão exigiria um submenu por direção (4 itens) — reavaliar se o uso mostrar que dividir para retomar é comum                                                                                                                                                                                                                                                                                                        |
| **Q-25** | _(v1.7)_ Vale navegar entre panes por teclado (`gotoNextSplitTerminal` do listener)?                                                                       | Adiado deliberadamente. O `default` da interface devolve `false`, então a ação nem aparece — custo zero por não implementar. Implementar exige ordenar as panes, que a árvore não dá de graça                                                                                                                                                                                                                                                                                              |
| **Q-26** | ~~_(v1.7)_ Com a aba dividida, o que o título "(encerrado)" deveria significar?~~                                                                          | ✅ **RESOLVIDO em v1.8.1 pelo uso real.** Significa "não há mais sessão viva nesta aba" — contando as panes na árvore, que era a saída descartada como cara em v1.7 e custou seis linhas. Virou RF-44, depois de DEF-03 mostrar o oposto na prática                                                                                                                                                                                                                                        |
| **Q-29** | _(v1.9)_ Vale aplicar a nova velocidade à fala **em curso**, e não só à próxima?                                                                           | **Recusado, com o motivo no mecanismo.** O piper sintetiza o áudio inteiro antes de tocar (`synthesize` lê todo o stdout e só então o `Clip` abre): não há stream a reajustar. Aplicar no meio seria re-sintetizar do zero e reposicionar por frame — fila e posição, exatamente o que RNF-23 mantém fora. O custo real é baixo: a fala típica dura segundos, e parar e tocar de novo já resolve                                                                                           |
| **Q-30** | ~~_(v1.9.3)_ O trecho enviado por `Ctrl+Alt+K` chega à sessão da janela dedicada?~~ | ✅ **RESPONDIDA em 2026-08-08 por T-3.59: chega.** O `@test.md#L3` apareceu na nossa pane com `1 line selected`. **Logo DEF-08 é ergonomia, não integração** — o conteúdo atravessa, só o foco vai para a janela errada. Rebaixa a prioridade do defeito e muda o conserto plausível: não é preciso tocar em protocolo, basta uma ação nossa |
| **Q-32** | _(v1.10)_ Instalado o `wl-clipboard`, o `Ctrl+V` com imagem chega ao CLI dentro da janela dedicada, ou é consumido antes pela ação de colagem da plataforma? | Em aberto, e é o **único** ponto não medido do Achado 33. A ordem em `JBTerminalPanel.handleKeyEvent` está lida (pre-handlers → escape listener → JediTerm), mas ler a ordem não diz quem consome o evento na prática. **Decide se RF-52 vira código ou é arquivada.** Mede-se com T-3.62, que exige o pacote instalado primeiro |
| **Q-31** | _(v1.9.4)_ Por que uma pane às vezes **não conecta** ao servidor MCP, ficando sem o indicador `In <arquivo>` e sem receber o `Ctrl+Alt+K`? | Em aberto. Observado em T-3.60: das duas panes, só a segunda estava conectada. Hipóteses não medidas: corrida entre a partida da sessão e o servidor do plugin oficial, ou sessão criada antes de o servidor subir. **Afeta RF-37**, que assume integração em todas as panes — e T-3.36 já mostrou as duas conectadas, então não é impossível, é intermitente · 🔍 **Mecanismo identificado em 2026-08-08 (Achado 32)**: `CLAUDE_CODE_SSE_PORT` cai em `0` por `getOrDefault`, e a mitigação de corrida do oficial só alcança a tool window `"Terminal"`. **Falta medir** — T-3.61 · ⚠️ **T-3.61 não reproduziu** (2026-08-08): duas panes integradas, mesma porta real `33471`. Controle passou; a hipótese continua **sem medição** · ⚰️ **ARQUIVADA em 2026-08-08** após **duas** não-reproduções, a segunda com a condição específica. Causa do caso de T-3.60 desconhecida; `deferSessionStartUntilUiShown` é a explicação provável de por que não nos atinge |
| **Q-28** | _(v1.8)_ Vale arrastar panes com o mouse para reorganizá-las, como o editor faz com as abas?                                                               | **Avaliado e adiado, com o levantamento feito.** Mecanismo existe (`DnDSupport`; `DockManager`/`DockContainer`). O que falta é **onde agarrar**: o editor arrasta o rótulo da aba, e as nossas panes não têm aba — a superfície delas é do terminal, onde arrastar é selecionar texto (RF-26). Exigiria barra de título por pane, UI permanente para ação ocasional. RF-43 cobre o uso de 2–4 panes por ações. Reabrir se o uso mostrar aninhamento profundo, onde trocar/girar não bastam |
| **Q-27** | _(v1.7)_ As degradações graciosas de engine (CB-26, CB-36, CB-47, R-15) deveriam ser removidas agora que o Achado 27 provou que a sessão é sempre CLASSIC? | Não. Custam uma linha (`?: return`) e protegem contra a plataforma mudar o retorno de `createTerminalWidget` num upgrade. O que mudou foi a **probabilidade** de R-15, não a decisão                                                                                                                                                                                                                                                                                                       |

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

### Achado 21 — A falha silenciosa do ESC constant _(v1.5, 2026-08-03)_

O plano temeu que síntese de fala no `piper` poderia travar a UI da EDT. A realidade: `ClaudeTtaSessions` executa síntese
via `ApplicationManager.getApplication().executeOnPooledThread { ... }`, mesma API que o projeto já usa para validar o
executável do `claude`. Nenhuma mudança de threading model foi necessária; o padrão existente escala direto.

O hotfix `097266b` (Ctrl+Backspace em vez de Backspace puro) tinha uma falha silenciosa que
quebrou tudo em ambos os engines (CLASSIC no WebStorm e REWORKED no IntelliJ): a lógica estava
correta, mas o `ESC` constant foi acidentalmente esvaziado.

```kotlin
// 679562f (antes):
private const val ESC = ""   // Correto

// 097266b (depois):
private const val ESC = ""   // Vazio!
```

Quando `connector.write(ESC)` era chamado, enviava uma string vazia para o shell. O evento de
teclado era interceptado e consumido corretamente, mas o resultado era inerte. Nenhuma exceção,
nenhum erro — apenas silêncio.

**Por que não foi visto?** A diferença visual é mínima, e o comportamento _funciona
parcialmente_ — o terminal não quebra, o `write()` não lança exceção. A falha é silenciosa.

**Impacto incorreto em Q-04 e Q-10:** ambas as perguntas abertas sobre divergência de engines
foram aparentemente confirmadas como "sim, há divergência", quando na verdade o problema era
anterior — nem tinha a ver com engine.

**Lição registrada:** constantes de bytes/caracteres merecem atenção em revisão. Uma string vazia
é tão fácil de passar quanto um `null` é de notar. Considerar adicionar testes que validem o
**valor** da constante, não só sua existência.

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

### Achado 25 — O nome do recurso sugeria o mecanismo errado _(v1.6)_

O pedido diz "exportar só o trecho selecionado", e no documento já existiam duas ações chamadas
export, **as duas passando pelo `/export` do CLI** (RF-22 e RF-24). O caminho de menor
resistência era escrever a terceira igual às outras: mandar um comando pelo `TtyConnector` e
esperar um arquivo aparecer.

Isso não teria funcionado, e o motivo não é sutil: **o `/export` exporta a conversa**. É o CLI
quem o executa, sobre as mensagens que ele tem em memória, e o único argumento que aceita é o
caminho do destino — verificado no binário ainda na v1.3, quando as funções `azb`/`u0n` foram
lidas para escrever RF-24. A informação necessária já estava neste documento há três rodadas.

O que mudou a resposta foi perguntar **de onde vem o texto**, e não **como as outras exportações
funcionam**. O trecho selecionado já está na mão do plugin desde RF-26 — é o mesmo
`widget.selectedText` que alimenta o botão de copiar. Com o texto em mãos, exportar é gravar um
arquivo: sem PTY, sem temporário, sem sondagem, sem prazo. Das três exportações do plugin, a
mais nova é a de menos peças.

**Lição registrada:** quando um pedido novo usa o nome de um mecanismo que já existe, a primeira
verificação é se ele é mesmo o mecanismo — e não como reusá-lo. A semelhança estava no nome do
recurso ("exportar"), não na natureza do dado. É o mesmo erro de forma do Achado 14 (uma ação
registrada no IDE não é uma capacidade disponível), agora do lado do CLI.

**Efeito colateral bom:** por não escrever no PTY, RF-33 é a primeira função de saída do plugin
que funciona com a sessão ocupada, encerrada, ou com o CLI no meio de uma resposta. As outras
duas dependem de um CLI vivo e responsivo.

### Achado 26 — O critério que segura o popup em dois botões _(v1.6)_

Este é o segundo pedido seguido de "mais um botão junto ao de copiar" — o primeiro foi o play
(RF-30), **recusado** em v1.5.1. Aceitar um e recusar o outro precisa de critério declarado,
senão a decisão vira gosto e o popup cresce até atrapalhar.

O critério que separou os dois casos é **capacidade, não simetria**:

| Pedido                   | Já existia em outro lugar da UI?                | Decisão           |
| ------------------------ | ----------------------------------------------- | ----------------- |
| Play do trecho (RF-30)   | Sim — menu "Áudio" no cabeçalho (RF-31)         | Recusado (v1.5.1) |
| Export do trecho (RF-33) | Não — o do cabeçalho exporta a conversa inteira | Aceito (v1.6)     |

Registrado em R-23 com teto explícito de dois botões. O próximo pedido de botão no popup passa
pela mesma pergunta antes de virar RF: **isso existe em algum outro lugar?** Se existir, o lugar
certo já tem dono.

> **Revogado em v1.10 (Achado 34).** A pergunta acima nunca foi feita ao botão de copiar, que
> ela também reprovaria — `Ctrl+C` já copiava. O critério media a existência da capacidade e
> ignorava o custo de alcançá-la. RF-50 aceita o play; o critério em vigor está no Achado 34.

### Achado 27 — Uma premissa carregada por seis rodadas estava errada _(v1.7)_

Desde a v1.2 o documento hedgeia contra "e se a sessão não for JediTerm?": CB-26 ("fora do
CLASSIC o `getText()` devolve vazio"), CB-36, CB-47 e R-15, este último com probabilidade
**Média**. O `HANDOFF.md` chegou a registrar "REWORKED no IntelliJ 2026.2", e Q-04/Q-10 passaram
rodadas em aberto por causa disso.

**Nada disso alcança o nosso caminho.** `AbstractTerminalRunner.startShellTerminalWidget` chama
`createTerminalWidget(...)`, cujo **tipo de retorno é `JBTerminalWidget`** — o widget JediTerm.
Quem escolhe entre `CLASSIC`, `REWORKED` e `NEW_TERMINAL` é o `TerminalToolWindowManager` da tool
window nativa, acima do ponto de entrada que usamos desde a v1.0.

A confusão tem uma explicação simples: o usuário **realmente** roda o Terminal nativo do IntelliJ
em REWORKED. A observação estava certa; o que estava errado foi concluir que isso valia para as
nossas abas. O engine é propriedade da tool window que cria o widget, não do IDE.

**O que muda:** R-15 cai de "Média" para "Baixa"; a redação que sugeria "o recurso pode não estar
disponível para você" era falsa e foi corrigida. **O que não muda:** as guardas continuam, porque
custam uma linha e protegem contra upgrade (Q-27).

**Lição registrada:** uma premissa não verificada não fica mais verdadeira por ser repetida em
seis versões do documento — fica mais **cara**, porque passa a sustentar requisitos, riscos e
perguntas em aberto. Esta atravessou cinco rodadas e só caiu quando um recurso novo dependeu
dela para existir. Vale reler os hedges antigos perguntando "isto ainda é hipótese ou virou fato
não verificado?".

### Achado 28 — O recurso já estava pedindo para ser implementado _(v1.7)_

Split parecia a feature mais cara desta série: divisor, ação, atalho, gerência de layout. A
investigação mostrou o contrário — `ShellTerminalWidget.getActions()` **já cria** as ações de
split, a partir de `getListener()`, e o `JBTerminalWidgetListener` já tem `canSplit`/`split` como
métodos default. Como o nosso widget nunca teve listener, as ações simplesmente não apareciam no
menu de contexto.

Ou seja: o plugin vinha ignorando um pedido que o widget fazia desde a v1.0. Implementar o
listener acendeu de uma vez o split, o "New Session", o "Close Session" e a navegação entre abas
pelo menu de contexto — quatro itens que estavam mortos pelo mesmo motivo.

**Padrão que se repete neste projeto:** a versão barata do recurso costuma ser "descobrir quem
na plataforma já faz isso e ligar o fio". Foi assim em RF-24 (`getText()` já lia o scrollback
inteiro), em RF-33 (o texto já estava na mão) e agora aqui. O contraexemplo é o `Esc` (RF-17),
onde a plataforma trabalhava **contra** — e foi o único que exigiu código de verdade.

**Lição registrada:** antes de desenhar UI para um recurso, procurar se o componente que já
hospedamos tem um ponto de extensão esperando por ele. O custo da busca é de minutos; o de errar
é um recurso inteiro reimplementado ao lado do que a plataforma daria pronto.

### Achado 29 — Entregue e invisível é quase o mesmo que não entregue _(v1.7.1)_

No primeiro uso real do split, o usuário pediu "uma opção de fechar esses splits". **A opção já
existia e estava na tela dele**: o item "Close Tab" (`Ctrl+W`) do menu de contexto chama
`onSessionClosed()`, que a v1.7 já ligava ao fechamento da pane com colapso do splitter — está
inclusive visível na captura que ele mandou.

O problema é o rótulo, e ele não é nosso: "Close Tab" vem da plataforma, pensado para um terminal
em que aba e sessão são a mesma coisa. Numa aba dividida, o nome diz **o oposto** do que a ação
faz. Ninguém arrisca `Ctrl+W` em quatro sessões abertas para descobrir o que acontece.

Isso conecta com o Achado 28 da mesma rodada, que festejou "o listener acende quatro itens de
graça". Metade da comemoração era falsa: os itens acenderam com os **nomes da plataforma**, e um
deles descreve mal o que passou a fazer. Herdar comportamento é de graça; herdar vocabulário não.

**O que mudou:** RF-41 dá o nome honesto ("Fechar divisão") no lugar onde o usuário procura, e
RF-42 conserta o efeito colateral que só apareceu ao olhar de perto — a aba ficava apontando para
a sessão morta, e as ações do cabeçalho paravam de achar sessão até alguém clicar numa pane.

**Lição registrada:** "a plataforma já oferece" responde se a **capacidade** existe, não se o
usuário a encontra. Ao ligar um ponto de extensão herdado, ler os rótulos que vêm junto e
perguntar se descrevem o que a ação faz **no nosso contexto** — no do terminal nativo, "Close
Tab" estava certo.

---

### Achado 33 — o recurso pedido já existia, e faltava um pacote do sistema _(v1.10)_

**Pedido:** colar print screen na janela do plugin. **Resultado da avaliação: não vira RF** — pelo
menos não a parte que resolve o problema.

O caminho de menor resistência era projetar interceptação de `Ctrl+V` no plugin. Teria funcionado,
e teria sido **a resposta certa para a pergunta errada**. A pergunta que mudou tudo foi a que este
documento já aprendeu a fazer no Achado 25: **de onde vem o dado?** Aqui, de `xclip`/`wl-paste` —
lidos no binário do CLI, não deduzidos —, e **nenhum dos dois está instalado nesta máquina**.

**O relato trazia a evidência decisiva e ela quase passou batido:** "o mesmo problema acontece
quando executo claude code no terminal". Nenhuma linha do plugin roda no terminal comum. Uma causa
que alcança os dois ambientes **não pode** estar no plugin — e essa frase, sozinha, já apontava
para fora antes de qualquer código ser lido.

**Lição registrada, e ela é irmã do Achado 17.** Antes de perguntar "conseguimos implementar?", vale
perguntar **"isto já não está implementado, e quebrado por outro motivo?"**. O custo da pergunta é
um `command -v`; o custo de pulá-la teria sido uma feature inteira para substituir uma que já
existe — e que continuaria quebrada no terminal do usuário.

**O que sobrou é honesto e pequeno:** uma pergunta medida (Q-32), um teste que a responde (T-3.62)
e um RF **condicional** (RF-52) que só existe se a medição pedir. Publicar RF-52 como decidido
seria repetir o Achado 30.

### Achado 34 — o teto de dois botões caiu, e o critério que o sustentava também _(v1.10)_

O popup da seleção tinha teto declarado de dois botões (R-23), e o play foi **recusado** duas
vezes: como RF-30 em v1.5.1, e de novo no Achado 26. O critério era **"isso já existe em outro
lugar da UI?"** — e existia, no menu "Áudio".

**O critério estava errado, e dá para dizer exatamente onde.** Ele mede a **existência** da
capacidade e ignora o **custo de chegar até ela**. Pelo mesmo critério, o botão de copiar do popup
nunca deveria ter existido: `Ctrl+C` já copiava. Ele existe porque atalho é invisível para quem
está com a mão no mouse — o próprio comentário do `ClaudeSelectionCopyButton` diz isso, com estas
palavras. **O argumento que salvou o botão de copiar salva o play**, e não foi aplicado a ele.

Há um agravante que só ficou visível depois: entre a recusa de v1.5.1 e hoje, o item "Tocar
seleção" do menu **não funcionava** (DEF-07, consertado só na v1.9). Ou seja, o play foi recusado
por já existir em um lugar onde, de fato, ele não existia.

**Critério novo, mais estreito que o antigo:** entra no popup o que opera **sobre o trecho
selecionado** e cabe em **um clique**. Copiar, exportar e tocar passam; qualquer coisa que precise
de diálogo, submenu ou alvo diferente do trecho, não. **Três é o novo teto** (R-29), e o próximo
pedido passa por essa pergunta antes de virar RF.

**Lição registrada:** um critério declarado não é um critério correto. Este durou três rodadas
porque nunca foi testado contra o caso que ele próprio já tinha aprovado — o botão de copiar.

### Achado 35 — o recurso foi aceito e o formato pedido, recusado _(v1.10)_

O pedido foi "um popup com o resumo de uso". A entrega é **um botão que envia `/usage` à sessão**.
Não é a mesma coisa, e a diferença é deliberada.

**O que separa os dois é onde mora o dado.** O `/usage` não imprime texto: desenha uma tela de TUI.
Não existe subcomando `claude usage` — a lista de comandos foi lida, e não está lá. O
`stats-cache.json` local guarda atividade, não limite de plano. O número que o popup mostraria vem
de `/api/oauth/usage`, autenticado com o token de `~/.claude/.credentials.json`.

**Então o popup tem um preço, e o preço é ler o segredo do usuário.** O plugin passaria a abrir um
arquivo `600` de credenciais e a falar com um endpoint não documentado — contra RNF-04, contra a
regra de segredos do `CLAUDE.md`, e quebrável em qualquer release do CLI. Raspar o buffer do
terminal para montar o popup também não serve: é o DEF-01 de volta (conteúdo repetido a cada
repintura), agora sobre uma tela que muda a cada frame.

**O botão entrega o que o pedido queria de fato** — parar de digitar `/usage` — e custa uma linha.
O popup entregaria a forma, e custaria a superfície de segurança do plugin.

**Lição registrada, e é a mesma do Achado 17 vista de outro ângulo:** quando o formato pedido é
caro e o valor pedido é barato, entrega-se o valor e explica-se a troca. O que não se faz é
implementar o formato caro em silêncio, nem recusar o pedido inteiro porque o formato não cabe.

---

## Anexo — Rastreabilidade das evidências

Toda afirmação técnica sobre o estado atual remonta a uma verificação direta, conforme a regra
"DON'T GUESS, VERIFY" do `CLAUDE.md`.

| Afirmação                                                                                                                                                                                                       | Evidência                                                                                                                                                                     |
| --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Oficial usa `createShellWidget` + `sendCommandToExecute`                                                                                                                                                        | `javap -c` de `com.anthropic.code.plugin.TerminalUtil`                                                                                                                        |
| Oficial prende a sessão à tool window "Terminal"                                                                                                                                                                | literal `"Terminal"` passado a `ToolWindowManager.getToolWindow` em `TerminalUtil`                                                                                            |
| Oficial hospeda servidor MCP                                                                                                                                                                                    | jars `ktor-server-{cio,websockets,sse}`, `kotlin-sdk-jvm-0.4.0`; classes `mcp/tools/*`, `WebSocketMcpServerTransport`                                                         |
| Injeção via `CLAUDE_CODE_SSE_PORT`                                                                                                                                                                              | literal em `TerminalCustomizer.class`; registro em `META-INF/plugin-terminal.xml`                                                                                             |
| Formato do lockfile                                                                                                                                                                                             | arquivos reais em `~/.claude/ide/*.lock`, incluindo um escrito por GoLand                                                                                                     |
| `TerminalToolWindowManager` delega a `startShellTerminalWidget`                                                                                                                                                 | `javap -c` de `TerminalToolWindowManager`                                                                                                                                     |
| `TerminalWidget` é embutível                                                                                                                                                                                    | interface estende `com.intellij.openapi.ui.ComponentContainer`                                                                                                                |
| API de terminal presente na build 262                                                                                                                                                                           | `javap` sobre `plugins/terminal/lib/terminal.jar`                                                                                                                             |
| `TerminalEngine` tem `CLASSIC`/`REWORKED`/`NEW_TERMINAL`                                                                                                                                                        | `javap` de `org.jetbrains.plugins.terminal.TerminalEngine`                                                                                                                    |
| Versões de ambiente                                                                                                                                                                                             | `java -version`, `gradle --version`, `claude --version`, `build.txt`, `product-info.json`                                                                                     |
| IntelliJ Platform Gradle Plugin 2.18.1                                                                                                                                                                          | portal de plugins do Gradle                                                                                                                                                   |
| _(v1.1)_ `Esc` só é entregue ao shell na tool window de id `"Terminal"`                                                                                                                                         | `javap -c` de `com.intellij.terminal.TerminalEscapeKeyListener.shouldSwitchFocusToEditor` e de `JBTerminalWidget.isTerminalToolWindow`                                        |
| _(v1.1)_ `JBTerminalPanel` roda pre-handlers antes do listener de `Esc` e respeita `isConsumed`                                                                                                                 | `javap -c` de `com.intellij.terminal.JBTerminalPanel.handleKeyEvent`                                                                                                          |
| _(v1.1)_ 2026.2 distribui `Terminal.SwitchFocusToEditor` sem atalho padrão                                                                                                                                      | ausência de `keyboard-shortcut` no `META-INF/plugin.xml` do terminal + chaves `escape.behavior.change.notification.*` em `TerminalBundle.properties`                          |
| _(v1.1)_ `CLAUDE_CONFIG_DIR` é variável reconhecida pelo CLI                                                                                                                                                    | literal encontrado no jar do plugin oficial (já registrado no `HANDOFF.md`)                                                                                                   |
| _(v1.1)_ `ToolWindowEx.emptyText`, `ShellStartupOptions.Builder.envVariables`, `JBTerminalPanel.addPreKeyEventHandler` existem em 262                                                                           | compilação bem-sucedida contra `intellijIdea("2026.2")`                                                                                                                       |
| _(v1.2)_ `TerminalWidget.getText()` devolve scrollback + tela no CLASSIC, e string vazia fora dele                                                                                                              | `javap -c` de `JBTerminalWidget.getText(TerminalPanel)`, de `JBTerminalWidget$TerminalWidgetBridge.getText` e do `default` da interface                                       |
| _(v1.2)_ Não há "copiar tudo" no engine CLASSIC                                                                                                                                                                 | `Terminal.SelectAll` só é referenciada em `Terminal.ReworkedTerminalContextMenu` (plugin.xml do terminal); `TerminalSelectAllAction.update` exige `isReworkedTerminalEditor`  |
| _(v1.2)_ `sendCommandToExecute` não serve para falar com um TUI vivo                                                                                                                                            | `javap -c` de `ShellTerminalWidget.executeCommand`: lança `IOException` quando `getTypedShellCommand()` não está vazio                                                        |
| _(v1.2)_ O CLI tem o slash command `/export`                                                                                                                                                                    | string no binário `claude` 2.1.220: `{type:"local-jsx", name:"export", description:"Export the current conversation to a file or clipboard", argumentHint:"[filename]"}`      |
| _(v1.2)_ O destino "clipboard" do `/export` depende de utilitário externo ausente nesta máquina                                                                                                                 | `grep` por `wl-copy`/`xclip`/`xsel`/`pbcopy` no binário + `command -v` (nenhum instalado; `XDG_SESSION_TYPE=wayland`)                                                         |
| _(v1.2)_ Limite do scrollback vem de `terminal.buffer.max.lines.count`                                                                                                                                          | `javap -c` de `JBTerminalSystemSettingsProviderBase.getBufferMaxLinesCount`                                                                                                   |
| _(v1.2)_ `CopyPasteManager.copyTextToClipboard` existe e `Content` é `UserDataHolder`                                                                                                                           | `javap` de `intellij.platform.editor.ui.jar` e de `com.intellij.ui.content.Content`                                                                                           |
| _(v1.3)_ **O `/export` produz saída limpa**: conversa uma única vez, sem caixa de input nem barra de status                                                                                                     | leitura do arquivo real gerado em 2026-08-01 16:23, `2026-08-01-162241-*.md`, 45 linhas                                                                                       |
| _(v1.3)_ O buffer contém a conversa repetida, um frame por repintura                                                                                                                                            | amostra colada pelo usuário: rodapé do 1º bloco marca `⧉ In README.md` e o do 2º, `⧉ In a.txt` — instantes diferentes                                                         |
| _(v1.3)_ `TerminalPanel.selectAll()` é **público** — o que falta é ação registrada, não a capacidade                                                                                                            | `javap` de `com.jediterm.terminal.ui.TerminalPanel` (corrige a redação da v1.2)                                                                                               |
| _(v1.3)_ Existe listener de seleção público: `addSelectionListener(TerminalSelectionChangesListener)`, com `selectionChanged(TerminalSelection)`                                                                | `javap` de `TerminalPanel` e de `TerminalSelectionChangesListener`                                                                                                            |
| _(v1.3)_ Não há conversão célula→pixel pública (`myCharSize` é `protected`); posicionar o botão flutuante exige a posição do mouse                                                                              | `javap -p` de `TerminalPanel`                                                                                                                                                 |
| _(v1.3)_ O CLI suporta `--print --output-format stream-json --input-format stream-json --include-partial-messages`                                                                                              | `claude --help` v2.1.220                                                                                                                                                      |
| _(v1.3)_ `/export <arquivo>` grava **sem UI**, sobrescreve, cria diretórios e acrescenta `.txt` se faltar extensão; o argumento é usado cru (`r.trim()`)                                                        | implementação extraída do binário `claude` 2.1.220 (funções `azb`, `Y5b`, `u0n`)                                                                                              |
| _(v1.3)_ `--ax-screen-reader` não tem restrição a `--print`, então vale em sessão interativa                                                                                                                    | `claude --help`: a descrição não traz a ressalva "(only works with --print)" presente em outras flags                                                                         |
| _(v1.3)_ `JBPopupFactory.createComponentPopupBuilder` + `setRequestFocus/setCancelOnClickOutside/setResizable/setMovable` e `JBPopup.show(RelativePoint)` existem em 262                                        | `javap` de `intellij.platform.ide.jar` e `intellij.platform.ide.core.jar`                                                                                                     |
| _(v1.4)_ **O customizer do plugin oficial devolve o comando intacto** e só mexe no ambiente — injeta `CLAUDE_CODE_SSE_PORT` e também `ENABLE_IDE_INTEGRATION=true`                                              | `javap -c` de `com.anthropic.code.plugin.TerminalCustomizer.customizeCommandAndEnvironment`: o método termina em `aload_3; areturn`                                           |
| _(v1.4)_ `JediTermWidget.getComponent()` devolve **o próprio widget** (`JPanel` com `BorderLayout`); o terminal e a barra ficam num `JLayeredPane` interno                                                      | `javap -c`: `aload_0; areturn`, e o construtor faz `add(myInnerPanel, "Center")`                                                                                              |
| _(v1.4)_ `TerminalPanel.getTerminalSizeFromComponent()` mede o **próprio painel** (`getWidth() - getInsetX()`), e `getInsetX()` devolve a constante `4`                                                         | `javap -c` de `TerminalPanel`                                                                                                                                                 |
| _(v1.4)_ `TerminalPanel.getBackground()` delega a `getWindowBackground()` → `SettingsProvider.getDefaultBackground()`, **recalculado a cada chamada**                                                           | `javap -c` de `TerminalPanel`                                                                                                                                                 |
| _(v1.4)_ Nem `JediTermWidget` nem `JBTerminalWidget` chamam `setBackground`: o `JPanel` fica na cor de painel do tema                                                                                           | `javap -c` das duas classes, sem ocorrência de `setBackground`                                                                                                                |
| _(v1.4)_ `PathEnvironmentVariableUtil.findInPath` usa `EnvironmentUtil.getValue("PATH")` — que **não** continha `~/.local/bin` neste ambiente, gerando falso negativo                                           | `javap -c` de `getPathVariableValue` + a notificação observada com o CLI funcionando                                                                                          |
| _(v1.4)_ `--ax-screen-reader` **não trava a sessão**: o processo segue vivo e o `$` é o prompt de entrada em modo plano                                                                                         | `script -qec` com `timeout 15`: saída `exit=124` (morto pelo timeout) e último byte `$` + `ESC[2G`                                                                            |
| _(v1.4)_ O modo também liga por `CLAUDE_AX_SCREEN_READER` ou pelo setting `axScreenReader`                                                                                                                      | string extraída do binário `claude` 2.1.220 (classe `ytu.isEnabled`)                                                                                                          |
| _(v1.4)_ Um shell interativo com `exec` preserva **variáveis exportadas** (`PATH` completo, `NVM_DIR`, `SDKMAN_DIR`, `PYENV_ROOT`…) e **perde** funções e aliases                                               | `env -i … zsh -i -c 'exec "$0" "$@"' env` sob PTY, comparando nomes; e `command -v` para `sdk`/funções                                                                        |
| _(v1.4)_ Sem PTY, o `.zshrc` do usuário **não** é carregado nem com `-i`                                                                                                                                        | mesmo comando sem `script`: nenhuma invocação resolveu o `claude`                                                                                                             |
| _(v1.4)_ `JBLoadingPanel` cobre com **véu translúcido**: o conteúdo por baixo continua legível                                                                                                                  | observação no IDE — o eco do comando aparecia através da capa                                                                                                                 |
| _(v1.4)_ `JLayeredPane.DEFAULT_LAYER` é `Integer`, e passá-lo direto ao `add` faz o Kotlin escolher o overload de **índice**, ignorando a camada                                                                | teste `T-1.20` falhou afirmando `getLayer(capa) > getLayer(terminal)`                                                                                                         |
| _(v1.4)_ `Box.setLayout` lança `AWTError("Illegal request")`                                                                                                                                                    | três testes de `ClaudeSessionLoading` falharam com essa exceção                                                                                                               |
| _(v1.4)_ `IconUtil.scale(Icon, Double)` está **depreciado** em 262; o substituto é `scale(Icon, Component?, Float)`                                                                                             | warning de compilação                                                                                                                                                         |
| _(v1.4)_ `TerminalProjectOptionsProvider.getShellPath()` é público e síncrono — dá o shell configurado em Settings > Tools > Terminal                                                                           | `javap` de `TerminalProjectOptionsProvider`                                                                                                                                   |
| _(v1.4)_ `JediTermWidget.getTerminalTextBuffer()`, `TerminalTextBuffer.addModelListener` e `getScreenLines()` são públicos — base para Q-16                                                                     | `javap` de `JediTermWidget` e `TerminalTextBuffer`                                                                                                                            |
| _(v1.5)_ Piper TTS está instalado e acessível                                                                                                                                                                   | `which piper`, `piper --help`, `pip show piper-tts`                                                                                                                           |
| _(v1.5)_ Modelo `.onnx` de voz está em `~/.claude/piper-voices/pt_BR-faber-medium.onnx` (63 MB)                                                                                                                 | `ls -la ~/.claude/piper-voices/`                                                                                                                                              |
| _(v1.5)_ Piper produz PCM cru (22050 Hz, 16-bit, mono, little-endian) sem erros                                                                                                                                 | `echo "teste" \| piper -m <modelo> --output-raw \| wc -c` (40960 bytes para "teste")                                                                                          |
| _(v1.5)_ Java Sound (`javax.sound.sampled.SourceDataLine`) consegue reproduzir o PCM do Piper                                                                                                                   | compilação e execução do teste `MixerCheck.java` com JDK 21 Zulu — `isLineSupported: true`                                                                                    |
| _(v1.5)_ Mixers de áudio estão disponíveis via `AudioSystem.getMixerInfo()` (ALSA/PipeWire)                                                                                                                     | listagem de mixers: HDMI, USB, Generic, default — nenhum mixer bloqueado                                                                                                      |
| _(v1.5)_ `DefaultActionGroup(text, true)` é aceito por `ToolWindow.setTitleActions(List<AnAction>)` (popup automático)                                                                                          | `javap` de `DefaultActionGroup implements AnAction` + conhecimento de padrão IntelliJ                                                                                         |
| _(v1.5)_ `ClaudeDockSettings.PersistentStateComponent` pode ter campos novos (`piperExecutable`, `piperModel`) sem migrações                                                                                    | padrão já usado com `claudeExecutable` em v1.0; XML serialization é transparente                                                                                              |
| _(v1.6)_ `FileSaverDescriptor(String, String, String...)`, `FileChooserFactory.createSaveFileDialog(descriptor, project)`, `FileSaverDialog.save(Path, String)` e `VirtualFileWrapper.getFile()` existem na 262 | `javap` sobre `intellij.platform.ide.jar` da distribuição `idea-2026.2` usada no build (IDE local: `IU-262.8665.337`)                                                         |
| _(v1.6)_ `save(...)` devolve `null` quando o usuário cancela                                                                                                                                                    | assinatura anulável de `FileSaverDialog.save` + contrato do `FileSaverDialogImpl`; é o que sustenta CB-44                                                                     |
| _(v1.6)_ O `/export` do CLI exporta **a conversa**, sem forma de restringi-lo a um trecho                                                                                                                       | funções `azb`/`u0n` já lidas do binário `claude` 2.1.220 em v1.3 — o único argumento é o caminho do arquivo, e o conteúdo vem de `lZo(t.messages, …)` (base de D-30)          |
| _(v1.6)_ O texto do trecho já está disponível ao plugin sem passar pelo CLI                                                                                                                                     | `JBTerminalWidget.getSelectedText()`, em uso desde RF-26 em `ClaudeSelectionCopyButton.selectedText()`                                                                        |
| _(v1.7)_ `ShellTerminalWidget.getActions()` cria as ações de split a partir de `getListener()`                                                                                                                  | `javap -c` de `ShellTerminalWidget`: duas chamadas a `TerminalSplitAction.create(Z, JBTerminalWidgetListener)` logo após `getListener()`                                      |
| _(v1.7)_ As ações de split delegam ao listener                                                                                                                                                                  | `javap -c` de `TerminalSplitAction`: `isEnabled` → `canSplit(Z)`, `actionPerformed` → `split(Z)`                                                                              |
| _(v1.7)_ `vertically = true` é "Split Right" (lado a lado)                                                                                                                                                      | `javap -c` de `TerminalSplitAction$Companion.create`: pareia `vertically` com `action.SplitVertically.text` e o atalho `TW.SplitRight`                                        |
| _(v1.7)_ `JBTerminalWidgetListener` tem `canSplit`/`split` como métodos **default**                                                                                                                             | `javap` da interface: `public default boolean canSplit(boolean)` e `public default void split(boolean)`                                                                       |
| _(v1.7)_ `JBTerminalWidget.setListener` é público                                                                                                                                                               | `javap` de `com.intellij.terminal.JBTerminalWidget`                                                                                                                           |
| _(v1.7)_ **A sessão criada por este plugin é sempre JediTerm (CLASSIC)**, qualquer que seja o `TerminalEngine` configurado                                                                                      | `javap -c` de `AbstractTerminalRunner.startShellTerminalWidget`: chama `createTerminalWidget(…)`, cujo tipo de retorno é `com.intellij.terminal.JBTerminalWidget` (Achado 27) |
| _(v1.7)_ `Content.getDisposer()`, `ContentManager.selectNextContent()/selectPreviousContent()` existem na 262                                                                                                   | `javap` de `com.intellij.ui.content.Content` e `ContentManager` em `intellij.platform.ide.core.jar`                                                                           |
| _(v1.7)_ `OnePixelSplitter(boolean vertical, float proportion)` empilha quando `vertical = true`                                                                                                                | **teste de geometria** T-1.34/T-1.35: depois do layout, a segunda pane tem `y` maior (empilhado) ou `x` maior (lado a lado)                                                   |
| _(v1.7.1)_ O item "Close Tab" do menu de contexto chama `listener.onSessionClosed()` — ou seja, a v1.7 **já** fechava a pane, sob o nome errado                                                                 | `javap -c -p` de `ShellTerminalWidget`: o lambda de `getActions` invoca `JBTerminalWidgetListener.onSessionClosed`; o rótulo vem de `getCloseTabActionPresentation()`         |
| _(v1.7.2)_ `ContentManagerImpl.selectNextContent()` e `selectPreviousContent()` começam com `LOG.assertTrue(getContentCount() > 1)`                                                                             | `javap -c -p` de `ContentManagerImpl`: `getContentCount` → `if_icmple` → `Logger.assertTrue` nas primeiras instruções de **ambos** (base de DEF-02)                           |
| _(v1.7.2)_ `ContentManagerImpl.removeContent(Content, boolean)` **não** tem assertion equivalente                                                                                                               | `javap -c -p` da mesma classe: nenhum `Logger.assertTrue` no método — auditado junto com DEF-02, para não corrigir só o caso relatado                                         |
| _(v1.8)_ `Splitter.swapComponents()` troca as duas referências internas e já faz `revalidate`/`repaint`, sem reparentar                                                                                         | `javap -c -p` de `com.intellij.openapi.ui.Splitter`: troca `myFirstComponent`/`mySecondComponent` e chama `revalidate()`+`repaint()` — 27 instruções                          |
| _(v1.8)_ `Splitter.setOrientation(boolean)` e `getOrientation()` são públicos                                                                                                                                   | `javap` da mesma classe                                                                                                                                                       |
| _(v1.8)_ O editor arrasta o **rótulo da aba**, não o corpo: `TabInfo.setDragOutDelegate` + `JBEditorTabs`, sobre `DockManager`/`DockContainer`                                                                  | `javap -p` de `EditorTabbedContainer` (campo `dragOutDelegate`, `JBEditorTabs`) e `javap` de `TabInfo` e `DockContainer` (base de Q-28)                                       |
| _(v1.8)_ O plugin de terminal da JetBrains **não** implementa DnD de panes                                                                                                                                      | listagem de `terminal.jar`: nenhuma classe com `dnd`/`DragAndDrop` no nome                                                                                                    |

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

_(v1.6)_ **Não verificado:** o comportamento do diálogo nativo de salvar sob Wayland com o
_native file chooser_ do IDE ligado (T-3.29); e se o nome sugerido chega preenchido em todos os
IDEs da família (T-3.5 cobre o resto do plugin, não este campo).

_(v1.7)_ **Não verificado:** se as ações "Split Right"/"Split Down" de fato aparecem no menu de
contexto com o listener instalado (T-3.34) — a cadeia foi lida no bytecode, mas não exercitada no
IDE; se duas sessões da mesma aba mantêm a integração do plugin oficial simultaneamente (T-3.36,
ligado a Q-02/CB-56); e o comportamento do foco ao fechar uma pane (T-3.38, T-3.39).

_(v1.7)_ ~~**Premissa não verificada carregada desde a v1.2:** o engine da sessão criada por este
plugin~~ ✅ **RESOLVIDA** — é sempre JediTerm/CLASSIC, ver Achado 27. Q-04 e Q-10 já haviam sido
fechadas por outro motivo (Achado 21); esta é a razão estrutural.
