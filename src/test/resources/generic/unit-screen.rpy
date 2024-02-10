screen game_menu(title, scroll=None, yinitial=0.0):

    if main_menu:
        add gui.main_menu_background
    else:
        add gui.game_menu_background

    frame:
        style "game_menu_outer_frame"

        hbox:

            ## Reserve space for the navigation section.
            frame:
                style "game_menu_navigation_frame"

            frame:
                style "game_menu_content_frame"

                if scroll == "viewport":

                    viewport:
                        yinitial yinitial
                        scrollbars "vertical"
                        mousewheel True
                        draggable True
                        pagekeys True

                        side_yfill True

                        vbox:
                            transclude

                elif scroll == "vpgrid":

                    vpgrid:
                        cols 1
                        yinitial yinitial

                        scrollbars "vertical"
                        mousewheel True
                        draggable True
                        pagekeys True

                        side_yfill True

                        transclude

                else:

                    transclude

    textbutton _("Return"):
        style "return_button"

        action Return()


screen cast():
    tag menu

    use game_menu(_("Characters"), scroll="viewport"):
        style_prefix "characters"
        # Build your screen here
        frame:
            yminimum 55
            xminimum 720
            vbox:
                hbox:
                    vbox:
                        add "/characters/a.webp"
                    vbox:
                        yalign 0.5
                        text _("Name: [mc_name] \"Crow\" Crauford")
                        text _("Profession: Slave fighter")
                        text _("Affiliations: None")
                        text _("Species: Hybrid")
                        text _("Status: Active")
                        text _("Parents: Unknown")
        frame:
            yminimum 55
            xminimum 720
            hbox:
                vbox:
                    add "/characters/b.webp"
                vbox:
                    yalign 0.5
                    text _("Name: B")
                    text _("Profession: Civilian")
                    text _("Affiliations: Republic")
                    text _("Species: Human")
                    if Alive == False:
                        text _("Status: Deceased")
                    else:
                        text _("Status: Active")
                    text _("Parents: C and A")
