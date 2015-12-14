package dk.etiktak.backend.bootstrap

import dk.etiktak.backend.repository.bootstrap.InfoChannelRoleBootstrap
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
class ApplicationStartup @Autowired constructor(
        private val infoChannelRoleBootstrap: InfoChannelRoleBootstrap) : ApplicationListener<ContextRefreshedEvent> {

    private val logger = LoggerFactory.getLogger(ApplicationStartup::class.java)

    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        logger.info("Bootstrapping application...");

        infoChannelRoleBootstrap.init()
    }
}