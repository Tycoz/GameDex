package com.example.data

import com.example.data.model.ConsoleCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsoleCatalogTest {

    @Test
    fun testAllGenerationsCovered() {
        val consoles = ConsoleCatalog.allConsoles
        assertTrue("Console catalog should contain more than 50 consoles", consoles.size >= 50)

        // Verify generation coverage from 1 to 9
        for (gen in 1..9) {
            val genConsoles = consoles.filter { it.generation == gen }
            assertTrue("Generation $gen should have consoles registered", genConsoles.isNotEmpty())
        }
    }

    @Test
    fun testKeyIconicMachinesExist() {
        val odyssey = ConsoleCatalog.findByName("Magnavox Odyssey")
        assertNotNull("Magnavox Odyssey should exist", odyssey)
        assertEquals(1, odyssey?.generation)

        val atari2600 = ConsoleCatalog.findByName("Atari 2600")
        assertNotNull("Atari 2600 should exist", atari2600)
        assertEquals(2, atari2600?.generation)

        val nes = ConsoleCatalog.findByName("NES")
        assertNotNull("NES should exist", nes)
        assertEquals(3, nes?.generation)

        val snes = ConsoleCatalog.findByName("Super Nintendo")
        assertNotNull("Super Nintendo should exist", snes)
        assertEquals(4, snes?.generation)

        val ps1 = ConsoleCatalog.findByName("PlayStation")
        assertNotNull("PlayStation should exist", ps1)
        assertEquals(5, ps1?.generation)

        val ps2 = ConsoleCatalog.findByName("PlayStation 2")
        assertNotNull("PlayStation 2 should exist", ps2)
        assertEquals(6, ps2?.generation)

        val x360 = ConsoleCatalog.findByName("Xbox 360")
        assertNotNull("Xbox 360 should exist", x360)
        assertEquals(7, x360?.generation)

        val ps4 = ConsoleCatalog.findByName("PlayStation 4")
        assertNotNull("PlayStation 4 should exist", ps4)
        assertEquals(8, ps4?.generation)

        val ps5 = ConsoleCatalog.findByName("PlayStation 5")
        assertNotNull("PlayStation 5 should exist", ps5)
        assertEquals(9, ps5?.generation)

        val switch2 = ConsoleCatalog.findByName("Nintendo Switch 2")
        assertNotNull("Nintendo Switch 2 should exist", switch2)
        assertEquals(9, switch2?.generation)
        assertEquals("Nintendo", switch2?.manufacturer)

        val steamDeck = ConsoleCatalog.findByName("Steam Deck")
        assertNotNull("Steam Deck should exist", steamDeck)
        assertEquals(9, steamDeck?.generation)

        val vectrex = ConsoleCatalog.findByName("Vectrex")
        assertNotNull("Vectrex should exist", vectrex)

        val dreamcast = ConsoleCatalog.findByName("Sega Dreamcast")
        assertNotNull("Sega Dreamcast should exist", dreamcast)

        val pcEngine = ConsoleCatalog.findByName("PC-Engine")
        assertNotNull("PC-Engine should exist", pcEngine)

        val neoGeo = ConsoleCatalog.findByName("Neo-Geo AES")
        assertNotNull("Neo-Geo AES should exist", neoGeo)
    }

    @Test
    fun testSearchAndFiltering() {
        // Query search
        val atariResults = ConsoleCatalog.search("Atari")
        assertTrue(atariResults.any { it.name.contains("2600") })
        assertTrue(atariResults.any { it.name.contains("Lynx") })
        assertTrue(atariResults.any { it.name.contains("Jaguar") })

        // Manufacturer filtering
        val nintendoResults = ConsoleCatalog.search("", manufacturer = "Nintendo")
        assertTrue(nintendoResults.all { it.manufacturer.contains("Nintendo") })

        // Generation filtering
        val gen4Results = ConsoleCatalog.search("", generationFilter = "4e gén 16-bits")
        assertTrue(gen4Results.all { it.generation == 4 })
        assertTrue(gen4Results.any { it.name.contains("Mega Drive") })
        assertTrue(gen4Results.any { it.name.contains("Super Nintendo") })
    }
}
