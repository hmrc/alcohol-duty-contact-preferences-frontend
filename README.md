# alcohol-duty-contact-preferences-frontend

This is the frontend microservice to capture an alcohol producer's communication preference.

## Creating pages

This project uses [hmrc-frontend-scaffold.g8](https://github.com/hmrc/hmrc-frontend-scaffold.g8) to create frontend
pages.

Please see this [wiki page](https://github.com/hmrc/hmrc-frontend-scaffold.g8/wiki/Usage) for guidance around how to
create new pages.

# Running the service

1. Make sure you run all the dependant services through the service manager:

   > `sm2 --start ALCOHOL_DUTY_CONTACT_PREFERENCES_MIN`

2. Stop the frontend microservice from the service manager and run it locally:

   > `sm2 --stop ALCOHOL_DUTY_CONTACT_PREFERENCES_FRONTEND`

   > `sbt run`

The service runs on port `16005` by default.

## Navigating the service

1. Navigate to the auth-login-stub: http://localhost:9949/auth-login-stub/gg-sign-in

2. Change the following:
    - **CredId**: * *Provide from stub data if email verification is to be stubbed, otherwise use any value* *
    - **Redirect URL**: 
      - User changing contact preference: http://localhost:16005/manage-alcohol-duty/contact-preference/start/contact-preference
      - User on email, updating their email address: http://localhost:16005/manage-alcohol-duty/contact-preference/start/update-email
      - User with bounced email: http://localhost:16005/manage-alcohol-duty/contact-preference/start/bounced-email
    - **Affinity group**: Organisation
    - **Enrolments**: Add an enrolment with:
      - **Enrolment Key**: HMRC-AD-ORG
      - **Identifier Name**: APPAID
      - **Identifier Value**: * *Provide from stub data* *

See the email-verification service's README for information on the
[email verification API endpoints](https://github.com/hmrc/email-verification?tab=readme-ov-file#api).

To get the email verification code, look inside the _journey_ collection in the email-verification Mongo repository.
Find the latest created document for the _credId_ supplied on the auth-login-stub.

### Test only

When running the frontend microservice, allow routes in test.routes to be called:

> `sbt "run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes"`

#### Clear all data

The following URL will clear all user answers in the cache:
http://localhost:16005/manage-alcohol-duty/contact-preference/test-only/clear-all

## Running tests

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

This code is open source software licensed under
the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
