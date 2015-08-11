# Couchbase by Example: Channel, Users, Roles

In `04-ios-sync-progress-indicator`, you learnt how to use RxJS and the request module to import documents into a Sync Gateway database from the Google Places API. To keep it simple, you enabled the GUEST user with access to all channels. In this tutorial, you will configure the Sync Function to allow authenticated users to post reviews.

The Sync Function validates document contents, and authorizes write access to documents by channel, user, and role.

In total, there will be 3 types of roles in the application:

 - **level-1**: users with the **level-1** role can post reviews but they must be accepted by users with the **level-3** role (i.e. moderators) to be public.
 - **level-2**: users can post reviews without validation needed from moderators. This means they can post a comment without requiring an approval.
 - **level-3**: users can approve reviews or reject them.

## Download Sync Gateway

Download Sync Gateway and unzip the file:

> http://www.couchbase.com/nosql-databases/downloads#Couchbase\_Mobile

You will find the Sync Gateway binary in the `bin` folder and examples of configuration files in the `examples` folder. Copy the `users-role.json` file to the root of your project:

```bash
cp ~/Downloads/couchbase-sync-gateway/examples/users-roles.json /path/to/proj/sync-gateway-config.json
```

In the next section, you will update the configuration file to create users and roles.

## Channels, Users and Roles

In `sync-gateway-config.json`, update the db object to read:

```javascript
{
  "log": ["*"],
  "databases": {
    "db": {
      "server": "walrus:",
      "users": {
        "jens": {
          "admin_roles": ["level-1"],
          "password": "letmein"
        },
        "andy": {
          "admin_roles": ["level-2"],
          "password": "letmein"
        },
        "william": {
          "admin_roles": [],
          "password": "letmein"
        },
        "traun": {
          "admin_roles": ["level-3"],
          "password": "letmein"
        }
      },
      "roles": {
        "level-1": {},
        "level-2": {},
        "level-3": {}
      },
    }
  }
}
```

A couple of things are happening above:

 1. You create the user `jens` with the `level-1` role.
 2. You create the user `andy` with the `level-2` role.
 3. You create the user `william` without any role.
 4. You create the user `traun` with the `level-3` role.
 5. You define the 3 roles. Just like users, roles must be explicitly created on the Admin REST API or in the config file. 

**Note on creating roles**

The easiest way to create roles is in the configuration file as you did above.

Another way to create roles is through the admin REST API. Provided that you expose an endpoint to create those roles from the application, you can create roles dynamically by sending a request to your app server (blue arrows) which will create the role and send back a 201 Created if it was successful (green arrows).

