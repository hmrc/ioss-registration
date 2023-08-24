
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

![image](https://github.com/hmrc/ioss-registration/assets/36073378/81b753d0-05e5-4f2c-b4ec-a3d834b4c299)

![image](https://user-images.githubusercontent.com/48218839/145842926-c318cb10-70c3-4186-a839-b1928c8e2625.png)

The VRN can be any 9-digit number.

To successfully register go through the journey providing the answers as follows:

1.
![image](https://github.com/hmrc/ioss-registration-frontend/assets/36073378/f56bd082-60c3-42e8-8036-5503b8ed844e)

2.
![image](https://github.com/hmrc/ioss-registration-frontend/assets/36073378/7db67246-ffae-4591-b4e1-1205e6c5fb78)

3.
![image](https://github.com/hmrc/ioss-registration-frontend/assets/36073378/ad366c1b-b922-4dcd-bda5-6678a51bc281)

4.
![image](https://github.com/hmrc/ioss-registration-frontend/assets/36073378/f73d60c6-912a-4b90-add2-fa5ac5aff5c1)

5.
![image](https://github.com/hmrc/ioss-registration-frontend/assets/36073378/27a2e705-17e8-4c6b-847a-215c3c7285b1)

6.
![image](https://github.com/hmrc/ioss-registration-frontend/assets/36073378/50b9ef63-26de-40c7-9e12-30af04c9a03e)

Or alternatively from step 5 above:
5.
![image](https://github.com/hmrc/ioss-registration-frontend/assets/36073378/1d5d89b1-e1c0-4507-8077-347ffd7018af)

6.
![image](https://github.com/hmrc/ioss-registration-frontend/assets/36073378/b8dc83d5-71f4-4a9d-b390-b262af02d13b)

7.
![image](https://github.com/hmrc/ioss-registration-frontend/assets/36073378/50b9ef63-26de-40c7-9e12-30af04c9a03e)

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
