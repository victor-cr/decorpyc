label show_hide:
    show screen a
    hide screen a

    show window
    hide window

    hide H CoCa001 with Fade(.3,0,0)
    show H CoCa002 with Fade(0,0,.3)

    hide H CoCa002 with Fade(.3,0,0)
    show H CoCa003 with flash01

    hide al 014 with diss04

    show logo base:
        # Show the logo at the upper right side of the screen.
        xalign 1.0 yalign 0.0

        # Take 1.0 seconds to move things back to the left.
        linear 1.0 xalign 0.0

        # Take 1.0 seconds to move things to the location specified in the
        # truecenter transform. Use the ease warper to do this.
        ease 1.0 truecenter

        # Just pause for a second.
        pause 1.0

        # Set the location to circle around.
        alignaround (.5, .5)

        # Use circular motion to bring us to spiral out to the top of
        # the screen. Take 2 seconds to do so.
        linear 2.0 yalign 0.0 clockwise circles 3

        # Use a spline motion to move us around the screen.
        linear 2.0 align (0.0, .33) knot (1.0, .66) knot (0.5, 1.0)

        # Changes xalign and yalign at thje same time.
        linear 2.0 xalign 1.0 yalign 1.0

        ## The same thing, using a block.
        ## Not supported in v7.3
        # linear 2.0:
        #     xalign 1.0
        #     yalign 1.0

    show logo base:
        alpha 0.0 xalign 0.0 yalign 0.0
        linear 1.0 alpha 1.0

        block:
            linear 1.0 xalign 1.0
            linear 1.0 xalign 0.0
            repeat

    show logo base:
        parallel:
            xalign 0.0
            linear 1.3 xalign 1.0
            linear 1.3 xalign 0.0
            repeat
        parallel:
            yalign 0.0
            linear 1.6 yalign 1.0
            linear 1.6 yalign 0.0
            repeat

    show logo base:
        function slide_function
        pause 1.0
        repeat

    show logo base:
        on show:
            alpha 0.0
            linear .5 alpha 1.0
        on hide:
            linear .5 alpha 0.0
