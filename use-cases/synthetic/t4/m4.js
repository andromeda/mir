// Number.prototype 
// toExponential,toFixed,toPrecision arent printed on Dynamic.json
const testNumber = new Number(123.5555);

testNumber.toExponential(2);
testNumber.toFixed(1);
testNumber.toLocaleString('ar-EG');
testNumber.toPrecision(1);
testNumber.toString();
testNumber.valueOf();
