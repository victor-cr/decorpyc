label start_jump:

    e "First, we will call a subroutine."

    jump jump_subroutine

    jump expression "jump_" + "sub" + "routine"

    return


label jump_subroutine:

    e "I came here [count] time(s)."
    e "Next, we will return from the subroutine."

    return