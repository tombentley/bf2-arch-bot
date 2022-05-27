# bf2-arch-bot Project

This project is a github bot used to support workflows on the [Application Services Architecture](https://github.com/bf2fc6cc711aee1a0c2a/architecture) repo.

Application Services Architecture has a number of different types of _record_:

* ADRs (Architecture Decision Records) -- Apply decisions (or APs) to a particular managed service.
* PADRs (Platform ADRs) -- Apply decisions across all services.
* APs (Architectural Patterns) -- candidates for PADRs.

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Features

### Creation of records

Record pages can be created automatically from GitHub issues via a comment.
The bot will respond to the comment, opening a PR which creates a new record and automatically merging it. 
The record id etc. are automatically managed.
See `org.bf2.arch.bot.CreateDraftRecordFlow` for more details.

From there the author writes their content and eventually opens a second

### Labelling PRs

When record-altering PRs get opened the bot will label them. 
A simple state machine model is used, driven by events (delivered via webhook) on the PR & its corresponding issue. 

```
   <start>
      |
      v
needs-reviewers
      |
      v
being-reviewed
      |
      v
awaiting-merge
      |
      v
    <end>
```

See `org.bf2.arch.bot.ArchReviewStateMachineFlow`.

Perodically the bot will check for issues where the discussion appears to have stalled and labels them for attention.
See `org.bf2.arch.bot.StalledDiscussionFlow`.

### Automatic reviewing of PRs

`org.bf2.arch.bot.PrReviewFlow` seeks to provide some basic automated review of PRs which touch records. 
The intent is to provide some consistency between records, while not being too annoying.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.recordType=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/bf2-arch-bot-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## Related Guides

- GitHub App ([guide](https://quarkiverse.github.io/quarkiverse-docs/quarkus-github-app/dev/index.html)): Automate GitHub tasks with a GitHub App
