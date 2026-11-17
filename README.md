![](img/bin/PNG/logo/256x256x32.png)

# this-or-that

"This or That" is an online tool to make group decisions.
Each participant of the voting can make votes based on images by selecting one of two displayed images.

## Environments
### Client
* Web client runs locally on http://localhost:8081 (or configured deployment URL)

### Server (API & Swagger)
When the server is running:
 * **Swagger UI**: http://localhost:8080/swagger-ui.html
 * **OpenAPI Spec (JSON)**: http://localhost:8080/v3/api-docs
 * **OpenAPI Spec (YAML)**: http://localhost:8080/v3/api-docs.yaml

## Requirements

### Server
 * Spring Boot 4.1.2
 * Java 25
 * Maven
 * MongoDB

### Client 

#### Web
 * NodeJS
 * NPM

## Build

Build the entire project (frontend and backend) with Maven:
```bash
mvn clean package
```
