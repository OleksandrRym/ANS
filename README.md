# 🐘🏎️ PostgreSQL Concurrency Control Guide 
## Repository that demonstrates basic concurrency concepts in PostgreSQL with Java examples

### 📂 `concurrency/`
#### 📂 `anomalies/`
Contains examples of concurrency anomalies that can occur in PostgreSQL:
- [NonRepeatableReadExample](concurency/anomaly/NonRepeatableRead.java)
- [Phantom read](concurency/anomaly/PhantomRead.java)
- [Serialization anomaly](concurency/anomaly/SerializableAnomaly.java)

#### 📂 `internal/`
- [MVCC](cuncurency/src/main/java/com/rymar/concurency/internal/MVCCDemo.java)
- [VACUUM](cuncurency/src/main/java/com/rymar/concurency/internal/VacuumDemo.java)

- 