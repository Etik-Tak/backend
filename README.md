Etik-Tak Backend
================

     _____ _   _ _       _____     _    
    | ____| |_(_) | __  |_   _|_ _| | __
    |  _| | __| | |/ /____| |/ _` | |/ /
    | |___| |_| |   <_____| | (_| |   < 
    |_____|\__|_|_|\_\    |_|\__,_|_|\_\


Getting Started
---------------

### Encryption

Be sure to install unlimited key strength (local_policy.jar & US_export_policy.jar) in .

### Database setup

Install postgresql. Then run the following psql:

`create database etiktak;`

`create role etiktak with password 'Test1234';`

`alter role etiktak with login;`

### Spring Boot

Build and start the bundled container:

`./gradlew bootRun`

Tests
-----

To run the tests:

`$ ./gradlew test`

Architecture
------------

Security Model
--------------

### Contribution model

Contribution model is rather simple. Each client has a trust level between 0 and 1. Likewise, each item that
can accept contributions, e.g. products and companies, have a trust score. This trust score is calculated based
upon the creators/editors trust level and all given votes. A client can only edit an item if the item's trust
score is lower than or equal to his own trust level. That is, items with low trust score can generally be edited
by many clients, while on the other hand items with high trust score can only be edited by clients with high
trust level.

When creating/editing an item, fx. a product, the item will receive a trust score equal to the client's current
trust level. When receiving votes, the item's trust score is updated based upon the outcome of the votes. The more
votes, the higher the weight the votes are given in the calculation, up until a certain percentage.

The client's trust level, on the other hand, is updated whenever an item he's created/edited or voted on receives
a new vote. In the latter case, if his vote is among the majority (e.g. the client trusted the item's details and
the majority of other votes trusted the details as well) it is beneficial for the client. On the other hand, if his
vote is among the minority it lowers his trust. If an item details is voted controversial, e.g. has close to equally
amount of trust- and not-trusted votes, it contributes only little to the client's trust level.

An effect of this contribution model is that a client with low trust level have difficulties in contributing with
edited contents. However, the client can rebuild trust slowly by voting on items he has not himself created/edited.
