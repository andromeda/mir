// a simple math library
module.exports = {
  add: (a, b) => {
    console.log('one');  
    return a + b;
  },
  sub: (a, b) => {
    global.timesAddCalled= 1;
    console.log('two');
    return a - b;
  },
  constants: {
    pi: 3.14,
    e: 2.71
  },
  fft: {
    add: (a, b) => {
      console.log(global.timesAddCalled);
      return a + b;
    },
    mul: (a, b) => {
      Math.PI;
      return a * b;
    },
    e: 2.71 //added e here
  },
  abc: {  //test
    add: (a,b) => a + b
  }
}
    
