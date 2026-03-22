package io.github.ricky9667.cubing_tw_notifier

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:cubing_tw_notifier_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
    ],
)
@ActiveProfiles("test")
class CubingTwNotifierApplicationTests {
    @Test
    fun contextLoads() {
    }
}
