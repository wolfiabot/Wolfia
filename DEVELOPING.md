# General Development Documentation

## Dev Environment & Tools
You should be a comfortable with the command line.

#### Operating System
Mac and Linux operating systems work best for development.
Using Windows is not recommended (utilizing the [Windows Subsystem for Linux](https://docs.microsoft.com/en-us/windows/wsl/install-win10) is an option however).

#### IDE
Recommended: [IntelliJ IDEA](https://www.jetbrains.com/idea/) Community Version. JetBrains is [sponsoring](https://www.jetbrains.com/community/opensource/#support)
ultimate licenses for core contributors of the project.

#### Java
Java is required to build the backend and execute any Gradle tasks.
The minimum required version is found in the [build.gradle](build.gradle) file (search for "JavaVersion").
Use [sdkman](https://sdkman.io/) to install and manage it.

#### Gradle
Installing Gradle is optional, as the project has the [gradle wrapper](./gradlew) available.
The currently used version can be found in the [gradle-wrapper.properties](gradle/wrapper/gradle-wrapper.properties) file.
Use [sdkman](https://sdkman.io/) to install and manage it.

#### Node/NPM
NodeJS/NPM a prerequisite for using Yarn.
The currently used version can be found in the [versions.gradle](gradle/versions.gradle) file.
[Get it from the official website](https://nodejs.org/en/).

#### Yarn
Yarn is used to develop the web frontend / dashboard.
The currently used version can be found in the [versions.gradle](gradle/versions.gradle) file.
[Get it from the official website](https://classic.yarnpkg.com/en/docs/install).

#### Docker & Docker Compose
[Docker](https://docs.docker.com/get-docker/) is required to build the project, as it uses testcontainers to start a
real database to be used during tests.

[Docker Compose](https://docs.docker.com/compose/install/) is handy to run containers with required services during
development.

#### PostgreSQL
This project uses the [PostgreSQL](https://www.postgresql.org/) relational database to store and query important data.
The currently used version can be found in the [databases' Dockerfile](docker/database/Dockerfile).
Installing it is optional, running the development `docker-compose` is recommended instead:
```shell script
docker-compose -f docker/dev/docker-compose.yaml up -d
```

#### Redis
This project uses the [Redis](https://redis.io/) in-memory key-value database to store expiring data and caches.
The currently used version can be found in the [dev docker-compose](docker/dev/docker-compose.yaml) file.
Installing it is optional, running the development `docker-compose` is recommended instead:
```shell script
docker-compose -f docker/dev/docker-compose.yaml up -d
```

## Code Formatting

The project comes with an [.editorconfig](.editorconfig) file. Please use an IDE that respects it. Currently there is no
autoformatter set up for the Java sources. If you know a decent one, please suggest it.

For the frontend, an eslint configuration is provided. Ideally, you should set up your IDE to run eslint on file changes
or on save. You can run eslint manually like this:
```
cd frontend
yarn run lint
```

## Philosophy

### Branches
Branches should be shortlived. Work should be merged into the master branch as often as possible.

There are several reasons to do so:
- Splitting up large tasks into smaller individual chunks helps focussing on what needs to be done, step by step
- Short living branches result in fewer merge conflicts. Noone likes solving merge conflicts.
- Code is deployed daily. This opportunity should be used to see it in action as fast a possible.
- Reviewing large Pull Requests is hard and takes a lot of time until feedback is ready.
- Getting feedback early and often is important for everyones happyness. Dropping a 2k lines of code pull request that, after review, needs to be done from scratch does not create any positive feelings.


## Libraries & Frameworks

### Frontend
The [web frontend](frontend) uses the [Vue.js framework](https://vuejs.org/) including
[Vue router](https://router.vuejs.org/) and [Vuex](https://vuex.vuejs.org/).

We also use the [Bulma](https://bulma.io/) CSS framework, together with [Buefy](https://buefy.org/) components
and a [Bulmaswatch](https://jenil.github.io/bulmaswatch/) theme.

### Backend

#### Database

##### PostgreSQL
[PostgreSQL](https://www.postgresql.org/) is the main database.
We deploy a customized [Docker image](docker/database/Dockerfile) that contains init scripts to set up required database
extensions as well as backup scripts to back up the data to [Backblaze B2](https://www.backblaze.com/b2).

##### Flyway & jOOQ
[Database schema migrations](database-codegen/src/main/resources/db/migrations) are handled by [Flyway](https://flywaydb.org/).

[jOOQ](https://www.jooq.org/) is used for database querying.

During build time, [Docker](https://docs.docker.com/get-docker/), [Flyway](https://flywaydb.org/) and [jOOQ](https://www.jooq.org/)
are using to run migrations against a temporary database container and generate code from the resulting database schema.

#### Spring Boot
The [Spring Framework](https://spring.io/) via [Spring Boot](https://spring.io/projects/spring-boot) is the main
framework used in the backend code base.

##### Inversion of Control
Spring provides annotation based dependency injection, an important pillar of SOLID software.

##### Eventsystem
We make use of the Spring Eventsystem to decouple our components where necessary. For example, the
`DiscordEventListenerPublisher` class publishes all Discord events. Across the codebase, there are then multiple
`@EventListener`s processing these events.

##### Web Server
We use Spring's web server abstraction to serve various endpoints for the frontend, receiving webhooks,
or allowing metrics collection from other parts of the infrastructure.

##### Three layers
The Spring Application Context is populated with Components.
Roughly, there are three types (or layers) to the various Spring Components:
1. Controllers & Commands aka C&C
2. Services
3. Repositories

There are also Components that do not belong to any of the layers, for example some of our components parse and execute
Commands, and some Components are necessary to interact with datasources like the database for example.

**Controllers** receive user input from the web. **Commands** receive user input from Discord.
Both of them make sure that the input has an expected format, identify the user,
and make sure they are allowed to do what they are trying to do.
C&C should only talk to services, not other C&Cs or Repositories.
Controllers can be identified by the `@RestController` annotation in the code.
Commands can be identified by the `@Command` annotation in the code.

**Services** are what happens in between C&C and Repositories. They are the largest layer.
There isn't a hard-and-fast rule what services actually are, it is easier to think of them as not being C&C and not
being Repositories. They usually contain the actual logic of an application.
Services may talk to other Services, or Repositories.
Services can be identified by the `@Service` annotation in the code.

**Repositories** handle storing and fetching data from various sources, mostly our database.
Repositories can be identified by the `@Repository` annotation in the code.
Repositories should not talk to anything else except the components directly necessary to interact with their data
source.



### TODO Topics not covered yet:
- file layout
- grouping by features
- immutables.org
- feature flags
- sonar
- testing
- oauth2
- metrics/prometheus/grafana/sentry
- command system
- JSON-RPC
- game engine (haha yeah it barely even exists)
- rendering discord responses
- DTOs/VOs etc
- spring profiles
- nullability
- travis
- .gitignore
- secrets file
- listings
