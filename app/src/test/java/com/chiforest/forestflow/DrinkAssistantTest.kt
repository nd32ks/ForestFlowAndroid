package com.chiforest.forestflow

import com.chiforest.forestflow.ai.AiTransport
import com.chiforest.forestflow.ai.ChatCompletionsTransport
import com.chiforest.forestflow.ai.DrinkAssistant
import com.chiforest.forestflow.ai.DrinkCatalog
import com.chiforest.forestflow.ai.DrinkOption
import com.chiforest.forestflow.ai.QuickMatcher
import com.chiforest.forestflow.ai.Recommendation
import com.chiforest.forestflow.core.RestockEta
import com.chiforest.forestflow.core.Seed
import com.chiforest.forestflow.core.SupplierState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DrinkAssistantTest {
    private fun options(peach: Int = 5, lychee: Int = 1, lime: Int = 8, plum: Int = 0) = DrinkCatalog.options(
        Seed.products, mapOf("White Peach" to peach, "Lychee" to lychee, "Calamansi" to lime, "Osmanthus" to plum),
    )

    private class Fake(var reply: String? = "{\"pick\":2,\"why\":\"sweet and light\"}", var fail: Boolean = false) : AiTransport {
        var calls = 0
        var lastUser = ""
        override suspend fun complete(system: String, user: String): String? {
            calls++
            lastUser = user
            if (fail) error("offline")
            return reply
        }
    }

    @Test fun clearRequestIsAnsweredOfflineWithoutCallingTheModel() = runBlocking {
        val ai = Fake()
        val r = DrinkAssistant(ai).recommend("lychee please", options())
        assertEquals("Lychee", r.option?.key)
        assertEquals(Recommendation.Source.Quick, r.source)
        assertEquals(0, ai.calls)
    }

    @Test fun vagueRequestAsksTheModelOnceAndRepeatsComeFromCache() = runBlocking {
        val ai = Fake()
        val a = DrinkAssistant(ai)
        val first = a.recommend("something sweet", options())
        assertEquals(Recommendation.Source.Ai, first.source)
        assertEquals("Lychee", first.option?.key)
        a.recommend("Something sweet ", options(peach = 9)) // same words, same availability: counts do not matter
        assertEquals(1, ai.calls)
        assertEquals(1, a.aiCalls)
    }

    @Test fun modelSeesOnlyInStockDrinksAndTheRequest() = runBlocking {
        val ai = Fake()
        DrinkAssistant(ai).recommend("surprise me", options())
        assertTrue("Osmanthus" !in ai.lastUser) // out of stock, so not sent
        assertTrue(ai.lastUser.endsWith("Request: surprise me"))
        assertEquals(3, ai.lastUser.lines().count { it.contains('|') })
    }

    @Test fun modelPickingAnOutOfStockDrinkFallsBackAndIsNotCached() = runBlocking {
        val ai = Fake(reply = "{\"pick\":4,\"why\":\"tart\"}")
        val a = DrinkAssistant(ai)
        val r = a.recommend("something different", options())
        assertEquals(Recommendation.Source.Quick, r.source)
        assertTrue(r.option?.inStock == true)
        a.recommend("something different", options())
        assertEquals(2, ai.calls) // failures are retried, not cached
    }

    @Test fun networkFailureAndNoKeyBothFallBackToTheOfflineMatcher() = runBlocking {
        val down = DrinkAssistant(Fake(fail = true)).recommend("surprise me", options())
        assertEquals(Recommendation.Source.Quick, down.source)
        assertNotNull(down.option)
        val noKey = DrinkAssistant(null).recommend("surprise me", options())
        assertEquals(Recommendation.Source.Quick, noKey.source)
    }

    @Test fun modelCallsAreCappedPerRun() = runBlocking {
        val ai = Fake()
        val a = DrinkAssistant(ai, maxAiCalls = 1)
        a.recommend("surprise me", options())
        a.recommend("anything cold", options())
        assertEquals(1, ai.calls)
    }

    @Test fun soldOutFavouriteSuggestsAnAlternative() {
        val r = QuickMatcher.pick("lychee", options(lychee = 0))
        assertTrue(r.best?.inStock == true)
        assertTrue("sold out" in r.reason)
    }

    @Test fun everythingSoldOutReturnsNoPick() = runBlocking {
        val r = DrinkAssistant(null).recommend("lychee", options(0, 0, 0, 0))
        assertNull(r.option)
    }

    @Test fun noSugarAvoidsTheSugaryOneAndIsNotConfidentAlone() {
        val r = QuickMatcher.pick("no sugar", options())
        assertTrue(r.best?.key != "Calamansi")
        assertTrue(!r.confident)
    }

    @Test fun chineseRequestsMatch() {
        assertEquals("Calamansi", QuickMatcher.pick("青桔", options()).best?.key)
    }

    @Test fun parseReplyAcceptsCodeFencesAndRejectsBadPicks() {
        val o = options()
        val ok = DrinkAssistant.parseReply("```json\n{\"pick\": 3, \"why\": \"citrus kick\"}\n```", o)
        assertEquals("Calamansi", ok?.option?.key)
        assertNull(DrinkAssistant.parseReply("{\"pick\": 9}", o))
        assertNull(DrinkAssistant.parseReply("no json here", o))
    }

    @Test fun extractContentReadsAChatCompletionResponse() {
        val body = """{"choices":[{"message":{"role":"assistant","content":"{\"pick\":1}"}}],"usage":{"total_tokens":50}}"""
        assertEquals("{\"pick\":1}", ChatCompletionsTransport.extractContent(body))
        assertNull(ChatCompletionsTransport.extractContent("{\"error\":\"bad key\"}"))
    }

    @Test fun restockEtaFollowsTheSupplierPipeline() {
        fun eta(stage: Int, confirmed: Boolean = false) = RestockEta.forNode("N01", SupplierState(stage = stage, restockConfirmed = confirmed))
        assertEquals(40, eta(1)?.minutes)
        assertEquals(25, eta(2)?.minutes)
        assertEquals(10, eta(3)?.minutes)
        assertEquals(0, eta(4)?.minutes)
        assertEquals(0, eta(2, confirmed = true)?.minutes)
        assertNull(RestockEta.forNode("N02", SupplierState()))
    }
}
