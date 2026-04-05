FROM eclipse-temurin:21-jre
EXPOSE 8090
WORKDIR /home/frozenflow
COPY build/libs/ ./
RUN groupadd -r --gid 10001 frozenflow && useradd -g frozenflow -m --shell /bin/false --uid 10001 frozenflow
USER frozenflow
ENTRYPOINT [ "java", "-jar", "frozen-flow.jar" ]
