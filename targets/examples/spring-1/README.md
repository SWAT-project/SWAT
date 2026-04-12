# Spring Boot Example with Symbolic Execution

This Spring Boot application demonstrates how controller parameters can be made symbolic for analysis. It showcases the integration of symbolic execution with a web application.

## Prerequisites

Before running this example, you need to build the engine:

```bash
gradle copyNativeLibs
gradle clean build -x test
```

## Running the Target

Start the target using:

```bash
./run.sh [mode]
```

Where `mode` can be:
- `local` (default): Uses `swat-local.cfg`
- `explorer`: Uses `swat-explorer.cfg`

### Configuration

The run script configures several important parameters:

```bash
java \
-Xmx32g \
-Dconfig.path=swat.cfg \                # Path to config file
-Djava.library.path=../../../libs/java-library-path \  # Path to native solver libraries
-Dagent.logging.level=DEBUG \
-javaagent:../../../symbolic-executor/lib/symbolic-executor.jar \  # Symbolic execution agent
-jar build/libs/spring-1-0.0.1-SNAPSHOT.jar  # Target application
```

### Configuration File

The `swat.cfg` file contains essential settings:

```ini
logging.debug=true
logging.classes=true
instrumentation.transformer=SPRING_ENDPOINT  # Type of values to make symbolic
instrumentation.includePackages=de/uzl/its/examples/sqlvulnerability  # Packages to instrument
logging.level=DEBUG
solver.mode=LOCAL  # Solve constraints in place
```

## Example Usage

### Basic Endpoint Example

Send a request to the safe search endpoint:

```bash
curl "http://localhost:8080/api/users/safe-search?username=user1&unfuzzable=10"
```

This produces a solution from the local solver:

```accesslog
+----------------------------------- Solution (LocalSolver) ------------------------------------+
|                                                                                               |
| [Endpoint] ...UserController/safeSearchUsers(Ljava/lang/String;I).../http/ResponseEntity;     |
| [Input] [java/lang/String_1: user1]                                                           |
| [Input] [I_2: 10]                                                                             |
| [Constraint] (declare-fun I_2 () Int)(assert (= I_2 9472))                                    |
| [Model] [I_2: 9472]                                                                           |
|                                                                                               |
+-----------------------------------------------------------------------------------------------+
```

Two symbolic values are registered:
- `java/lang/String_1` → `user1`
- `I_2` → `10`

These correspond to the endpoint parameters:
```java
@GetMapping("/safe-search")
public ResponseEntity<List<User>> safeSearchUsers(
    @RequestParam String username,
    @RequestParam int unfuzzable) {
```
Both were automatically detected by the instrumentation and lifting capabilities were added by the `SprintSpringEndpointTransformer`. For non-web targets where each execution requires a restart of the target, the mapping from the variabel to the symbolic name is consistent and can be used to `inject` values for future executions (see `examples/basic-1` for a demo). But for Spring the handling has to be done by the target driver such as the fuzzer. 

### Symbolic Object Handling

The engine can track DTO objects symbolically. For example, with the `/create` endpoint:

```java
@PostMapping("/create")
public ResponseEntity<User> createUser(@RequestBody User user) {
```

The user object is tracked symbolically. This means that all of its fields are assumed to be under the users controll and are tracked symbolically. 

Send a request with a user object:
```bash
curl -X POST http://localhost:8080/api/users/create \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "newpassword",
    "email": "newuser@example.com"
  }'
```

The solution shows symbolic tracking of all user-controllable fields:

```accesslog
+----------------------------- Solution (LocalSolver) -------------------------------+
|                                                                                    |
| [Endpoint] ...UserController/createUser(...model/User;)...http/ResponseEntity;     |
| [Input] [java/lang/String_3: newuser]                                              |
| [Input] [java/lang/String_4: newpassword]                                          |
| [Input] [java/lang/String_5: newuser@example.com]                                  |
| [Constraint] (declare-fun java/lang/String_3 () String)                            |
|        (assert (not (= (ite (= java/lang/String_3 "secret_admin") 1 0) 0)))        |
| [Model] [java/lang/String_3: secret_admin]                                         |
|                                                                                    |
+------------------------------------------------------------------------------------+
```

When looking at the `User.java` class:
```java
[...]
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

private String username;
private String password;
private String email;
[...]
```

Three user-controllable fields are present that can also be seen in the solution above as `Input`.


# Running with external symbolic explorer

1. Start the explorer first:
```bash
python3 ../../../symbolic-explorer/SymbolicExplorer.py \
    --config swat-explorer.cfg \
    --mode passive \
    --agent ../../../symbolic-executor/lib/symbolic-executor.jar \
    --z3dir ../../../libs/java-library-path
```

2. Start the target in explorer mode:
```bash
./run.sh explorer
```

The key differences in `swat-explorer.cfg` are:
```ini
solver.mode=HTTP
explorer.host=localhost
explorer.port=8078
```

When sending requests, the solving happens automatically in the explorer (in passive mode, it doesn't directly interact with the target).