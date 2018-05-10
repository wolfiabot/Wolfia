This is a customized PostgreSQL docker image that is used for Wolfia, but can be easily adapted to
other applications.

This image is built abd published manually whenever there is an actual change to it (rare).

Some of the things here might looks complicated, they are the result of lessons learned when 
supporting selfhosting.

Rationale behind the way this image creates the hstore extension:

- We can't create the Hstore extension from application code, because it requires using a role
that has administrative priviledges. We don't want such a role available to application code.
- We could create the Hstore extension as part of the init db script / another .sql file, but those
are only run exactly when the database (not the docker container) is created. Since we persist
the database with a volume, it is only ever created once. We would not be able to create another
extension that way.
- It is a soft verification that the role, database, and required extensions are always present.


### Additional Features:
- daily, weekly, and monthly backups to Backblaze B2 Cloud Storage via cronjobs



### Parameters :
```
ROLE                        user / role of the application
DB                          database used by the application

BACKUP_DB                   database to backup
BACKUP_APPNAME              name of the application to backup (meta information)
BACKUP_BUCKET_DAILY         b2 bucket for daily backups
BACKUP_BUCKET_WEEKLY        b2 bucket for weekly backups
BACKUP_BUCKET_MONTHLY       b2 bucket for monthly backups
BACKUP_ACCOUNT_ID           b2 account id
BACKUP_APP_KEY              b2 app key
```
