label say_test:

    "This is narration."

    "Eileen" "This is dialogue, with an explicit character name."

    e "This is dialogue, using a character object instead."

    e "Hello, world." (what_color="#8c8")

    "I've walked past a *sign* saying, \"WARNING:\nEscaped characters like {{\\}, {{%%}, or {{[[}, or {{【【}\""

    "Bam!!" with vpunch
    "Bam!!" (what_color="#8c8") with vpunch

    e mad "I'm a little upset at you."

    e happy @ vhappy "But it's just a passing thing."

    e "Still happy, though not very much"

    e -happy "I'm not sure what to think now."

    e @ right -mad "My anger is temporarily suspended..."

    label say_sub(a, b):
        e "[b] HOWEVER ! [a:.2]"
