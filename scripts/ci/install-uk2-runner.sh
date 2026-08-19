#!/usr/bin/env bash
# Idempotent GitHub Actions runner for replaycore-java-sdk on ReplayCore UK-2.
# Run on UK-2 as the replaycore user. Does not touch uk2-replaycore-1/2/3/gate.
set -euo pipefail

REG_TOKEN="${REG_TOKEN:-}"
RUNNER_NAME="${RUNNER_NAME:-uk2-replaycore-java-sdk-1}"
LABELS="${LABELS:-linux,x64,replaycore-java-sdk}"
REPO_URL="${REPO_URL:-https://github.com/forgevector-software-limited/replaycore-java-sdk}"
RUNNER_DIR="${RUNNER_DIR:-$HOME/actions-runner-java-sdk}"
SVC_USER="${SVC_USER:-replaycore}"
RUNNER_VERSION="${RUNNER_VERSION:-2.335.1}"
RUNNER_SHA256="${RUNNER_SHA256:-4ef2f25285f0ae4477f1fe1e346db76d2f3ebf03824e2ddd1973a2819bf6c8cf}"

if [[ "${EUID}" -eq 0 ]]; then
  echo 'Run as the replaycore user, not root. svc.sh uses sudo internally.' >&2
  exit 1
fi

if [[ -f "${RUNNER_DIR}/.runner" ]]; then
  echo "Runner already configured at ${RUNNER_DIR}."
  if [[ -x "${RUNNER_DIR}/svc.sh" ]]; then
    sudo "${RUNNER_DIR}/svc.sh" start || true
    sudo "${RUNNER_DIR}/svc.sh" status || true
  fi
  exit 0
fi

if [[ -z "${REG_TOKEN}" ]]; then
  echo 'REG_TOKEN is required for first-time registration.' >&2
  exit 1
fi

mkdir -p "${RUNNER_DIR}"
cd "${RUNNER_DIR}"

archive="actions-runner-linux-x64-${RUNNER_VERSION}.tar.gz"
if [[ ! -f "${archive}" ]]; then
  curl --fail --location --retry 3 --silent --show-error \
    --output "${archive}" \
    "https://github.com/actions/runner/releases/download/v${RUNNER_VERSION}/${archive}"
fi
printf '%s  %s\n' "${RUNNER_SHA256}" "${archive}" | sha256sum --check --status

tar -xzf "${archive}"
sudo ./bin/installdependencies.sh

./config.sh \
  --url "${REPO_URL}" \
  --token "${REG_TOKEN}" \
  --name "${RUNNER_NAME}" \
  --labels "${LABELS}" \
  --work '_work' \
  --replace \
  --unattended

sudo ./svc.sh install "${SVC_USER}"
sudo ./svc.sh start
sudo ./svc.sh status
echo "Registered ${RUNNER_NAME} for ${REPO_URL}."
