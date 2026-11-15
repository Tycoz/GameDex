package com.example.data.model

data class ConsolePlatform(
    val id: String,
    val name: String,
    val shortName: String,
    val manufacturer: String,
    val generation: Int,
    val generationLabel: String,
    val type: String, // "Salon", "Portable", "Hybride", "Micro-console", "PC / Arcade"
    val releaseYear: Int,
    val defaultFormat: String = "Cartouche"
)

object ConsoleCatalog {

    val allConsoles: List<ConsolePlatform> = listOf(
        // ==================== 1RE GÉNÉRATION (1972-1980) ====================
        ConsolePlatform(
            id = "magnavox_odyssey",
            name = "Magnavox Odyssey",
            shortName = "Odyssey",
            manufacturer = "Magnavox",
            generation = 1,
            generationLabel = "1re génération (1972-1980)",
            type = "Salon",
            releaseYear = 1972,
            defaultFormat = "Cartouche de circuits imprimés"
        ),
        ConsolePlatform(
            id = "atari_home_pong",
            name = "Atari Home Pong",
            shortName = "Pong",
            manufacturer = "Atari",
            generation = 1,
            generationLabel = "1re génération (1972-1980)",
            type = "Salon",
            releaseYear = 1975,
            defaultFormat = "Jeu intégré"
        ),
        ConsolePlatform(
            id = "philips_tele_game",
            name = "Philips Tele-Game",
            shortName = "Tele-Game",
            manufacturer = "Philips",
            generation = 1,
            generationLabel = "1re génération (1972-1980)",
            type = "Salon",
            releaseYear = 1975,
            defaultFormat = "Jeu intégré"
        ),
        ConsolePlatform(
            id = "coleco_telstar",
            name = "Coleco Telstar",
            shortName = "Telstar",
            manufacturer = "Coleco",
            generation = 1,
            generationLabel = "1re génération (1972-1980)",
            type = "Salon",
            releaseYear = 1976,
            defaultFormat = "Jeu intégré"
        ),
        ConsolePlatform(
            id = "apf_tv_fun",
            name = "APF TV Fun",
            shortName = "TV Fun",
            manufacturer = "APF",
            generation = 1,
            generationLabel = "1re génération (1972-1980)",
            type = "Salon",
            releaseYear = 1976,
            defaultFormat = "Jeu intégré"
        ),
        ConsolePlatform(
            id = "binatone_tv_master",
            name = "Binatone TV Master",
            shortName = "TV Master",
            manufacturer = "Binatone",
            generation = 1,
            generationLabel = "1re génération (1972-1980)",
            type = "Salon",
            releaseYear = 1976,
            defaultFormat = "Jeu intégré"
        ),
        ConsolePlatform(
            id = "nintendo_color_tv_game",
            name = "Nintendo Color TV-Game",
            shortName = "Color TV-Game",
            manufacturer = "Nintendo",
            generation = 1,
            generationLabel = "1re génération (1972-1980)",
            type = "Salon",
            releaseYear = 1977,
            defaultFormat = "Jeu intégré"
        ),
        ConsolePlatform(
            id = "interton_video_2000",
            name = "Interton Video 2000",
            shortName = "Video 2000",
            manufacturer = "Interton",
            generation = 1,
            generationLabel = "1re génération (1972-1980)",
            type = "Salon",
            releaseYear = 1977,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "zanussi_ping_o_tronic",
            name = "Zanussi Ping-O-Tronic",
            shortName = "Ping-O-Tronic",
            manufacturer = "Zanussi",
            generation = 1,
            generationLabel = "1re génération (1972-1980)",
            type = "Salon",
            releaseYear = 1974,
            defaultFormat = "Jeu intégré"
        ),

        // ==================== 2E GÉNÉRATION (1976-1983) ====================
        ConsolePlatform(
            id = "fairchild_channel_f",
            name = "Fairchild Channel F",
            shortName = "Channel F",
            manufacturer = "Fairchild",
            generation = 2,
            generationLabel = "2e génération (1976-1983)",
            type = "Salon",
            releaseYear = 1976,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "atari_2600",
            name = "Atari 2600",
            shortName = "2600",
            manufacturer = "Atari",
            generation = 2,
            generationLabel = "2e génération (1976-1983)",
            type = "Salon",
            releaseYear = 1977,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "bally_astrocade",
            name = "Bally Astrocade",
            shortName = "Astrocade",
            manufacturer = "Bally",
            generation = 2,
            generationLabel = "2e génération (1976-1983)",
            type = "Salon",
            releaseYear = 1977,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "magnavox_odyssey_2",
            name = "Magnavox Odyssey² / Philips Videopac",
            shortName = "Videopac",
            manufacturer = "Magnavox / Philips",
            generation = 2,
            generationLabel = "2e génération (1976-1983)",
            type = "Salon",
            releaseYear = 1978,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "apf_m1000",
            name = "APF-M1000",
            shortName = "APF-M1000",
            manufacturer = "APF",
            generation = 2,
            generationLabel = "2e génération (1976-1983)",
            type = "Salon",
            releaseYear = 1979,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "mattel_intellivision",
            name = "Mattel Intellivision",
            shortName = "Intellivision",
            manufacturer = "Mattel",
            generation = 2,
            generationLabel = "2e génération (1976-1983)",
            type = "Salon",
            releaseYear = 1979,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "milton_bradley_microvision",
            name = "Microvision",
            shortName = "Microvision",
            manufacturer = "Milton Bradley",
            generation = 2,
            generationLabel = "2e génération (1976-1983)",
            type = "Portable",
            releaseYear = 1979,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "vtech_creativision",
            name = "VTech CreatiVision",
            shortName = "CreatiVision",
            manufacturer = "VTech",
            generation = 2,
            generationLabel = "2e génération (1976-1983)",
            type = "Salon",
            releaseYear = 1981,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "epoch_cassette_vision",
            name = "Epoch Cassette Vision",
            shortName = "Cassette Vision",
            manufacturer = "Epoch",
            generation = 2,
            generationLabel = "2e génération (1976-1983)",
            type = "Salon",
            releaseYear = 1981,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "emerson_arcadia_2001",
            name = "Emerson Arcadia 2001",
            shortName = "Arcadia 2001",
            manufacturer = "Emerson",
            generation = 2,
            generationLabel = "2e génération (1976-1983)",
            type = "Salon",
            releaseYear = 1982,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "colecovision",
            name = "ColecoVision",
            shortName = "ColecoVision",
            manufacturer = "Coleco",
            generation = 2,
            generationLabel = "2e génération (1976-1983)",
            type = "Salon",
            releaseYear = 1982,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "atari_5200",
            name = "Atari 5200",
            shortName = "5200",
            manufacturer = "Atari",
            generation = 2,
            generationLabel = "2e génération (1976-1983)",
            type = "Salon",
            releaseYear = 1982,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "vectrex",
            name = "Vectrex",
            shortName = "Vectrex",
            manufacturer = "GCE / Milton Bradley",
            generation = 2,
            generationLabel = "2e génération (1976-1983)",
            type = "Salon",
            releaseYear = 1982,
            defaultFormat = "Cartouche"
        ),

        // ==================== 3E GÉNÉRATION (1983-1997 - ÈRE 8-BITS) ====================
        ConsolePlatform(
            id = "sega_sg_1000",
            name = "Sega SG-1000",
            shortName = "SG-1000",
            manufacturer = "Sega",
            generation = 3,
            generationLabel = "3e génération (1983-1997)",
            type = "Salon",
            releaseYear = 1983,
            defaultFormat = "Cartouche / My Card"
        ),
        ConsolePlatform(
            id = "nes",
            name = "NES",
            shortName = "NES",
            manufacturer = "Nintendo",
            generation = 3,
            generationLabel = "3e génération (1983-1997)",
            type = "Salon",
            releaseYear = 1983,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "famicom",
            name = "Famicom",
            shortName = "Famicom",
            manufacturer = "Nintendo",
            generation = 3,
            generationLabel = "3e génération (1983-1997)",
            type = "Salon",
            releaseYear = 1983,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "famicom_disk_system",
            name = "Famicom Disk System",
            shortName = "FDS",
            manufacturer = "Nintendo",
            generation = 3,
            generationLabel = "3e génération (1983-1997)",
            type = "Salon",
            releaseYear = 1986,
            defaultFormat = "Disquette QuickDisk"
        ),
        ConsolePlatform(
            id = "casio_pv_1000",
            name = "Casio PV-1000",
            shortName = "PV-1000",
            manufacturer = "Casio",
            generation = 3,
            generationLabel = "3e génération (1983-1997)",
            type = "Salon",
            releaseYear = 1983,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "epoch_super_cassette_vision",
            name = "Epoch Super Cassette Vision",
            shortName = "SCV",
            manufacturer = "Epoch",
            generation = 3,
            generationLabel = "3e génération (1983-1997)",
            type = "Salon",
            releaseYear = 1984,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "sega_master_system",
            name = "Sega Master System",
            shortName = "Master System",
            manufacturer = "Sega",
            generation = 3,
            generationLabel = "3e génération (1983-1997)",
            type = "Salon",
            releaseYear = 1985,
            defaultFormat = "Cartouche / Sega Card"
        ),
        ConsolePlatform(
            id = "atari_7800",
            name = "Atari 7800",
            shortName = "7800",
            manufacturer = "Atari",
            generation = 3,
            generationLabel = "3e génération (1983-1997)",
            type = "Salon",
            releaseYear = 1986,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "atari_xegs",
            name = "Atari XEGS",
            shortName = "XEGS",
            manufacturer = "Atari",
            generation = 3,
            generationLabel = "3e génération (1983-1997)",
            type = "Salon",
            releaseYear = 1987,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "amstrad_gx4000",
            name = "Amstrad GX-4000",
            shortName = "GX4000",
            manufacturer = "Amstrad",
            generation = 3,
            generationLabel = "3e génération (1983-1997)",
            type = "Salon",
            releaseYear = 1990,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "c64_gs",
            name = "Commodore 64 Games System",
            shortName = "C64GS",
            manufacturer = "Commodore",
            generation = 3,
            generationLabel = "3e génération (1983-1997)",
            type = "Salon",
            releaseYear = 1990,
            defaultFormat = "Cartouche"
        ),

        // ==================== 4E GÉNÉRATION (1987-1998 - ÈRE 16-BITS) ====================
        ConsolePlatform(
            id = "pc_engine",
            name = "PC-Engine / TurboGrafx-16",
            shortName = "PC-Engine",
            manufacturer = "NEC",
            generation = 4,
            generationLabel = "4e génération (1987-1998)",
            type = "Salon",
            releaseYear = 1987,
            defaultFormat = "HuCard"
        ),
        ConsolePlatform(
            id = "pc_engine_cd",
            name = "PC-Engine CD-ROM² / Super CD-ROM²",
            shortName = "PC-Engine CD",
            manufacturer = "NEC",
            generation = 4,
            generationLabel = "4e génération (1987-1998)",
            type = "Salon",
            releaseYear = 1988,
            defaultFormat = "Disque CD-ROM"
        ),
        ConsolePlatform(
            id = "sega_mega_drive",
            name = "Sega Mega Drive",
            shortName = "Mega Drive",
            manufacturer = "Sega",
            generation = 4,
            generationLabel = "4e génération (1987-1998)",
            type = "Salon",
            releaseYear = 1988,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "game_boy",
            name = "Game Boy",
            shortName = "Game Boy",
            manufacturer = "Nintendo",
            generation = 4,
            generationLabel = "4e génération (1987-1998)",
            type = "Portable",
            releaseYear = 1989,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "atari_lynx",
            name = "Atari Lynx",
            shortName = "Lynx",
            manufacturer = "Atari",
            generation = 4,
            generationLabel = "4e génération (1987-1998)",
            type = "Portable",
            releaseYear = 1989,
            defaultFormat = "Cartouche plate"
        ),
        ConsolePlatform(
            id = "sega_game_gear",
            name = "Sega Game Gear",
            shortName = "Game Gear",
            manufacturer = "Sega",
            generation = 4,
            generationLabel = "4e génération (1987-1998)",
            type = "Portable",
            releaseYear = 1990,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "super_nintendo",
            name = "Super Nintendo",
            shortName = "SNES",
            manufacturer = "Nintendo",
            generation = 4,
            generationLabel = "4e génération (1987-1998)",
            type = "Salon",
            releaseYear = 1990,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "super_famicom",
            name = "Super Famicom",
            shortName = "SFC",
            manufacturer = "Nintendo",
            generation = 4,
            generationLabel = "4e génération (1987-1998)",
            type = "Salon",
            releaseYear = 1990,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "neo_geo_aes",
            name = "Neo-Geo AES",
            shortName = "Neo-Geo",
            manufacturer = "SNK",
            generation = 4,
            generationLabel = "4e génération (1987-1998)",
            type = "Salon",
            releaseYear = 1990,
            defaultFormat = "Cartouche Neo-Geo"
        ),
        ConsolePlatform(
            id = "pc_engine_gt",
            name = "PC-Engine GT / TurboExpress",
            shortName = "PC-Engine GT",
            manufacturer = "NEC",
            generation = 4,
            generationLabel = "4e génération (1987-1998)",
            type = "Portable",
            releaseYear = 1990,
            defaultFormat = "HuCard"
        ),
        ConsolePlatform(
            id = "commodore_cdtv",
            name = "Commodore CDTV",
            shortName = "CDTV",
            manufacturer = "Commodore",
            generation = 4,
            generationLabel = "4e génération (1987-1998)",
            type = "Salon",
            releaseYear = 1991,
            defaultFormat = "Disque CD-ROM"
        ),
        ConsolePlatform(
            id = "philips_cd_i",
            name = "Philips CD-i",
            shortName = "CD-i",
            manufacturer = "Philips",
            generation = 4,
            generationLabel = "4e génération (1987-1998)",
            type = "Salon",
            releaseYear = 1991,
            defaultFormat = "Disque CD-i"
        ),
        ConsolePlatform(
            id = "sega_mega_cd",
            name = "Sega Mega-CD",
            shortName = "Mega-CD",
            manufacturer = "Sega",
            generation = 4,
            generationLabel = "4e génération (1987-1998)",
            type = "Salon",
            releaseYear = 1991,
            defaultFormat = "Disque CD-ROM"
        ),
        ConsolePlatform(
            id = "watara_supervision",
            name = "Watara Supervision",
            shortName = "Supervision",
            manufacturer = "Watara",
            generation = 4,
            generationLabel = "4e génération (1987-1998)",
            type = "Portable",
            releaseYear = 1992,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "pioneer_laseractive",
            name = "Pioneer LaserActive",
            shortName = "LaserActive",
            manufacturer = "Pioneer",
            generation = 4,
            generationLabel = "4e génération (1987-1998)",
            type = "Salon",
            releaseYear = 1993,
            defaultFormat = "LaserDisc / Cartouche"
        ),
        ConsolePlatform(
            id = "sega_32x",
            name = "Sega 32X",
            shortName = "32X",
            manufacturer = "Sega",
            generation = 4,
            generationLabel = "4e génération (1987-1998)",
            type = "Salon",
            releaseYear = 1994,
            defaultFormat = "Cartouche 32X"
        ),
        ConsolePlatform(
            id = "neo_geo_cd",
            name = "Neo-Geo CD",
            shortName = "Neo-Geo CD",
            manufacturer = "SNK",
            generation = 4,
            generationLabel = "4e génération (1987-1998)",
            type = "Salon",
            releaseYear = 1994,
            defaultFormat = "Disque CD-ROM"
        ),
        ConsolePlatform(
            id = "sega_nomad",
            name = "Sega Nomad",
            shortName = "Nomad",
            manufacturer = "Sega",
            generation = 4,
            generationLabel = "4e génération (1987-1998)",
            type = "Portable",
            releaseYear = 1995,
            defaultFormat = "Cartouche Mega Drive"
        ),

        // ==================== 5E GÉNÉRATION (1993-2000 - ÈRE 32/64 BITS & 3D) ====================
        ConsolePlatform(
            id = "fm_towns_marty",
            name = "FM Towns Marty",
            shortName = "Marty",
            manufacturer = "Fujitsu",
            generation = 5,
            generationLabel = "5e génération (1993-2000)",
            type = "Salon",
            releaseYear = 1993,
            defaultFormat = "Disque CD-ROM"
        ),
        ConsolePlatform(
            id = "amiga_cd32",
            name = "Amiga CD32",
            shortName = "CD32",
            manufacturer = "Commodore",
            generation = 5,
            generationLabel = "5e génération (1993-2000)",
            type = "Salon",
            releaseYear = 1993,
            defaultFormat = "Disque CD-ROM"
        ),
        ConsolePlatform(
            id = "panasonic_3do",
            name = "3DO Interactive Multiplayer",
            shortName = "3DO",
            manufacturer = "Panasonic / Goldstar / Sanyo",
            generation = 5,
            generationLabel = "5e génération (1993-2000)",
            type = "Salon",
            releaseYear = 1993,
            defaultFormat = "Disque CD-ROM"
        ),
        ConsolePlatform(
            id = "atari_jaguar",
            name = "Atari Jaguar",
            shortName = "Jaguar",
            manufacturer = "Atari",
            generation = 5,
            generationLabel = "5e génération (1993-2000)",
            type = "Salon",
            releaseYear = 1993,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "atari_jaguar_cd",
            name = "Atari Jaguar CD",
            shortName = "Jaguar CD",
            manufacturer = "Atari",
            generation = 5,
            generationLabel = "5e génération (1993-2000)",
            type = "Salon",
            releaseYear = 1995,
            defaultFormat = "Disque CD-ROM"
        ),
        ConsolePlatform(
            id = "sega_saturn",
            name = "Sega Saturn",
            shortName = "Saturn",
            manufacturer = "Sega",
            generation = 5,
            generationLabel = "5e génération (1993-2000)",
            type = "Salon",
            releaseYear = 1994,
            defaultFormat = "Disque CD-ROM"
        ),
        ConsolePlatform(
            id = "playstation_1",
            name = "PlayStation",
            shortName = "PS1",
            manufacturer = "Sony",
            generation = 5,
            generationLabel = "5e génération (1993-2000)",
            type = "Salon",
            releaseYear = 1994,
            defaultFormat = "Disque CD-ROM"
        ),
        ConsolePlatform(
            id = "pc_fx",
            name = "PC-FX",
            shortName = "PC-FX",
            manufacturer = "NEC",
            generation = 5,
            generationLabel = "5e génération (1993-2000)",
            type = "Salon",
            releaseYear = 1994,
            defaultFormat = "Disque CD-ROM"
        ),
        ConsolePlatform(
            id = "virtual_boy",
            name = "Virtual Boy",
            shortName = "Virtual Boy",
            manufacturer = "Nintendo",
            generation = 5,
            generationLabel = "5e génération (1993-2000)",
            type = "Portable",
            releaseYear = 1995,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "casio_loopy",
            name = "Casio Loopy",
            shortName = "Loopy",
            manufacturer = "Casio",
            generation = 5,
            generationLabel = "5e génération (1993-2000)",
            type = "Salon",
            releaseYear = 1995,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "apple_bandai_pippin",
            name = "Apple Bandai Pippin",
            shortName = "Pippin",
            manufacturer = "Apple / Bandai",
            generation = 5,
            generationLabel = "5e génération (1993-2000)",
            type = "Salon",
            releaseYear = 1995,
            defaultFormat = "Disque CD-ROM"
        ),
        ConsolePlatform(
            id = "nintendo_64",
            name = "Nintendo 64",
            shortName = "N64",
            manufacturer = "Nintendo",
            generation = 5,
            generationLabel = "5e génération (1993-2000)",
            type = "Salon",
            releaseYear = 1996,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "game_boy_color",
            name = "Game Boy Color",
            shortName = "GBC",
            manufacturer = "Nintendo",
            generation = 5,
            generationLabel = "5e génération (1993-2000)",
            type = "Portable",
            releaseYear = 1998,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "game_com",
            name = "Tiger Game.com",
            shortName = "Game.com",
            manufacturer = "Tiger Electronics",
            generation = 5,
            generationLabel = "5e génération (1993-2000)",
            type = "Portable",
            releaseYear = 1997,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "neo_geo_pocket",
            name = "Neo-Geo Pocket",
            shortName = "NGP",
            manufacturer = "SNK",
            generation = 5,
            generationLabel = "5e génération (1993-2000)",
            type = "Portable",
            releaseYear = 1998,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "neo_geo_pocket_color",
            name = "Neo-Geo Pocket Color",
            shortName = "NGPC",
            manufacturer = "SNK",
            generation = 5,
            generationLabel = "5e génération (1993-2000)",
            type = "Portable",
            releaseYear = 1999,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "bandai_wonderswan",
            name = "WonderSwan",
            shortName = "WonderSwan",
            manufacturer = "Bandai",
            generation = 5,
            generationLabel = "5e génération (1993-2000)",
            type = "Portable",
            releaseYear = 1999,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "sony_pocketstation",
            name = "PocketStation",
            shortName = "PocketStation",
            manufacturer = "Sony",
            generation = 5,
            generationLabel = "5e génération (1993-2000)",
            type = "Portable",
            releaseYear = 1999,
            defaultFormat = "Carte mémoire LCD"
        ),
        ConsolePlatform(
            id = "bandai_wonderswan_color",
            name = "WonderSwan Color",
            shortName = "WSC",
            manufacturer = "Bandai",
            generation = 5,
            generationLabel = "5e génération (1993-2000)",
            type = "Portable",
            releaseYear = 2000,
            defaultFormat = "Cartouche"
        ),

        // ==================== 6E GÉNÉRATION (1998-2005 - ÈRE 128-BITS) ====================
        ConsolePlatform(
            id = "sega_dreamcast",
            name = "Sega Dreamcast",
            shortName = "Dreamcast",
            manufacturer = "Sega",
            generation = 6,
            generationLabel = "6e génération (1998-2005)",
            type = "Salon",
            releaseYear = 1998,
            defaultFormat = "Disque GD-ROM"
        ),
        ConsolePlatform(
            id = "playstation_2",
            name = "PlayStation 2",
            shortName = "PS2",
            manufacturer = "Sony",
            generation = 6,
            generationLabel = "6e génération (1998-2005)",
            type = "Salon",
            releaseYear = 2000,
            defaultFormat = "Disque DVD"
        ),
        ConsolePlatform(
            id = "game_boy_advance",
            name = "Game Boy Advance",
            shortName = "GBA",
            manufacturer = "Nintendo",
            generation = 6,
            generationLabel = "6e génération (1998-2005)",
            type = "Portable",
            releaseYear = 2001,
            defaultFormat = "Cartouche GBA"
        ),
        ConsolePlatform(
            id = "gamepark_gp32",
            name = "GamePark GP32",
            shortName = "GP32",
            manufacturer = "GamePark",
            generation = 6,
            generationLabel = "6e génération (1998-2005)",
            type = "Portable",
            releaseYear = 2001,
            defaultFormat = "Carte SmartMedia"
        ),
        ConsolePlatform(
            id = "nintendo_gamecube",
            name = "Nintendo GameCube",
            shortName = "GameCube",
            manufacturer = "Nintendo",
            generation = 6,
            generationLabel = "6e génération (1998-2005)",
            type = "Salon",
            releaseYear = 2001,
            defaultFormat = "Disque optique propriétaire (Mini-DVD)"
        ),
        ConsolePlatform(
            id = "xbox",
            name = "Xbox",
            shortName = "Xbox",
            manufacturer = "Microsoft",
            generation = 6,
            generationLabel = "6e génération (1998-2005)",
            type = "Salon",
            releaseYear = 2001,
            defaultFormat = "Disque DVD"
        ),
        ConsolePlatform(
            id = "bandai_swancrystal",
            name = "SwanCrystal",
            shortName = "SwanCrystal",
            manufacturer = "Bandai",
            generation = 6,
            generationLabel = "6e génération (1998-2005)",
            type = "Portable",
            releaseYear = 2002,
            defaultFormat = "Cartouche"
        ),
        ConsolePlatform(
            id = "nokia_n_gage",
            name = "Nokia N-Gage",
            shortName = "N-Gage",
            manufacturer = "Nokia",
            generation = 6,
            generationLabel = "6e génération (1998-2005)",
            type = "Portable",
            releaseYear = 2003,
            defaultFormat = "Carte MMC"
        ),
        ConsolePlatform(
            id = "tapwave_zodiac",
            name = "Tapwave Zodiac",
            shortName = "Zodiac",
            manufacturer = "Tapwave",
            generation = 6,
            generationLabel = "6e génération (1998-2005)",
            type = "Portable",
            releaseYear = 2003,
            defaultFormat = "Carte SD"
        ),

        // ==================== 7E GÉNÉRATION (2004-2012 - ÈRE HD) ====================
        ConsolePlatform(
            id = "nintendo_ds",
            name = "Nintendo DS",
            shortName = "DS",
            manufacturer = "Nintendo",
            generation = 7,
            generationLabel = "7e génération (2004-2012)",
            type = "Portable",
            releaseYear = 2004,
            defaultFormat = "Carte DS"
        ),
        ConsolePlatform(
            id = "playstation_portable",
            name = "PlayStation Portable",
            shortName = "PSP",
            manufacturer = "Sony",
            generation = 7,
            generationLabel = "7e génération (2004-2012)",
            type = "Portable",
            releaseYear = 2004,
            defaultFormat = "Disque UMD"
        ),
        ConsolePlatform(
            id = "gizmondo",
            name = "Gizmondo",
            shortName = "Gizmondo",
            manufacturer = "Tiger Telematics",
            generation = 7,
            generationLabel = "7e génération (2004-2012)",
            type = "Portable",
            releaseYear = 2005,
            defaultFormat = "Carte SD"
        ),
        ConsolePlatform(
            id = "gamepark_gp2x",
            name = "GamePark GP2X",
            shortName = "GP2X",
            manufacturer = "GamePark Holdings",
            generation = 7,
            generationLabel = "7e génération (2004-2012)",
            type = "Portable",
            releaseYear = 2005,
            defaultFormat = "Carte SD"
        ),
        ConsolePlatform(
            id = "xbox_360",
            name = "Xbox 360",
            shortName = "X360",
            manufacturer = "Microsoft",
            generation = 7,
            generationLabel = "7e génération (2004-2012)",
            type = "Salon",
            releaseYear = 2005,
            defaultFormat = "Disque DVD-DL"
        ),
        ConsolePlatform(
            id = "playstation_3",
            name = "PlayStation 3",
            shortName = "PS3",
            manufacturer = "Sony",
            generation = 7,
            generationLabel = "7e génération (2004-2012)",
            type = "Salon",
            releaseYear = 2006,
            defaultFormat = "Disque Blu-ray"
        ),
        ConsolePlatform(
            id = "nintendo_wii",
            name = "Nintendo Wii",
            shortName = "Wii",
            manufacturer = "Nintendo",
            generation = 7,
            generationLabel = "7e génération (2004-2012)",
            type = "Salon",
            releaseYear = 2006,
            defaultFormat = "Disque optique Wii"
        ),
        ConsolePlatform(
            id = "nintendo_dsi",
            name = "Nintendo DSi",
            shortName = "DSi",
            manufacturer = "Nintendo",
            generation = 7,
            generationLabel = "7e génération (2004-2012)",
            type = "Portable",
            releaseYear = 2008,
            defaultFormat = "Carte DSi / Dématérialisé"
        ),
        ConsolePlatform(
            id = "zeebo",
            name = "Zeebo",
            shortName = "Zeebo",
            manufacturer = "Tectoy / Qualcomm",
            generation = 7,
            generationLabel = "7e génération (2004-2012)",
            type = "Salon",
            releaseYear = 2009,
            defaultFormat = "Dématérialisé (3G)"
        ),
        ConsolePlatform(
            id = "dingoo_a320",
            name = "Dingoo A320",
            shortName = "Dingoo",
            manufacturer = "Dingoo Digital",
            generation = 7,
            generationLabel = "7e génération (2004-2012)",
            type = "Portable",
            releaseYear = 2009,
            defaultFormat = "Mémoire Flash"
        ),

        // ==================== 8E GÉNÉRATION (2011-2020) ====================
        ConsolePlatform(
            id = "nintendo_3ds",
            name = "Nintendo 3DS",
            shortName = "3DS",
            manufacturer = "Nintendo",
            generation = 8,
            generationLabel = "8e génération (2011-2020)",
            type = "Portable",
            releaseYear = 2011,
            defaultFormat = "Carte 3DS"
        ),
        ConsolePlatform(
            id = "playstation_vita",
            name = "PlayStation Vita",
            shortName = "PS Vita",
            manufacturer = "Sony",
            generation = 8,
            generationLabel = "8e génération (2011-2020)",
            type = "Portable",
            releaseYear = 2011,
            defaultFormat = "Carte PlayStation Vita"
        ),
        ConsolePlatform(
            id = "nintendo_wii_u",
            name = "Nintendo Wii U",
            shortName = "Wii U",
            manufacturer = "Nintendo",
            generation = 8,
            generationLabel = "8e génération (2011-2020)",
            type = "Salon",
            releaseYear = 2012,
            defaultFormat = "Disque optique Wii U"
        ),
        ConsolePlatform(
            id = "ouya",
            name = "Ouya",
            shortName = "Ouya",
            manufacturer = "Ouya Inc.",
            generation = 8,
            generationLabel = "8e génération (2011-2020)",
            type = "Micro-console",
            releaseYear = 2013,
            defaultFormat = "Dématérialisé"
        ),
        ConsolePlatform(
            id = "playstation_tv",
            name = "PlayStation TV",
            shortName = "PS TV",
            manufacturer = "Sony",
            generation = 8,
            generationLabel = "8e génération (2011-2020)",
            type = "Micro-console",
            releaseYear = 2013,
            defaultFormat = "Carte PS Vita / Dématérialisé"
        ),
        ConsolePlatform(
            id = "playstation_4",
            name = "PlayStation 4",
            shortName = "PS4",
            manufacturer = "Sony",
            generation = 8,
            generationLabel = "8e génération (2011-2020)",
            type = "Salon",
            releaseYear = 2013,
            defaultFormat = "Disque Blu-ray"
        ),
        ConsolePlatform(
            id = "xbox_one",
            name = "Xbox One",
            shortName = "Xbox One",
            manufacturer = "Microsoft",
            generation = 8,
            generationLabel = "8e génération (2011-2020)",
            type = "Salon",
            releaseYear = 2013,
            defaultFormat = "Disque Blu-ray"
        ),
        ConsolePlatform(
            id = "nvidia_shield_tv",
            name = "Nvidia Shield TV",
            shortName = "Shield",
            manufacturer = "Nvidia",
            generation = 8,
            generationLabel = "8e génération (2011-2020)",
            type = "Micro-console",
            releaseYear = 2015,
            defaultFormat = "Dématérialisé / Cloud"
        ),
        ConsolePlatform(
            id = "nintendo_switch",
            name = "Nintendo Switch",
            shortName = "Switch",
            manufacturer = "Nintendo",
            generation = 8,
            generationLabel = "8e génération (2011-2020)",
            type = "Hybride",
            releaseYear = 2017,
            defaultFormat = "Carte de jeu Switch"
        ),
        ConsolePlatform(
            id = "nintendo_switch_lite",
            name = "Nintendo Switch Lite",
            shortName = "Switch Lite",
            manufacturer = "Nintendo",
            generation = 8,
            generationLabel = "8e génération (2011-2020)",
            type = "Portable",
            releaseYear = 2019,
            defaultFormat = "Carte de jeu Switch"
        ),

        // ==================== 9E GÉNÉRATION (DEPUIS 2020) ====================
        ConsolePlatform(
            id = "playstation_5",
            name = "PlayStation 5",
            shortName = "PS5",
            manufacturer = "Sony",
            generation = 9,
            generationLabel = "9e génération (depuis 2020)",
            type = "Salon",
            releaseYear = 2020,
            defaultFormat = "Disque Ultra HD Blu-ray"
        ),
        ConsolePlatform(
            id = "xbox_series_x",
            name = "Xbox Series X",
            shortName = "Series X",
            manufacturer = "Microsoft",
            generation = 9,
            generationLabel = "9e génération (depuis 2020)",
            type = "Salon",
            releaseYear = 2020,
            defaultFormat = "Disque Ultra HD Blu-ray"
        ),
        ConsolePlatform(
            id = "xbox_series_s",
            name = "Xbox Series S",
            shortName = "Series S",
            manufacturer = "Microsoft",
            generation = 9,
            generationLabel = "9e génération (depuis 2020)",
            type = "Salon",
            releaseYear = 2020,
            defaultFormat = "Dématérialisé"
        ),
        ConsolePlatform(
            id = "blaze_evercade",
            name = "Evercade",
            shortName = "Evercade",
            manufacturer = "Blaze Entertainment",
            generation = 9,
            generationLabel = "9e génération (depuis 2020)",
            type = "Portable",
            releaseYear = 2020,
            defaultFormat = "Cartouche Evercade"
        ),
        ConsolePlatform(
            id = "atari_vcs",
            name = "Atari VCS",
            shortName = "VCS (2021)",
            manufacturer = "Atari",
            generation = 9,
            generationLabel = "9e génération (depuis 2020)",
            type = "Salon",
            releaseYear = 2021,
            defaultFormat = "Dématérialisé"
        ),
        ConsolePlatform(
            id = "analogue_pocket",
            name = "Analogue Pocket",
            shortName = "Analogue Pocket",
            manufacturer = "Analogue",
            generation = 9,
            generationLabel = "9e génération (depuis 2020)",
            type = "Portable",
            releaseYear = 2021,
            defaultFormat = "Cartouche originale (GB/GBC/GBA)"
        ),
        ConsolePlatform(
            id = "nintendo_switch_oled",
            name = "Nintendo Switch (OLED)",
            shortName = "Switch OLED",
            manufacturer = "Nintendo",
            generation = 9,
            generationLabel = "9e génération (depuis 2020)",
            type = "Hybride",
            releaseYear = 2021,
            defaultFormat = "Carte de jeu Switch"
        ),
        ConsolePlatform(
            id = "nintendo_switch_2",
            name = "Nintendo Switch 2",
            shortName = "Switch 2",
            manufacturer = "Nintendo",
            generation = 9,
            generationLabel = "9e génération (depuis 2020)",
            type = "Hybride",
            releaseYear = 2025,
            defaultFormat = "Carte de jeu Switch 2"
        ),
        ConsolePlatform(
            id = "valve_steam_deck",
            name = "Steam Deck",
            shortName = "Steam Deck",
            manufacturer = "Valve",
            generation = 9,
            generationLabel = "9e génération (depuis 2020)",
            type = "Portable",
            releaseYear = 2022,
            defaultFormat = "Dématérialisé (Steam)"
        ),
        ConsolePlatform(
            id = "panic_playdate",
            name = "Playdate",
            shortName = "Playdate",
            manufacturer = "Panic",
            generation = 9,
            generationLabel = "9e génération (depuis 2020)",
            type = "Portable",
            releaseYear = 2022,
            defaultFormat = "Dématérialisé (Saisons)"
        ),
        ConsolePlatform(
            id = "asus_rog_ally",
            name = "Asus ROG Ally",
            shortName = "ROG Ally",
            manufacturer = "Asus",
            generation = 9,
            generationLabel = "9e génération (depuis 2020)",
            type = "Portable",
            releaseYear = 2023,
            defaultFormat = "Dématérialisé (Windows/Steam/Game Pass)"
        ),
        ConsolePlatform(
            id = "lenovo_legion_go",
            name = "Lenovo Legion Go",
            shortName = "Legion Go",
            manufacturer = "Lenovo",
            generation = 9,
            generationLabel = "9e génération (depuis 2020)",
            type = "Portable",
            releaseYear = 2023,
            defaultFormat = "Dématérialisé (Windows/Steam)"
        ),

        // ==================== CLASSIQUES / PC / ARCADE / AUTRES ====================
        ConsolePlatform(
            id = "pc_windows",
            name = "PC",
            shortName = "PC",
            manufacturer = "Microsoft / IBM",
            generation = 0,
            generationLabel = "PC & Rétro-informatique",
            type = "PC",
            releaseYear = 1981,
            defaultFormat = "Disquette / Disque CD-ROM / Numérique"
        ),
        ConsolePlatform(
            id = "arcade",
            name = "Arcade",
            shortName = "Arcade",
            manufacturer = "Divers",
            generation = 0,
            generationLabel = "Bornes d'arcade & MVS",
            type = "Arcade",
            releaseYear = 1971,
            defaultFormat = "PCB / Cartouche MVS"
        ),
        ConsolePlatform(
            id = "amiga",
            name = "Commodore Amiga",
            shortName = "Amiga",
            manufacturer = "Commodore",
            generation = 0,
            generationLabel = "Micro-ordinateur",
            type = "Micro-ordinateur",
            releaseYear = 1985,
            defaultFormat = "Disquette 3.5\""
        ),
        ConsolePlatform(
            id = "atari_st",
            name = "Atari ST",
            shortName = "Atari ST",
            manufacturer = "Atari",
            generation = 0,
            generationLabel = "Micro-ordinateur",
            type = "Micro-ordinateur",
            releaseYear = 1985,
            defaultFormat = "Disquette 3.5\""
        )
    )

