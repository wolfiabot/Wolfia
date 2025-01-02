package space.npstr.wolfia.db

import java.util.concurrent.TimeUnit.SECONDS
import javax.sql.DataSource
import net.ttddyy.dsproxy.listener.SingleQueryCountHolder
import net.ttddyy.dsproxy.listener.logging.DefaultQueryLogEntryCreator
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel.WARN
import net.ttddyy.dsproxy.listener.logging.SLF4JSlowQueryListener
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.stereotype.Component

@Component
class DataSourceProxyPostProcessor(
	// Use ObjectProvider to avoid warnings about this bean being initialized too early for post processing
	queryCountHolder: ObjectProvider<SingleQueryCountHolder>,
) : BeanPostProcessor {

	private val proxyDataSourceBuilder by lazy {
		ProxyDataSourceBuilder()
			.listener(buildSlf4jSlowQueryListener())
			.multiline()
			.name("postgres")
			.countQuery(queryCountHolder.getObject())
	}

	override fun postProcessAfterInitialization(bean: Any, beanName: String): Any? {
//		if (bean is HikariDataSource) {
//			//TODO does spring boot actuatur do this for us?
//			logger().warn("PLONK setting metrics factory over {}", bean.metricsTrackerFactory?.javaClass?.simpleName)
//			bean.metricsTrackerFactory = PrometheusMetricsTrackerFactory()
//		}

		if (bean is DataSource) {
			return proxyDataSourceBuilder.dataSource(bean).buildProxy()
		}
		return bean
	}

	private fun buildSlf4jSlowQueryListener(): SLF4JSlowQueryListener {
		val listener = SLF4JSlowQueryListener(30, SECONDS).apply {
			logLevel = WARN
			setLogger("SlowQueryLog")
			queryLogEntryCreator = buildMultilineQueryLogEntryCreator()
		}
		return listener
	}

	private fun buildMultilineQueryLogEntryCreator(): DefaultQueryLogEntryCreator {
		val entryCreator = DefaultQueryLogEntryCreator()
		entryCreator.isMultiline = true
		return entryCreator
	}

}
