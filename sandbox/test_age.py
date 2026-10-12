#!/usr/bin/env python3
"""The sandbox's age reckoning (EE-POA-027): whole years from 00:00 of the anniversary,
a 29 February birthday on 28 February in a common year (TsUS 136(4))."""
import datetime as dt
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from server import age_on  # noqa: E402

d = dt.date
assert age_on(d(2008, 5, 10), d(2026, 5, 9)) == 17
assert age_on(d(2008, 5, 10), d(2026, 5, 10)) == 18
assert age_on(d(2008, 2, 29), d(2026, 2, 27)) == 17
assert age_on(d(2008, 2, 29), d(2026, 2, 28)) == 18
assert age_on(d(2008, 2, 29), d(2028, 2, 28)) == 19
assert age_on(d(2008, 2, 29), d(2028, 2, 29)) == 20
print("sandbox age reckoning ok")
