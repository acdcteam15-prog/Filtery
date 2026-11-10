package com.example.filtery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FiltersTest {
    @Test fun hasAboutAHundredFiltersWithUniqueIds() {
        val ids = Filters.all.map { it.id }
        assertEquals("duplicate filter ids", ids.size, ids.toSet().size)
        assertTrue("expected 100+ filters, got ${Filters.count}", Filters.count >= 100)
    }

    @Test fun everyCategoryHasFilters() {
        for (c in Cat.values()) assertTrue("empty category $c", Filters.inCat(c).isNotEmpty())
    }

    @Test fun funnyFiltersComeFirst() {
        assertEquals(Cat.FACES, Filters.all[1].cat)
        assertEquals(Cat.FACES, Filters.inCat(Cat.FACES).first().cat)
        assertTrue(Filters.inCat(Cat.FACES).size + Filters.inCat(Cat.WARPS).size + Filters.inCat(Cat.FX).size > Filters.count / 2)
    }

    @Test fun pixelToolsBehave() {
        assertEquals(0xFF000000.toInt(), Px.gray(0))
        val pal = Px.palette(0xFF000000.toInt(), 0xFFFFFFFF.toInt())
        assertEquals(0xFF000000.toInt(), pal[0]); assertEquals(0xFFFFFFFF.toInt(), pal[255])
        assertEquals(255, Px.clamp(999)); assertEquals(0, Px.clamp(-5))
    }

    @Test fun faceBoxTemplatePutsEyesAboveMouth() {
        val f = Face.fromBox(1, 0.5f, 0.5f, 0.4f, 0.3f, 0f, 0.75f)
        assertTrue(f.ly < f.ny && f.ny < f.my)
        assertTrue(f.lx < f.rx)
    }
}
