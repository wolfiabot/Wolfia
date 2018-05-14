FROM postgres:10
MAINTAINER napster@npstr.space

RUN apt-get update && apt-get install -y python-pip
RUN pip install b2

ENV POSTGRES_USER wolfia

COPY initdb.sh /usr/local/bin/
COPY run.sh /usr/local/bin/
COPY pg_b2_backup.sh /usr/local/bin/

RUN touch /var/log/pg_backup.log

ADD crontab /etc/cron.d/pg_backup
RUN chmod 0644 /etc/cron.d/pg_backup
RUN touch /var/log/cron.log
RUN /usr/bin/crontab /etc/cron.d/pg_backup

HEALTHCHECK CMD pg_isready -U $POSTGRES_USER

ENTRYPOINT ["/bin/bash", "run.sh"]