    /**
     * Complete list of all console names alphabetically sorted.
     */
    val allPlatformNames: List<String> = allConsoles.map { it.name }.distinct().sorted()

    /**
     * Complete list of all console names with "Toutes" at index 0 for filter bars.
     */
    val allPlatformNamesWithAll: List<String> = listOf("Toutes") + allPlatformNames

    /**
     * Top popular / most collected platforms for quick filter chips.
     */
    val popularPlatforms: List<String> = listOf(
        "Nintendo Switch",
        "Nintendo Switch 2",
        "PlayStation 5",
        "PlayStation 4",
        "PlayStation 2",
        "PlayStation",
        "Nintendo 64",
        "Super Nintendo",
        "NES",
        "Game Boy",
        "Game Boy Advance",
        "Nintendo GameCube",
        "Nintendo DS",
        "Nintendo 3DS",
        "Sega Mega Drive",
        "Sega Saturn",
        "Sega Dreamcast",
        "Xbox",
        "Xbox 360",
        "Xbox Series X",
        "Steam Deck",
        "Atari 2600",
        "PC-Engine / TurboGrafx-16",
        "Neo-Geo AES",
        "Vectrex",
        "PC"
    )

    /**
     * List of unique manufacturers in the catalog.
     */
    val manufacturers: List<String> = listOf(
        "Tous",
        "Nintendo",
        "Sony",
        "Sega",
        "Microsoft",
        "Atari",
        "SNK",
        "NEC",
        "Commodore",
        "Magnavox / Philips",
        "Coleco",
        "Bandai",
        "Autres"
    )

