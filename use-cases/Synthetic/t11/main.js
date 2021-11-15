const m1 = require('./m1.js');

m1.w(); // ~> r
m1.r(); // ~> x
m1.x(); // ~> r

m1.w = m1.r
m1.r = m1.x
m1.x = m1.w


