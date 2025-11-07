FROM quay.io/wildfly/wildfly:34.0.1.Final-jdk21

ENV KEYSTORE_PASS=changeit \
    TRUSTSTORE_PASS=changeit \
    MGMT_USER=admin \
    MGMT_PASS=admin123!

USER root

RUN mkdir -p /opt/certs && chown -R jboss:root /opt/certs && chmod 0750 /opt/certs
COPY certs/server.p12     /opt/certs/server.p12
COPY certs/truststore.jks /opt/certs/truststore.jks
RUN chown jboss:root /opt/certs/* && chmod 0640 /opt/certs/*

COPY build/libs/*.war /opt/jboss/wildfly/standalone/deployments/ROOT.war

COPY docker/entrypoint.sh /opt/jboss/entrypoint.sh
RUN chmod +x /opt/jboss/entrypoint.sh

EXPOSE 8080 8443 9990
USER jboss
ENTRYPOINT ["/opt/jboss/entrypoint.sh"]
