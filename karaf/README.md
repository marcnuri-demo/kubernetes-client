## Building project

Following command will create a Karaf assembly in `target/assembly`:
```shell script
$ mvn clean install
```

## Running the project
```shell script
$ ./target/assembly/bin/karaf run
```

## Debugging the project
```shell script
# No suspend
$ ./target/assembly/bin/karaf run debug
# Suspend
$ ./target/assembly/bin/karaf run debugs
```

# About classloaders and ServiceLoader API

- https://blog.osgi.org/2011/05/what-you-should-know-about-class.html
- https://blog.osgi.org/2013/02/javautilserviceloader-in-osgi.html