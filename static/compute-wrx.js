const fs = require("fs");
const dynAll = JSON.parse(fs.readFileSync("top30-dyn-perms-all.json").toString())
const staticPerPkg = JSON.parse(fs.readFileSync("top200-20deps-static-perms-installed.json").toString())

var keys = Object.keys(dynAll);
var sumA = 0, sumB = 0;
var res = "";
for (var i = 0; i < 20; i++) {
	sumA += Object.keys(dynAll[keys[i]]).length;	
	
	if (keys[i] == "react-dom") {
		var ofInterest = 0;
		var reactK = Object.keys(dynAll[keys[i]])
		for (k in reactK)
			if (reactK[k].indexOf(".__SECRET_INTERNALS_DO_NOT_USE_OR_YOU_WILL_BE_FIRED") != -1)
				ofInterest++
		console.log(ofInterest + "/" + reactK.length);
	}
	if (keys[i] == "express") {
		console.log("Exp: "+ Object.keys(dynAll[keys[i]]).length)
		console.log(Object.keys(dynAll[keys[i]]));
	}

	var avgW = average(staticPerPkg[keys[i]],"w");
	var avgR = average(staticPerPkg[keys[i]],"r");
	var avgX = average(staticPerPkg[keys[i]],"x");
	var redF = parseInt(Object.keys(dynAll[keys[i]]).length/average(staticPerPkg[keys[i]]));
	if (redF)
		sumB += redF;
	
	if (i < 20)
		res += keys[i] + "," + avgW + "," + avgR + "," + avgX + "," + + parseInt(Object.keys(dynAll[keys[i]]).length/average(staticPerPkg[keys[i]])) + "\n"; //Object.keys(dynAll[keys[i]]).length
	//console.log(keys[i] + "," + Object.keys(dynAll[keys[i]]).length + "," + avg);
}

console.log("Average," + (sumA/keys.length) + "," + (sumB/keys.length))
//res += "Average," + (sumA/keys.length) + "," + (sumB/keys.length) + "\n";
fs.writeFileSync("./figs/privilege-reduction-new.csv", res);
//console.log(Object.keys(dynAll).length + " vs. " + Object.keys(staticPerPkg).length);

function average(entry, perm) {
	if (!entry)
		return 0;
	var keys = Object.keys(entry);
	var sum = 0;
	for (var i = 0; i < keys.length; i++) {		
		if (perm) {
			var keysP = Object.keys(entry[keys[i]]);
			for (var j = 0; j < keysP.length; j++)
				if (entry[keys[i]][keysP[j]].indexOf(perm) != -1)				
					sum++;
		} else {
			sum += Object.keys(entry[keys[i]]).length
		}
	}
	return sum / keys.length;
}
