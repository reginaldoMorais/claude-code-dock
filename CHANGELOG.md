# Changelog

Todas as mudanças relevantes deste projeto são registradas aqui.

O formato segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/), e o versionamento
segue [SemVer](https://semver.org/lang/pt-BR/).

---

## [0.8.0] — 2026-08-07

Versão da **velocidade da fala**: o áudio do Piper deixa de sair sempre no ritmo do modelo.
Acompanha a correção de um item do menu que existia desde a v0.6 e nunca funcionou.

### Adicionado

- **Velocidade da fala configurável** — 0,25x / 0,5x / 0,75x / 1x / 1,25x / 1,5x / 1,75x / 2x —
  em dois lugares ligados ao mesmo valor: um submenu **"Velocidade"** dentro do menu "Áudio" e um
  seletor em _Settings > Tools > Claude Code Dock_. Os dois mostram a mesma lista, e escolher em
  um marca o outro. A escolha vale para todos os projetos e persiste entre reinícios do IDE.
- O rótulo do submenu mostra a velocidade em vigor sem precisar abri-lo ("Velocidade (1,5x)").

### Corrigido

- **"Tocar seleção" nunca tocou nada.** O item está no menu "Áudio" desde a v0.6, mas procurava
  o terminal no lugar errado e desistia em silêncio a cada clique. Agora toca o trecho
  selecionado na sessão em foco — e avisa quando não há sessão aberta ou nada selecionado, em
  vez de não fazer nada.

### Notas

- O padrão continua sendo **o do próprio modelo de voz**, e não 1.0: em 100% o plugin não passa
  `--length-scale` ao Piper, deixando valer o que o autor da voz calibrou no `config.json` dela.
  Por isso o preset se chama "1x (padrão do modelo)".
- A velocidade se aplica à **próxima** fala. O Piper sintetiza o áudio inteiro antes de tocar,
  então mudar o valor no meio de uma reprodução não a altera.

### Documentação

- `plans/sdd/SPEC.md` evoluiu de 1.8.2 para 1.9: RF-47 e RF-48, RNF-31 (formatação numérica
  independente de locale em argumentos de processo), DEF-07, decisões D-39 e D-40, Q-29 e o
  Achado 31.
- Registrado nos Fatos verificados o que a CLI do Piper aceita e de onde vêm os seus defaults —
  incluindo a medição que confirma que `--length-scale` é o **inverso** da velocidade.
- **Achado 31:** o SPEC prometia desde a v1.5 um timeout de 20 s na síntese que nunca foi
  implementado. A documentação foi corrigida; o timeout continua pendente.

---

## [0.7.0] — 2026-08-03

Versão da **divisão de abas**: uma aba passa a hospedar várias sessões do Claude Code lado a
lado ou empilhadas. Acompanha a exportação de trechos selecionados e três correções encontradas
no uso real.

### Adicionado

- **Dividir a aba em várias sessões.** "Split Right" e "Split Down" no menu de contexto da
  sessão (com os atalhos do keymap), e um menu **"Dividir"** no cabeçalho da tool window. Cada
  divisão é uma sessão independente, com processo próprio e integração com o plugin oficial
  ativa. As divisões aninham à vontade — não há limite de panes por aba.
- **Reposicionar as divisões:** "Trocar de lado" inverte a sessão em foco com a vizinha, e
  "Girar divisão" alterna entre lado a lado e empilhado.
- **Fechar divisões:** "Fechar esta sessão" fecha só a pane em foco, devolvendo o espaço à
  vizinha; "Fechar todas as sessões" fecha a aba inteira, com todas as divisões.
- **Exportar o trecho selecionado.** O popup que aparece ao selecionar texto com o mouse ganhou
  um segundo botão, que grava **apenas o trecho** no arquivo escolhido no diálogo do IDE. O
  botão de exportar do cabeçalho continua exportando a conversa inteira pelo `/export` do CLI —
  são destinos diferentes, e nenhum substitui o outro.
- **Itens nativos do terminal que estavam inertes** passaram a funcionar na janela dedicada:
  "New Tab", "Close Tab", "Select Previous Tab" e "Select Next Tab" no menu de contexto.

### Corrigido

- **O cabeçalho parava de responder ao fechar a primeira sessão de uma aba dividida.** A aba
  continuava apontando para o componente já descartado, o foco não ia a lugar nenhum e nenhuma
  ação funcionava — nem "Nova sessão" — até se clicar em outra aba.
- **A aba era marcada como "(encerrado)" com uma sessão viva ao lado**, ao fechar uma divisão.
  O sufixo agora significa o que diz: a aba não tem mais nenhuma sessão em execução.
- **"Select Previous/Next Tab" com uma aba só** registrava uma exceção no log do IDE, em vez de
  não fazer nada.

### Alterado

- Os itens de fechamento passaram a dizer **quantas sessões encerram**: "Fechar esta sessão" e
  "Fechar todas as sessões". O nome anterior ("Fechar divisão") era lido como "fechar as
  divisões".
- As ações que só fazem sentido com a aba dividida aparecem **desabilitadas** quando não há
  divisão, em vez de avisar depois do clique.

### Documentação

- `plans/sdd/SPEC.md` evoluiu de 1.5.1 para 1.8.2 (RF-33 a RF-46, DEF-02 a DEF-06, decisões
  D-30 a D-38 e achados 25 a 30), e o `plans/sdd/HANDOFF.md` registra cada rodada.
- Registrada a avaliação do **arraste de panes** (Q-28): tecnicamente possível pela plataforma,
  recusado porque exigiria uma barra de título por pane — a superfície da sessão já é do
  terminal, onde arrastar significa selecionar texto.
- Corrigida uma premissa que o SPEC carregava desde a sua v1.2: as sessões criadas por este
  plugin são **sempre** JediTerm/CLASSIC, independentemente do `TerminalEngine` configurado no
  IDE, que só governa a tool window "Terminal" nativa.

### Testes

113 testes automatizados (eram 82 na 0.6.0), todos passando.

---

## [0.6.0] — 2026-08-02

### Adicionado

- **Leitura em voz alta via Piper TTS**, com menu "Áudio" no cabeçalho oferecendo pausar,
  retomar e parar a fala em curso.
- Configuração do executável do `piper` e do caminho para o modelo de voz `.onnx` em
  _Settings → Tools → Claude Code Dock_. Sem os dois configurados, a síntese fica desabilitada.

### Notas

- Sem gerência de vozes, sem controle de velocidade e sem fila: uma fala por vez, e um novo
  play interrompe a anterior.

---

## Versões anteriores

As versões `0.5.x` e anteriores são anteriores a este arquivo e não estão detalhadas aqui. O
histórico completo, com as decisões de projeto e as descobertas de cada rodada, está em
[`plans/sdd/HANDOFF.md`](plans/sdd/HANDOFF.md).

[0.8.0]: https://github.com/reginaldoMorais/claude-code-dock/compare/v0.7.0...v0.8.0
[0.7.0]: https://github.com/reginaldoMorais/claude-code-dock/compare/v0.6.0...v0.7.0
[0.6.0]: https://github.com/reginaldoMorais/claude-code-dock/compare/v0.5.2...v0.6.0
