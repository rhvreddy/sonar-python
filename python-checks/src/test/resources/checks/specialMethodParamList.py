class Foo(object):
    def __mul__(self, other, unexpected): # Noncompliant {{Remove 1 parameters. Method __mul__ should have 2 parameters.}}
       #^^^^^^^              ^^^^^^^^^^<
        return 42

    def __add__(self): # Noncompliant {{Add 1 parameters. Method __add__ should have 2 parameters.}}
       #^^^^^^^
        return 42

    def __sub__(self, other):
        return 42

    def __lt__(): # Noncompliant {{Add 2 parameters. Method __lt__ should have 2 parameters.}}
       #^^^^^^
        return True

    def __eq__((self, other)): # Noncompliant
        pass

    def __ne__(self, other, foo, bar): # Noncompliant {{Remove 2 parameters. Method __ne__ should have 2 parameters.}}
       #^^^^^^              ^^^< ^^^<
        return 42

    def __mul__(self, other, unexpected): # Noncompliant {{Remove 1 parameters. Method __mul__ should have 2 parameters.}}
       #^^^^^^^              ^^^^^^^^^^<
        return 42

    def __not_magic(self):
        pass

    def still_not_magic__(self):
        pass

    def __init__(self, x, y, z):
        pass

    def __new__(self, a, b, c):
        pass

    def __exit__(self, *args):
        pass

    # Check if we raise correctly with a line break in the def
    def __gt__(self, a, b, c, # Noncompliant
               d):
        pass

def bar():
    pass

import zope

class MyInterface(zope.interface.Interface):
    # Do not raise for zope interfaces
    def __iter__():
        pass

class EdgeCase:
    def __eq__(self, other, x): # FN
        pass

EdgeCase = 42
