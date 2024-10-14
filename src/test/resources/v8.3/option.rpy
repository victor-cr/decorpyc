define config.name = _("Ren'Py Decompiler Test Suite")
define gui.show_name = True
define config.version = "8.3"
define build.name = "decorpyc"
define build.destination = "archive"
define config.has_sound = True
define config.has_music = True
define config.has_voice = True
define config.enter_transition = dissolve
define config.exit_transition = dissolve
define config.after_load_transition = None
define config.end_game_transition = None
define config.window = "auto"
define config.window_show_transition = Dissolve(.2)
define config.window_hide_transition = Dissolve(.2)
default preferences.text_cps = 0
default preferences.afm_time = 15
define config.save_directory = "decorpyc"

init python:
    ## Ignore files

    build.classify("**~", None)
    build.classify("**.bak", None)
    build.classify("**/.**", None)
    build.classify("**/#**", None)
    build.classify("**/thumbs.db", None)

    ## To archive files, classify them as 'archive'.

    build.classify("**/*.rpy", "archive")
    build.classify("**/*.rpyc", "archive")

    ## Files matching documentation patterns are duplicated in a mac app build,
    ## so they appear in both the app and the zip file.

    build.documentation("*.html")
    build.documentation("*.txt")
