image eileen happy = "eileen_happy.png"
image black = "#000"
image bg tiled = Tile("tile.jpg")

image eileen happy question = VBox(
    "question.png",
    "eileen_happy.png",
    )

image H ZFi052 = Movie(play="movies/ZFi052.webm", pos=(0,0), anchor=(0,0))
image H ZFi053 = Movie(play="movies/ZFi053.webm", pos=(0,0), anchor=(0,0))
image H ZFi054 = Movie(play="movies/ZFi054.webm", pos=(0,0), anchor=(0,0))

####################################################################################################
###########################          DECLARACION DE ANIMACIONES         ############################
####################################################################################################

image el 001:
    "images/elly/elly_001.webp"
    pause 0.1
    "images/elly/elly_002.webp"
    pause 0.1
    "images/elly/elly_003.webp"
    pause 0.1
    "images/elly/elly_002.webp"
    pause 0.1
    repeat

image presentation:
    "images/alice/alice_138.webp" with diss05
    pause 3.0
    "images/bazzer/bazzer_159.webp" with diss05
    pause 3.0
    "images/elly/elly_090.webp" with diss05
    pause 3.0
    "images/lucile/lucile_047.webp" with diss05
    pause 3.0
    "images/nicole/nicole_070.webp" with diss05
    pause 3.0
    "images/lucile/lucile_024.webp" with diss05
    pause 3.0
    "images/alice/alice_230.webp" with diss05
    pause 3.0
    "images/bazzer/bazzer_259.webp" with diss05
    pause 3.0
    "images/elly/elly_313.webp" with diss05
    pause 3.0
    "images/lucile/lucile_420.webp" with diss05
    pause 3.0
    "images/nicole/nicole_227.webp" with diss05
    pause 3.0
    "images/alice/alice_226.webp" with diss05
    pause 3.0
    "images/elly/elly_388.webp" with diss05
    pause 3.0
    "images/lucile/lucile_080.webp" with diss05
    pause 3.0
    "images/nicole/nicole_265.webp" with diss05
    pause 3.0
    "images/lucile/lucile_603.webp" with diss05
    pause 3.0
    "images/nicole/el_ni_004.webp" with diss05
    pause 3.0

    repeat

image al 027:
    "images/alice/alice_298.webp" with diss25
    pause 0.75
    "images/alice/alice_299.webp" with diss25
    "images/alice/alice_300.webp" with diss25
    pause 1.0
    repeat

image lucile_sepia01:
    im.Sepia("images/lucile/lucile_310.webp")
image lucile_sepia02:
    im.Sepia("images/lucile/lucile_399.webp")
image lucile_sepia03:
    im.Sepia("images/lucile/lucile_336.webp")
