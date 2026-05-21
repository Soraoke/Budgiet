package com.example.budgiet

import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import kotlin.Result

fun isDescendantOf(node: SemanticsNodeInteraction): SemanticsMatcher {
    val id = node.fetchSemanticsNode().id
    return hasAnyAncestor(SemanticsMatcher("Node is descendant of #$id") {
        it.id == id
    })
}

/** Finds [all nodes][ComposeContentTestRule.onAllNodes] whose *ancestors* contain the *receiver* node (i.e. **descendant** nodes).
 * The matcher checks whether a node is a **descendant** by checking the [ID][androidx.compose.ui.semantics.SemanticsNode.id].
 *
 * This function must take the [rule][ComposeContentTestRule] to search for all nodes from the root. */
fun SemanticsNodeInteraction.onDescendants(
    rule: ComposeContentTestRule,
    useUnmergedTree: Boolean = false
) = rule.onAllNodes(
    matcher = isDescendantOf(this),
    useUnmergedTree = useUnmergedTree,
)

fun SemanticsNodeInteractionCollection.filterNodes(filter: (SemanticsNode) -> Boolean): SemanticsNodeInteractionCollection
    = this.filter(SemanticsMatcher("Node matches arbitrary filter", filter))

/** Get the **value** of a property from a [SemanticsNode][SemanticsNodeInteraction].
 * Throws exception if property was *not found* or value was `null`.
 *
 * Use one of [SemanticsProperties] as the **key**.
 * These have the same names as the Semantics properties you see in the printed Node. */
fun <T> SemanticsNodeInteraction.getSemanticsProperty(key: SemanticsPropertyKey<T>): Result<T>
    = this.fetchSemanticsNode()
        .config
        .getOrNull(key)
        .runCatching {
            this ?: throw RuntimeException("SemanticsProperty with key \"${key.name}\" was not found or was null.")
        }

/** Returns the [Class] of the **value** of a [SemanticsProperty][SemanticsConfiguration].
 *
 * > Note: disregard the following 2 paragraphs :D
 *
 * This function is only used for a developer to guess the type `T` of a [SemanticsPropertyKey]
 * and then implement the test with [getSemanticsProperty] with that type `T`.
 * [getSemanticsProperty] needs the generic for the **value**, but the **type** of the value is not immediately clear,
 * this function is necessary to be able to use the semantics properties at all.
 *
 * This pattern is certainly weird because the [SemanticsConfiguration] has accessor fields for each semantics property so that the developer does not need to ,
 * but none of them work because apparently it overrides [Map.get] to always throw an [java.lang.UnsupportedOperationException],
 * telling the caller to use *getOrNull* or *getOrElse* instead.
 *
 * ------
 *
 * Actually, nah bruh imbouta crash out. Went on this whole damn rabbit hole trying to figure out how on earth to get a Semantics property,
 * and the relevant classes have NO DOCUMENTATION WHATSOEVER on how the hell to do that.
 * Not to mention that when you type one of the SemanticsPropertyKeys (e.g. EditableText) to see what the IDE can recommend,
 * none of them come up even though they exist and are public.
 * Wasted 2 hours on this only to find an inconspicuous stack overflow post detailing the exact way to do it,
 * even though all my previous searches DID NOT come up with this post, and the search that did come up with this post was unrelated to Semantics properties.
 *
 * This function is no longer necessary, but im keeping this here out of spite. */
@Suppress("unused")
fun SemanticsNodeInteraction.getSemanticsPropertyType(key: String): Class<*> {
    // WHY DOES IT HAVE TO BE DONE THIS WAY ;-;-;-;
    for (prop in this.fetchSemanticsNode().config) {
        if (prop.key.name == key) {
            if (prop.value == null) {
                throw NullPointerException("Value of SemanticsProperty \"$key\" is NULL")
            }
            return prop.value!!.javaClass
        }
    }

    throw RuntimeException("SemanticsProperty with key \"$key\" was not found.")
}
