# SymantecPAM-PostgreSQL
Symantec PAM connector to verify/update accounts in a PostgreSQL database

This connector is used with a PostgresSQL database to verify and update passwords
for accounts within the database. 


## Build PostgreSQL connector

### Environment
The environment used is as follows:

- CentOS 9 (with SELINUX)
- Java JDK, version 17.0.12
- Apache Tomcat, version 10.1.30
- Symantec PAM, version 4.2.0.826
- capamextensioncore, version 4.21.0.82
- PostgreSQL, version 17.0-1 (Windows)

### Installation
- Download the project sources from GitHub.
- Add the `capamextensioncore.jar` from Symantec PAM as part of local Maven repository.
- Edit the files `postgresql_messages.properties` and `PostgreSQLMessageConstants.java`
and adjust the message numbers to to match your environment.
It is important that the numbers does not conflict with any other numbers from other connectors.
- There is an important variable in the PostgreSQL.java file. It is the constant EXTENDED_DEBUG. If this is set to true when compiling the connector, additional debugging information may be written to the catalina.out log file. If it is written will depend on the loglevel for the connector as defined in the Tomcat logging.properties file. If extended debugging is enabled at compile time, additional information including current and new passwords may be visible in the catalina.out log file. This should not be enabled when compiling the connector for a production environment.
- In the resource file `extensions.properties` there is a setting to control debugging in the Java JDBC driver. 
After deployment to the TCF server, the file is found in `.../webapps_targetconnectors/postgresql/WEB-INF/classes` directory.
The parameter is `postgresql.driver.loglevel`. Valid values are OFF, DEBUG or TRACE. Default is OFF. This can be changed after deployment to a TCF server.
- Run the command `mvnw package` to compile the connector.
- Copy the target connector `postgresqlwar` to the Tomcat `webapps_targetconnectors` directory.
- It is recommended to enable logging from the connector by adding the following to the
Tomcat `logging.properties` file.

```
#
# Target Connectors
#
ch.pam_exchange.pam_tc.postgresql.api.level = FINE
ch.pam_exchange.pam_tc.postgresql.api.handlers= java.util.logging.ConsoleHandler
```
- Finally start/restart Tomcat

## PostgreSQL connector in PAM

The setup uses a master account `pamMaster`, which changes its own password. The dependent account `adm1` is 
found in a different database, thus an application for each database is needed.

### Application

For the `pamMaster` account this is the application used. The account is found in the database `postgre`.

![PostgreSQL Application for pamMaster](/docs/PostgreSQL-application-pamMaster-1.png)
![PostgreSQL Application for pamMaster](/docs/PostgreSQL-application-pamMaster-2.png)

For the dependent `adm1` account this is the application used. The account is found in the database `testdb`.

![PostgreSQL Application for adm1](/docs/PostgreSQL-application-adm1-1.png)
![PostgreSQL Application for adm1](/docs/PostgreSQL-application-adm1-2.png)


The fields used are:

- Database name  
API/CLI field: `database`  
This is the database name where accounts for this application are found.


- Connection Timeout  
API/CLI field: `connectionTimeout`  
This is the connection timeout when establishing a connection to the PostgreSQL database. The time is in milliseconds.


- Login Timeout  
API/CLI field: `loginTimeout`  
This is the timeout for login to the database. The time is in milliseconds.


- Port 
API/CLI field: `port`   
This is the port used when communicating to the PostgreSQL server.


### Account

The account information for `pamMaster` account is fairly simple. If the account is in a different database
than the dependent accounts different applications are used.

![PostgreSQL Account-pamMaster](/docs/PostgreSQL-account-pamMaster-1.png)
![PostgreSQL Account-pamMaster](/docs/PostgreSQL-account-pamMaster-2.png)

The dependent account `adm1` is found in a different database and uses a different application.

![PostgreSQL Account-adm1](/docs/PostgreSQL-account-adm1-1.png)
![PostgreSQL Account-adm1](/docs/PostgreSQL-account-adm1-2.png)


The fields used are:

- Change Process  
API/CLI field name: `changeProcess`  
Valid values are `own` and `other`.  
This radio button controls if the master account is used or not. If set to `Account can change own password` any account
specified as master account is ignored.


- Master Account  
API/CLI field name: `otherAccount`  
The master account is of type PostgreSQL. In the GUI select a master account. In the API/CLI command use the internal ID for the 
master account. The master account is the account used when connecting/logging into the PostgreSQL database. 
The master account must have permissions in PostgreSQL to update the password for other accounts.


## Version history

1.0.0 - Initial release

