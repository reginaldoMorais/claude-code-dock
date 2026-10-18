#!/usr/bin/env bash
# Testes de `release-version.sh`. Sem framework: cada caso roda o script num subprocesso e confere
# o código de saída e a saída. Roda local e no `release.yml`:
#
#   scripts/release-version.test.sh
set -uo pipefail

readonly SCRIPT="$(dirname "$0")/release-version.sh"
failures=0
total=0

# expect <descrição> <código esperado> <trecho esperado na saída> <stdin> <args...>
expect() {
  local description=$1 expected_code=$2 expected_output=$3 input=$4
  shift 4
  total=$((total + 1))
  local output code
  output=$(printf '%s' "$input" | bash "$SCRIPT" "$@" 2>&1)
  code=$?
  if [[ $code -ne $expected_code || $output != *"$expected_output"* ]]; then
    failures=$((failures + 1))
    echo "FALHOU: $description"
    echo "  esperado: código $expected_code, saída contendo \"$expected_output\""
    echo "  obtido:   código $code, saída \"$output\""
  fi
}

readonly MAIN=refs/heads/main
readonly FEATURE=refs/heads/feature/x
readonly TAGS=$'v0.2.4\nv0.2.5\nexperimento\nv0.3.0-rc.1\n'

# Formato
expect "aceita versão final" 0 "v1.2.3" "" check 1.2.3 "$MAIN"
expect "aceita rc" 0 "v1.2.3-rc.1" "" check 1.2.3-rc.1 "$MAIN"
expect "aceita partes acima de 99" 0 "v1.100.250" "" check 1.100.250 "$MAIN"
expect "recusa rc sem número" 1 "Versão inválida" "" check 0.3.0-rc "$MAIN"
expect "recusa rc zero" 1 "Versão inválida" "" check 0.3.0-rc.0 "$MAIN"
expect "recusa sufixo desconhecido" 1 "Versão inválida" "" check 0.3.0-beta.1 "$MAIN"
expect "recusa zero à esquerda" 1 "Versão inválida" "" check 01.2.3 "$MAIN"
expect "recusa \"v\" na versão" 1 "Versão inválida" "" check v1.2.3 "$MAIN"
expect "recusa versão incompleta" 1 "Versão inválida" "" check 1.2 "$MAIN"
expect "recusa metacaractere de shell" 1 "Versão inválida" "" check '1.2.3;rm -rf /' "$MAIN"

# Ordem e tags existentes
expect "aceita a próxima rc" 0 "v0.3.0-rc.2" "$TAGS" check 0.3.0-rc.2 "$MAIN"
expect "aceita a final depois de uma rc (o sort -V recusaria)" 0 "v0.3.0" "$TAGS" check 0.3.0 "$MAIN"
expect "aceita rc de uma versão maior" 0 "v0.4.0-rc.1" "$TAGS" check 0.4.0-rc.1 "$MAIN"
expect "recusa tag repetida" 1 "A tag v0.3.0-rc.1 já existe" "$TAGS" check 0.3.0-rc.1 "$MAIN"
expect "recusa versão menor que a última" 1 "maior que a última tag (v0.3.0-rc.1)" "$TAGS" check 0.2.6 "$MAIN"
expect "recusa rc de uma final já publicada" 1 "maior que a última tag (v0.3.0)" $'v0.3.0\n' check 0.3.0-rc.2 "$MAIN"
expect "compara número, não texto (0.10.0 > 0.9.0)" 1 "maior que a última tag (v0.10.0)" $'v0.9.0\nv0.10.0\n' check 0.9.1 "$MAIN"
expect "rc.10 vem depois de rc.9" 0 "v1.0.0-rc.10" $'v1.0.0-rc.9\n' check 1.0.0-rc.10 "$MAIN"
expect "ignora tags fora do formato" 0 "v0.0.1" $'experimento\nv1.0\nfoo-v9.9.9\n' check 0.0.1 "$MAIN"

# Branch
expect "recusa final fora da main" 1 "Versão final só sai da main" "" check 1.0.0 "$FEATURE"
expect "sugere rc ao recusar final fora da main" 1 "use uma rc: 1.0.0-rc.1" "" check 1.0.0 "$FEATURE"
expect "aceita rc fora da main" 0 "v1.0.0-rc.1" "" check 1.0.0-rc.1 "$FEATURE"

# Tag recebida pelo release-plugin.yml
expect "tag final não é pré-release" 0 "prerelease=false" "" tag v1.2.3
expect "tag rc é pré-release" 0 "prerelease=true" "" tag v1.2.3-rc.4
expect "tag sem \"v\" é recusada" 1 "sem o \"v\" inicial" "" tag 1.2.3
expect "tag fora do formato é recusada" 1 "Versão inválida" "" tag v1.2.3-rc1

# Uso
expect "comando desconhecido mostra o uso" 2 "Uso:" "" foo
expect "check sem ref mostra o uso" 1 "Uso: release-version.sh check" "" check 1.2.3

if [[ $failures -gt 0 ]]; then
  echo "$failures de $total testes falharam."
  exit 1
fi
echo "$total testes passaram."
