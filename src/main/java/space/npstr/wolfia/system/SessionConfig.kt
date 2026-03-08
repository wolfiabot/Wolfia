/*
 * Copyright (C) 2016-2026 the original author or authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package space.npstr.wolfia.system

import java.io.InputStream
import java.io.OutputStream
import org.springframework.beans.factory.BeanClassLoaderAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.support.GenericConversionService
import org.springframework.core.serializer.Deserializer
import org.springframework.core.serializer.Serializer
import org.springframework.core.serializer.support.DeserializingConverter
import org.springframework.core.serializer.support.SerializingConverter
import org.springframework.security.jackson.SecurityJacksonModules
import org.springframework.session.config.SessionRepositoryCustomizer
import org.springframework.session.jdbc.JdbcIndexedSessionRepository
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper


/**
 * See https://docs.spring.io/spring-session/reference/configuration/jdbc.html
 */
@Configuration
class SessionConfig : BeanClassLoaderAware {

	private lateinit var classLoader: ClassLoader

	override fun setBeanClassLoader(classLoader: ClassLoader) {
		this.classLoader = classLoader
	}

	@Bean("springSessionConversionService")
	fun springSessionConversionService(jsonMapper: JsonMapper): GenericConversionService {
		val copy = jsonMapper.rebuild()
			// Register Spring Security Jackson Modules
			.addModules(SecurityJacksonModules.getModules(this.classLoader))
			.build()

		// Activate default typing explicitly if not using Spring Security
		// copy.activateDefaultTyping(copy.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
		val converter = GenericConversionService()
		converter.addConverter(
			Any::class.java,
			ByteArray::class.java,
			SerializingConverter(JsonSerializer(copy))
		)
		converter.addConverter(
			ByteArray::class.java,
			Any::class.java,
			DeserializingConverter(JsonDeserializer(copy))
		)
		return converter
	}

	private class JsonSerializer(
		private val objectMapper: ObjectMapper,
	) : Serializer<Any> {

		override fun serialize(`object`: Any, outputStream: OutputStream) {
			this.objectMapper.writeValue(outputStream, `object`)
		}
	}

	private class JsonDeserializer(
		private val objectMapper: ObjectMapper,
	) : Deserializer<Any> {
		override fun deserialize(inputStream: InputStream): Any {
			return this.objectMapper.readValue<Any>(inputStream, Any::class.java)
		}
	}

	companion object {
		private val CREATE_SESSION_ATTRIBUTE_QUERY: String = """
			INSERT INTO %TABLE_NAME%_ATTRIBUTES (SESSION_PRIMARY_ID, ATTRIBUTE_NAME, ATTRIBUTE_BYTES)
			VALUES (?, ?, convert_from(?, 'UTF8')::jsonb)
			""".trimIndent()

		private val UPDATE_SESSION_ATTRIBUTE_QUERY: String = """
			UPDATE %TABLE_NAME%_ATTRIBUTES
			SET ATTRIBUTE_BYTES = convert_from(?, 'UTF8')::jsonb
			WHERE SESSION_PRIMARY_ID = ?
			AND ATTRIBUTE_NAME = ?
			""".trimIndent()
	}

	@Bean
	fun customizer(): SessionRepositoryCustomizer<JdbcIndexedSessionRepository> {
		return SessionRepositoryCustomizer { sessionRepository: JdbcIndexedSessionRepository ->
			sessionRepository.setCreateSessionAttributeQuery(CREATE_SESSION_ATTRIBUTE_QUERY)
			sessionRepository.setUpdateSessionAttributeQuery(UPDATE_SESSION_ATTRIBUTE_QUERY)
		}
	}
}
