// RangeError
function check(value)
{
    if(["apple", "banana", "carrot"].includes(value) === false)
    {
        throw new RangeError("The argument must be an \"apple\", \"banana\", or \"carrot\".");
    }
}
try
{
    check("cabbage");
}
catch(error)
{
    if(error instanceof RangeError)
    {
        // Handle the error.
    }
}

// ReferenceError
try {
  var a = undefinedVariable;
} catch (e) {
  e instanceof ReferenceError; // true
};

// SyntaxError
try {
  eval('hoo bar');
} catch (e) {
  e instanceof SyntaxError; // true
};

// TypeError
try {
  null.f();
} catch (e) {
  e instanceof TypeError; // true
}

// URIError
try {
  decodeURIComponent('%');
} catch (e) {
  e instanceof URIError; // true
}

// RegExp
var regex1 = /\w+/;
var regex2 = new RegExp('\\w+');

// WeakMap - WeakSet
const wm1 = new WeakMap();
var ws = new WeakSet();

require('./m4.js')