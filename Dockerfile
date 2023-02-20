FROM gradle:8.0.0-jdk19 as builder

WORKDIR /build

COPY build.gradle /build
COPY gradle.properties /build
COPY src /build/src

RUN gradle shadowJar
RUN ls -l /build/build/libs/

FROM openjdk:19-alpine
COPY --from=builder "/build/build/libs/build-1.0.0-all.jar" "UnRealWeb-1.0.0-all.jar"


CMD ["java", "-jar", "UnRealWeb-1.0.0-all.jar"]