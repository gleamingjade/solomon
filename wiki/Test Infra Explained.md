### TestContainers doc
I'm using TestContainers in order to gurantee the same environment with production as possible as i can for the test. 

* https://java.testcontainers.org/features/creating_container/
* https://www.scylladb.com/2025/04/30/how-to-use-testcontainers-with-scylladb/

### If you wanna test async works are done rightly
There are plenty of async based feature. So we have to test them up to the way async does.

* https://github.com/awaitility/awaitility/wiki/Usage#usage-examples

음..근데 로컬은 그냥 컴포즈로 묶어서 쳐도 되는데 
나중에 분리를 생각하면 별도의 컨테이너로 가야 겠는디

