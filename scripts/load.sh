#!/usr/bin/env bash
# Tráfego sintético end-to-end:
#   1. cria consent via POST /open-banking/payments/v1/consents
#   2. autoriza inline via POST /sim/holder/consents/{id}/authorise (mimic SCA)
#   3. cria payment via POST /open-banking/payments/v1/pix/payments
#
#   make load
#   HOST=http://10.0.0.5:8082 ./scripts/load.sh
#
# Pré-requisitos: app rodando com profile simulator (`make run-sim`).

set -u

HOST="${HOST:-http://localhost:8082}"
DELAY_MS="${DELAY_MS:-100}"

curl_post() {
    local path=$1
    local body=$2
    curl -s -o /dev/null -w "%{http_code}\n" \
        -H "Content-Type: application/json" \
        -X POST "$HOST$path" -d "$body" || true
}

create_consent() {
    local cents=$1
    local amount=$(awk -v c="$cents" 'BEGIN{printf "%.2f", c/100.0}')
    curl -s -H "Content-Type: application/json" \
        -X POST "$HOST/open-banking/payments/v1/consents" \
        -d "{\"loggedUser\":{\"name\":\"Loja Demo\",\"document\":\"12345678901\",\"documentType\":\"CPF\"},\"creditor\":{\"ispb\":\"60746948\",\"issuer\":\"0001\",\"number\":\"00012345-6\",\"type\":\"CACC\"},\"amount\":\"$amount\",\"currency\":\"BRL\"}" \
        | grep -o '"consentId":"[^"]*"' | sed 's/"consentId":"//;s/"$//'
}

authorise() {
    local consent=$1
    curl_post "/sim/holder/consents/$consent/authorise" "{}"
}

create_payment() {
    local consent=$1
    curl_post "/open-banking/payments/v1/pix/payments" \
        "{\"consentId\":\"$consent\",\"debtor\":{\"ispb\":\"99988877\",\"issuer\":\"0002\",\"number\":\"1234567\",\"type\":\"CACC\"}}"
}

trap 'echo; echo "stopping load"; exit 0' INT TERM

echo "loading $HOST (delay=${DELAY_MS}ms). Ctrl+C to stop."
i=0
while true; do
    AMOUNTS=(150 250 500 1000 2500 4999 9925)
    cents=${AMOUNTS[$((i % 7))]}
    consent=$(create_consent "$cents")
    if [ -n "$consent" ]; then
        authorise "$consent" >/dev/null
        create_payment "$consent" >/dev/null
    fi
    sleep "0.$((DELAY_MS / 100))"
    i=$((i + 1))
done
