FROM postgres:11
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

HEALTHCHECK --interval=1s --retries=1 CMD /usr/bin/psql -U $POSTGRES_USER -tAc "SELECT 1 FROM pg_database WHERE datname='$DB';" | grep -q 1 || exit 1

ENTRYPOINT ["/bin/bash", "run.sh"]
