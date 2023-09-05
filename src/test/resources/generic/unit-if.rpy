label conditions:
    if flag:
        e "You've set the flag!"

    if points >= 10:
        jump best_ending
    elif points >= 5:
        jump good_ending
    elif points >= 1:
        jump bad_ending
    else:
        if test != 0:
            jump worst_ending
        else:
            jump no_ending

    if points >= 10:
        "You're doing great!"
    elif points >= 1:
        pass
    else:
        "Things aren't looking so good."
