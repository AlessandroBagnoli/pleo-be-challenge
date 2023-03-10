FROM alpine:latest

RUN apk add --no-cache tzdata busybox-suid curl
ENV TZ=UTC

COPY crontab /etc/crontabs/root
COPY scripts /scripts/

RUN chmod +x /scripts/*

CMD crond -f