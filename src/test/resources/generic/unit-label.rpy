label top:

    "Test message 1"
    label sub:
        "Test message 2"

label single_param(a):
    "Test message 3"

label single_default_param(a="default value"):
    "Test message 4: [a]"

label complex_param(a, b="default value", c=None):
    "Test message 5: [a] [b] [c]"
