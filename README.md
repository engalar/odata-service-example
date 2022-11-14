# Olingo OData service for Redshift (Example)

This project demonstrates simple implementation of OData v4 service on top of AWS Redshift database.
It is not a complete implementation, but rather a demonstration of how Olingo library can be used to create OData service on top of a JDBC data source.

With some modifications this example can be expanded to work with multiple JDBC data sources.

## Starting the project
Before starting the project make sure you have the following:

- JDK 11
- sbt
- Redshift cluster

Before starting the project, you need to configure redshift settings and make sure you can access the cluster.
This can be done in two ways.

- Change `conf/application.conf` or
- Export environment variables
```shell
export ODATA_DP_REDSHIFT_CONNECTION_STRING="jdbc:redshift://<redshift-cluster-endpoint>:5439/<database-name>"
export ODATA_DP_REDSHIFT_USERNAME=<username>
export ODATA_DP_REDSHIFT_PASSWORD=<password>
``` 

Once that is done, you can start the project by navigating to the project directory and running:
```shell
sbt run
```

By default, application will start on:  
`http://localhost:9000`   
To access the service, you can navigate to:   
`http://localhost:9000/service/<database-name>/$metadata`   
This should show you the metadata for all tables in the given database.  

By navigating to:  
`http://localhost:9000/service/<database-name>/<table-name>`   
You should see first 100 records from the database.
If you are using a browser, results will be returned in XML.
To get the results in JSON, you need to set `Accept` header to `application/json`.

## Implementation
[Apache Olingo](https://olingo.apache.org/doc/odata4/index.html) is used for OData implementation.
[Play Framweork](https://www.playframework.com/) is used as our application framework.
[Scala](https://www.scala-lang.org/) is used as the programing language on top of JVM.
[sbt](https://www.scala-sbt.org/) is the build tool.

Entire implementation is under `/app` directory.
If you are not familiar with Play, it is a web application framework following MVC pattern.  
Our endpoint is therefore implemented in a controller called `ODataController`.  
Actions in the controller are exposed via routes.
Routes are defined in `/conf/routes`.

### Request processing
To process OData request, we are using Apache Olingo.
Since Olingo does not have default support of Play, in order to translate Play HTTP Request into `ODataRequest` we needed to implement custom `ODataHandler`.  
You can find the implementation of the custom handler in `PlayODataHandler`.
`PlayODataHandler` inspects incoming request and maps http url and headers to `ODataRequest`.
In the end, it will call the underlying Olingo base class.

Next part in the processing step is implementation of Olingo `EntityCollectionProcessor`.
Example implements `RedshiftEntityCollectionProcessor`.  
Job of this processor is to orchestrate the rest of request processing.
It needs to make sense of the `ODataRequest` and produce `ODataResponse`.
Dodo this, it needs to extract OData query, get proper entity information and get the data from the underlying data provider.
Olingo library handles most of this processing.
The only thing it does not handle are things specific to the data provider (data source).
For example, translation of OData `$filter` expresion into the expression data provider understands.
This part is handled by `FilterExpressionRedshiftVisitor`.
This example does not implement entire conversion.
It only implements some basic binary operations and `contains` function.
Rest of the implementation should be relatively straight forward.

### Metadata generation
As you know, OData specifies a metadata format.
Generation of the metadata is handled in `RedshiftEdmProvider` which is an implementation of Olingo `CsdlAbstractEdmProvider`.
This class will fetch metadata information for a database and convert it to required Csdl object structure.
Example implementation does this conversion on every request.
Depending on requirements, it is usually a good idea to cache the resulting Csdl object structure.
During metadata generation database schemas are ignored and all generated queries use `public` schema.


### Queries
This implementation does not support full OData query language.
For simplicity, it is limited to only `$select` and simple `$filter` expressions.
All query results are limited to 100 results by appending `LIMIT 100` to the end of every query.
