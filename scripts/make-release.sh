#!/usr/bin/env bash
# 打包「仅源码」的发布包，自动排除 node_modules / target / dist / .idea / 日志
# 用法：bash scripts/make-release.sh            -> 生成 library-system-src.tgz
#       bash scripts/make-release.sh zip        -> 生成 library-system-src.zip
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NAME="library-system-src"
FORMAT="${1:-tgz}"

cd "$ROOT"

EXCLUDES=(
  --exclude=node_modules
  --exclude=target
  --exclude=dist
  --exclude=.idea
  --exclude=logs
  --exclude="*.log"
  --exclude=".env"
  --exclude="*.rar"
  --exclude="*.zip"
  --exclude="*.tgz"
)

cleanup_old() { rm -f "$ROOT/../$NAME.$1" "$ROOT/$NAME.$1" 2>/dev/null || true; }

if [ -d .git ]; then
  echo "==> 检测到 Git 仓库，使用 git archive（只含已提交文件，最干净）"
  case "$FORMAT" in
    zip*) git archive --format=zip -o "../$NAME.zip" HEAD ;;
    *)    git archive --format=tar.gz -o "../$NAME.tgz" HEAD ;;
  esac
  OUT="../$NAME.$([ "${FORMAT#zip}" != "$FORMAT" ] && echo zip || echo tgz)"
else
  echo "==> 未检测到 .git，使用 tar 按规则排除产物"
  echo "    （建议先 git init 并提交，之后打包会更精确）"
  case "$FORMAT" in
    zip*)
      command -v zip >/dev/null 2>&1 || { echo "缺少 zip 命令，请改用 bash scripts/make-release.sh"; exit 1; }
      cleanup_old zip
      # 输出到上级目录，避免把压缩包本身也打进包里
      OUT="$(cd "$ROOT/.." && pwd)/$NAME.zip"
      zip -qr "$OUT" . "${EXCLUDES[@]}" -x "*.git*"
      ;;
    *)
      cleanup_old tgz
      OUT="$(cd "$ROOT/.." && pwd)/$NAME.tgz"
      tar -czf "$OUT" "${EXCLUDES[@]}" .
      ;;
  esac
fi

SIZE="$(du -h "$OUT" | cut -f1)"
COUNT="$(tar -tf "$OUT" 2>/dev/null | wc -l || true)"
echo "==> 完成：$OUT（$SIZE）"
echo "    对比：直接压缩整个目录约 54MB（含 node_modules/target），源码包仅 $SIZE"
