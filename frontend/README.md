# Dashboard Dev Docs

Some hints and documentation on how to develop the Wolfia web frontend

## How to start everything

There are three levels of frontend development accessible:
1. To develop only publicly available parts, skip right ahead to [starting the frontend](#start-frontend)
2. To develop publicly available parts that rely on data from the backend (e.g. team page), skip ahead to the [backend setup](#backend-setup)
3. To develop all parts including the dashboard which is visible only to authenticated users, keep on reading.

### Set up  port forwarding
[Ngrok](https://ngrok.com/) or [localhost.run](https://localhost.run/) or a similar service is necessary to enable
Discord authentication during development.
Start it on the port where the frontend dev server will run later:
```
ngrok http 8080
```
Copy the assigned subdomain, we'll need it later. The assigned subdomain is found here:
![ngrok subdomain](https://i.imgur.com/0DB8TjW.png)

### Backend

#### Backend Setup
- Install [Java 11+ using sdkman](https://sdkman.io/)
- Install [docker](https://docs.docker.com/engine/install/)
- Install [docker-compose](https://docs.docker.com/compose/install/)
- Fill out the [wolfia-secrets.example.yaml](../wolfia-secrets.example.yaml) file
- Rename the [wolfia-secrets.example.yaml](../wolfia-secrets.example.yaml) to `wolfia-secrets.yaml`
- Copy or move it to [src/main/resources/](../src/main/resources) (a symlink is also a great idea)


#### Start Backend
- Start docker containers with:
```shell script
docker-compose -f docker/dev/docker-compose.yaml up -d
```
- Start backend with:
```shell script
./gradlew bootRun --args='--spring.profiles.active=dev'
```


### Frontend

#### Frontend Setup
- Install [NodeJs](https://nodejs.org)
- Install [Yarn](https://classic.yarnpkg.com/en/docs/install)
- Configure OAuth2 redirects in the [Discord developer console](https://discord.com/developers/applications) with your
ngrok subdomain:
![AOuth2 Redirect Config](https://i.imgur.com/ISnzOgq.png)
```
# Important:
https://xyz.ngrok.io/dashboard
https://xyz.ngrok.io/login/oauth2/code/discord

# Not so important:
https://discord.com/invite/nvcfX3q
https://discord.gg/nvcfX3q
```
- Add your ngrok subdomain to the `devHost` constant in the [vue.config.js](./vue.config.js) file
- Install Frontend dependencies if you haven't done so yet or in a while:
```shell script
yarn install
```

#### Start Frontend
- Run the frontend dev server:
```shell script
yarn serve
```
- Open browser at your ngrok address https://xyz.ngrok.io


## API

### Style

We're creating a JSON-RPC style API.
That's like REST but without the unnecessary parts like lots of relational endpoints with lots of HTTP verbs and lots of
status codes. We don't want any of those. Keep the API small and to the point, create API endpoints ad hoc as necessary
when new features are being built.

The existing backend api endpoints are located in [src/main/java/space/npstr/wolfia/webapi](../src/main/java/space/npstr/wolfia/webapi)

### Generally used status codes

200 is sent for successes that have a response body.

204 is sent for successes that do not have a response body.

401 means the user is not logged in or the Discord token timed out. Either way, they should log in (again).

403 means a user is logged in but tried to access or modify a resource they should have not access to.

500 means the server is down or the internet connection between client and backend is bad.
