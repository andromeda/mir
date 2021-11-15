const fs = require('fs');
const m1 = require('./m1.js');

var logger = fs.createWriteStream('timetest.txt', {
  flags: 'a' // 'a' means appending (old data will be preserved)
})
