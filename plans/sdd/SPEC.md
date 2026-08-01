# SPEC — Claude Code Dock: tool window dedicada para JetBrains

- **Versão:** 1.1
- **Data:** 2026-08-01
- **Status:** Aprovado — v1.1 incorpora três ajustes vindos do primeiro teste no IDE real
- **Autor:** Reginaldo Morais (com assistência do Claude Code)

> **Histórico de versões**
>
> | Versão | Data       | Mudança                                                                                                                      |
> | ------ | ---------- | ---------------------------------------------------------------------------------------------------------------------------- |
> | 1.0    | 2026-08-01 | Especificação inicial (Fases 1–3 do SDD)                                                                                     |
> | 1.1    | 2026-08-01 | RF-17 (Esc devolvido ao shell), RF-18 (estado vazio utilizável) e RF-19 (`CLAUDE_CONFIG_DIR` por projeto), após teste manual |

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
- **Publicação na JetBrains Marketplace.**
- **Suporte a Remote Development, split mode (frontend/backend) e WSL.** O alvo é execução local
  monolítica. Ver [Riscos](#riscos).
- **Substituição do plugin oficial.** Os dois coexistem; o oficial permanece a fonte da integração.
- **Autenticação, billing ou qualquer manipulação de credenciais do Claude Code.**

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

### API de terminal disponível na build 262

| Símbolo                                                                                                 | Situação                                       |
| ------------------------------------------------------------------------------------------------------- | ---------------------------------------------- |
| `AbstractTerminalRunner.startShellTerminalWidget(Disposable, ShellStartupOptions, boolean)`             | presente                                       |
| `LocalTerminalDirectRunner.createTerminalRunner(Project)`                                               | presente                                       |
| `ShellStartupOptions` (com `workingDirectory`, `shellCommand`, `envVariables`)                          | presente                                       |
| `TerminalWidget` (`sendCommandToExecute`, `addTerminationCallback`, `requestFocus`, `getTerminalTitle`) | presente                                       |
| `TerminalEngine`                                                                                        | enum com `CLASSIC`, `REWORKED`, `NEW_TERMINAL` |
| `LocalTerminalCustomizer.EP_NAME`                                                                       | presente                                       |

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
| Janela dedicada dentro do IDE                          | **este plugin**                | ✅ sim                       |
| Emulação de terminal, cópia/colagem, scrollback, busca | JediTerm / plataforma IntelliJ | ❌ não                       |
| Diff, seleção, diagnostics no IDE                      | plugin oficial da Anthropic    | ❌ não                       |
| Histórico e retomada de conversas                      | CLI (`claude --resume`)        | ❌ não                       |
| Autenticação, modelos, billing                         | CLI                            | ❌ não                       |

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

| #         | Requisito                                                                                                                                                  |
| --------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **RF-01** | O plugin DEVE registrar uma tool window dedicada, de título "Claude Code Dock", distinta da tool window "Terminal".                                        |
| **RF-02** | A tool window DEVE ancorar por padrão à direita (`ToolWindowAnchor.RIGHT`) e ser reposicionável pelo usuário através dos mecanismos padrão do IDE.         |
| **RF-03** | Ao ser aberta pela primeira vez, a tool window DEVE criar uma sessão de terminal cujo diretório de trabalho é a raiz do projeto (`project.basePath`).      |
| **RF-04** | Criada a sessão, o plugin DEVE executar automaticamente o comando `claude` configurado.                                                                    |
| **RF-05** | A criação da sessão DEVE ser **preguiçosa**: nenhum processo é iniciado enquanto a tool window não for aberta pelo usuário.                                |
| **RF-06** | A tool window DEVE suportar múltiplas abas, cada uma com sessão independente.                                                                              |
| **RF-07** | O plugin DEVE oferecer a ação "Nova sessão", que abre uma nova aba executando `claude`.                                                                    |
| **RF-08** | O plugin DEVE oferecer a ação "Retomar sessão", que abre uma nova aba executando `claude --resume`.                                                        |
| **RF-09** | O plugin DEVE oferecer uma ação global "Abrir Claude Code Dock", acessível por _Find Action_ e associável a atalho pelo usuário.                           |
| **RF-10** | O plugin DEVE expor uma tela de configurações com o caminho do executável `claude` (valor padrão: `claude`).                                               |
| **RF-11** | Encerrado o processo `claude` de uma aba, a aba DEVE exibir indicação visual de sessão terminada, sem fechar-se automaticamente.                           |
| **RF-12** | Fechada uma aba, o processo correspondente DEVE ser encerrado e todos os recursos liberados.                                                               |
| **RF-13** | O plugin NÃO DEVE alterar, remover ou interferir no comportamento do plugin oficial nem da tool window "Terminal".                                         |
| **RF-14** | Se o executável `claude` não for encontrado, o plugin DEVE exibir notificação acionável com link para as configurações.                                    |
| **RF-15** | Se o plugin de terminal (`org.jetbrains.plugins.terminal`) estiver ausente, o plugin DEVE degradar sem erro: a tool window não é registrada.               |
| **RF-16** | O título de cada aba DEVE identificar a sessão de forma distinguível (ex.: `Claude`, `Claude (2)`).                                                        |
| **RF-17** | _(v1.1)_ A tecla `Esc` DEVE ser entregue ao processo da sessão, e não consumida pelo IDE para mover o foco ao editor.                                      |
| **RF-18** | _(v1.1)_ Com a tool window sem nenhuma aba, o estado vazio DEVE oferecer caminho visível para abrir uma nova sessão ou retomar uma anterior.               |
| **RF-19** | _(v1.1)_ O plugin DEVE permitir configurar `CLAUDE_CONFIG_DIR` **por projeto** (= por janela do IDE), injetando-o no ambiente das sessões daquele projeto. |
| **RF-20** | _(v1.1)_ `CLAUDE_CONFIG_DIR` vazio DEVE significar "não injetar", preservando o padrão do CLI (`~/.claude`).                                               |

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
├── ClaudeTabTitle.kt               # puro: títulos distinguíveis de aba
├── ClaudeWorkingDirectory.kt       # puro: resolução do diretório de trabalho
├── settings/
│   ├── ClaudeDockSettings.kt        # PersistentStateComponent (nível aplicação): executável
│   ├── ClaudeDockProjectSettings.kt # PersistentStateComponent (nível projeto): CLAUDE_CONFIG_DIR
│   └── ClaudeDockConfigurable.kt    # tela em Settings > Tools (projectConfigurable)
└── actions/
    ├── NewSessionAction.kt
    ├── ResumeSessionAction.kt
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
| Ações                          | traduzir intenção do usuário em chamadas às duas primeiras   | detalhes de implementação de ambas |

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

| #         | Caso                                                                             | Tratamento                                                                                                       |
| --------- | -------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| **CB-01** | `claude` não está no `PATH`                                                      | Notificação acionável (RF-14); aba permanece com shell utilizável                                                |
| **CB-02** | Caminho configurado aponta para arquivo inexistente ou sem permissão de execução | Validação na tela de configurações + notificação ao criar sessão                                                 |
| **CB-03** | Plugin oficial ausente ou desabilitado                                           | Degradação graciosa: janela funciona, integração não (RNF-11, Fluxo C)                                           |
| **CB-04** | Plugin oficial **e** este plugin abertos ao mesmo tempo                          | Coexistem: duas sessões independentes do CLI, ambas conectadas ao mesmo servidor MCP. Ver [Riscos R-05](#riscos) |
| **CB-05** | Projeto sem `basePath` (ex.: janela sem projeto, arquivo avulso)                 | Fallback para o diretório home do usuário                                                                        |
| **CB-06** | Múltiplos projetos abertos                                                       | Serviços com escopo de `Project`; uma tool window por projeto (RNF-17)                                           |
| **CB-07** | Usuário encerra o `claude` com `/exit` ou `Ctrl+D`                               | `addTerminationCallback` marca a aba; scrollback preservado (RF-11)                                              |
| **CB-08** | Usuário fecha a aba com processo vivo                                            | `Disposable` encerra o processo e libera o PTY (RNF-09, RF-12)                                                   |
| **CB-09** | Projeto fechado com sessões ativas                                               | Disposables de escopo de projeto encerram todas as sessões                                                       |
| **CB-10** | IDE sem o plugin de terminal                                                     | Tool window não registrada; nenhum erro (RF-15)                                                                  |
| **CB-11** | `TerminalEngine.REWORKED` ativo em vez de `CLASSIC`                              | Usar `startShellTerminalWidget`, que respeita o engine configurado; **validar ambos os engines nos testes**      |
| **CB-12** | Projeto não confiável (_Trusted Projects_)                                       | A plataforma bloqueia criação de terminal; propagar a decisão do IDE sem contorná-la                             |
| **CB-13** | Nome de aba duplicado com muitas sessões                                         | Sufixo numérico incremental (RF-16)                                                                              |
| **CB-14** | Usuário troca o caminho do `claude` com sessões abertas                          | Sessões existentes não são afetadas; a mudança vale para as próximas                                             |
| **CB-15** | Upgrade do IDE quebra a assinatura de `startShellTerminalWidget`                 | Falha isolada em uma classe (RNF-15); tratamento em [Riscos R-01](#riscos)                                       |
| **CB-16** | Remote Dev / split mode (frontend e backend separados)                           | Fora de escopo; o customizer roda no backend e o caminho de código difere. **Documentar como não suportado**     |
| **CB-17** | Diretório do projeto em WSL (Windows)                                            | Fora de escopo nesta versão                                                                                      |
| **CB-18** | _(v1.1)_ `Esc` pressionado com modificador (`Shift+Esc`, `Ctrl+Esc`)             | Não é interceptado: segue para o tratamento normal do IDE, preservando `Ctrl+Esc` do oficial (RF-13, T-5.2)      |
| **CB-19** | _(v1.1)_ `Esc` antes do PTY existir (sessão adiada por RNF-02)                   | `ttyConnector` nulo: o pre-handler não consome e o comportamento padrão prevalece — não há sessão para receber   |
| **CB-20** | _(v1.1)_ Usuário fecha a última aba da tool window                               | Estado vazio com links "Nova sessão" / "Retomar sessão" (RF-18); `createToolWindowContent` não roda de novo      |
| **CB-21** | _(v1.1)_ `CLAUDE_CONFIG_DIR` informado com `~`                                   | Expandido no plugin: variável de ambiente não passa por expansão do shell                                        |
| **CB-22** | _(v1.1)_ `CLAUDE_CONFIG_DIR` aponta para diretório inexistente                   | Repassado como está; a criação é responsabilidade do CLI. Sem validação própria para não divergir do CLI         |
| **CB-23** | _(v1.1)_ `CLAUDE_CONFIG_DIR` alterado com sessões abertas                        | Como CB-14: sessões vivas não mudam; vale para as próximas abas                                                  |

---

## Riscos

| #        | Risco                                                                                                                                                                                                                | Impacto     | Prob. | Mitigação                                                                                                                                                                                                                                                                                                                                                        |
| -------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------- | ----- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **R-01** | `AbstractTerminalRunner` / `TerminalToolWindowManager` são API de plataforma sujeita a mudança sem aviso entre builds                                                                                                | Alto        | Média | Todo o acoplamento em uma única classe (RNF-15); smoke test manual a cada upgrade de IDE; sem `until-build`, o plugin carrega e falha de forma isolada e diagnosticável, em vez de ser silenciosamente desabilitado                                                                                                                                              |
| **R-02** | ~~A premissa central (customizer alcança widget customizado) pode estar errada~~ ✅ **FECHADO em 2026-08-01**                                                                                                         | ~~Crítico~~ | —     | Validado empiricamente por `TerminalCustomizerReachTest` (T-4), com teste de controle. É `configureStartupOptions` que aplica os customizers, e `startShellTerminalWidget` passa por ele. O teste permanece como regressão a cada upgrade de IDE                                                                                                                 |
| **R-03** | A Anthropic pode alterar `CLAUDE_CODE_SSE_PORT` ou o mecanismo de descoberta                                                                                                                                         | Médio       | Média | Este plugin **não depende** da variável: apenas não a atrapalha. Mudanças quebram integração, não a janela (degradação graciosa)                                                                                                                                                                                                                                 |
| **R-04** | O plugin oficial pode ganhar tool window dedicada nativamente, tornando este projeto obsoleto                                                                                                                        | Baixo       | Média | Resultado aceitável: o custo afundado é pequeno (poucas centenas de linhas) e desinstalar é trivial. Acompanhar o changelog do oficial                                                                                                                                                                                                                           |
| **R-05** | Duas sessões simultâneas (aba nativa + janela dedicada) conectadas ao mesmo servidor MCP podem gerar comportamento ambíguo — ex.: um diff aberto por uma sessão atribuído à outra                                    | Médio       | Média | Documentar no README a recomendação de usar uma janela por vez; investigar durante T-4 se o servidor distingue clientes. Considerar ocultar o botão do oficial via `CLAUDE_CODE_JETBRAINS_PLUGIN_HIDE_BUTTON` (string encontrada no jar oficial — **usar apenas após verificar semântica**)                                                                      |
| **R-06** | `TerminalEngine.REWORKED` pode ter comportamento distinto de `CLASSIC` ao ser embutido fora da tool window nativa                                                                                                    | Médio       | Média | Testar explicitamente com os dois engines (CB-11); fixar `CLASSIC` como fallback documentado se houver divergência                                                                                                                                                                                                                                               |
| **R-07** | Plugin não assinado instalado via disco dispara aviso de segurança do IDE                                                                                                                                            | Baixo       | Alta  | Esperado para uso local; documentar no README                                                                                                                                                                                                                                                                                                                    |
| **R-08** | Sem `until-build`, uma versão futura incompatível do IDE pode causar exceções em runtime                                                                                                                             | Médio       | Média | Aceito conscientemente em troca de não travar a cada upgrade; mitigado por R-01 e pelo tratamento de erro do Fluxo E                                                                                                                                                                                                                                             |
| **R-09** | _(v1.1)_ A correção do `Esc` (RF-17) depende de `JBTerminalPanel.addPreKeyEventHandler` e de `JBTerminalWidget.asJediTermWidget` — API pública, mas sem garantia de estabilidade, e específica do engine **CLASSIC** | Médio       | Média | Falha degrada, não quebra: `asJediTermWidget` nulo faz o `install` retornar sem efeito, e o pior caso é voltar ao bug atual (`Esc` move o foco), nunca uma exceção. Coberto por T-3.7. Se a plataforma migrar a janela para o engine REWORKED, reavaliar contra `TerminalEscapeHandler` (EP `org.jetbrains.plugins.terminal.escapeHandler`, já existente em 262) |
| **R-10** | _(v1.1)_ A JetBrains pode corrigir a assimetria do `TerminalEscapeKeyListener` e passar a entregar o `Esc` também fora da tool window "Terminal", tornando o pre-handler redundante — ou duplicando o envio          | Baixo       | Baixa | O pre-handler consome o evento antes do listener, então mesmo com a correção upstream o caminho continua único: nós enviamos, a plataforma não reenvia. Revalidar em T-5.3 a cada upgrade                                                                                                                                                                        |

---

## Estratégia de Testes

### Testes unitários

Base: `BasePlatformTestCase` (IntelliJ Test Framework), executados por `./gradlew test`.

| #          | Alvo                              | Verificação                                                                                                                                 |
| ---------- | --------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| **T-1.1**  | `ClaudeDockSettings`              | Valor padrão é `claude`; estado sobrevive a serialização/desserialização                                                                    |
| **T-1.2**  | `ClaudeDockSettings`              | Caminho customizado é persistido e recuperado                                                                                               |
| **T-1.3**  | Montagem de `ShellStartupOptions` | `workingDirectory` recebe `project.basePath`                                                                                                |
| **T-1.4**  | Montagem de `ShellStartupOptions` | Projeto sem `basePath` cai no diretório home (CB-05)                                                                                        |
| **T-1.5**  | Construção do comando             | "Nova sessão" produz `claude`; "Retomar" produz `claude --resume`                                                                           |
| **T-1.6**  | Construção do comando             | Caminho com espaços é passado como argumento único, sem interpretação de shell (RNF-08)                                                     |
| **T-1.7**  | Nomeação de abas                  | Sessões sucessivas geram títulos distintos (RF-16, CB-13)                                                                                   |
| **T-1.8**  | `ClaudeEnvironment`               | _(v1.1)_ Config dir ausente/em branco não injeta variável alguma (RF-20)                                                                    |
| **T-1.9**  | `ClaudeEnvironment`               | _(v1.1)_ Caminho vira `CLAUDE_CONFIG_DIR`, com trim e expansão de `~` (RF-19, CB-21)                                                        |
| **T-1.10** | `ClaudeDockProjectSettings`       | _(v1.1)_ Padrão vazio, persistência via `loadState`, caminho efetivo com trim                                                               |
| **T-1.11** | `ClaudeEscapeForwarder`           | _(v1.1)_ Só `Esc` puro em `KEY_PRESSED` é encaminhado; modificador, outras teclas e evento já consumido são ignorados (RF-17, CB-18, CB-19) |

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

| #         | Roteiro                                                                                                                                                                              |
| --------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **T-3.1** | Instalar via _Install Plugin from Disk_; reiniciar; abrir a janela; conversar com o Claude                                                                                           |
| **T-3.2** | Pedir uma edição de arquivo e confirmar que o diff abre no IDE                                                                                                                       |
| **T-3.3** | Selecionar código no editor, usar `Ctrl+Alt+K` do oficial e confirmar que a referência chega à sessão da janela dedicada                                                             |
| **T-3.4** | Executar "Retomar sessão" e confirmar que o seletor do CLI lista sessões anteriores                                                                                                  |
| **T-3.5** | Repetir T-3.1 em ao menos dois IDEs distintos (ex.: IntelliJ e GoLand)                                                                                                               |
| **T-3.6** | Desinstalar o plugin e confirmar que o oficial segue íntegro e funcional                                                                                                             |
| **T-3.7** | _(v1.1)_ Na janela dedicada, rodar `/usage` e sair com `Esc`; conferir que o foco **não** vai para o editor (RF-17)                                                                  |
| **T-3.8** | _(v1.1)_ Fechar a última aba e confirmar que os links "Nova sessão"/"Retomar sessão" aparecem e funcionam (RF-18, CB-20)                                                             |
| **T-3.9** | _(v1.1)_ Definir `CLAUDE_CONFIG_DIR` em Settings, abrir nova sessão e conferir `echo $CLAUDE_CONFIG_DIR` no terminal; confirmar que `CLAUDE_CODE_SSE_PORT` continua presente (RF-19) |

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
2. **Fase 1 — uso pessoal.** Instalar em um IDE, usar por alguns dias, registrar achados no `HANDOUT.md`.
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
- Registrar observações no `HANDOUT.md`.

---

## Perguntas em Aberto

| #        | Questão                                                                                                                       | Situação                                                                                                                                                                 |
| -------- | ----------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Q-01** | ~~O `TerminalCustomizer` da Anthropic de fato alcança widgets criados fora da tool window nativa?~~                           | ✅ **RESOLVIDO em 2026-08-01.** Sim. Validado por `TerminalCustomizerReachTest` (T-4) com teste de controle. Ponto exato: `configureStartupOptions` aplica os customizers |
| **Q-02** | Duas sessões simultâneas conectadas ao mesmo servidor MCP causam ambiguidade de atribuição (ex.: qual sessão recebe um diff)? | Em aberto (R-05). Investigar durante T-4                                                                                                                                 |
| **Q-03** | Qual a semântica exata de `CLAUDE_CODE_JETBRAINS_PLUGIN_HIDE_BUTTON`?                                                         | String encontrada no jar oficial; **semântica não verificada**. Não usar antes de confirmar                                                                              |
| **Q-04** | O engine `REWORKED` se comporta como o `CLASSIC` fora da tool window nativa?                                                  | Em aberto (R-06, CB-11). Cobrir em T-2.6                                                                                                                                 |
| **Q-05** | Vale ocultar o botão/ação do plugin oficial para evitar confusão de dois pontos de entrada?                                   | Decisão de produto, adiada até haver uso real. Depende de Q-03                                                                                                           |
| **Q-06** | Suportar Remote Development e WSL no futuro?                                                                                  | Fora de escopo agora (CB-16, CB-17). Reavaliar conforme necessidade                                                                                                      |
| **Q-07** | A tool window deve restaurar automaticamente as sessões ao reabrir o projeto?                                                 | Não previsto. `isTerminalSessionPersistent` existe na plataforma, mas persistência acrescenta complexidade sem demanda comprovada                                        |
| **Q-08** | Qual o `since-build` mínimo realmente testável?                                                                               | Definido como `252` por conservadorismo; **testado apenas em `262`**. Builds anteriores não foram verificadas                                                            |
| **Q-09** | _(v1.1)_ Vale permitir `CLAUDE_CONFIG_DIR` **por aba**, e não só por projeto?                                                 | Adiado. Em IDEs JetBrains uma janela é um projeto, então o escopo atual já atende o pedido. Por aba exigiria diálogo a cada "Nova sessão" — reavaliar se houver demanda  |
| **Q-10** | _(v1.1)_ O `TerminalEscapeKeyListener` se comporta igual no engine `REWORKED`?                                                | Em aberto. Lá o `Esc` passa por `Terminal.Escape` + EP `escapeHandler`, caminho diferente do pre-handler adotado. Ligado a Q-04 e R-09                                   |
| **Q-11** | _(v1.1)_ `CLAUDE_CONFIG_DIR` deveria ser versionável (`.idea/`) em vez de ficar no workspace?                                 | Decidido pelo workspace (RNF-05). Reavaliar só se surgir caso de config dir relativo ao repositório, compartilhável pelo time                                            |

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

---

## Anexo — Rastreabilidade das evidências

Toda afirmação técnica sobre o estado atual remonta a uma verificação direta, conforme a regra
"DON'T GUESS, VERIFY" do `CLAUDE.md`.

| Afirmação                                                                                                                             | Evidência                                                                                                                                            |
| ------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| Oficial usa `createShellWidget` + `sendCommandToExecute`                                                                              | `javap -c` de `com.anthropic.code.plugin.TerminalUtil`                                                                                               |
| Oficial prende a sessão à tool window "Terminal"                                                                                      | literal `"Terminal"` passado a `ToolWindowManager.getToolWindow` em `TerminalUtil`                                                                   |
| Oficial hospeda servidor MCP                                                                                                          | jars `ktor-server-{cio,websockets,sse}`, `kotlin-sdk-jvm-0.4.0`; classes `mcp/tools/*`, `WebSocketMcpServerTransport`                                |
| Injeção via `CLAUDE_CODE_SSE_PORT`                                                                                                    | literal em `TerminalCustomizer.class`; registro em `META-INF/plugin-terminal.xml`                                                                    |
| Formato do lockfile                                                                                                                   | arquivos reais em `~/.claude/ide/*.lock`, incluindo um escrito por GoLand                                                                            |
| `TerminalToolWindowManager` delega a `startShellTerminalWidget`                                                                       | `javap -c` de `TerminalToolWindowManager`                                                                                                            |
| `TerminalWidget` é embutível                                                                                                          | interface estende `com.intellij.openapi.ui.ComponentContainer`                                                                                       |
| API de terminal presente na build 262                                                                                                 | `javap` sobre `plugins/terminal/lib/terminal.jar`                                                                                                    |
| `TerminalEngine` tem `CLASSIC`/`REWORKED`/`NEW_TERMINAL`                                                                              | `javap` de `org.jetbrains.plugins.terminal.TerminalEngine`                                                                                           |
| Versões de ambiente                                                                                                                   | `java -version`, `gradle --version`, `claude --version`, `build.txt`, `product-info.json`                                                            |
| IntelliJ Platform Gradle Plugin 2.18.1                                                                                                | portal de plugins do Gradle                                                                                                                          |
| _(v1.1)_ `Esc` só é entregue ao shell na tool window de id `"Terminal"`                                                               | `javap -c` de `com.intellij.terminal.TerminalEscapeKeyListener.shouldSwitchFocusToEditor` e de `JBTerminalWidget.isTerminalToolWindow`               |
| _(v1.1)_ `JBTerminalPanel` roda pre-handlers antes do listener de `Esc` e respeita `isConsumed`                                       | `javap -c` de `com.intellij.terminal.JBTerminalPanel.handleKeyEvent`                                                                                 |
| _(v1.1)_ 2026.2 distribui `Terminal.SwitchFocusToEditor` sem atalho padrão                                                            | ausência de `keyboard-shortcut` no `META-INF/plugin.xml` do terminal + chaves `escape.behavior.change.notification.*` em `TerminalBundle.properties` |
| _(v1.1)_ `CLAUDE_CONFIG_DIR` é variável reconhecida pelo CLI                                                                          | literal encontrado no jar do plugin oficial (já registrado no `HANDOUT.md`)                                                                          |
| _(v1.1)_ `ToolWindowEx.emptyText`, `ShellStartupOptions.Builder.envVariables`, `JBTerminalPanel.addPreKeyEventHandler` existem em 262 | compilação bem-sucedida contra `intellijIdea("2026.2")`                                                                                              |

**Não verificado (declarado como suposição):** semântica de
`CLAUDE_CODE_JETBRAINS_PLUGIN_HIDE_BUTTON` (Q-03); comportamento de builds anteriores a `262`
(Q-08); alcance efetivo do customizer em widget customizado (Q-01 / T-4); comportamento do `Esc`
sob o engine `REWORKED` (Q-10).