    /**
     * Generation filters for browsing.
     */
    val generationFilters: List<String> = listOf(
        "Toutes",
        "1re gén (1972-80)",
        "2e gén (1976-83)",
        "3e gén 8-bits",
        "4e gén 16-bits",
        "5e gén 32/64-bits",
        "6e gén 128-bits",
        "7e gén HD",
        "8e gén",
        "9e gén Actuelle"
    )

    fun findByName(name: String): ConsolePlatform? {
        return allConsoles.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: allConsoles.firstOrNull { it.shortName.equals(name, ignoreCase = true) }
            ?: allConsoles.firstOrNull { it.name.contains(name, ignoreCase = true) }
    }

    fun search(query: String, manufacturer: String = "Tous", generationFilter: String = "Toutes"): List<ConsolePlatform> {
        val q = query.trim().lowercase()
        return allConsoles.filter { console ->
            val matchQuery = q.isEmpty() ||
                    console.name.lowercase().contains(q) ||
                    console.shortName.lowercase().contains(q) ||
                    console.manufacturer.lowercase().contains(q) ||
                    console.type.lowercase().contains(q) ||
                    console.releaseYear.toString().contains(q)

            val matchManufacturer = when (manufacturer) {
                "Tous" -> true
                "Autres" -> !listOf("Nintendo", "Sony", "Sega", "Microsoft", "Atari", "SNK", "NEC", "Commodore", "Magnavox", "Philips", "Coleco", "Bandai")
                    .any { console.manufacturer.contains(it, ignoreCase = true) }
                else -> console.manufacturer.contains(manufacturer, ignoreCase = true)
            }

            val matchGeneration = when (generationFilter) {
                "Toutes" -> true
                "1re gén (1972-80)" -> console.generation == 1
                "2e gén (1976-83)" -> console.generation == 2
                "3e gén 8-bits" -> console.generation == 3
                "4e gén 16-bits" -> console.generation == 4
                "5e gén 32/64-bits" -> console.generation == 5
                "6e gén 128-bits" -> console.generation == 6
                "7e gén HD" -> console.generation == 7
                "8e gén" -> console.generation == 8
                "9e gén Actuelle" -> console.generation == 9
                else -> true
            }

            matchQuery && matchManufacturer && matchGeneration
        }
    }
}
