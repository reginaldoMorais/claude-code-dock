# Ícone das ações renderizado junto do texto no menu

> **Status:** implementado e aprovado no IDE real em 2026-10-18 (T-3.87). 187 testes verdes.

@CLAUDE.md

## Contexto

O ícone do plugin, no item "Enviar Seleção para o Claude Code" do menu de contexto do editor,
aparece **colado ao texto**, na coluna do rótulo, em vez de ficar na coluna de ícones (gutter) onde
estão os ícones dos outros itens ("Compare with Clipboard", "Diagrams").

## Diagnóstico (verificado)

### Causa: o SVG declara 20×20, e o gutter do menu aceita no máximo 18 px

- `src/main/resources/META-INF/plugin-terminal.xml` usa `icon="/icons/claudeDuckMono.svg"` em três
  lugares: a tool window, a ação `SendSelection` e a ação `OpenClaudeDock`.
- `claudeDuckMono.svg` (e `_dark`) declaram `width="20" height="20"` — o tamanho escolhido para a
  barra lateral da tool window, travado pelo `ClaudeIconTest.test icones declaram tamanho em pixel`.
- Bytecode de `com.intellij.ui.plaf.beg.BegMenuItemUI` (IntelliJ 2026.2,
  `intellij.platform.ide.impl.jar`), que pinta os itens do menu:
  - o construtor inicializa `myMaxGutterIconWidth = 18` (sobrescrevível por
    `UIUtil.getPropertyMaxGutterIconWidth` em `installDefaults`);
  - `getCheckIcon()` devolve o ícone do item **só se** `icon.getIconWidth() <= myMaxGutterIconWidth`;
    caso contrário devolve `null`;
  - `getIcon()` devolve o ícone **só quando** `getCheckIcon()` é `null` — e esse ícone entra no
    `SwingUtilities.layoutCompoundLabel`, ou seja, é desenhado **como parte do rótulo**, ao lado do
    texto.
- Resultado: 20 > 18 → o ícone sai do gutter e vai para a coluna do texto. É exatamente o que a
  imagem mostra.
- `ActionMenuItem` usa `EmptyIcon.ICON_16` como placeholder e `PoppedIcon(icon, 16, 16)` — o
  tamanho que a plataforma espera de um ícone de ação em menu é **16×16**.

### Achados colaterais

- `OpenClaudeDock` ("Abrir Claude Code Dock", menu _Tools_) usa o mesmo SVG e tem o **mesmo defeito**.
- `SendSelectionAction` passa `AllIcons.Actions.MoveTo2` no construtor, mas o `icon=` do XML o
  sobrescreve (a imagem mostra o pato, não o `MoveTo2`). O ícone do construtor é código morto e
  engana quem lê a classe.

## Solução proposta

Separar o ícone **da tool window** (20×20, continua como está) do ícone **das ações** (16×16, novo).

1. **Novos recursos** `src/main/resources/icons/claudeDuckAction.svg` e `claudeDuckAction_dark.svg`:
   cópia exata de `claudeDuckMono.svg` / `_dark.svg` trocando apenas `width="20" height="20"` por
   `width="16" height="16"`. O `viewBox` (`0.35 -3.67 92.16 92.16`) é mantido — a arte escala junto,
   sem redesenho. Comentário do cabeçalho em português explicando o porquê dos 16 px (gutter de menu).
2. **`plugin-terminal.xml`**: trocar o `icon=` de `SendSelection` e `OpenClaudeDock` para
   `/icons/claudeDuckAction.svg`. A `toolWindow` continua em `claudeDuckMono.svg`.
3. **`SendSelectionAction`**: remover o `AllIcons.Actions.MoveTo2` (e o import) do construtor, para
   que a única fonte do ícone seja o XML — como já é na prática. Texto e descrição do construtor
   ficam.
4. **`ClaudeIconTest`** (testes ajustados/novos):
   - `test barra de ferramentas usa a versao monocromatica` → passa a distinguir: o `icon=` da
     `<toolWindow>` deve ser `MONO`; os `icon=` das `<action>` devem ser `ACTION`. Mantém a guarda de
     "regex não vazia".
   - `test icones declaram tamanho em pixel` → acrescenta `ACTION` e `ACTION_DARK` em `"16"`.
   - `test variante escura acompanha a geometria da clara` → acrescenta o par `ACTION`/`ACTION_DARK`.
   - **novo** `test icone de acao cabe no gutter do menu`: largura declarada de todo ícone usado em
     `<action>` ≤ 16 — trava a regressão pela regra, não só pelo nome do arquivo.
5. **Documentação**: `CHANGELOG.md` (seção _Corrigido_ da próxima versão patch, 0.11.1) e uma nota
   curta no `plans/sdd/SPEC.md` registrando o achado (limite de 18 px do gutter em `BegMenuItemUI`)
   e a decisão de ter dois tamanhos de ícone.

## Trade-offs

- **Dois arquivos a mais (claro/escuro) com a mesma arte.** Alternativa seria reduzir o
  `claudeDuckMono.svg` para 16×16 e usar um só — mas a barra lateral da tool window foi
  deliberadamente fixada em 20×20 (travado em teste). Duplicar dois SVGs pequenos é mais barato que
  reabrir essa decisão. Se a arte mudar, os quatro arquivos precisam mudar juntos; o teste de
  `viewBox` ajuda a pegar divergência de geometria, não de desenho.
- **Ícone de 16 px fica visualmente menor que o da tool window.** É o tamanho padrão dos ícones de
  ação da plataforma; ficará do mesmo tamanho dos vizinhos no menu.
- **Remover o ícone do construtor** muda nada em runtime (o XML já vencia), mas se um dia a ação for
  registrada só por código, ela nasceria sem ícone. Aceitável: o registro é e continua sendo pelo XML.

## Ponto em aberto a confirmar no `runIde`

- O limite de 18 é um valor bruto no bytecode; não verifiquei se é escalado em HiDPI (o
  `getIconWidth()` do ícone é). Na escala 1× a conta 20 > 18 explica a imagem. A verificação visual
  em `./gradlew runIde` (menu de contexto do editor e menu _Tools_, temas claro e escuro) fecha a
  questão para as escalas em uso.

## Passos de execução

1. Criar `claudeDuckAction.svg` e `claudeDuckAction_dark.svg`.
2. Atualizar `plugin-terminal.xml`.
3. Limpar o construtor de `SendSelectionAction`.
4. Ajustar e criar os testes em `ClaudeIconTest`.
5. `./gradlew test` — todos devem passar.
6. `./gradlew runIde` — conferir o ícone no gutter nos dois menus, tema claro e escuro.
7. Atualizar `CHANGELOG.md` e `plans/sdd/SPEC.md`.

## Commit sugerido (para o usuário executar)

```bash
git checkout -b bugfix/action-icon-gutter
git add src/main/resources/icons/claudeDuckAction*.svg \
        src/main/resources/META-INF/plugin-terminal.xml \
        src/main/kotlin/dev/reginaldomorais/claudedock/actions/SendSelectionAction.kt \
        src/test/kotlin/dev/reginaldomorais/claudedock/ClaudeIconTest.kt \
        CHANGELOG.md plans/sdd/SPEC.md plans/20261018-action-icon-gutter.md
git commit -m "fix(icons): use 16px icon for actions so it renders in the menu gutter"
```
