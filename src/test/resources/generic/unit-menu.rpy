default menuset = set()

menu chapter_1_places:

    set menuset
    "Where should I go?"

    "Go to class.":
        jump go_to_class

    "Go to the bar.":
        jump go_to_bar

    "Go to jail.":
        jump go_to_jail

label menus:
    menu:

        "Drink coffee.":
            "I drink the coffee, and it's good to the last drop."

        "Drink tea.":
            $ drank_tea = True

            "I drink the tea, trying not to make a political statement as I do."

        "Genuflect.":
            "What should I do?"
            menu:
                "Go left.":
                    pass
                "Go right.":
                    pass
                "Fly above." if drank_tea:
                    pass

            jump genuflect_ending

    menu ("jfk", screen="airport"):

        "Chicago, IL" (200):
            jump chicago_trip

        "Dallas, TX" (150, sale=True):
            jump dallas_trip

        "Hot Springs, AR" (300) if secret_unlocked:
            jump hot_springs_trip

label after_menu:

    "After having my drink, I got on with my morning."