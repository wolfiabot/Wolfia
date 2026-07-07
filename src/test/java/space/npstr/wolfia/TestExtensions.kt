package space.npstr.wolfia

import java.util.function.Consumer
import kotlin.contracts.ExperimentalContracts
import org.assertj.core.api.AbstractAssert


object TestExtensions {

	// return non-null typed assertion after using isNotNull
	@OptIn(ExperimentalContracts::class)
	fun <SELF : AbstractAssert<SELF, ACTUAL?>, ACTUAL> AbstractAssert<SELF, ACTUAL?>.isNotNullKt(): AbstractAssert<SELF, ACTUAL> {
		@Suppress("UNCHECKED_CAST")
		return isNotNull as AbstractAssert<SELF, ACTUAL>
	}

	// workaround for https://github.com/assertj/assertj/issues/2357
	fun <T : Any?> AbstractAssert<*, T>.satisfiesKt(requirements: Consumer<T>): AbstractAssert<*, T> {
		@Suppress("UNCHECKED_CAST")
		return this.satisfies(requirements) as AbstractAssert<*, T>
	}
}
