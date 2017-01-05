## Couchbase by Example: Couchbase Mobile and Continuous Deployment with Docker Hub and Tutum

Deploying the back-end architecture to support a mobile application can be a daunting and repetitive task but thanks to different
tools such as Docker and Tutum we can automate that process into a deployment pipeline. You only have to set it
up once and then it will automatically re-deploy the app to a staging or production server when new commits are detected
in a particular GitHub repository.

In this tutorial you will use 3 components to support the back-end infrastructure of your mobile application:

- Couchbase Server: the persistence layer in the Couchbase Mobile stack.
- Sync Gateway: the syncing layer replicating the data to and from mobile devices.
- Node.js App Server: your app server with additional business logic.

The release pipeline will look like this:

![](http://cl.ly/image/081O0Y381z1o/flow.png)

### Getting Started

In this tutorial you will deploy the different components in [this GitHub repository](https://github.com/jamiltz/CityExplorer) to a production environment on Tutum. Clone the project to a new directory:

```
$ git clone git@github.com:jamiltz/CityExplorer.git
```

**CityExplorer** is an application to sync interesting places and venues from the Google Places API to Couchbase Lite. The `ios-sync-progress-indicator` and `android-fast-iterations` tutorials go into more detail about the application.

All you have to know for this tutorial is that there is a webhook on the Sync Gateway to notify the App Server to pull in data from the Google Places API and save them into Sync Gateway using the Admin REST API.

![](http://cl.ly/image/3q0S3e2A3K1H/Android%20Fast%20Iterations.png)

The repository holds the code for the App Server, the Sync Gateway configuration file and Dockerfiles that you will write in the following sections.

### Dockerizing Sync Gateway and the App Server

The App Server will need to access the Admin REST API on port 4985. Currently the Admin REST API is only accessible from localhost (127.0.0.1). For that reason, you will run Sync Gateway and the App Server in the same docker image.

![](http://cl.ly/image/1b332G300d0c/containers.png)

There is an official docker image for Sync Gateway on Docker Hub that makes it very easy to start a container running Sync Gateway:

> https://hub.docker.com/r/couchbase/sync-gateway/

You will base your Dockerfile on the Sync Gateway image as follows:

```
# 1) Set the base image to Ubuntu
FROM couchbase/sync-gateway

# Install tools
RUN yum install -y \
      curl \
      git \
      perl \
      which \
 && yum clean all

# 2) Install Node.js
RUN curl -sL -o ns.rpm https://rpm.nodesource.com/pub/el/7/x86_64/nodejs-0.10.31-1nodesource.el7.centos.x86_64.rpm \
 && rpm -i --nosignature --force ns.rpm \
 && rm -f ns.rpm

RUN npm install -g pangyp\
 && ln -s $(which pangyp) $(dirname $(which pangyp))/node-gyp\
 && npm cache clear\
 && node-gyp configure || echo ""

ENV NODE_ENV production

# Install babel
RUN npm install -g babel

# Provides cached layer for node_modules
ADD package.json /tmp/package.json
RUN cd /tmp && npm install
RUN mkdir -p /src && cp -a /tmp/node_modules /src/

# 3) Define working directory
WORKDIR /src
ADD . /src

RUN chmod +x /src/run.sh

# 6) Configuration
ENTRYPOINT ["/bin/sh", "-c"]

CMD ["/src/run.sh"]

EXPOSE 8000 4984
```

Here's what is happening step by step:

1. Use the `sync-gateway` base image to base your work on the existing image.
2. Install Node.js, npm, node modules and babel. Those are the dependencies for the App Server that's written in Node.js.
3. Set the working directory to `/src` and copy all files in the current directory of the host to the filesystem of the container in `/src`.
4. Override the entrypoint and the command to run a bash script named `run.sh` in `/src`. Finally expose port 8000 (App Server) and 4984 (Sync Gateway public endpoint).

Create a new file `run.sh` with the following commands:

```bash
#!/usr/bin/env bash
sleep 20
/usr/local/bin/sync_gateway ./sync-gateway-config.json &
babel-node server.js
```

This bash script consists of 3 instructions:

- Wait 20 seconds. Indeed, Sync Gateway will return a 502 error if the Couchbase Server it tries to connect to isn't running. The sleep command will leave us enough time to bootstrap the Couchbase Server starting process
- Run Sync Gateway passing in the configuration file located in the current directory
- Start the App Server

At this stage you might think that it's enough to create a new docker compose file with your cityexplorer image and the [Couchbase Server image](https://hub.docker.com/r/couchbase/server/) from Docker Hub.

However, the bucket specified in the Sync Gateway configuration file must exist in order for Sync Gateway to connect successfully. You will need to add a bit more configuration to create a bucket automatically on Couchbase Server if it doesn't already exist.

### Couchbase Server container

Create a new folder called `server` and add a new `Dockerfile`:

```
FROM couchbase/server

RUN mkdir -p /src

WORKDIR /src/
ADD . /src/

RUN chmod +x /src/run.sh

ENTRYPOINT ["/bin/sh", "-c"]

CMD ["/src/run.sh"]

EXPOSE 8091
```

In this image you're only serving your own commands to bootstrap Couchbase Server. In `run.sh`, add the following:

```
#!/usr/bin/env bash
/entrypoint.sh couchbase-server &

sleep 15

buckets=$(/opt/couchbase/bin/couchbase-cli bucket-list \
-c 127.0.0.1 \
-u Administrator -p password)

if [ -z "$buckets" ]
	then
		/opt/couchbase/bin/couchbase-cli cluster-init \
		-c 127.0.0.1 \
		--cluster-init-username=Administrator \
		--cluster-init-password=password \
		--cluster-init-ramsize=600 \
		-u admin -p password

		/opt/couchbase/bin/couchbase-cli bucket-create \
		-c 127.0.0.1:8091 \
		--bucket=default \
		--bucket-type=couchbase \
		--bucket-port=11211 \
		--bucket-ramsize=600 \
		--bucket-replica=1 \
		-u Administrator -p password
fi

while true ; do continue ; done
```

Here's what's happening:

- Wait for 15 seconds to make sure the server is up
- Create a user and check if there is already an existing bucket and create one named `default` if it doesn't exist

### Docker Compose

Docker Compose is a tool to orchestrate Docker containers.

Follow the instructions [here](https://docs.docker.com/compose/install/) to install Docker Compose on your machine.

Add the docker-compose file with the following code:

```
cityexplorer:
  build: ./
  ports:
    - "4984:4984"
  links:
    - couchbaseserver
couchbaseserver:
  build: ./server
  ports:
    - "8091:8091"
```

In this file, you're doing the following:

1. **cityexplorer**: Build the image based on the instructions in *Dockerfile*. Expose port `4984` as mobile devices will connect on that port and link this container to `couchbaseserver`.
2. **couchbaseserver**: Build the image based on the instructions in *server/Dockerfile*. Expose port `8091` to access the Couchbase Server admin console remotely. 

Run this command to start both containers:

```
$ docker-compose up
```

![](http://cl.ly/image/1b3b1l173W1t/Screen%20Shot%202015-08-30%20at%2010.35.06.png)

Great! You have dockerized your app server and configured Docker Compose to use both images. In the next section you will learn how to deploy them to Docker Hub and setup an automated build for the `cityexplorer` image.

### Configuring Docker Hub

The image for Couchbase Server we configured is available on Docker Hub and you can use it for your own application since there is nothing specific to this application in it. It's just creating an admin user (`Administrator` and `password`) and a bucket for Sync Gateway to connect to:

> https://hub.docker.com/r/jamiltz/mycbserver/

Let's now focus on the automated build for the `cityexplorer` image. Head over to Docker Hub:

1. Choose the `Create` menu and select `Create Automated Build`.
2. Link to your GitHub account and select the repository that you cloned earlier (you may have to fork the `cityexplorer` repository on GitHub to be able to select it). Docker Hub will trigger an initial build.

Each time you push to GitHub, Docker Hub will generate a new build from scratch.

### Tutum

Tutum manages the orchestration and deployment of Docker images and containers. Create a new account on [Tutum](http://tutum.co) and link it to your Digital Ocean account. Create a new Node.

On the **Stacks** tab, click the **Create stack** button. Give a name to the stack like `CityExplorer` and paste the following in the stackfile input box:

```
cityexplorer:
  image: jamiltz/cityexplorer
  ports:
    - "4984:4984"
  links:
    - couchbaseserver
  tags:
    - cityexplorer-staging
couchbaseserver:
  image: jamiltz/mycbserver:v1
  ports:
      - "8091:8091"
```

You should now see a new stack and you can click the start button:

![](http://cl.ly/image/2R0X0k1u2p3Y/Screen%20Shot%202015-08-30%20at%2010.55.05.png)

Once the stack is up and running click on it and you will see 2 services running (couchbaseserver and cityexplorer). Click on cityexplorer and open the **Logs** tab:

![](http://cl.ly/image/2u0p1x0z0H0r/Screen%20Shot%202015-08-30%20at%2010.58.03.png)

You should see the same output as the one you saw when you were running the containers locally.

You will find the Digital Ocean IP address on the Nodes tab and opening `http://5.101.104.197:4984` in the browser should display the Sync Gateway welcome message.

### Continuous Delivery

Finally, we need to sync Docker Hub with Tutum so that when a new build is created on Docker Hub, the services are rebuilt and redeployed on Tutum automatically.

In the Tutum UI, under the *Services*, click the *web* service and choose the *webhooks* tab. Create a new webhook and copy the URL. In the Docker Hub UI choose the *webhooks* and add a new one pasting in the URL from Tutum.

### Running Sync Gateway Admin Commands

Since the Sync Gateway Admin REST API can only be accessed on localhost (127.0.0.1) where it's running you will have to run a Terminal session in the `cityexplorer` container. Tutum provides an easy way to do so in the *Services* tab:

![](http://cl.ly/image/1G3F382R1h1D/68747470733a2f2f692e6779617a6f2e636f6d2f36313037333961646438383063333833353033656136303761613031633533302e676966.gif) 

### Conclusion

Using a continuous delivery workflow will ease the development and maintenance necessary to release new versions of your application. Grab the final code from the [repo](https://github.com/jamiltz/CityExplorer).

Also make sure to check out the Couchbase Mobile continuous deployment slide deck [here](https://speakerdeck.com/jamiltz/continuous-deployment-with-couchbase-mobile-docker-and-tutum).