# Changelog

Todas as mudanças relevantes deste projeto são registradas aqui.

O formato segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/), e o versionamento
segue [SemVer](https://semver.org/lang/pt-BR/).

---

## [0.10.0] — 2026-08-10

Versão da **voz**. Entra o Kokoro-ONNX, com vozes bem melhores e escolha entre as 54 do modelo, em
9 idiomas. E a fala deixa de esperar: antes o plugin sintetizava o trecho **inteiro** antes de
emitir o primeiro som — um parágrafo grande custava uns 18 segundos de silêncio. Agora começa a
falar em torno de 2,5 segundos, independente do tamanho do trecho. O Piper continua disponível, e
ganha a mesma melhoria de brinde.

### Adicionado

- **Kokoro-ONNX como motor de voz**, agora o padrão. Escolha entre ele e o Piper em
  _Settings → Tools → Claude Code Dock_. Quem já tinha o Piper configurado continua funcionando
  sem mexer em nada. O Kokoro precisa de um Python com os pacotes `kokoro-onnx` e `onnxruntime`,
  mais o modelo e o arquivo de vozes — os três se apontam na mesma tela.
- **Seletor de voz para os dois motores.** As vozes do Kokoro são lidas do próprio arquivo de
  vozes e vêm rotuladas por idioma: as três de português (`pf_dora`, `pm_alex`, `pm_santa`) e mais
  51 em inglês, espanhol, francês, italiano, hindi, japonês e mandarim. As do Piper são as vozes
  instaladas ao lado do modelo escolhido, rotuladas pelo idioma que cada arquivo declara.

### Alterado

- **A fala começa assim que o primeiro pedaço de áudio existe**, em vez de esperar a síntese
  terminar. No mesmo trecho de ~800 caracteres: Kokoro saiu de ~18 s para ~2,4 s, e o Piper de
  ~5,4 s para ~2,2 s. O tempo até a primeira palavra passa a ser praticamente o mesmo para um
  parágrafo ou para a tela inteira.
- **Pausar agora pausa também a síntese**, e não só o som — consequência natural da reprodução
  contínua.
- **0,25x saiu da lista de velocidades.** O Kokoro não aceita nada abaixo de 0,5x, e a lista é a
  mesma para os dois motores. Quem tinha 0,25x escolhido passa a 0,5x automaticamente, sem precisar
  reconfigurar.

### Corrigido

- **Vozes do Piper em qualidade `low` e `x_low` tocavam aceleradas e com o tom alterado.** Elas são
  gravadas a 16000 Hz, e o plugin tocava tudo a 22050 Hz — 38% mais rápido. São 40 das 173 vozes do
  catálogo oficial, uma delas em português (`pt_BR-edresson-low`). A taxa passa a ser lida do
  arquivo que acompanha cada voz. Quem usa vozes `medium` ou `high`, como a `pt_BR-faber-medium`,
  nunca foi afetado.
- **Parar uma fala deixou de ser registrado como erro no log.** Interromper uma fala, ou começar
  outra por cima, produzia um aviso de falha no `idea.log` — barulho que atrapalha justamente quem
  for investigar um problema real.

### Notas

- **O motor não era o gargalo — a reprodução era.** Os dois motores sempre produziram áudio aos
  poucos; o desenho anterior descartava isso ao exigir a fala inteira em memória antes de tocar.
  Por isso o Piper, sem nenhuma mudança própria, ficou mais rápido nesta versão.
- **Quatro otimizações foram medidas e descartadas**, com os números registrados para não voltarem
  como sugestão: manter o modelo carregado em segundo plano (compraria 1,6 s ao custo de ~500 MB
  parados), o modelo `int8` de 92 MB (**4,5× mais lento**, não mais rápido), um utilitário de linha
  de comando próprio, e o cache de grafo do ONNX.
- **Se a primeira fala demorar muito mais que isso**, o suspeito é o Python: o caminho do
  interpretador precisa apontar para aquele que tem os pacotes instalados. Em instalações com
  `pyenv` ou `asdf`, use o caminho absoluto — o PATH que o IDE herda pode não incluir a pasta dos
  atalhos.

### Documentação

- `plans/sdd/SPEC.md` evoluiu de 1.10.2 para 1.11: RF-54 a RF-57, RNF-35 e RNF-36, o defeito das
  vozes `low` (DEF-10) e os achados 37 a 41.
- **Achado 39:** o `Clip` era o gargalo, e o motor levou a culpa. A troca de motor parecia uma
  escolha entre qualidade de voz e tempo de espera; medir o tempo até o **primeiro som**, em vez do
  tempo total, mostrou que nenhum dos dois números era do motor.
- **DEF-10 registra uma lição que não é sobre áudio:** um valor verificado uma vez virou constante
  no código, e a verificação media uma amostra de um.

### Testes

163 testes automatizados (eram 142 na 0.9.0), todos passando, sem avisos de compilação.

---

## [0.9.0] — 2026-08-09

Versão do **trecho selecionado**: a barra flutuante ganha um play e, enfim, fica de pé sobre uma
sessão viva — sobre um TUI que rola sozinho ela nascia e sumia antes de dar tempo de clicar.
Acompanha o consumo do Claude Code a um clique do cabeçalho.

### Adicionado

- **Tocar o trecho selecionado direto da barra flutuante** — terceiro botão, ao lado de copiar e
  exportar. Passa pelo mesmo caminho de síntese do menu "Áudio", então a velocidade da fala
  (v0.8.0), a pausa/retomada e a regra de uma fala por vez continuam valendo, com um dono só: o
  play na barra e o do menu comandam a mesma reprodução.
- **"Uso" no cabeçalho**, logo depois de "Retomar Sessão": envia `/usage` à sessão em foco. O
  resultado aparece **dentro da própria sessão**, como a tela do CLI (`Esc` sai dela e devolve o
  prompt), e não em popup. Sem sessão aberta, avisa.

### Corrigido

- **A barra da seleção aparecia e sumia sobre uma sessão em uso.** Ela nascia e era destruída
  ~270 ms depois, "quase piscando", sem tempo de clicar em nada. Quem a derrubava era o
  fechamento em massa de popups da plataforma, disparado por um evento de foco. A barra deixou de
  ser um popup da plataforma: desenha igual, e agora fica de pé — com e sem `Shift`.
- **Os botões liam a seleção na hora do clique.** Uma rolagem do terminal entre a barra aparecer e
  o clique fazia copiar, exportar e tocar agirem sobre nada — em silêncio, no caso do copiar.
  Agora agem sobre o trecho capturado no instante em que a barra aparece.
- **Tocar sem o Piper configurado não dizia nada** — só registrava no log. Agora notifica, tanto
  pelo botão quanto pelo menu: o aviso passou para o serviço, por onde os dois caminhos passam.

### Notas

- **Colar print screen com `Ctrl+V` já funciona** e não exigiu código: o `Ctrl+V` chega ao CLI
  dentro da janela dedicada e a imagem é anexada normalmente. O que faltava era um pacote do
  sistema (`wl-clipboard`, no Wayland). Avaliado como pedido e arquivado sem implementação.

### Documentação

- `plans/sdd/SPEC.md` evoluiu de 1.9 para 1.10.2: RF-50, RF-51 e RF-53; DEF-08 e DEF-09; decisões
  D-41 e D-42; achados 32 a 36; e o teto de dois botões no popup (R-23) revogado e substituído por
  um critério mais estreito (R-29): entra na barra o que opera sobre o trecho selecionado e cabe
  em um clique.
- **Achado 36:** mecanismo confirmado não é o mesmo que causa observada. O primeiro diagnóstico de
  DEF-09 — a rolagem do terminal apagando a seleção — é real e verificado no bytecode, e **não**
  era o que fechava a barra. Ficou registrado como tentativa fracassada, em vez de reescrito.
- A investigação da pane que às vezes não conectava ao MCP foi **arquivada** após duas
  não-reproduções: o Achado 32 documenta o mecanismo do plugin oficial, mas a causa do caso real
  segue desconhecida.

### Testes

142 testes automatizados (eram 121 na 0.8.0), todos passando.

---

## [0.8.1] — 2026-08-09

Versão de **dívida paga**: duas promessas que a documentação fazia desde a v0.6 e o código nunca
cumpriu, e a lacuna de testes de integração mais antiga do projeto.

### Adicionado

- **"Enviar Seleção para o Claude Code"**, no menu de contexto do editor e no menu _Tools_: manda
  a referência do trecho selecionado (`@arquivo#Lx-y`) para a sessão **em foco** e traz a janela à
  frente. Não substitui o `Ctrl+Alt+K` do plugin oficial — resolve o que ele não faz: entregar a
  uma pane só, e nesta janela. Sem atalho padrão, de propósito; quem quiser liga um em
  _Settings > Keymap_.

### Corrigido

- **"Parar" não parava o Piper, e a síntese não tinha prazo.** O processo nunca era registrado,
  então nem o limite de 20 s prometido desde a v0.6 nem a interrupção da fala anterior por um novo
  play alcançavam o `piper` — o áudio em curso seguia até o fim, e uma síntese travada travava com
  ela. A leitura da saída passou para thread própria: ela bloqueia até o processo terminar, e sem
  isso o prazo seria decorativo.

### Documentação

- `plans/sdd/SPEC.md` evoluiu de 1.9 para 1.9.4. **DEF-08:** o `Ctrl+Alt+K` do plugin oficial
  nunca poderia focar esta janela — ele varre só a tool window nativa "Terminal". O conteúdo do
  trecho **chega** às nossas panes; o que vai para a janela errada é o foco. É defeito de
  ergonomia, e a ação nova o contorna sem tocar no protocolo privado do oficial.

### Testes

136 testes automatizados (eram 121 na 0.8.0), com os **primeiros testes de integração** do projeto:
registro da tool window, fiação da sessão, isolamento entre abas e a cadeia de `Disposable` que
libera o PTY.

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

[0.10.0]: https://github.com/reginaldoMorais/claude-code-dock/compare/v0.9.0...v0.10.0
[0.9.0]: https://github.com/reginaldoMorais/claude-code-dock/compare/v0.8.1...v0.9.0
[0.8.1]: https://github.com/reginaldoMorais/claude-code-dock/compare/v0.8.0...v0.8.1
[0.8.0]: https://github.com/reginaldoMorais/claude-code-dock/compare/v0.7.0...v0.8.0
[0.7.0]: https://github.com/reginaldoMorais/claude-code-dock/compare/v0.6.0...v0.7.0
[0.6.0]: https://github.com/reginaldoMorais/claude-code-dock/compare/v0.5.2...v0.6.0
