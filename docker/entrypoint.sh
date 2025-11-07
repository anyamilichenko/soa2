#!/usr/bin/env bash
set -euo pipefail

JBOSS_HOME="/opt/jboss/wildfly"

export JAVA_OPTS="${JAVA_OPTS:-} -Djavax.net.ssl.trustStore=/opt/certs/truststore.jks -Djavax.net.ssl.trustStorePassword=${TRUSTSTORE_PASS:-changeit}"

export JAVA_OPTS="${JAVA_OPTS} -Djboss.bind.address=0.0.0.0 -Djboss.bind.address.management=0.0.0.0"

echo "[entrypoint] configuring HTTPS listener if missing..."
cat > /tmp/configure-https.cli <<'CLI'
embed-server --std-out=echo --server-config=standalone.xml

if (outcome != success) of /subsystem=elytron/key-store=serverKS:read-resource
  /subsystem=elytron/key-store=serverKS:add(path="/opt/certs/server.p12",type="PKCS12",credential-reference={clear-text="${env.KEYSTORE_PASS:changeit}"})
end-if

if (outcome != success) of /subsystem=elytron/key-manager=serverKM:read-resource
  /subsystem=elytron/key-manager=serverKM:add(key-store=serverKS,credential-reference={clear-text="${env.KEYSTORE_PASS:changeit}"})
end-if

if (outcome != success) of /subsystem=elytron/server-ssl-context=serverSSC:read-resource
  /subsystem=elytron/server-ssl-context=serverSSC:add(key-manager=serverKM,protocols=["TLSv1.3","TLSv1.2"])
end-if

if (outcome != success) of /socket-binding-group=standard-sockets/socket-binding=https:read-resource
  /socket-binding-group=standard-sockets/socket-binding=https:add(port=8443,interface=public)
end-if

if (outcome != success) of /subsystem=undertow/server=default-server/https-listener=https:read-resource
  /subsystem=undertow/server=default-server/https-listener=https:add(socket-binding=https,ssl-context=serverSSC,verify-client=NONE)
end-if

if (outcome != success) of /socket-binding-group=standard-sockets/socket-binding=https:read-resource
  /socket-binding-group=standard-sockets/socket-binding=https:add(port=8443,interface=public)
end-if
if (outcome != success) of /subsystem=undertow/server=default-server/https-listener=https:read-resource
  /subsystem=undertow/server=default-server/https-listener=https:add(socket-binding=https,ssl-context=serverSSC,verify-client=NONE,proxy-address-forwarding=true)
else
  /subsystem=undertow/server=default-server/https-listener=https:write-attribute(name=ssl-context,value=serverSSC)
end-if

if (outcome == success) of /subsystem=undertow/server=default-server/http-listener=default:read-resource
  /subsystem=undertow/server=default-server/http-listener=default:remove
end-if

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

if (outcome != success) of /subsystem=undertow/server=default-server/host=default-host/setting=access-log:read-resource
  /subsystem=undertow/server=default-server/host=default-host/setting=access-log:add(pattern="%h %l %u %t \"%r\" %s %b \"%{i,Host}\"")
end-if

if (outcome == success) of /subsystem=remoting/http-connector=http-remoting-connector:read-resource
  /subsystem=remoting/http-connector=http-remoting-connector:write-attribute(name=connector-ref,value=https)
else
  /subsystem=remoting/http-connector=http-remoting-connector:add(connector-ref=https)
end-if

if (outcome == success) of /subsystem=undertow/server=default-server/http-invoker=http-invoker:read-resource
  /subsystem=undertow/server=default-server/http-invoker=http-invoker:write-attribute(name=enabled,value=true)
end-if


stop-embedded-server
CLI

"$JBOSS_HOME/bin/jboss-cli.sh" --file=/tmp/configure-https.cli
rm -f /tmp/configure-https.cli

echo "[entrypoint] starting WildFly..."
exec "$JBOSS_HOME/bin/standalone.sh" -b 0.0.0.0 -bmanagement 0.0.0.0
