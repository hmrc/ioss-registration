
# ioss-registration

This is the repository for Import One Stop Shop Registration Backend

Frontend: https://github.com/hmrc/ioss-registration-frontend

Stub: https://github.com/hmrc/ioss-registration-stub

Requirements
------------

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs at least a [JRE] to run.

## Run the application

To update from Nexus and start all services from the RELEASE version instead of snapshot
```
sm --start IMPORT_ONE_STOP_SHOP_ALL
```

### To run the application locally execute the following:
```
sm --stop IOSS_REGISTRATION
```
and
```
sbt 'run 10191'
```

### Running correct version of mongo
We have introduced a transaction to the call to be able to ensure that both the vatreturn and correction get submitted to mongo.
Your local mongo is unlikely to be running a latest enough version and probably not in a replica set.
To do this, you'll need to stop your current mongo instance (docker ps to get the name of your mongo docker then docker stop <name> to stop)
Run at least 4.4 with a replica set:
```  
docker run --restart unless-stopped -d -p 27017-27019:27017-27019 --name mongo4 mongo:4.4 --replSet rs0
```
Connect to said replica set:
```
docker exec -it mongo4 mongo
```
When that console is there:
```
rs.initiate()
```
You then should be running 4.4 with a replica set. You may have to re-run the rs.initiate() after you've restarted


### Using the application
To log in using the Authority Wizard provide "continue url", "affinity group" and "enrolments" as follows:

![image](https://user-images.githubusercontent.com/48218839/145985763-ffb28570-7679-46a9-96fa-e93996f03c23.png)

![image](https://user-images.githubusercontent.com/48218839/145842926-c318cb10-70c3-4186-a839-b1928c8e2625.png)

The VRN can be any 9-digit number.

[//]: # (To successfully register go through the journey providing the answers as follows:)

[//]: # (1.)

[//]: # (![image]&#40;https://user-images.githubusercontent.com/48218839/145986022-f387e3d0-0a41-47d7-9d39-f3b290b8e3ea.png&#41;)

[//]: # ()
[//]: # (2.)

[//]: # (![image]&#40;https://user-images.githubusercontent.com/48218839/145986122-d6f513ba-be1a-4a8c-9e9a-671580719bcd.png&#41;)

[//]: # ()
[//]: # (3.)

[//]: # (![image]&#40;https://user-images.githubusercontent.com/48218839/145986164-4cd4a00a-ec91-4167-be36-e35b9232e672.png&#41;)

[//]: # ()
[//]: # (4.)

[//]: # (![image]&#40;https://user-images.githubusercontent.com/48218839/145986669-58fecb10-16b3-4822-9c8c-6efd3bff325a.png&#41;)

[//]: # ()
[//]: # (5.)

[//]: # (![image]&#40;https://user-images.githubusercontent.com/48218839/145986756-de78aad1-a215-42a0-a71b-aff8ee771103.png&#41;)

[//]: # ()
[//]: # (6.)

[//]: # (![image]&#40;https://user-images.githubusercontent.com/48218839/145986873-fbae18cd-ce3d-46fa-aa10-c9d51aa0cabc.png&#41;)

[//]: # ()
[//]: # (7.)

[//]: # (![image]&#40;https://user-images.githubusercontent.com/48218839/145987158-977dac92-15bb-40e5-84b1-835f836b020f.png&#41;)

Unit and Integration Tests
------------

To run the unit and integration tests, you will need to open an sbt session on the browser.

### Unit Tests

To run all tests, run the following command in your sbt session:
```
test
```

To run a single test, run the following command in your sbt session:
```
testOnly <package>.<SpecName>
```

An asterisk can be used as a wildcard character without having to enter the package, as per the example below:
```
testOnly *RegistrationControllerSpec
```

### Integration Tests

To run all tests, run the following command in your sbt session:
```
it:test
```

To run a single test, run the following command in your sbt session:
```
it:testOnly <package>.<SpecName>
```

An asterisk can be used as a wildcard character without having to enter the package, as per the example below:
```
it:testOnly *RegistrationRepositorySpec
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
