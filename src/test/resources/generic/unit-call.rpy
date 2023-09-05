label start:

    e "First, we will call a subroutine."

    call subroutine

    call subroutine(2)

    call expression "sub" + "routine" pass (count=3)

    return


label subroutine(count=1):

    e "I came here [count] time(s)."
    e "Next, we will return from the subroutine."

    return