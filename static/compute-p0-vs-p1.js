const fs = require("fs");
const dynAll = JSON.parse(fs.readFileSync("top30-dyn-perms-all.json").toString())
const staticPerPkg = JSON.parse(fs.readFileSync("top200-20deps-static-perms-installed.json").toString())

var keys = Object.keys(dynAll);
var sumA = 0, sumB = 0;
var res = "";
for (var i = 0; i < keys.length; i++) {
	sumA += Object.keys(dynAll[keys[i]]).length;		
	var avg = average(staticPerPkg[keys[i]]);
	sumB += avg;
	if (i < 20)
		res += keys[i] + "," + Object.keys(dynAll[keys[i]]).length + "," + avg + "\n";
	console.log(keys[i] + "," + Object.keys(dynAll[keys[i]]).length + "," + avg);
}

console.log("Average," + (sumA/keys.length) + "," + (sumB/keys.length))
//res += "Average," + (sumA/keys.length) + "," + (sumB/keys.length) + "\n";
fs.writeFileSync("./figs/privilege-reduction.csv", res);
//console.log(Object.keys(dynAll).length + " vs. " + Object.keys(staticPerPkg).length);

function average(entry) {
	if (!entry)
		return 0;
	var keys = Object.keys(entry);
	var sum = 0;
	for (var i = 0; i < keys.length; i++)
		sum += Object.keys(entry[keys[i]]).length
	return sum / keys.length;
}
