label loops:
    while True:

        while event.step():
            pass

        if test != 0:
            jump worst_ending
        else:
            jump no_ending

    $ count = 10

    while count > 0:

        "T-minus [count]."

        $ count -= 1

    "Liftoff!"

    $ lines = ["sounds/three.mp3", "sounds/two.mp3", "sounds/one.mp3"]
    while lines: # evaluates to True as long as the list is not empty
        play sound lines.pop(0) # removes the first element
        pause

    while True:

        "This is the song that never terminates."
        "It goes on and on, my compatriots."