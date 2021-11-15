var _;
var foo = {};

_ = Error.length;
_ = Error.name;
_ = Error.prototype;
_ = Error.captureStackTrace;
_ = Error.stackTraceLimit;
Error();
Error.captureStackTrace(foo);
