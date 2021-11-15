let start = new Date();
for (var i = 0; i < 100000000; i++) {
  if ((new Date() - start) > 1000) {
    break;
  }
	let y=0;
};

console.log('m2: 1 second');
require('./m3.js');
require('./m4.js');
