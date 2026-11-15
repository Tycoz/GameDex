package com.example.data.model

data class CatalogGame(
    val title: String,
    val platform: String,
    val genre: String,
    val releaseYear: Int,
    val publisher: String,
    val format: String,
    val region: String = "PAL",
    val barcode: String = "",
    val averageMarketValue: Double,
    val coverUrl: String = "",
    val description: String = ""
)

object GameCatalog {
    val sampleGames = listOf(
        CatalogGame(
            title = "The Legend of Zelda: Ocarina of Time",
            platform = "Nintendo 64",
            genre = "Action-Aventure",
            releaseYear = 1998,
            publisher = "Nintendo",
            format = "Cartouche",
            region = "PAL",
            barcode = "045496870232",
            averageMarketValue = 65.0,
            coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co5vmg.webp",
            description = "Chef-d'œuvre fondateur de l'aventure 3D sur Nintendo 64."
        ),
        CatalogGame(
            title = "Super Mario 64",
            platform = "Nintendo 64",
            genre = "Plateforme 3D",
            releaseYear = 1996,
            publisher = "Nintendo",
            format = "Cartouche",
            region = "PAL",
            barcode = "045496870027",
            averageMarketValue = 45.0,
            coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co20wa.webp",
            description = "Révolution du jeu de plateforme tridimensionnel au château de Peach."
        ),
        CatalogGame(
            title = "Final Fantasy VII",
            platform = "PlayStation",
            genre = "JRPG",
            releaseYear = 1997,
            publisher = "Square Soft",
            format = "Disque (3 CD)",
            region = "PAL",
            barcode = "711719602422",
            averageMarketValue = 55.0,
            coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co2kx2.webp",
            description = "L'épopée de Cloud Strife et du groupe Avalanche contre Shinra et Sephiroth."
        ),
        CatalogGame(
            title = "Metal Gear Solid",
            platform = "PlayStation",
            genre = "Infiltration / Action",
            releaseYear = 1998,
            publisher = "Konami",
            format = "Disque (2 CD)",
            region = "PAL",
            barcode = "5016553805030",
            averageMarketValue = 60.0,
            coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1x77.webp",
            description = "Tactical Espionage Action culte signé Hideo Kojima sur Shadow Moses."
        ),
        CatalogGame(
            title = "Chrono Trigger",
            platform = "Super Nintendo",
            genre = "JRPG",
            releaseYear = 1995,
            publisher = "Square Soft",
            format = "Cartouche",
            region = "NTSC-U",
            barcode = "045496830427",
            averageMarketValue = 240.0,
            coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1n5v.webp",
            description = "La Dream Team réunie (Sakaguchi, Horii, Toriyama, Mitsuda) pour un voyage temporel légendaire."
        ),
        CatalogGame(
            title = "Super Mario World",
            platform = "Super Nintendo",
            genre = "Plateforme 2D",
            releaseYear = 1990,
            publisher = "Nintendo",
            format = "Cartouche",
            region = "PAL",
            barcode = "045496830021",
            averageMarketValue = 35.0,
            coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co68k6.webp",
            description = "Le classique intemporel de la SNES avec l'introduction de Yoshi."
        ),
        CatalogGame(
            title = "Pokémon Version Rouge",
            platform = "Game Boy",
            genre = "RPG",
            releaseYear = 1996,
            publisher = "Nintendo",
            format = "Cartouche",
            region = "PAL",
            barcode = "045496731021",
            averageMarketValue = 75.0,
            coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co8fv5.webp",
            description = "Attrapez-les tous ! La genèse du phénomène mondial à Kanto."
        ),
        CatalogGame(
            title = "Sonic the Hedgehog",
            platform = "Sega Mega Drive",
            genre = "Plateforme",
            releaseYear = 1991,
            publisher = "Sega",
            format = "Cartouche",
            region = "PAL",
            barcode = "4974365611033",
            averageMarketValue = 25.0,
            coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co20vm.webp",
            description = "La mascotte ultra-rapide de Sega dans Green Hill Zone."
        ),
        CatalogGame(
            title = "Streets of Rage 2",
            platform = "Sega Mega Drive",
            genre = "Beat'em up",
            releaseYear = 1992,
            publisher = "Sega",
            format = "Cartouche",
            region = "PAL",
            barcode = "4974365611422",
            averageMarketValue = 70.0,
            coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co2kww.webp",
            description = "Le sommet du Beat'em up 16-bit sublimé par la bande-son de Yuzo Koshiro."
        ),
        CatalogGame(
            title = "Silent Hill 2",
            platform = "PlayStation 2",
            genre = "Survival Horror",
            releaseYear = 2001,
            publisher = "Konami",
            format = "Disque DVD",
            region = "PAL",
            barcode = "5016553818320",
            averageMarketValue = 85.0,
            coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co2156.webp",
            description = "L'exploration psychologique torturée de James Sunderland dans la brume."
        ),
        CatalogGame(
            title = "Grand Theft Auto: San Andreas",
            platform = "PlayStation 2",
            genre = "Monde ouvert / Action",
            releaseYear = 2004,
            publisher = "Rockstar Games",
            format = "Disque DVD",
            region = "PAL",
            barcode = "5026555403214",
            averageMarketValue = 20.0,
            coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co2lb7.webp",
            description = "L'épopée mythique de Carl Johnson à travers Los Santos, San Fierro et Las Venturas."
        ),
        CatalogGame(
            title = "The Legend of Zelda: Breath of the Wild",
            platform = "Nintendo Switch",
            genre = "Action-Aventure",
            releaseYear = 2017,
            publisher = "Nintendo",
            format = "Cartouche",
            region = "PAL",
            barcode = "045496420123",
            averageMarketValue = 45.0,
            coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co3p2d.webp",
            description = "La réinvention magistrale du monde ouvert au cœur d'Hyrule déchu."
        ),
        CatalogGame(
            title = "Elden Ring",
            platform = "PlayStation 5",
            genre = "Action-RPG",
            releaseYear = 2022,
            publisher = "Bandai Namco",
            format = "Disque Blu-ray",
            region = "PAL",
            barcode = "3391892014021",
            averageMarketValue = 40.0,
            coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co4jni.webp",
            description = "L'Entre-terre signée Hidetaka Miyazaki et George R. R. Martin."
        ),
        CatalogGame(
            title = "Resident Evil 4",
            platform = "Nintendo GameCube",
            genre = "Survival Horror / TPS",
            releaseYear = 2005,
            publisher = "Capcom",
            format = "Mini-DVD (2 disques)",
            region = "PAL",
            barcode = "5055060960412",
            averageMarketValue = 45.0,
            coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1r76.webp",
            description = "Leon S. Kennedy face aux Ganados dans les montagnes espagnoles."
        ),
        CatalogGame(
            title = "Castlevania: Symphony of the Night",
            platform = "PlayStation",
            genre = "Metroidvania",
            releaseYear = 1997,
            publisher = "Konami",
            format = "Disque",
            region = "PAL",
            barcode = "5016553803128",
            averageMarketValue = 280.0,
            coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1ncq.webp",
            description = "Alucard dans le château inversé de Dracula, acte de naissance du Metroidvania moderne."
        ),
        CatalogGame(
            title = "Halo: Combat Evolved",
            platform = "Xbox",
            genre = "FPS",
            releaseYear = 2001,
            publisher = "Microsoft",
            format = "Disque DVD",
            region = "PAL",
            barcode = "0805529124403",
            averageMarketValue = 18.0,
            coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1xat.webp",
            description = "Le Master Chief débarque sur l'anneau mystérieux avec Cortana."
        ),
        CatalogGame(
            title = "Space Invaders",
            platform = "Atari 2600",
            genre = "Shoot 'em up",
            releaseYear = 1980,
            publisher = "Atari",
            format = "Cartouche",
            region = "PAL",
            barcode = "0020626026325",
            averageMarketValue = 25.0,
            coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co21cl.webp",
            description = "Le classique arcade de Taito adapté sur la légendaire console Atari 2600."
        ),
        CatalogGame(
            title = "Shenmue",
            platform = "Sega Dreamcast",
            genre = "Aventure / RPG",
            releaseYear = 1999,
            publisher = "Sega",
            format = "Disque GD-ROM (4 CD)",
            region = "PAL",
            barcode = "4974365610882",
            averageMarketValue = 75.0,
            coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1r3k.webp",
            description = "La quête de vengeance de Ryo Hazuka à Yokosuka, chef-d'œuvre de Yu Suzuki."
        ),
        CatalogGame(
            title = "Metal Slug",
            platform = "Neo-Geo AES",
            genre = "Run and Gun",
            releaseYear = 1996,
            publisher = "SNK",
            format = "Cartouche Neo-Geo",
            region = "NTSC-J",
            barcode = "4964808101037",
            averageMarketValue = 1800.0,
            coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1x9m.webp",
            description = "L'un des jeux d'action 2D les plus recherchés et prestigieux sur Neo-Geo AES."
        ),
        CatalogGame(
            title = "PC Kid (Bonk's Adventure)",
            platform = "PC-Engine / TurboGrafx-16",
            genre = "Plateforme 2D",
            releaseYear = 1989,
            publisher = "Hudson Soft",
            format = "HuCard",
            region = "PAL / NTSC-J",
            barcode = "4988607050184",
            averageMarketValue = 60.0,
            coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co2k39.webp",
            description = "La mascotte préhistorique emblématique de la PC-Engine."
        )
    )

    /**
     * Complete platform list sourced from the Wikipedia 'Liste de consoles de jeux vidéo'
     * with 'Toutes' at index 0 for UI filter bars.
     */
    val platforms: List<String> = ConsoleCatalog.allPlatformNamesWithAll

    /**
     * Curated list of popular consoles for quick-filter chips.
     */
    val popularPlatforms: List<String> = ConsoleCatalog.popularPlatforms

    val completenessOptions = listOf(
        "CIB" to "Complet en boîte (Boîte + Manuel + Jeu)",
        "Boîte sans notice" to "Jeu en boîte originale mais sans livret",
        "Loose" to "Cartouche ou disque seul sans boîte",
        "Neuf scellé" to "Neuf sous blister d'origine"
    )

    val conditions = listOf(
        "Neuf sous blister",
        "Comme neuf",
        "Très bon état",
        "Bon état",
        "État moyen",
        "Mauvais état",
        "Absent"
    )
}
