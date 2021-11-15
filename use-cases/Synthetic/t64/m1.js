var _;
_ = DataView.length;
_ = DataView.name;
_ = DataView.prototype;
DataView.length = {};
DataView.name = {};
DataView.prototype = {};
new DataView(new ArrayBuffer('hello'));
