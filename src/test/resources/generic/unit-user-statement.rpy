play music "mozart.ogg"
play sound "woof.mp3"
play myChannel "punch.wav" # 'myChannel' needs to be defined with renpy.music.register_channel().

"We can also play a list of sounds, or music."
play music [ "a.ogg", "b.ogg" ] fadeout 1.0 fadein 1.0

play sound "woof.mp3" #volume 0.5

play audio "sfx1.opus"
play audio "sfx2.opus"

play music illurock

stop sound
stop music fadeout 1.0

queue sound "woof.mp3"
queue music [ "a.ogg", "b.ogg" ]

play sound "woof.mp3" #volume 0.25
queue sound "woof.mp3" #volume 0.5
queue sound "woof.mp3" #volume 0.75
queue sound "woof.mp3" #volume 1.0

define audio.woof = "woof.mp3"

# ...

play sound woof

play music "<from 5 to 15.5>waves.opus"
play music "<loop 6.333>song.opus"

play music_2 [ "<sync music_1>layer_2.opus", "layer_2.opus" ]

play audio [ "<silence .5>", "boom.opus" ]

define audio.sunflower = "music/sun-flower-slow-jam.ogg"
play music sunflower

play music "mytrack.opus"
$ renpy.music.pump()
stop music fadeout 4