Etik-Tak Backend
================

Getting Started
---------------

Install postgresql. Then run the following psql:

`create database etiktak;`

`create role etiktak with password 'Test1234';`

`alter role etiktak with login;`

Build and start the bundled Jetty container:

`./gradlew build && java -jar build/libs/etik-tak-0.1.0.jar`

Tests
-----

To run the tests:

`./gradlew test`

Architecture
------------

Security Model
--------------

