label call_start:

    e "First, we will call a subroutine."

    call call_subroutine

    call call_subroutine(2)

    call expression "call_sub" + "routine" pass (count=3)

    return


label call_subroutine(count=1):

    e "I came here [count] time(s)."
    e "Next, we will return from the subroutine."

    return