# Load Balancer Demo — Spring Boot + Eureka + Spring Cloud LoadBalancer

3 microservices, built with Java 17 + Spring Boot 3.3, to show **client-side load
balancing** — the pattern most real Spring microservices use.

```
loadbalancer-demo/
├── eureka-server/     (port 8761) - service registry ("phone book")
├── product-service/   (port 8081/8082/8083) - run 2-3 instances of THIS
├── order-service/     (port 8090) - calls product-service through the load balancer
└── api-gateway/       (port 8080) - single front door for everything below it
```

## How to run (import each folder as a separate Maven project in IntelliJ/Eclipse)

### 1. Start Eureka Server first
```
cd eureka-server
mvn spring-boot:run
```
Open http://localhost:8761 — you'll see the Eureka dashboard, empty for now.

### 2. Start product-service TWICE (or three times), on different ports

Terminal 1:
```
cd product-service
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Terminal 2:
```
cd product-service
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8082
```

(Optional) Terminal 3 for a 3rd instance:
```
cd product-service
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8083
```

Refresh http://localhost:8761 — under "Instances currently registered", you should
now see `PRODUCT-SERVICE` with 2 (or 3) entries. This is the key thing to understand:
**they're all registered under the same name**, so to the outside world they look
like one service with multiple copies.

### 3. Start order-service
```
cd order-service
mvn spring-boot:run
```

### 4. Start api-gateway
```
cd api-gateway
mvn spring-boot:run
```
Check http://localhost:8761 again — you should now see 4 apps registered:
`EUREKA-SERVER` isn't listed (it's the registry itself), but `API-GATEWAY`,
`ORDER-SERVICE`, and `PRODUCT-SERVICE` (x2) should all show up.

### 5. Hit everything through the gateway only (port 8080)
From now on you don't need to remember order-service is on 8090 or
product-service is on 8081/8082 — the gateway is the only port you talk to:
```
http://localhost:8080/api/order/place
http://localhost:8080/api/order/stress-test
http://localhost:8080/api/products
```
The gateway looks at the path (`/api/order/**` or `/api/products/**`), strips
`/api`, and forwards the rest to whichever service + instance the load
balancer picks — same round-robin behavior as before, just now with one
public-facing port instead of three.

### 6. Watch the load balancer in action
Hit this endpoint repeatedly (browser refresh, or Postman, or curl in a loop):
```
http://localhost:8080/api/order/place
```
Look at `productServiceResponse.servedByPort` in the JSON response. It will
alternate: 8081, 8082, 8081, 8082... That's round-robin load balancing.

Or hit this once to fire 10 calls back-to-back and see the whole pattern:
```
http://localhost:8080/api/order/stress-test
```

### 7. Kill one instance mid-demo (optional but instructive)
Stop the product-service running on 8082 (Ctrl+C in its terminal). Within ~30
seconds Eureka will notice it's gone (heartbeat missed) and stop routing traffic
to it. Hit `/api/order/place` again — now every response shows `servedByPort: 8081`
only. This is what "load balancer + service discovery" gives you for free:
automatic failover, no manual config change needed.

## The concept, in order of "what actually happens"

1. **product-service** starts up and registers itself with Eureka: "Hi, I'm
   `product-service`, I'm running at `192.168.x.x:8081`." Do this twice → Eureka
   now has 2 addresses under one name.
2. **order-service** wants to call product-service. Instead of hardcoding
   `http://localhost:8081`, it calls `http://product-service/products` — a made-up
   hostname that only means something inside this Eureka-aware system.
3. The `@LoadBalanced RestTemplate` (see `RestTemplateConfig.java`) intercepts that
   call, asks Eureka "who is `product-service` right now?", gets back the list of
   live instances, and picks ONE using an algorithm — **round robin by default**
   in Spring Cloud LoadBalancer (each successive call goes to the next instance
   in rotation).
4. This is called **client-side load balancing** — the *calling* service
   (order-service) does the picking, using a locally cached copy of the registry.
   Compare to **server-side load balancing** (like Nginx or AWS ELB) where a
   separate proxy in front of the servers does the picking, and the client has no
   idea multiple instances even exist.
5. **api-gateway** adds a second, complementary layer on top of all this: instead
   of external clients (Postman, a frontend app, mobile app) needing to know that
   order-service lives on 8090 and product-service on 8081/8082, they only ever
   talk to the gateway on port 8080. The gateway itself uses `lb://order-service`
   style routing internally — same Eureka + load-balancer mechanism as step 3,
   just applied one layer further out. This is also where you'd normally bolt on
   cross-cutting stuff like auth, rate limiting, and request logging, since it's
   the one place all external traffic passes through.

## Why this matters in production
- **Scaling**: traffic too high on product-service? Spin up a 3rd/4th instance.
  Nothing in order-service's code changes — it just starts seeing more addresses
  from Eureka.
- **Zero-downtime deploys**: roll out a new version to instance 8082 while 8081
  keeps serving traffic, then flip.
- **Failover**: an instance crashes → Eureka's heartbeat mechanism notices →
  traffic automatically stops going there. No manual intervention.

## Notes
- This sandbox couldn't run `mvn` against Maven Central to verify the build (no
  network egress to repo.maven.apache.org), so please run `mvn spring-boot:run`
  on your own machine as the real test. The code follows standard, well-tested
  Spring Cloud 2023.0.1 / Boot 3.3.0 conventions, so it should build cleanly with
  JDK 17 and normal internet access.
- If you want this containerized instead of running 3 terminals, I can add a
  `docker-compose.yml` next — just say the word.
