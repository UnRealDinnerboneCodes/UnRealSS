FROM gradle:6.3.0-jre14 as builder

WORKDIR /build

COPY build.gradle /build
COPY src /build/src

RUN gradle shadowJar

FROM openjdk:14
COPY --from=builder /build/build/libs/UnRealSS-1.0.0-all.jar UnRealSS-1.0.0-all.jar

CMD ["java", "-jar", "UnRealSS-1.0.0-all.jar"]