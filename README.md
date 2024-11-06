
# alcohol-duty-contact-preferences-frontend

This is the frontend service to capture an alcohol producers communication preference

## Creating pages
This project uses [hmrc-frontend-scaffold.g8](https://github.com/hmrc/hmrc-frontend-scaffold.g8) to create frontend pages.

Please see this [wiki page](https://github.com/hmrc/hmrc-frontend-scaffold.g8/wiki/Usage) for guidance around how to create new pages.

# Running the service

1. Make sure you run all the dependant services through the service manager:

   > `sm2 --start ALCOHOL_DUTY_CONTACT_PREFERENCES_MIN`

2. Stop the frontend microservice from the service manager and run it locally:

   > `sm2 --stop ALCOHOL_DUTY_CONTACT_PREFERENCES_FRONTEND`

   > `sbt run`
> 
> ## Running tests

### Unit tests

> `sbt test`

### Integration tests

> `sbt it/test`

## Scalafmt and Scalastyle

To check if all the scala files in the project are formatted correctly:
> `sbt scalafmtCheckAll`

To format all the scala files in the project correctly:
> `sbt scalafmtAll`

To check if there are any scalastyle errors, warnings or infos:
> `sbt scalastyle`
>

## All tests and checks

This is an sbt command alias specific to this project. It will run a scala format
check, run a scala style check, run unit tests, run integration tests and produce a coverage report:
> `sbt runAllChecks`

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").