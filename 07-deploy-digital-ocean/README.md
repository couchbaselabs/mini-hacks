## Couchbase by Example: Deploy Digital Ocean

So far, you have been running Sync Gateway and perhaps Couchbase Server locally to follow the tutorials. Now it's time to learn how to easily deploy both applications and perhaps an App Server sitting alongside Sync Gateway to a PaaS.

You will use the Docker images for Sync Gateway and [Couchbase Server](https://registry.hub.docker.com/u/couchbase/server/) available in the Docker registry.

Then, you will create a `Dockerfile` for a NodeJS app that imports data from the Google Places API to Sync Gateway. The NodeJS app is taken from the `04-ios-sync-progress-indicator` tutorial.

## Why Docker?

If you've ever done any application development and deployment then you know how difficult it can be to ensure that your development and production servers are the same or at least similar enough that it is not causing any major issue.

With docker, you can build a container that houses everything you need to configure your application dependencies and services.

This container can then be shared and run on any server or computer without having to do a whole bunch of setup and configuration.

Create a new Digital Ocean Droplet

> https://cloud.digitalocean.com/droplets/new

Pick 2GB for the RAM:

![](http://cl.ly/image/2D2w0H0a0a2W/Screen%20Shot%202015-07-08%20at%2009.27.21.png)

Choose Ubuntu for the distribution and on the Applications tab, select Docker. Create the Droplet and connect to it via SSH.

## Installing Sync Gateway and Couchbase Server

In the command line, install Couchbase Server with the following:

```
docker run -d -p 8091:8091 couchbase/server
```

Open the getting started wizard in Chrome on port `http://<host>:8091`:

![](http://cl.ly/image/2o07072W1l3T/Screen%20Shot%202015-07-08%20at%2009.47.34.png)

After completing the wizard, navigate to the `Data Buckets` tab to see the created bucket (you will configure Sync Gateway to connect to the `default` bucket):

![](http://cl.ly/image/0s1f3m2O1v1r/Screen%20Shot%202015-07-08%20at%2009.48.47.png)

Copy the necessary files from the `04-ios-sync-progress` tutorial to this project:

```
git clone git@github.com:couchbaselabs/Couchbase-by-Example.git
cd couchbase-by-example/04-ios-sync-progress-indicator
cp requestRx.js sync-gateway-config.json sync.js package.json ./../07-deploy-digital-ocean/
```

Create a new copy of the config file to set it up with Couchbase Server:

```
cd 07-deploy-digital-ocean/
cp sync-gateway-config.json production-sync-gateway-config.json
```

Update `production-sync-gateway-config.json` with the server IP and bucket name:

```javascript
{
  "log": ["*"],
  "databases": {
    "db": {
      "server": "http://46.101.14.135:8091/",
      "bucket": "default",
      "users": { "GUEST": { "disabled": false, "admin_channels": ["*"] } }
    }
  }
}
```

Push the files to a github repository.

Specify the url to the production config file in the docker command to run the Sync Gateway container:

```bash
$ docker run -d -p 4984:4984 -p 4985:4985 couchbase/sync-gateway http://git.io/vq25r
```

**NOTE**: You ran `docker run` but this time specified the `-d` flag. It tells Docker to run the container and put it in the background, to daemonize it.

Run the `docker ps` command to check that both containers are running:

```
root@MyApp:~# docker ps
CONTAINER ID        IMAGE                    COMMAND                CREATED             STATUS              PORTS                                                                           NAMES
ca7d4358941a        couchbase/sync-gateway   "/usr/local/bin/sync   2 minutes ago       Up 2 minutes        0.0.0.0:4984-4985->4984-4985/tcp                                                focused_bell
c9411d002831        couchbase/server         "couchbase-start cou   52 minutes ago      Up 52 minutes       8092/tcp, 11207/tcp, 11210-11211/tcp, 0.0.0.0:8091->8091/tcp, 18091-18092/tcp   grave_feynman
```

Use the `docker logs` command specifying the container id to print the STDOUT to your console.

**TIP**: Use the `-f` flag to follow the logs.

In the next section, you will write a simple Dockerfile to deploy the NodeJS application to the same Droplet.

## Deploying an App Server

Open a new file named `Dockerfile` and paste the following:

```
# Set the base image to Ubuntu
FROM ubuntu

# Install Node.js and other dependencies
RUN apt-get update && \
    apt-get -y install curl && \
    curl -sL https://deb.nodesource.com/setup | sudo bash - && \
    apt-get -y install python build-essential nodejs

# Install nodemon
RUN npm install -g babel

# Provides cached layer for node_modules
ADD package.json /tmp/package.json
RUN cd /tmp && npm install
RUN mkdir -p /src && cp -a /tmp/node_modules /src/

# Define working directory
WORKDIR /src
ADD . /src
```

- Ubuntu base image pulled from Docker Hub
- Install Node.js and dependencies using apt-get
- Install babel to run `sync.js` because it's written in ES6.
- Run npm install in a temporary directory and copy to src (for caching node_modules)
- Copy the application source from the host directory to src within the container

Build a Docker image using the Dockerfile:

```
docker build -t myapp .
```

Create a container for the custom image:
```
docker run -it myapp /bin/bash
```

**NOTE**: The `-i` flag is used to keep STDIN open and `-t` to allocate a pseudo-TTY.

Notice the command line prompt in the container. From there you can run the import script:

```
babel-node sync.js
```

Use `Ctrl + D` to exit the container.

You can push the Dockerfile to the GitHub repo and pull the changes in the droplet. Then run the same commands to build and run the image.

## Conclusion

Yay! Now you know how to use the docker images to deploy Sync Gateway and Couchbase Server and creating your own images for other applications running alongside them.

## Extra: Accessing the Admin Dashboard

The Admin Dashboard is available on `http://localhost:4985/_admin/` and only accessible on the internal network where Sync Gateway is running.

However, you can use SSH tunnelling to create a connection with the droplet. This currently doesn't work with Sync Gateway instances running in Docker containers.

If you already have a Docker instance running Sync Gateway, you can stop it with:

```
docker stop <container_id>
```

Install Sync Gateway with wget:

```
wget http://packages.couchbase.com/releases/couchbase-sync-gateway/1.0.0/couchbase-sync-gateway-community_1.0.0_x86_64.deb
sudo dpkg -i couchbase-sync-gateway-community_1.0.0_x86_64.deb
```

By default it will be installed to **/opt/couchbase-sync-gateway/bin/sync_gateway**.

Start it with the command:

```
$ /opt/couchbase-sync-gateway/bin/sync_gateway
```

### Creating an SSH tunnel

Open a terminal on your host machine and enter the following command where <host> is the public ip address of the droplet:

```
ssh -ND 8080 root@<host>
```

**Explanation**

- N: hides the output from the SSH connection. It is optional. If you wish to use the SSH connection to run commands on the server as you normally would, remove the N switch.
- D: 8080 creates a dynamic port, in this case 8080, on your local computer. This is how your browser, or other software, will connect to the tunnel.

### Using as a browser proxy

1. Open `System Preferences > Network > Advanced > Proxies`
2. Check the checkbox next to SOCKS Proxy
3. Under SOCKS Proxy Server, enter `localhost` and `8080` as the port. Leave all the other fields blank.
   ![](http://cl.ly/image/2v1l1W002T3T/Screen%20Shot%202015-07-08%20at%2015.02.56.png)
4. Click OK and Apply.

Open `http://localhost:4985/_admin` in Chrome and you should see the Admin Dashboard.

![](http://cl.ly/image/0E0F2b200P0Y/Screen%20Shot%202015-07-08%20at%2015.04.30.png)

**Reference**

> https://whatbox.ca/wiki/SSH_Tunneling