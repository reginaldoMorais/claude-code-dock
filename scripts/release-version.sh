#!/usr/bin/env bash
# Versão de release: `X.Y.Z` ou `X.Y.Z-rc.N`. É a regra única dos dois workflows — o
# `release.yml` valida a versão antes de criar a tag, e o `release-plugin.yml` confere a tag que
# chegou (inclusive a empurrada à mão) antes de publicar.
#
#   git tag --list 'v*' | scripts/release-version.sh check 0.3.0-rc.1 refs/heads/main
#   scripts/release-version.sh tag v0.3.0-rc.1
#
# Sem limite por parte: ao contrário do app Android de onde a regra veio, aqui não há versionCode.
set -euo pipefail

# Sem zero à esquerda (semver), e a rc começa em 1.
readonly VERSION_PATTERN='^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-rc\.([1-9][0-9]*))?$'
# Ocupa o lugar da rc na versão final: ela vem depois de toda rc da mesma X.Y.Z.
readonly FINAL_SLOT=999999999

fail() {
  echo "$1" >&2
  exit 1
}

is_valid_version() {
  [[ $1 =~ $VERSION_PATTERN ]]
}

require_valid_version() {
  is_valid_version "$1" ||
    fail "Versão inválida: \"$1\". Use X.Y.Z ou X.Y.Z-rc.N (ex.: 1.2.3 ou 1.2.3-rc.1)."
}

is_prerelease() {
  [[ $1 == *-rc.* ]]
}

# Chave "major minor patch rc" para comparar números, nunca texto. O `sort -V` punha
# `0.3.0-rc.1` depois de `0.3.0` e recusaria a final depois de uma rc.
sort_key() {
  [[ $1 =~ $VERSION_PATTERN ]]
  echo "${BASH_REMATCH[1]} ${BASH_REMATCH[2]} ${BASH_REMATCH[3]} ${BASH_REMATCH[5]:-$FINAL_SLOT}"
}

# Devolve 0 quando a primeira versão é maior que a segunda.
is_greater() {
  local -a first second
  read -ra first <<<"$(sort_key "$1")"
  read -ra second <<<"$(sort_key "$2")"
  local i
  for i in 0 1 2 3; do
    ((first[i] > second[i])) && return 0
    ((first[i] < second[i])) && return 1
  done
  return 1
}

# Maior versão entre as tags da entrada padrão. Tags fora do formato (criadas à mão, experimentos)
# não entram na comparação.
latest_tagged_version() {
  local latest="" tag version
  while IFS= read -r tag; do
    tag=${tag//[[:space:]]/}
    [[ $tag == v* ]] || continue
    version=${tag#v}
    is_valid_version "$version" || continue
    if [[ -z $latest ]] || is_greater "$version" "$latest"; then
      latest=$version
    fi
  done
  echo "$latest"
}

# Validação do `release.yml`, antes de a tag existir: formato, tag repetida, versão que não sobe e
# branch de origem. Imprime a tag a criar.
check_new_version() {
  local version=$1 ref=$2 tags
  require_valid_version "$version"
  tags=$(cat)

  if grep -qxF "v$version" <<<"$tags"; then
    fail "A tag v$version já existe."
  fi

  local latest
  latest=$(latest_tagged_version <<<"$tags")
  if [[ -n $latest ]] && ! is_greater "$version" "$latest"; then
    fail "A versão precisa ser maior que a última tag (v$latest)."
  fi

  # Final só sai da main; rc sai de qualquer branch, para provar uma `feature/*` antes do merge.
  if ! is_prerelease "$version" && [[ $ref != refs/heads/main ]]; then
    fail "Versão final só sai da main (veio de $ref). De outra branch, use uma rc: $version-rc.1."
  fi

  echo "v$version"
}

# Validação do `release-plugin.yml`: a tag precisa do "v" e de uma versão válida.
check_tag() {
  local tag=$1
  [[ $tag == v* ]] || fail "Tag sem o \"v\" inicial: \"$tag\"."
  require_valid_version "${tag#v}"
  if is_prerelease "${tag#v}"; then echo "prerelease=true"; else echo "prerelease=false"; fi
}

main() {
  case "${1:-}" in
    check)
      [[ $# -eq 3 ]] || fail "Uso: release-version.sh check <versão> <ref>"
      check_new_version "$2" "$3"
      ;;
    tag)
      [[ $# -eq 2 ]] || fail "Uso: release-version.sh tag <tag>"
      check_tag "$2"
      ;;
    *)
      echo "Uso: release-version.sh check <versão> <ref>  |  release-version.sh tag <tag>" >&2
      exit 2
      ;;
  esac
}

# Permite o teste carregar as funções com `source` sem executar o main.
if [[ ${BASH_SOURCE[0]} == "$0" ]]; then
  main "$@"
fi
