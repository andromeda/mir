let env;
let groundTruth;

// We need to get the path of the main module in order to find dynamic json
const getAnalysisData = () => {
  // We save all the json data inside an object
  const path = require('path');
  // TODO: Take this from env---relative to the file calling require('lya')!
  const appDir = path.join(path.dirname(require.main.filename), 'dynamic.json');
  let dynamicData;
  try {
    dynamicData = require(appDir);
  } catch (e) {
    throw new Error('The dynamic.json file was not found!');
  }
  return dynamicData;
};

// TODO: Make the path of the imported analysis result  not absolute
// etc.. /greg/home/lya/tst/main.js ~> main.js
const checkRWX = (storedCalls, truename, modeGrid) => {
  for (const key in modeGrid) {
    if (Object.prototype.hasOwnProperty.call(modeGrid, key)) {
      const mode = modeGrid[key];
      if (Object.prototype.hasOwnProperty.
          call(storedCalls, truename) === false) {
        throw new Error('Invalid access in: ' + truename + ' and mode ' + mode);
      } else {
        const permissions = storedCalls[truename];
        if (!permissions.includes(mode)) {
          throw new Error('Invalid access in: ' + truename +
            ' and mode ' + mode);
        }
      }
    }
  }
};

// Analyses provided by LYA.
// onRead <~ is called before every object is read
const onRead = (info) => {
  if (info.nameToStore != 'global') {
    const pattern = /require[(](.*)[)]/;
    if (pattern.test(info.nameToStore)) {
      checkRWX(groundTruth[info.currentModule],
          info.nameToStore.match(pattern)[0], ['r']);
    } else {
      checkRWX(groundTruth[info.currentModule],
          info.nameToStore.split('.')[0], ['r']);
    }
    checkRWX(groundTruth[info.currentModule],
        info.nameToStore, ['r']);
  }
};

// onWrite <~ is called before every write of an object
const onWrite = (info) => {
  checkRWX(groundTruth[info.currentModule], info.parentName, ['r']);
  checkRWX(groundTruth[info.currentModule], info.nameToStore, ['w']);
};

// onCallPre <~ is called before the execution of a function
const onCallPre = (info) => {
  if (info.typeClass === 'module-locals') {
    checkRWX(groundTruth[info.currentModule],
        'require', ['r', 'x']);
    checkRWX(groundTruth[info.currentModule],
        info.nameToStore, ['i']);
  } else {
    if (info.typeClass === 'node-globals') {
      checkRWX(groundTruth[info.declareModule],
          info.nameToStore.split('.')[0], ['r']);
    }
    checkRWX(groundTruth[info.declareModule],
        info.nameToStore, ['r', 'x']);
  }
};

module.exports = (e) => {
  env = e;
  groundTruth = env.conf.rules? require(env.conf.rules) : getAnalysisData();
  return {
    onRead: onRead,
    onCallPre: onCallPre,
    onWrite: onWrite,
  };
};
