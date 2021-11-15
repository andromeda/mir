constructor.length = {};
constructor.name = {};
constructor.prototype = {};
constructor.assign = {};
constructor.getOwnPropertyDescriptor = {};
constructor.getOwnPropertyDescriptors = {};
constructor.getOwnPropertyNames = {};
constructor.getOwnPropertySymbols = {};
constructor.is = {};
constructor.preventExtensions = {};
constructor.seal = {};
constructor.create = {};
constructor.defineProperties = {};
constructor.defineProperty = {};
constructor.freeze = {};
constructor.getPrototypeOf = {};
constructor.setPrototypeOf = {};
constructor.isExtensible = {};
constructor.isFrozen = {};
constructor.isSealed = {};

// This breaks the test with lya
// because every time we try to use Object.keys(source)
// in txfm now is empty. If we want it to fail uncomment the
// following line
// constructor.keys = {};

constructor.entries = {};
constructor.values = {};
