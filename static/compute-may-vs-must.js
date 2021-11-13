const fs = require("fs");
const mayRes = JSON.parse(fs.readFileSync("policy-may-192-packages.txt").toString())
const mustRes = JSON.parse(fs.readFileSync("policy-must-192-packages.txt").toString())

console.log(Object.keys(mayRes).length + " vs. " + Object.keys(mustRes).length);
