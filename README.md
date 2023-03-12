## pleo-be-challenge

This repository is a personal solution proposal of the [Pleo Backend Challenge](https://github.com/pleo-io/antaeus).
The task is to build the logic that will schedule payments of invoices on the first of the month. The solution requires
docker to be installed on your machine, and relies on Postgres for the persistence layer and on Google Pub/Sub (via the
official emulator) for the asynchronous communication between different application components and other external
systems.

<!-- TOC -->
  * [pleo-be-challenge](#pleo-be-challenge)
  * [Running the solution](#running-the-solution)
  * [Architectural overview of the proposed solution](#architectural-overview-of-the-proposed-solution)
  * [Process](#process)
    * [Familiarize with the project (1h spent)](#familiarize-with-the-project--1h-spent-)
    * [Work plan (1h spent)](#work-plan--1h-spent-)
    * [Core functionality (1h spent)](#core-functionality--1h-spent-)
    * [Scheduling (1h spent)](#scheduling--1h-spent-)
    * [Focus on scalability (4h)](#focus-on-scalability--4h-)
    * [Testing (3h spent)](#testing--3h-spent-)
  * [Future improvements](#future-improvements)
<!-- TOC -->

## Running the solution

Running `./docker-start.sh` will execute a `docker-compose up -d` that will start automatically the needed services.

Once the services are up and running, the `scheduler` will execute the jobs as explained above. However, if you don't
want to wait for the first day of the month (understandable :smile:), you can import in Postman the collection located
in the `collection` folder. It contains basically the same requests that the `scheduler` makes: one which publishes a
message on PubSub for triggering the processing of the invoices in `PENDING` status, and the other which does the same
for the invoices in `RETRY` status.

## Architectural overview of the proposed solution

![Building blocks](docs/building_blocks.png)

## Process

### Familiarize with the project (1h spent)

Studied the project structure, understanding what is already there and what needs to be done reading the requirements.
Updated Gradle and dependencies to the newest versions

### Work plan (1h spent)

1. Write the main business logic for fetching PENDING invoices from the DAL, and process each one of them
2. Error handling
3. Scheduling
4. Focus on scalability and reliability
5. Unit and functional testing

### Core functionality (1h spent)

### Scheduling (1h spent)

The first approach I used included a Kotlin coroutine running indefinitely...I then chose to use a containerized crontab

### Focus on scalability (4h)

Decoupled the BillingService in order to process each invoice totally independently via pubsub

Replaced sqlite with postgres

### Testing (3h spent)

The testing was done via unit and functional testing, in particular I unit tested both the BillingService and the
InvoiceHandler. The former is responsible for gathering all the invoices in a given state, while the latter takes care
of handling a single invoice, atomically.

However, unit testing may not be enough for this kind of projects: we need to make
sure that also the interactions with the external components like the Postgres DB and PubSub work as expected before
deploying and testing the real system. With that in mind, I used [testcontainers](https://www.testcontainers.org/)
to effectively test all the application components which required external system to be up and running, without mocking
them. What it does is creating short-lived docker containers to mimic a real world environment during the execution of
the test suite.

So, the first thing I did was using a [gradle plugin](https://github.com/unbroken-dome/gradle-testsets-plugin) which
enables to separate the unit tests from the functional tests in different folders, and also split their execution with
dedicated gradle tasks, since functional tests requires docker, and they are way more expensive than unit tests, we may
not want to always execute them.

After that, I functionally tested all the PubSub publishers and subscribers, the DAL, ending with a full flow functional
test which:

* Instantiates all the application components and does the DI job
* Mocks the `PaymentProvider` to always return `true` (notice that this is the only mandatory mocked component)
* Saves an Invoice in `PENDING` status into the DB
* Publishes the message used for triggering the invoice processing flow on the dedicated PubSub topic
* Awaits for the successfully paid invoice notification to be correctly delivered
* Asserts that on the DB the invoice is in `PAID` status

## Future improvements
