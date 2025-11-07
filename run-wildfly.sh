#!/usr/bin/env bash
set -euo pipefail

WF_VER="34.0.1.Final"
BASE_DIR="$(pwd)"
WF_DIR="$BASE_DIR/wildfly-$WF_VER"
JBOSS_HOME="$WF_DIR"

# Параметры (можно переопределять через env)
KEYSTORE_PASS="${KEYSTORE_PASS:-changeit}"
TRUSTSTORE_PASS="${TRUSTSTORE_PASS:-changeit}"
MGMT_USER="${MGMT_USER:-admin}"
MGMT_PASS="${MGMT_PASS:-admin123!}"

WAR_SRC="$(ls -1 "$BASE_DIR"/build/libs/*.war 2>/dev/null | head -n1 || true)"

die(){ echo "ERROR: $*" >&2; exit 1; }

command -v java >/dev/null || die "Java не найдена в PATH."
[[ -f "$BASE_DIR/certs/server.p12" ]] || die "Нет certs/server.p12"
[[ -f "$BASE_DIR/certs/truststore.jks" ]] || die "Нет certs/truststore.jks"
[[ -n "${WAR_SRC}" && -f "${WAR_SRC}" ]] || die "WAR не найден в build/libs/*.war"

download() {
  local url="$1" out="$2"
  if command -v curl >/dev/null; then
    curl -fsSL "$url" -o "$out"
  elif command -v wget >/dev/null; then
    wget -qO "$out" "$url"
  else
    die "Нужен curl или wget для скачивания $url"
  fi
}

# --- Скачиваем/распаковываем WildFly
if [[ ! -d "$WF_DIR" ]]; then
  ZIP="wildfly-$WF_VER.zip"
  URL="https://github.com/wildfly/wildfly/releases/download/$WF_VER/wildfly-$WF_VER.zip"
  echo "[wildfly] downloading $URL"
  download "$URL" "$ZIP"
  unzip -q "$ZIP"
  rm -f "$ZIP"
fi

# --- Копируем сертификаты и WAR
CERTS_DIR="$BASE_DIR/certs"
DEPLOY_DIR="$JBOSS_HOME/standalone/deployments"
mkdir -p "$DEPLOY_DIR"
cp -f "$WAR_SRC" "$DEPLOY_DIR/ROOT.war"
cp -f "$CERTS_DIR/server.p12" "$BASE_DIR/server.p12"
cp -f "$CERTS_DIR/truststore.jks" "$BASE_DIR/truststore.jks"

# --- JAVA_OPTS для truststore
export JAVA_OPTS="${JAVA_OPTS:-} -Djavax.net.ssl.trustStore=$BASE_DIR/truststore.jks -Djavax.net.ssl.trustStorePassword=$TRUSTSTORE_PASS"
# Привязки
export JAVA_OPTS="${JAVA_OPTS} -Djboss.bind.address=0.0.0.0 -Djboss.bind.address.management=0.0.0.0"

# --- Создаем mgmt-пользователя (идемпотентно)
if [[ ! -f "$JBOSS_HOME/standalone/configuration/mgmt-users.properties" ]] || \
   ! grep -q "^${MGMT_USER}=" "$JBOSS_HOME/standalone/configuration/mgmt-users.properties"; then
  "$JBOSS_HOME/bin/add-user.sh" --user "$MGMT_USER" --password "$MGMT_PASS" -s
fi

# --- Конфигурация HTTPS через CLI (идемпотентно)
CLI_FILE="$(mktemp)"
cat > "$CLI_FILE" <<'CLI'
embed-server --std-out=echo --server-config=standalone.xml

# Elytron: key-store / key-manager / ssl-context
if (outcome != success) of /subsystem=elytron/key-store=serverKS:read-resource
  /subsystem=elytron/key-store=serverKS:add(path="${env.PWD}/server.p12", type="PKCS12", credential-reference={clear-text="${env.KEYSTORE_PASS:changeit}"})
end-if

if (outcome != success) of /subsystem=elytron/key-manager=serverKM:read-resource
  /subsystem=elytron/key-manager=serverKM:add(key-store=serverKS, credential-reference={clear-text="${env.KEYSTORE_PASS:changeit}"})
end-if

if (outcome != success) of /subsystem=elytron/server-ssl-context=serverSSC:read-resource
  /subsystem=elytron/server-ssl-context=serverSSC:add(key-manager=serverKM, protocols=["TLSv1.3","TLSv1.2"])
end-if

# HTTPS socket-binding
if (outcome != success) of /socket-binding-group=standard-sockets/socket-binding=https:read-resource
  /socket-binding-group=standard-sockets/socket-binding=https:add(port=8443, interface=public)
else
  /socket-binding-group=standard-sockets/socket-binding=https:write-attribute(name=port, value=8443)
end-if

# Undertow HTTPS listener (и выключаем HTTP)
if (outcome == success) of /subsystem=undertow/server=default-server/http-listener=default:read-resource
  /subsystem=undertow/server=default-server/http-listener=default:remove
end-if

if (outcome != success) of /subsystem=undertow/server=default-server/https-listener=https:read-resource
  /subsystem=undertow/server=default-server/https-listener=https:add(socket-binding=https, ssl-context=serverSSC, verify-client=NONE, proxy-address-forwarding=true)
else
  /subsystem=undertow/server=default-server/https-listener=https:write-attribute(name=ssl-context, value=serverSSC)
end-if

# Алиасы хоста
try
  /subsystem=undertow/server=default-server/host=default-host:list-add(name=alias,value=service-b)
catch
end-try
try
  /subsystem=undertow/server=default-server/host=default-host:list-add(name=alias,value=localhost)
catch
end-try
try
  /subsystem=undertow/server=default-server/host=default-host:list-add(name=alias,value=127.0.0.1)
catch
end-try

# Remoting через HTTPS + включить http-invoker
if (outcome == success) of /subsystem=remoting/http-connector=http-remoting-connector:read-resource
  /subsystem=remoting/http-connector=http-remoting-connector:write-attribute(name=connector-ref, value=https)
else
  /subsystem=remoting/http-connector=http-remoting-connector:add(connector-ref=https)
end-if

if (outcome == success) of /subsystem=undertow/server=default-server/http-invoker=http-invoker:read-resource
  /subsystem=undertow/server=default-server/http-invoker=http-invoker:write-attribute(name=enabled, value=true)
end-if

stop-embedded-server
CLI

# Передаем переменные окружения для CLI
export KEYSTORE_PASS
"$JBOSS_HOME/bin/jboss-cli.sh" --file="$CLI_FILE"
rm -f "$CLI_FILE"

echo "[wildfly] starting on https://0.0.0.0:8443  (mgmt http://0.0.0.0:9990, пользователь: $MGMT_USER)"
exec "$JBOSS_HOME/bin/standalone.sh" -b 0.0.0.0 -bmanagement 0.0.0.0
