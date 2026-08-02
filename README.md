<p align="center">
  <img src="src/main/resources/icons/claudeDuck.svg" alt="Claude Code Dock" width="128">
</p>

# Claude Code Dock - JetBrains Plugin

Plugin pessoal para JetBrains que abre uma janela separada dentro do IDE, onde possamos interagir com o Claude Code de forma mais fluida, com cópia de texto, histórico de conversas, e talvez até integração com o código do projeto.

## Requisitos

- JDK 21 (o build usa `jvmToolchain(21)`)
- [Claude Code](https://code.claude.com/docs/en/overview) instalado e no `PATH`
- IDE JetBrains build 252 ou superior (sem `until-build`)

Gradle não precisa ser instalado — use o wrapper (`./gradlew`, Gradle 9.2.0).

## Build

```bash
./gradlew buildPlugin
```

Gera `build/distributions/claude-code-dock-<versão>.zip`, instalável via
_Settings → Plugins → ⚙ → Install Plugin from Disk…_

## Rodar em uma IDE de testes

```bash
./gradlew runIde
```

Abre uma instância sandbox da IDE com o plugin carregado. O estado da sandbox fica
em `.intellijPlatform/` (ignorado pelo git).

## Testes

```bash
./gradlew test
```

JUnit 4 + `TestFrameworkType.Platform`. Relatórios em `build/reports/tests/test/index.html`.

## Verificação de compatibilidade

```bash
./gradlew verifyPlugin
```

Roda o IntelliJ Plugin Verifier contra as IDEs recomendadas.

## Configuração do build

Parâmetros ficam em [gradle.properties](gradle.properties):

| Propriedade        | Valor                            | Descrição                         |
| ------------------ | -------------------------------- | --------------------------------- |
| `pluginGroup`      | `dev.reginaldomorais.claudedock` | groupId do artefato               |
| `pluginVersion`    | `0.1.0`                          | versão do plugin                  |
| `platformVersion`  | `2026.2`                         | IntelliJ IDEA usado na compilação |
| `pluginSinceBuild` | `252`                            | build mínima suportada            |

A partir de 2025.3 (253) a JetBrains unificou a distribuição — não há mais IC
publicada separadamente, então compilamos contra a distribuição única.

## Estrutura

```text
src/main/kotlin/dev/reginaldomorais/claudedock/
  ClaudeToolWindowFactory.kt      # tool window dedicada
  ClaudeTerminalSessionFactory.kt # criação de sessões de terminal
  ClaudeCommand.kt                # montagem do comando `claude`
  actions/                        # nova sessão, resume, abrir dock
  settings/                       # Settings → Tools → Claude Code Dock
src/main/resources/META-INF/
  plugin.xml                      # declaração base
  plugin-terminal.xml             # opcional: só carrega com o plugin de terminal
```
