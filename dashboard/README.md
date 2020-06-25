# Dashboard Dev Docs

Some hints and documentation on how to develop the Wolfia Dashboard

## How to start everything

### Set up ngrok
[Ngrok](https://ngrok.com/) or a similar service is necessary to enable Discord authentication during development.
Install it and start it on the port where the frontend dev server will run later:
```
ngrok http 8080
```
Copy the assigned subdomain, we'll need it later. The subdomain is found here:
![ngrok subdomain](https://i.imgur.com/0DB8TjW.png)

### Running the backend
- Install [Java 11+ using sdkman](https://sdkman.io/)
- Install [docker](https://docs.docker.com/engine/install/)
- Install [docker-compose](https://docs.docker.com/compose/install/)
- Fill out the [wolfia.example.yaml](../wolfia.example.yaml) file
- Rename the [wolfia.example.yaml](../wolfia.example.yaml) to `wolfia.yaml`
- Copy (or better, symlink) it to [./src/main/resources/](../src/main/resources)
- Start docker containers with:
```shell script
docker-compose -f docker/dev/docker-compose.yaml up -d
```
- Start backend with:
```shell script
./gradlew bootRun --args='--spring.profiles.active=dev'
```


### Running the frontend
- Configure OAuth2 redirects in the [Discord developer console](https://discord.com/developers/applications) with your
ngrok subdomain:
![AOuth2 Redirect Config](https://i.imgur.com/ISnzOgq.png)
```
# Important:
http://xyz.ngrok.io/dashboard
https://xyz.ngrok.io/dashboard
http://xyz.ngrok.io/login/oauth2/code/discord
https://xyz.ngrok.io/login/oauth2/code/discord

# Not so important:
https://discord.com/invite/nvcfX3q
https://discord.gg/nvcfX3q
https://discordapp.com/invite/nvcfX3q
```
- Add your ngrok subdomain to the [vue.config.js](./vue.config.js) file
- Install Frontend dependencies if you haven't done so yet:
```shell script
yarn install
```
- Run the frontend dev server:
```shell script
yarn serve
```
- Open browser at your ngrok address https://xyz.ngrok.io


## Implemented status codes

200 is sent for any success, to keep things uncomplicated.

401 or 403 either means the user is not logged in, or the Discord token timed out on the backend. Either way, they need to log in again.

500 usually means the server is down.
