// Testing correct overhead attribution from Lya's performance analysis.
// (Massive performance overhead by 'with')

let m1 = require('./m1.js');
global.test = 1;
global.test = 1;