![](http://cl.ly/image/3D0606230F1C/Dynamic%20Roles.png)

In the next section, you will add the Sync Function to handle write and read operations for the three different types of documents (`restaurant`, `review`, `profile`).

## Sync Function

Roles and users can both be granted access to channels. Users can be granted roles, and inherit any channel access for those roles.

Channel access determines a user’s read security. Write security can also be based on channels (using requireAccess), but can also be based on user/role (requireUser and requireRole), or document content (using throw).

Read and write access to documents are independent. In fact write access is entirely governed by your sync function: unless the sync function rejects the revision, a client can modify any document. All the require* functions act as validators but also write access APIs.

It's very common to see sync function creating lots and lots of channels. This is absolutely fine. However, it can get cumbersome to assign each user in turn to a channel. Instead you can use a role!

Let this sink in one more time, users can be granted roles and inherit any channel access for those roles.

This means you can grant a user access to multiple channels by simply assigning a role. This is very powerful and can greatly simplify the data model in your application.

With roles, you don't need to assign every single user to a channel. You simply grant the role access to the channel and assign the users to the role.

With that in mind, replace the sync function in `sync-gateway-config.json`:

```javascript
function(doc, oldDoc) {
  if (doc.type == "restaurant"){
    channel(doc.restaurant_id);
  } else if (doc.type == "review") {
    switch(doc.role) {
      case "level-1": // Step 1
        requireRole(doc.role);
        var channelname = doc.owner + "-in-review";
        channel(channelname);
        access(doc.owner, channelname);
        access("role:level-3", channelname);
        break;
    case "level-2": // Step 2
      requireRole(doc.role);
      channel(doc.restaurant_id);
      break;
    case "level-3": // Step 3
      requireRole(doc.role);
      channel(doc.restaurant_id);
      break;
    }
  } else if (doc.type == "profile") {
    requireRole("level-3");
    role(doc.name, "role:" + doc.role);
  }
}
```

Here's what's happening:

 1. Users with the **level-1** role have write access because you call the `channel` function. Then grant that user and the **level-3** access to this channel. This is where the power of roles really shines. By granting a role access, you are granting all the users with that role access to the channel. The call to `requireRole` will grant the write permission.
 2. Documents of type `review` created by a **level-2** role: the document should go in the same channel as the restaurant it belongs to. The call to `requireRole` will grant the write permission.
 3. Documents of type `review` created by a **level-3** role: the document should go in the channel for that restaurant. **level-3** users also have read access to all the `{user_name}-in-review` channels. They can approve/reject the pending reviews of other users.

Start Sync Gateway with the updated configuration file:

```bash
$ ~/Downloads/couchbase-sync-gateway/bin/sync_gateway /path/to/proj/sync-gateway-config.json
```

In this example, you are utilising the 3 main features of roles:

 - Granting a role access to a channel and indirectly to all the users with that role.
 - Granting write permission using a requireRole.
 - Assigning a role to a user.

Now you can test the Sync Function behaves as expected with the following HTTP requests.

**Scenario 1**

Documents of type `review` created by a **level-1** user: the document should go in the `{user_name}-in-review` channel and the users with the **level-3** role should have access to this channel too.

Login as the user `jens`:

```bash
curl -vX POST -H 'Content-Type: application/json' \
     :4984/db/_session \
     -d '{"name": "jens", "password": "letmein"}'
     
> POST /db/_session HTTP/1.1
> User-Agent: curl/7.37.1
> Host: :4984
> Accept: */*
> Content-Type: application/json
> Content-Length: 39
> 
* upload completely sent off: 39 out of 39 bytes
< HTTP/1.1 200 OK
< Content-Length: 103
< Content-Type: application/json
* Server Couchbase Sync Gateway/1.1.0 is not blacklisted
< Server: Couchbase Sync Gateway/1.1.0
< Set-Cookie: SyncGatewaySession=6c52b8cd2c706d55e97d9606058c0abd90a5d200; Path=/db/; Expires=Tue, 07 Jul 2015 08:23:03 UTC
< Date: Mon, 06 Jul 2015 08:23:03 GMT
< 
* Connection #0 to host  left intact
{"authentication_handlers":["default","cookie"],"ok":true,"userCtx":{"channels":{"!":1},"name":"jens"}}⏎                                                                            
```

Save a new document of type `review` (substitute the token with the one returned in the `Set-Cookie` header above):

```bash
curl -vX POST -H 'Content-Type: application/json' \
     --cookie 'SyncGatewaySession=d007ceb561f0111512c128040c32c02ea9d90234' \
     :4984/db/ \
     -d '{"type": "review", "role": "level-1", "owner": "jens"}'
```

 - Check that user `jens` has access to the channel `jens-in-review` and the comment document is in there.

 ![](http://cl.ly/image/190111230227/Screen%20Shot%202015-07-06%20at%2010.48.44.png)

 - Check that user `traun` has access to channel `jens-in-review`.

 ![](http://cl.ly/image/2j2f0z2z1K0M/Screen%20Shot%202015-07-06%20at%2010.50.13.png)

You can also view the channels this document belongs to and roles/users that have access to it in the `Documents` tab:

![](http://cl.ly/image/1p3J410N0L2C/Screen%20Shot%202015-07-06%20at%2010.53.19.png)

**Scenario 2**

Granting write access using a role.

Login as `andy` and replace the token with the one you got back from the login request.

```bash
curl -vX POST -H 'Content-Type: application/json' \
              --cookie 'SyncGatewaySession=6e7ce145ae53c83de436b47ae37d8d94beebebea' \
              :4984/db/ \
              -d '{"type": "review", "role": "level-2", "owner": "andy", "restaurant_id": "123"}'
```

- Check that the comment was added to the restaurant channel (named `123` in this example).

![](http://cl.ly/image/1g283S032M0w/Screen%20Shot%202015-07-06%20at%2010.53.01.png)

**Scenario 3**

Assigning a role to a user.

Login as `traun` and replace the token with the one you got back from the login request.

```bash
curl -vX POST -H 'Content-Type: application/json' \
              --cookie 'SyncGatewaySession=3a5c5a67ff67643f8ade175363c65354584429e9' \
              :4984/db/ \
              -d '{"type": "profile", "name": "william", "role": "level-3"}'
```

 - Check that `william` has role `level-3`.
 - Check that `william` has access to the `jens-in-review` channel.

 ![](http://cl.ly/image/092F173R350B/Screen%20Shot%202015-07-06%20at%2010.55.37.png)

## Conclusion

In this tutorial, you learnt how to use channels and requireRole to dynamically validate and perform write operations. You also assigned multiple channels at once to multiple users using the role API.